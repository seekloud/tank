package com.neo.sk.tank.core.bot

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import com.neo.sk.tank.core.game.{GameContainerServerImpl, TankServerImpl}
import com.neo.sk.tank.shared.protocol.TankGameEvent
import org.seekloud.byteobject.MiddleBufferInJvm
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import com.neo.sk.tank.Boot.{executor, roomManager, scheduler, timeout}
import com.neo.sk.tank.core.{RoomActor,RoomManager}
import com.neo.sk.tank.core.bot.BotManager.{Stopmap,StopBot}
/**
  * Created by sky
  * Date on 2019/1/10
  * Time at 下午8:59
  */
object BotActor {
  object Keymap{
    val move="move"
    val key="key"
    val click="click"
  }

  private val log = LoggerFactory.getLogger(this.getClass)
  private final val InitTime = Some(5.minutes)
  private final val moveTime = 5.seconds
  private final val keyTime = 30.seconds
  private final val clickTime = 30.seconds

  trait Command

  case class JoinRoomSuccess(tank: TankServerImpl,roomActor:ActorRef[RoomActor.Command]) extends Command

  private final case object BehaviorChangeKey
  private final case object MoveTimeKey
  private final case object KeyTimeKey
  private final case object ClickTimeKey


  final case class SwitchBehavior(
                                   name: String,
                                   behavior: Behavior[Command],
                                   durationOpt: Option[FiniteDuration] = None,
                                   timeOut: TimeOut = TimeOut("busy time error")
                                 ) extends Command

  case class TimeOut(msg: String) extends Command

  private[this] def switchBehavior(ctx: ActorContext[Command],
                                   behaviorName: String, behavior: Behavior[Command], durationOpt: Option[FiniteDuration] = None, timeOut: TimeOut = TimeOut("busy time error"))
                                  (implicit stashBuffer: StashBuffer[Command],
                                   timer: TimerScheduler[Command]) = {
    log.debug(s"${ctx.self.path} becomes $behaviorName behavior.")
    timer.cancel(BehaviorChangeKey)
    durationOpt.foreach(timer.startSingleTimer(BehaviorChangeKey, timeOut, _))
    stashBuffer.unstashAll(ctx, behavior)
  }

  def create(bId: String,
             name: String,
             gameContainer: GameContainerServerImpl,
             roomId: Long): Behavior[Command] = {
    Behaviors.setup[Command] { ctx =>
      log.debug(s"${ctx.self.path} is starting...")
      implicit val stashBuffer = StashBuffer[Command](Int.MaxValue)
      Behaviors.withTimers[Command] { implicit timer =>
        implicit val sendBuffer = new MiddleBufferInJvm(8192)
        roomManager ! RoomManager.BotJoinRoom(bId,None,name,System.currentTimeMillis(),ctx.self,roomId)
        switchBehavior(ctx, "init", init(bId,name,gameContainer,roomId), InitTime, TimeOut("init"))
      }
    }
  }

  def init(bId: String,
           name: String,
           gameContainer: GameContainerServerImpl,
           roomId: Long)(
            implicit stashBuffer: StashBuffer[Command],
            sendBuffer: MiddleBufferInJvm,
            timer: TimerScheduler[Command]
          ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case msg:TimeOut=>
          log.debug(s"create bot error ${msg.msg} after $InitTime")
          Behaviors.stopped
        case msg: JoinRoomSuccess =>
          timer.startPeriodicTimer(MoveTimeKey,TimeOut(Keymap.move),moveTime)
          timer.startPeriodicTimer(KeyTimeKey,TimeOut(Keymap.key),keyTime)
          timer.startPeriodicTimer(ClickTimeKey,TimeOut(Keymap.click),clickTime)
          switchBehavior(ctx, "play", play(bId,name,BotControl(bId,msg.tank.tankId,name,msg.roomActor,gameContainer),roomId,msg.tank), InitTime, TimeOut("init"))
      }
    }

  def play(id: String,
           name: String,
           bot:BotControl,
           roomId: Long,
           tank: TankServerImpl)(
            implicit stashBuffer: StashBuffer[Command],
            sendBuffer: MiddleBufferInJvm,
            timer: TimerScheduler[Command]
          ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case msg:TimeOut=>
          msg.msg match {
            //todo 动作定时操作
            case Keymap.move=>

            case Keymap.key=>

            case Keymap.move=>

          }
          Behaviors.same

        case msg:StopBot=>
          msg.state match {
            case Stopmap.stop=>
              Behaviors.same
            case Stopmap.delete=>
              Behaviors.stopped
          }
        case unknowMsg =>
          stashBuffer.stash(unknowMsg)
          Behavior.same

      }
    }


}
