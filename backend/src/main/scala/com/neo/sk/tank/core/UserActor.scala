package com.neo.sk.tank.core

import akka.actor.Terminated
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Flow
import akka.stream.typed.scaladsl.{ActorSink, ActorSource}
import com.neo.sk.tank.models.TankGameUserInfo
import com.neo.sk.tank.protocol.EsheepProtocol.{GetRecordFrameRsp, GetUserInRecordRsp, PlayerList, RecordFrameInfo}
import org.seekloud.byteobject.MiddleBufferInJvm
import com.neo.sk.tank.shared.protocol.TankGameEvent.ReplayFrameData
import org.slf4j.LoggerFactory
//import com.neo.sk.tank.Boot.roomActor
import com.neo.sk.tank.Boot.{roomManager,esheepSyncClient}
import com.neo.sk.tank.core.game.TankServerImpl
import com.neo.sk.tank.shared.config.TankGameConfigImpl
import com.neo.sk.tank.shared.protocol.TankGameEvent

import scala.concurrent.duration._
import scala.language.implicitConversions
import org.seekloud.byteobject.ByteObject._
/**
  * Created by hongruying on 2018/7/9
  *
  */
object UserActor {

  private val log = LoggerFactory.getLogger(this.getClass)

  private final val InitTime = Some(5.minutes)
  private final case object BehaviorChangeKey

  sealed trait Command

  case class WebSocketMsg(reqOpt:Option[TankGameEvent.WsMsgFront]) extends Command

  case object CompleteMsgFront extends Command
  case class FailMsgFront(ex: Throwable) extends Command

  //fixme 等待变更
  /**此消息用于外部控制状态转入初始状态，以便于重建WebSocket*/
  case object ChangeBehaviorToInit extends Command

  /**外部调用*/
  final case class GetUserInRecord(recordId:Long,replyTo:ActorRef[GetUserInRecordRsp]) extends Command
  final case class GetRecordFrame(recordId:Long,replyTo:ActorRef[GetRecordFrameRsp]) extends Command

  case class UserFrontActor(actor:ActorRef[TankGameEvent.WsMsgSource]) extends Command

  case class DispatchMsg(msg:TankGameEvent.WsMsgSource) extends Command

  case object StartGame extends Command
  case class JoinRoom(uid:Long,tankIdOpt:Option[Int],name:String,userActor:ActorRef[UserActor.Command]) extends Command with RoomManager.Command
  case class JoinRoomSuccess(tank:TankServerImpl,config:TankGameConfigImpl,uId:Long,roomActor: ActorRef[RoomActor.Command]) extends Command with RoomManager.Command

  case class UserLeft[U](actorRef:ActorRef[U]) extends Command

  case class StartReplay(rid:Long, wid:Long, f:Int) extends Command

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

  def flow(actor:ActorRef[UserActor.Command]):Flow[WebSocketMsg,TankGameEvent.WsMsgSource,Any] = {
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

  //
  def create(uId:Long, userInfo:TankGameUserInfo):Behavior[Command] = {
    Behaviors.setup[Command]{ctx =>
      log.debug(s"${ctx.self.path} is starting...")
      implicit val stashBuffer = StashBuffer[Command](Int.MaxValue)
      Behaviors.withTimers[Command] { implicit timer =>
        implicit val sendBuffer = new MiddleBufferInJvm(8192)
        switchBehavior(ctx,"init",init(uId, userInfo),InitTime,TimeOut("init"))
      }
    }
  }

  private def init(uId:Long,userInfo:TankGameUserInfo)(
    implicit stashBuffer:StashBuffer[Command],
    sendBuffer:MiddleBufferInJvm,
    timer:TimerScheduler[Command]
  ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case UserFrontActor(frontActor) =>
          ctx.watchWith(frontActor,UserLeft(frontActor))
          switchBehavior(ctx,"idle",idle(uId, userInfo, frontActor))

        case UserLeft(actor) =>
          log.info("webSocket--error in init")
          ctx.unwatch(actor)
          Behaviors.stopped

        case msg:GetUserInRecord=>
          getGameReplay(ctx,msg.recordId) ! GamePlayer.GetUserInRecord(msg.replyTo)
          Behaviors.same

        case msg:GetRecordFrame=>
          getGameReplay(ctx,msg.recordId) ! GamePlayer.GetRecordFrame(msg.replyTo)
          Behaviors.same

        case TimeOut(m) =>
          log.debug(s"${ctx.self.path} is time out when busy,msg=${m}")
          Behaviors.stopped

        case unknowMsg =>
          stashBuffer.stash(unknowMsg)
//          log.warn(s"got unknown msg: $unknowMsg")
          Behavior.same
      }
    }



  private def idle(uId:Long, userInfo: TankGameUserInfo, frontActor:ActorRef[TankGameEvent.WsMsgSource])(
    implicit stashBuffer:StashBuffer[Command],
    timer:TimerScheduler[Command],
    sendBuffer:MiddleBufferInJvm
  ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case StartGame =>
          //todo 往roomActor发消息获取坦克数据和当前游戏桢数据
          /**换成给roomManager发消息,告知uId,name
            * 还要给userActor发送回带roomId的数据
            * */
          roomManager ! JoinRoom(uId,None,userInfo.name,ctx.self)
          Behaviors.same

        case StartReplay(rid,uid,f) =>
          getGameReplay(ctx,rid) ! GamePlayer.InitReplay(frontActor,uid,f)
          Behaviors.same

        case JoinRoomSuccess(tank,config,uId,roomActor) =>
          //获取坦克数据和当前游戏桢数据
          //给前端Actor同步当前桢数据，然后进入游戏Actor
//          println("渲染数据")
          val startTime = System.currentTimeMillis()
          frontActor ! TankGameEvent.Wrap(TankGameEvent.YourInfo(uId,tank.tankId, userInfo.name, config).asInstanceOf[TankGameEvent.WsMsgServer].fillMiddleBuffer(sendBuffer).result())
          switchBehavior(ctx,"play",play(uId, userInfo,tank,startTime,frontActor,roomActor))


        case WebSocketMsg(reqOpt) =>
          reqOpt match {
            case Some(t:TankGameEvent.RestartGame) =>
              roomManager ! JoinRoom(uId,t.tankIdOpt,t.name,ctx.self)
              idle(uId, userInfo.copy(name = t.name), frontActor)
            case _ =>
              Behaviors.same
          }

        case ChangeBehaviorToInit=>
          switchBehavior(ctx,"init",init(uId, userInfo),InitTime,TimeOut("init"))

        case UserLeft(actor) =>
          log.info("webSocket--error in idle")
          ctx.unwatch(actor)
          Behaviors.stopped

        case msg:GetUserInRecord=>
          getGameReplay(ctx,msg.recordId) ! GamePlayer.GetUserInRecord(msg.replyTo)
          Behaviors.same

        case msg:GetRecordFrame=>
          getGameReplay(ctx,msg.recordId) ! GamePlayer.GetRecordFrame(msg.replyTo)
          Behaviors.same

        case unknowMsg =>
//          log.warn(s"got unknown msg: $unknowMsg")
          Behavior.same
      }
    }

  private def play(
                    uId:Long,
                    userInfo:TankGameUserInfo,
                    tank:TankServerImpl,
                    startTime:Long,
                    frontActor:ActorRef[TankGameEvent.WsMsgSource],
                    roomActor: ActorRef[RoomActor.Command])(
                    implicit stashBuffer:StashBuffer[Command],
                    timer:TimerScheduler[Command],
                    sendBuffer:MiddleBufferInJvm
                  ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case WebSocketMsg(reqOpt) =>
          //todo 处理前端的请求数据
          reqOpt match {
            case Some(t:TankGameEvent.UserActionEvent) =>
              //分发数据给roomActor
              roomActor ! RoomActor.WebSocketMsg(uId,tank.tankId,t)
            case Some(t:TankGameEvent.PingPackage) =>

              frontActor !TankGameEvent.Wrap(t.asInstanceOf[TankGameEvent.WsMsgServer].fillMiddleBuffer(sendBuffer).result())

            case _ =>

          }
          Behaviors.same

        case DispatchMsg(m) =>
          if(m.asInstanceOf[TankGameEvent.Wrap].isKillMsg) {
            if(tank.lives -1 <= 0 && uId > 0){
               val endTime = System.currentTimeMillis()
               esheepSyncClient ! EsheepSyncClient.InputRecord(uId,userInfo.nickName,tank.killTankNum,tank.config.getTankLivesLimit,tank.damageStatistics, startTime, endTime)
            }
            frontActor ! m
            roomManager ! RoomActor.LeftRoomByKilled(uId,tank.tankId,userInfo.name)
            switchBehavior(ctx,"idle",idle(uId,userInfo,frontActor))
          }else{
              frontActor ! m
              Behaviors.same
          }

        case UserLeft(actor) =>
          log.info("webSocket--error in play")
          if(uId > 0){
            val endTime = System.currentTimeMillis()
            val killed = tank.config.getTankLivesLimit - tank.lives
            esheepSyncClient ! EsheepSyncClient.InputRecord(uId,userInfo.nickName,tank.killTankNum,killed,tank.damageStatistics, startTime, endTime)
          }
          ctx.unwatch(actor)
          roomManager ! RoomManager.LeftRoom(uId,tank.tankId,userInfo.name,Some(uId))
          Behaviors.stopped



        case unknowMsg =>
//          log.warn(s"got unknown msg: $unknowMsg")
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

  /**
    * replay-actor*/
  private def getGameReplay(ctx: ActorContext[Command],recordId:Long): ActorRef[GamePlayer.Command] = {
    val childName = s"gameReplay--$recordId"
    ctx.child(childName).getOrElse {
      val actor = ctx.spawn(GamePlayer.create(recordId), childName)
      actor
    }.upcast[GamePlayer.Command]
  }

}
