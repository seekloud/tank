package com.neo.sk.tank.core

import java.util.concurrent.atomic.AtomicLong

import akka.actor.PoisonPill
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import com.neo.sk.tank.common.Constants
import com.neo.sk.tank.core.BotActor.{ConnectToUserActor}
import com.neo.sk.tank.core.UserManager.{ChildDead, Command}
import com.neo.sk.tank.core.game.BotControl
import com.neo.sk.tank.models.TankGameUserInfo
import com.neo.sk.tank.shared.protocol.TankGameEvent.WsMsgSource
import org.slf4j.LoggerFactory

object BotManager {

  private var childList = List[ActorRef[WsMsgSource]]()

  trait Command

  final case class CreateABot(length:Int, count:Int) extends Command
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
        case CreateABot(len,count) =>
          for(i <- 1 to count){
            val botName = generateAName(len)
            val botActor = getBotActor(ctx, uidGenerator.getAndIncrement().toString, botName, None)
            childList = childList :+ botActor
            botActor ! ConnectToUserActor(botName, None)
          }
          Behaviors.same

        case DeleteChild =>
          childList.foreach(r => ctx.stop(r))
          childList = List.empty[ActorRef[WsMsgSource]]
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

  private def getBotActor(ctx: ActorContext[Command], id:String, name:String, roomId:Option[Long] = None):ActorRef[WsMsgSource] = {
    val childName = s"BotActor-${id}"
    ctx.child(childName).getOrElse{
      val actor = ctx.spawn(BotActor.create(id, name, roomId), childName)
      ctx.watchWith(actor, ChildDead(childName, actor))
      actor
    }.upcast[WsMsgSource]
  }

  private def generateAName(len:Int):String = {
    var name = ""
    for(i <- 0 to len){
      val ch = (new util.Random).nextInt(26) + 65
      name = name + ch.toChar
    }
    println(s"the name is ${name}")
    name
  }

}
