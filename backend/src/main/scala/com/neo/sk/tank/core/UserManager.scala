package com.neo.sk.tank.core

import java.util.concurrent.atomic.AtomicLong

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.stream.{ActorAttributes, Supervision}
import akka.stream.scaladsl.Flow
import akka.util.ByteString
import com.neo.sk.tank.shared.protocol.TankGameEvent
import io.circe.{Decoder, Encoder}
import org.slf4j.LoggerFactory

/**
  * Created by hongruying on 2018/7/9
  */
object UserManager {

  import io.circe.generic.auto._
  import io.circe.syntax._
  import org.seekloud.byteobject.ByteObject._
  import org.seekloud.byteobject.MiddleBufferInJvm

  sealed trait Command

  final case class ChildDead[U](name: String, childRef: ActorRef[U]) extends Command

  final case class GetWebSocketFlow(name: String, replyTo: ActorRef[Flow[Message, Message, Any]]) extends Command

  final case class GetReplaySocketFlow(name: String, uid: Long, rid: Long, f:Int, replyTo: ActorRef[Flow[Message, Message, Any]]) extends Command

//  final case class GetWebSocketFlow(name:String, userId:Long ,roomIdOpt:Option[Long], replyTo:ActorRef[Flow[Message,Message,Any]]) extends Command

  private val log = LoggerFactory.getLogger(this.getClass)

  def create(): Behavior[Command] = {
    log.debug(s"UserManager start...")
    Behaviors.setup[Command] {
      ctx =>
        Behaviors.withTimers[Command] {
          implicit timer =>
            val uidGenerator = new AtomicLong(1L)
            idle(uidGenerator)
        }
    }
  }

  private def idle(uidGenerator: AtomicLong)
                  (
                    implicit timer: TimerScheduler[Command]
                  ): Behavior[Command] = {
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case GetWebSocketFlow(name, replyTo) =>
          replyTo ! getWebSocketFlow(0,getUserActor(ctx, uidGenerator.getAndIncrement(), name))
          Behaviors.same

        case GetReplaySocketFlow(name, uid, rid, f, replyTo) =>
          replyTo ! getWebSocketFlow(1,getUserActor(ctx, uid, name),Some(rid),Some(f))
          Behaviors.same

        case ChildDead(child, childRef) =>
          ctx.unwatch(childRef)
          Behaviors.same

        case unknow =>
          log.error(s"${ctx.self.path} recv a unknow msg when idle:${unknow}")
          Behaviors.same
      }
    }
  }

  private def getWebSocketFlow(flag:Int,userActor: ActorRef[UserActor.Command],rid:Option[Long]=None,f:Option[Int]=None): Flow[Message, Message, Any] = {
    import scala.language.implicitConversions
    import org.seekloud.byteobject.ByteObject._


    implicit def parseJsonString2WsMsgFront(s: String): Option[TankGameEvent.WsMsgFront] = {
      import io.circe.generic.auto._
      import io.circe.parser._

      try {
        val wsMsg = decode[TankGameEvent.WsMsgFront](s).right.get
        Some(wsMsg)
      } catch {
        case e: Exception =>
          log.warn(s"parse front msg failed when json parse,s=${s}")
          None
      }
    }

    Flow[Message]
      .collect {
        case TextMessage.Strict(m) =>
          UserActor.WebSocketMsg(m)

        case BinaryMessage.Strict(m) =>
          val buffer = new MiddleBufferInJvm(m.asByteBuffer)
          bytesDecode[TankGameEvent.WsMsgFront](buffer) match {
            case Right(req) => UserActor.WebSocketMsg(Some(req))
            case Left(e) =>
              log.error(s"decode binaryMessage failed,error:${e.message}")
              UserActor.WebSocketMsg(None)
          }
      }.via(UserActor.flow(flag,userActor,rid,f))
      .map {
        case t: TankGameEvent.Wrap =>
          BinaryMessage.Strict(ByteString(t.ws))

        case t: TankGameEvent.ReplayFrameData =>
          BinaryMessage.Strict(ByteString(t.ws))

        case x =>
          log.debug(s"akka stream receive unknown msg=${x}")
          TextMessage.apply("")
      }.withAttributes(ActorAttributes.supervisionStrategy(decider))

  }

  private val decider: Supervision.Decider = {
    e: Throwable =>
      e.printStackTrace()
      log.error(s"WS stream failed with $e")
      Supervision.Resume
  }


  /**
    * 创建userActor使用鉴权ID*/
  @deprecated
  private def getUserActor(ctx: ActorContext[Command], id: Long, name: String): ActorRef[UserActor.Command] = {
    val childName = s"UserActor-${id}"
    ctx.child(childName).getOrElse {
      val actor = ctx.spawn(UserActor.create(id, name), childName)
      ctx.watchWith(actor, ChildDead(childName, actor))
      actor
    }.upcast[UserActor.Command]
  }

}
