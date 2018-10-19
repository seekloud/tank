package com.neo.sk.tank.core

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Flow
import akka.stream.typed.scaladsl.{ActorSink, ActorSource}
import com.neo.sk.tank.Boot.roomManager
import com.neo.sk.tank.core.RoomActor.{JoinRoom4Watch, LeftRoom4Watch}
import com.neo.sk.tank.core.UserActor.JoinRoom
import com.neo.sk.tank.core.game.TankServerImpl
import com.neo.sk.tank.shared.config.TankGameConfigImpl
import com.neo.sk.tank.shared.game.GameContainerAllState
import com.neo.sk.tank.shared.protocol.TankGameEvent
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
  case class UserFrontActor(roomId:Int,playerId:Long,actor:ActorRef[TankGameEvent.WsMsgSource]) extends Command
  case class JoinRoomSuccess4Watch(tank:TankServerImpl,
                                   config:TankGameConfigImpl,uId:Long,
                                   roomActor:ActorRef[RoomActor.Command],
                                   gameContainerAllState:GameContainerAllState
                                  ) extends Command
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

  def flow(roomId:Int,playerId:Long,actor:ActorRef[Command]):Flow[WebSocketMsg,TankGameEvent.WsMsgSource,Any] = {
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
      ).mapMaterializedValue(outActor => actor ! UserFrontActor(roomId:Int,playerId:Long,outActor))
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
        case UserFrontActor(roomId,playerId,frontActor) =>
          ctx.watchWith(frontActor,UserLeft(frontActor))
//          roomManager ! JoinRoom4Watch(uId,roomId,playerId,ctx.self)
          switchBehavior(ctx,"idle",idle(uId,frontActor,roomId,playerId))

        case UserLeft(actor) =>
          ctx.unwatch(actor)
          Behaviors.stopped

        case TimeOut(m) =>
          log.debug(s"${ctx.self.path} is time out when busy,msg=${m}")
          Behaviors.stopped

        case unknowMsg =>
          stashBuffer.stash(unknowMsg)
          Behavior.same
      }
    }


  private def idle(uId:Long,frontActor:ActorRef[TankGameEvent.WsMsgSource],roomId:Int,playerId:Long)(
                    implicit stashBuffer:StashBuffer[Command],
                    sendBuffer:MiddleBufferInJvm,
                    timer:TimerScheduler[Command]
                  ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case JoinRoomSuccess4Watch(tank,config,uId,roomActor,gameContainerAllState) =>
          log.debug(s"${ctx.self.path} first sync gameContainerState")
          frontActor ! TankGameEvent.Wrap(TankGameEvent.YourInfo(uId,tank.tankId, tank.name, config).asInstanceOf[TankGameEvent.WsMsgServer].fillMiddleBuffer(sendBuffer).result())
          frontActor ! TankGameEvent.Wrap(TankGameEvent.FirstSyncGameAllState(gameContainerAllState,
            tank.tankId,tank.name,config).asInstanceOf[TankGameEvent.WsMsgServer]
            .fillMiddleBuffer(sendBuffer).result())
          switchBehavior(ctx,"watch",watch(uId,frontActor,roomId,playerId,roomActor,tank,config))

        case DispatchMsg(m) =>
          frontActor ! m
          Behaviors.same

        case WebSocketMsg(reqOpt) =>
          reqOpt match {
            case Some(t:TankGameEvent.RestartGame) =>
              log.debug(s"restart game")
            case _ =>
          }
          Behaviors.same

        case UserLeft(actor) =>
          ctx.unwatch(actor)

          Behaviors.stopped

        case TimeOut(m) =>
          log.debug(s"${ctx.self.path} is time out when busy,msg=${m}")
          Behaviors.stopped

        case unknowMsg =>
          stashBuffer.stash(unknowMsg)
          Behavior.same
      }
    }

  private def watch(uId:Long,frontActor:ActorRef[TankGameEvent.WsMsgSource],roomId:Int,playerId:Long,roomActor:ActorRef[RoomActor.Command],
                   tank:TankServerImpl,config:TankGameConfigImpl)(
                    implicit stashBuffer:StashBuffer[Command],
                    sendBuffer:MiddleBufferInJvm,
                    timer:TimerScheduler[Command]
                  ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case DispatchMsg(m) =>
          if(m.asInstanceOf[TankGameEvent.Wrap].isKillMsg) {
            frontActor ! m
            switchBehavior(ctx,"idle",idle(uId,frontActor,roomId,playerId))
          }else{
            frontActor ! m
            Behaviors.same
          }

        case WebSocketMsg(reqOpt) =>
          reqOpt match {
            case Some(t:TankGameEvent.UserActionEvent) =>
              roomActor ! RoomActor.WebSocketMsg(uId,tank.tankId,t)
            case Some(t:TankGameEvent.PingPackage) =>
              frontActor ! TankGameEvent.Wrap(t.asInstanceOf[TankGameEvent.WsMsgServer].fillMiddleBuffer(sendBuffer).result())
            case _ =>
          }
          Behaviors.same

        case UserLeft(actor) =>
          ctx.unwatch(actor)
          roomActor ! LeftRoom4Watch(uId,playerId)
          Behaviors.stopped

        case TimeOut(m) =>
          log.debug(s"${ctx.self.path} is time out when busy,msg=${m}")
          Behaviors.stopped

        case unknowMsg =>
          stashBuffer.stash(unknowMsg)
          Behavior.same
      }
    }

}
