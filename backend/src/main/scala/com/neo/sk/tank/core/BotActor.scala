package com.neo.sk.tank.core

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.Cancellable
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import org.seekloud.byteobject.MiddleBufferInJvm
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import com.neo.sk.tank.Boot.userManager
import com.neo.sk.tank.core.UserActor.WebSocketMsg
import com.neo.sk.tank.core.UserManager.GetMsgFromBot

import com.neo.sk.tank.shared.protocol.TankGameEvent
import com.neo.sk.tank.shared.protocol.TankGameEvent.WsMsgSource
import org.seekloud.byteobject.ByteObject.bytesDecode
import com.neo.sk.tank.Boot.{executor, scheduler}
import com.neo.sk.tank.core.game.BotControl

import scala.language.postfixOps

object BotActor {


  private val log = LoggerFactory.getLogger(this.getClass)
  private final val InitTime = Some(5.minutes)

  final case class ConnectToUserActor(name: String, roomId: Option[Long] = None) extends WsMsgSource
  final case class StartAGame() extends WsMsgSource
  final case class GetMsgFromUserManager(userActor:ActorRef[UserActor.Command]) extends WsMsgSource

  private final case object BehaviorChangeKey

  final case class SwitchBehavior(
                                   name: String,
                                   behavior: Behavior[WsMsgSource],
                                   durationOpt: Option[FiniteDuration] = None,
                                   timeOut: TimeOut = TimeOut("busy time error")
                                 ) extends WsMsgSource

  case class TimeOut(msg: String) extends WsMsgSource

  private[this] def switchBehavior(ctx: ActorContext[WsMsgSource],
                                   behaviorName: String, behavior: Behavior[WsMsgSource], durationOpt: Option[FiniteDuration] = None, timeOut: TimeOut = TimeOut("busy time error"))
                                  (implicit stashBuffer: StashBuffer[WsMsgSource],
                                   timer: TimerScheduler[WsMsgSource]) = {
    log.debug(s"${ctx.self.path} becomes $behaviorName behavior.")
    timer.cancel(BehaviorChangeKey)
    durationOpt.foreach(timer.startSingleTimer(BehaviorChangeKey, timeOut, _))
    stashBuffer.unstashAll(ctx, behavior)
  }

  def create(uId: String,
             name: String,
             roomId: Option[Long]): Behavior[WsMsgSource] = {
    Behaviors.setup[WsMsgSource] { ctx =>
      log.debug(s"${ctx.self.path} is starting...")
      implicit val stashBuffer = StashBuffer[WsMsgSource](Int.MaxValue)
      Behaviors.withTimers[WsMsgSource] { implicit timer =>
        implicit val sendBuffer = new MiddleBufferInJvm(8192)
        switchBehavior(ctx, "init", init(name, roomId), InitTime, TimeOut("init"))
      }
    }
  }

  def init(name: String,
           roomId: Option[Long])(
    implicit stashBuffer: StashBuffer[WsMsgSource],
    sendBuffer: MiddleBufferInJvm,
    timer: TimerScheduler[WsMsgSource]
  ): Behavior[WsMsgSource] =
    Behaviors.receive[WsMsgSource] { (ctx, msg) =>
      msg match{
        case ConnectToUserActor(name, roomId) =>
          log.debug(s"the path is ${ctx.self.path}")
          userManager ! GetMsgFromBot(name, roomId, ctx.self)
          Behaviors.same

        case GetMsgFromUserManager(userActor) =>
          ctx.self ! StartAGame()
          val bc = new BotControl(name, userActor)
          switchBehavior(ctx, "play", play(name, roomId, bc), InitTime, TimeOut("init"))
      }
    }

  def play(name: String,
           roomId: Option[Long],
           botControl: BotControl)(
    implicit stashBuffer: StashBuffer[WsMsgSource],
    sendBuffer: MiddleBufferInJvm,
    timer: TimerScheduler[WsMsgSource]
  ): Behavior[WsMsgSource] =
    Behaviors.receive[WsMsgSource] { (ctx, msg) =>
      msg match{
        case msg: TankGameEvent.Wrap =>
          val buffer = new MiddleBufferInJvm(msg.ws)
          bytesDecode[TankGameEvent.WsMsgServer](buffer) match {
            case Right(req) =>
              botControl.wsMsgHandler(req)
            case Left(e) =>
              println(s"decode binaryMessage failed,error:${e.message}")
              botControl.wsMsgHandler(TankGameEvent.DecodeError())
          }
          Behaviors.same

        case StartAGame() =>
          scheduler.scheduleOnce(1 seconds){botControl.sendMsg2Actor}
          scheduler.scheduleOnce(1 seconds){botControl.findTarget}
          Behaviors.same

        case unknowMsg =>
          stashBuffer.stash(unknowMsg)
          Behavior.same

      }
    }



}
