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
import com.neo.sk.tank.shared.protocol.TankGameEvent.{RestartGame, WsMsgSource}
import org.seekloud.byteobject.ByteObject.bytesDecode
import com.neo.sk.tank.Boot.{executor, scheduler}
import com.neo.sk.tank.core.game.BotControl

import scala.language.postfixOps

object BotActor {


  private val log = LoggerFactory.getLogger(this.getClass)
  private final val InitTime = Some(5.minutes)

  case object ConnectToUserActor extends WsMsgSource
  final case class GetMsgFromUserManager(userActor:ActorRef[UserActor.Command]) extends WsMsgSource

  case class StartUserKeyUp(keyCode:Int) extends WsMsgSource
  case class UserKeyUpTimeOut(keyCode:Int) extends WsMsgSource
  case object UserKeyUpKey

  case object RestartAGame extends WsMsgSource
  case object RestartGameTimeOut extends WsMsgSource
  case object ReStartKey


  case object StartUserAction extends WsMsgSource
  case object UserActionTimeOut extends WsMsgSource
  case object UserActionKey

  case object StartGameLoop extends WsMsgSource
  case object GameLoopTimeOut extends WsMsgSource
  case object StopGameLoop extends WsMsgSource
  case object GameLoopKey


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
        case ConnectToUserActor =>
          log.debug(s"the path is ${ctx.self.path}")
          userManager ! GetMsgFromBot(name, roomId, ctx.self)
          Behaviors.same

        case GetMsgFromUserManager(userActor) =>
          val bc = new BotControl(name, ctx.self)
          switchBehavior(ctx, "play", play(name, roomId, userActor, bc), InitTime, TimeOut("init"))
      }
    }

  def play(name: String,
           roomId: Option[Long],
           userActor: ActorRef[UserActor.Command],
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

        case StartGameLoop =>
          timer.startPeriodicTimer(GameLoopKey, GameLoopTimeOut,100 milliseconds)
          Behaviors.same

        case GameLoopTimeOut =>
          botControl.gameLoop()
          Behaviors.same

        case StopGameLoop =>
          timer.cancel(GameLoopKey)
          timer.cancel(UserActionKey)
          Behaviors.same

        case StartUserAction =>
          timer.startPeriodicTimer(UserActionKey, UserActionTimeOut, 1 seconds)
          Behaviors.same

        case UserActionTimeOut =>
          botControl.sendMsg2Actor(userActor)
          Behaviors.same

        case RestartAGame =>
          timer.startSingleTimer(ReStartKey, RestartGameTimeOut, 3 seconds)
          Behaviors.same

        case RestartGameTimeOut =>
          botControl.reStart(userActor)
          Behaviors.same

        case StartUserKeyUp(keyCode) =>
          timer.startSingleTimer(UserKeyUpKey, UserKeyUpTimeOut(keyCode), 1 seconds)
          Behaviors.same

        case UserKeyUpTimeOut((keyCode)) =>
          botControl.userKeyUp(keyCode, userActor)
          Behaviors.same

        case unknowMsg =>
          stashBuffer.stash(unknowMsg)
          Behavior.same

      }
    }



}
