package com.neo.sk.tank.core

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Flow
import akka.stream.typed.scaladsl.{ActorSink, ActorSource}
import com.neo.sk.tank.core.tank.TankServerImpl
import com.neo.sk.tank.shared.ptcl.protocol.WsProtocol
import org.slf4j.LoggerFactory
import com.neo.sk.tank.Boot.roomActor
import scala.concurrent.duration._
/**
  * Created by hongruying on 2018/7/9
  *
  */
object UserActor {

  private val log = LoggerFactory.getLogger(this.getClass)

  private final val InitTime = Some(5.minutes)
  private final case object BehaviorChangeKey

  sealed trait Command

  case class WebSocketMsg(reqOpt:Option[WsProtocol.WsMsgFront]) extends Command

  case object CompleteMsgFront extends Command
  case class FailMsgFront(ex: Throwable) extends Command

  case class UserFrontActor(actor:ActorRef[WsProtocol.WsMsgServer]) extends Command

  case class DispatchMsg(msg:WsProtocol.WsMsgServer) extends Command

  case object StartGame extends Command


  case class JoinRoomSuccess(tank:TankServerImpl) extends Command

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


  private def sink(actor: ActorRef[Command]) = ActorSink.actorRef[Command](
    ref = actor,
    onCompleteMessage = CompleteMsgFront,
    onFailureMessage = FailMsgFront.apply
  )

  def flow(actor:ActorRef[UserActor.Command]):Flow[WebSocketMsg,WsProtocol.WsMsgServer,Any] = {
    val in = Flow[WebSocketMsg].to(sink(actor))
    val out =
      ActorSource.actorRef[WsProtocol.WsMsgServer](
        completionMatcher = {
          case WsProtocol.CompleteMsgServer ⇒
        },
        failureMatcher = {
          case WsProtocol.FailMsgServer(e)  ⇒ e
        },
        bufferSize = 8,
        overflowStrategy = OverflowStrategy.fail
      ).mapMaterializedValue(outActor => actor ! UserFrontActor(outActor))
    Flow.fromSinkAndSource(in, out)
  }

  //
  def create(uId:Long,name:String):Behavior[Command] = {
    Behaviors.setup[Command]{ctx =>
      log.debug(s"${ctx.self.path} is starting...")
      implicit val stashBuffer = StashBuffer[Command](Int.MaxValue)
      Behaviors.withTimers[Command] { implicit timer =>

        switchBehavior(ctx,"init",init(uId,name),InitTime,TimeOut("init"))
      }
    }
  }

  private def init(uId:Long,name:String)(
    implicit stashBuffer:StashBuffer[Command],
    timer:TimerScheduler[Command]
  ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case UserFrontActor(frontActor) =>
          ctx.self ! StartGame
          switchBehavior(ctx,"idle",idle(uId,name,frontActor))

        case TimeOut(m) =>
          log.debug(s"${ctx.self.path} is time out when busy,msg=${m}")
          Behaviors.stopped

        case unknowMsg =>
          stashBuffer.stash(unknowMsg)
          Behavior.same
      }
    }


  private def idle(uId:Long,name:String,frontActor:ActorRef[WsProtocol.WsMsgServer])(
    implicit stashBuffer:StashBuffer[Command],
    timer:TimerScheduler[Command]
  ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case StartGame =>
          //todo 往roomActor发消息获取坦克数据和当前游戏桢数据
          roomActor ! RoomActor.JoinRoom(uId,name,ctx.self)
          Behaviors.same

        case JoinRoomSuccess(tank) =>
          //获取坦克数据和当前游戏桢数据
          //给前端Actor同步当前桢数据，然后进入游戏Actor
          play(uId,name,tank,frontActor)


        case WebSocketMsg(reqOpt) =>

        //todo 如果是重玩游戏，往roomActor发消息获取坦克数据和当前游戏桢数据
          Behaviors.same



        case unknowMsg =>
          Behavior.same
      }
    }


  private def play(
                    uId:Long,
                    name:String,
                    tank:TankServerImpl,
                    frontActor:ActorRef[WsProtocol.WsMsgServer])(
    implicit stashBuffer:StashBuffer[Command],
    timer:TimerScheduler[Command]
  ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case WebSocketMsg(reqOpt) =>
          //todo 处理前端的请求数据
          reqOpt match {
            case Some(t:WsProtocol.TankAction) =>
              //分发数据给roomActor
            case _ =>

          }
          Behaviors.same

        case DispatchMsg(m) =>
          frontActor ! m
          Behaviors.same








        case unknowMsg =>
          Behavior.same
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
