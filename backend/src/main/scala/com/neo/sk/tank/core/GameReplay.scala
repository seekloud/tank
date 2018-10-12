package com.neo.sk.tank.core

import java.io.File

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.{ActorContext, StashBuffer, TimerScheduler}
import com.neo.sk.tank.common.AppSettings
import org.slf4j.LoggerFactory

import scala.language.implicitConversions
import scala.concurrent.duration._
import com.neo.sk.utils.ESSFSupport._
/**
  * User: sky
  * Date: 2018/10/12
  * Time: 16:06
  */
object GameReplay {
  private final val log = LoggerFactory.getLogger(this.getClass)

  sealed trait Command
  private final case object BehaviorChangeKey
  final case class SwitchBehavior(
                                   name: String,
                                   behavior: Behavior[Command],
                                   durationOpt: Option[FiniteDuration] = None,
                                   timeOut: TimeOut = TimeOut("busy time error")
                                 ) extends Command

  case class TimeOut(msg:String) extends Command

  private[this] def switchBehavior(ctx: ActorContext[Command],
                                   behaviorName: String, behavior: Behavior[Command], durationOpt: Option[FiniteDuration] = None,timeOut: TimeOut  = TimeOut("busy time error"))
                                  (implicit stashBuffer: StashBuffer[Command],
                                   timer:TimerScheduler[Command]) = {
    log.debug(s"${ctx.self.path} becomes $behaviorName behavior.")
    timer.cancel(BehaviorChangeKey)
    durationOpt.foreach(timer.startSingleTimer(BehaviorChangeKey,timeOut,_))
    stashBuffer.unstashAll(ctx,behavior)
  }

  /**actor内部消息*/
  case class InitFileRead(fileRoute:String) extends Command


  def create(recordId:Long):Behavior[Command] = {
    Behaviors.setup[Command]{ctx=>
      log.info(s"${ctx.self.path} is starting..")
      implicit val stashBuffer = StashBuffer[Command](Int.MaxValue)
      Behaviors.withTimers[Command] { implicit timer =>
        busy()
      }
    }
  }

  def work()(implicit stashBuffer:StashBuffer[Command],
             timer:TimerScheduler[Command]):Behavior[Command]={
    Behaviors.receive[Command]{(ctx,msg)=>
      msg match {
        case msg:InitFileRead=>

          Behaviors.same
        case unKnowMsg =>
          stashBuffer.stash(unKnowMsg)
          Behavior.same
      }
    }
  }

  private def busy()(
    implicit stashBuffer:StashBuffer[Command],
    timer:TimerScheduler[Command]
  ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case SwitchBehavior(name, behavior,durationOpt,timeOut) =>
          switchBehavior(ctx,name,behavior,durationOpt,timeOut)

        case TimeOut(m) =>
          log.debug(s"${ctx.self.path} is time out when busy,msg=${m}")
          Behaviors.stopped

        case unknowMsg =>
          stashBuffer.stash(unknowMsg)
          Behavior.same
      }
    }
}
