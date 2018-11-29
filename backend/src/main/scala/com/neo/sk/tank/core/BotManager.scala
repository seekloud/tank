package com.neo.sk.tank.core

import java.util.concurrent.atomic.AtomicLong

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import com.neo.sk.tank.core.BotActor.{ConnectToUserActor}
import com.neo.sk.tank.shared.protocol.TankGameEvent.WsMsgSource
import org.slf4j.LoggerFactory
import scala.collection.mutable.Map

object BotManager {

  private var childList = Map[Long, ActorRef[WsMsgSource]]()

  trait Command

  final case class CreateABot(count:Int) extends Command
  final case class DeleteChild(botId:Long) extends Command
  case object DeleteChild extends Command
  final case class ChildDead[U](name: String, childRef: ActorRef[U]) extends Command

  private val log = LoggerFactory.getLogger(this.getClass)

  def create(): Behavior[Command] = {
    log.debug(s"BotManager start...")
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
        case CreateABot(count) =>
          for(i <- 1 to count){
            val botId = uidGenerator.getAndIncrement()
            val botActor = getBotActor(ctx, botId.toString, None)
            childList(botId) = botActor
            botActor ! ConnectToUserActor
          }
          Behaviors.same

        case DeleteChild =>
          childList.foreach(r => ctx.stop(r._2))
          childList = Map.empty[Long, ActorRef[WsMsgSource]]
          Behaviors.same

        case DeleteChild(botId) =>
          if(childList.contains(botId)){
            ctx.stop(childList(botId))
            childList -= botId
          }
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

  private def getBotActor(ctx: ActorContext[Command], id:String, roomId:Option[Long] = None):ActorRef[WsMsgSource] = {
    val childName = s"BotActor-${id}"
    val botName = s"tankBot-${id}"
    ctx.child(childName).getOrElse{
      val actor = ctx.spawn(BotActor.create(id, botName, roomId), childName)
      ctx.watchWith(actor, ChildDead(childName, actor))
      actor
    }.upcast[WsMsgSource]
  }

}
