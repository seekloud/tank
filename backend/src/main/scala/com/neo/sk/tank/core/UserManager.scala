package com.neo.sk.tank.core

import java.util.concurrent.atomic.AtomicLong

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.{ActorAttributes, Supervision}
import akka.stream.scaladsl.Flow
import com.neo.sk.tank.shared.ptcl.protocol.WsProtocol
import org.slf4j.LoggerFactory

/**
  * Created by hongruying on 2018/7/9
  */
object UserManager {

  import io.circe.generic.auto._
  import io.circe.syntax._

  sealed trait Command

  final case class ChildDead[U](name:String,childRef:ActorRef[U]) extends Command

  final case class GetWebSocketFlow(name:String,replyTo:ActorRef[Flow[Message,Message,Any]]) extends Command

  private val log = LoggerFactory.getLogger(this.getClass)

  def create():Behavior[Command] ={
    log.debug(s"UserManager start...")
    Behaviors.setup[Command]{
      ctx =>
        Behaviors.withTimers[Command]{
          implicit timer =>
            val uidGenerator = new AtomicLong(1L)
            idle(uidGenerator)
        }
    }
  }

  private def idle(uidGenerator:AtomicLong)
                  (
                    implicit timer:TimerScheduler[Command]
                  ):Behavior[Command] = {
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case GetWebSocketFlow(name,replyTo) =>
          replyTo ! getWebSocketFlow(getUserActor(ctx,uidGenerator.getAndIncrement(),name))
          Behaviors.same


        case ChildDead(child,childRef) =>
          ctx.unwatch(childRef)
          Behaviors.same

        case unknow =>
          log.error(s"${ctx.self.path} recv a unknow msg when idle:${unknow}")
          Behaviors.same
      }
    }
  }

  private def getWebSocketFlow(userActor: ActorRef[UserActor.Command]):Flow[Message,Message,Any] = {

    import scala.language.implicitConversions
    implicit def parseJsonString2WsMsgFront(s:String):Option[WsProtocol.WsMsgFront] = {
      import io.circe.generic.auto._
      import io.circe.parser._

      try {
        val wsMsg = decode[WsProtocol.WsMsgFront](s).right.get
        Some(wsMsg)
      }catch {
        case e:Exception =>
          log.warn(s"parse front msg failed when json parse,s=${s}")
          None
      }
    }

    Flow[Message]
      .collect{
        case TextMessage.Strict(m) =>
          UserActor.WebSocketMsg(m)
      }.via(UserActor.flow(userActor))
        .map {
        case _ => TextMessage.apply("")

      }.withAttributes(ActorAttributes.supervisionStrategy(decider))

  }

  private val decider: Supervision.Decider = {
    e: Throwable =>
      e.printStackTrace()
      log.error(s"WS stream failed with $e")
      Supervision.Resume
  }





  private def getUserActor(ctx: ActorContext[Command],id:Long,name:String):ActorRef[UserActor.Command] = {
    val childName = s"UserActor-${id}"
    ctx.child(childName).getOrElse{
      val actor = ctx.spawn(UserActor.create(id,name),childName)
      ctx.watchWith(actor,ChildDead(childName,actor))
      actor
    }.upcast[UserActor.Command]
  }

}
