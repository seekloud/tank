package com.neo.sk.tank.core

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Flow
import akka.stream.typed.scaladsl.{ActorSink, ActorSource}
import com.neo.sk.tank.Boot.roomManager
import com.neo.sk.tank.core.RoomActor.{JoinRoom4Watch, LeftRoom4Watch}
import com.neo.sk.tank.core.game.TankServerImpl
import com.neo.sk.tank.shared.config.TankGameConfigImpl
import com.neo.sk.tank.shared.game.GameContainerAllState
import com.neo.sk.tank.shared.protocol.TankGameEvent
import com.neo.sk.tank.shared.protocol.TankGameEvent.WatchGame
import org.seekloud.byteobject.MiddleBufferInJvm
import org.slf4j.LoggerFactory
import org.seekloud.byteobject.ByteObject._

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._
object UserActor4WatchGame {

  private val log = LoggerFactory.getLogger(this.getClass)
  private final val InitTime = Some(5.minutes)
  private final case object BehaviorChangeKey

  case class TimeOut(msg:String) extends Command
  trait Command

  case class UserLeft[U](actorRef:ActorRef[U]) extends Command
  case class UserFrontActor(actor:ActorRef[TankGameEvent.WsMsgSource]) extends Command
//  case class WatchGame(roomId:Int,playerId:Long) extends Command
  case class JoinRoomSuccess4Watch(tank:TankServerImpl,
                                   config:TankGameConfigImpl,uId:Long,
                                   roomActor:ActorRef[RoomActor.Command],
                                   gameContainerAllState:GameContainerAllState) extends Command
  case class WebSocketMsg(reqOpt:Option[TankGameEvent.WsMsgFront]) extends Command
  case class DispatchMsg(msg:TankGameEvent.WsMsgSource) extends Command
  case object CompleteMsgFront extends Command
  case class FailMsgFront(ex: Throwable) extends Command


  private[this] def switchBehavior(ctx: ActorContext[Command],
                                   behaviorName: String, behavior: Behavior[Command], durationOpt: Option[FiniteDuration] = None,timeOut: TimeOut  = TimeOut("busy time error"))
                                  (implicit stashBuffer: StashBuffer[Command],
                                   timer:TimerScheduler[Command]) = {
    log.debug(s"${ctx.self.path} becomes $behaviorName behavior.")
    timer.cancel(BehaviorChangeKey)
    durationOpt.foreach(timer.startSingleTimer(BehaviorChangeKey,timeOut,_))
    stashBuffer.unstashAll(ctx,behavior)
  }


  private def sink(actor: ActorRef[Command]) = ActorSink.actorRef[Command](
    ref = actor,
    onCompleteMessage = CompleteMsgFront,
    onFailureMessage = FailMsgFront.apply
  )

  def flow(actor:ActorRef[Command]):Flow[WebSocketMsg,TankGameEvent.WsMsgSource,Any] = {
    val in = Flow[WebSocketMsg].to(sink(actor))
    val out =
      ActorSource.actorRef[TankGameEvent.WsMsgSource](
        completionMatcher = {
          case TankGameEvent.CompleteMsgServer ⇒
        },
        failureMatcher = {
          case TankGameEvent.FailMsgServer(e)  ⇒ e
        },
        bufferSize = 128,
        overflowStrategy = OverflowStrategy.dropHead
      ).mapMaterializedValue(outActor => actor ! UserFrontActor(outActor))
    Flow.fromSinkAndSource(in, out)
  }

  def create(uId:Long):Behavior[Command] = {
    Behaviors.setup[Command]{ctx =>
      log.debug(s"${ctx.self.path} is starting...")
      implicit val stashBuffer = StashBuffer[Command](Int.MaxValue)
      Behaviors.withTimers[Command] { implicit timer =>
        implicit val sendBuffer = new MiddleBufferInJvm(8192)
        switchBehavior(ctx,"init",init(uId),InitTime,TimeOut("init"))
      }
    }
  }

  private def init(uId:Long)(
    implicit stashBuffer:StashBuffer[Command],
    sendBuffer:MiddleBufferInJvm,
    timer:TimerScheduler[Command]
  ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case UserFrontActor(frontActor) =>
          //websocket建立的时候发送UserFrontActor消息
          ctx.watchWith(frontActor,UserLeft(frontActor))
          switchBehavior(ctx,"idle",idle(uId,frontActor,None,None,None))

        case UserLeft(actor) =>
          ctx.unwatch(actor)
          Behaviors.stopped

        case TimeOut(m) =>
          log.debug(s"${ctx.self.path} is time out when busy,msg=${m}")
          Behaviors.stopped

        case unknowMsg =>
          stashBuffer.stash(unknowMsg)
          //          log.warn(s"got unknown msg: $unknowMsg")
          Behavior.same
      }
    }

  private def idle(uId:Long,frontActor:ActorRef[TankGameEvent.WsMsgSource],roomActorOpt:Option[ActorRef[RoomActor.Command]] = None,
                   tankOpt:Option[TankServerImpl],configOpt:Option[TankGameConfigImpl])(
    implicit stashBuffer:StashBuffer[Command],
    timer:TimerScheduler[Command],
    sendBuffer:MiddleBufferInJvm
  ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {

        case WebSocketMsg(reqOpt) =>
          reqOpt match {
            case Some(t:TankGameEvent.WatchGame) =>
              log.debug(s"${ctx.self.path} watch game:$t.roomId,playerId:${t.playerId}")
              roomManager ! JoinRoom4Watch(uId,t.roomId,t.playerId,ctx.self)
              Behaviors.same
            case _ =>
              log.debug(s"there is no msg")
              Behaviors.same
          }

        case JoinRoomSuccess4Watch(tank,config,uId,roomActor,gameContainerAllState) =>
          log.debug(s"${ctx.self.path} first sync gameContainerState")
          frontActor ! TankGameEvent.Wrap(TankGameEvent.FirstSyncGameAllState(Some(gameContainerAllState),
            Some(tank.tankId),Some(tank.name),Some(config)).asInstanceOf[TankGameEvent.WsMsgServer]
            .fillMiddleBuffer(sendBuffer).result())
          idle(uId,frontActor,Some(roomActor),Some(tank),Some(config))

        case DispatchMsg(m) =>
          //给前端分发数据
          log.debug(s"${ctx.self.path} dispatchMsg")
          frontActor ! m
          Behaviors.same

        case UserLeft(actor) =>
          ctx.unwatch(actor)
          roomActorOpt match{
            case Some(roomActor) =>
              //如果用户离开，清除观战用户列表
              roomActor ! LeftRoom4Watch(uId)
            case None =>
          }
          Behaviors.stopped



        case unknowMsg =>
          //          log.warn(s"got unknown msg: $unknowMsg")
          Behavior.same
      }
    }

}
