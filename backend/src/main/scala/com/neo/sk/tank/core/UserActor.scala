package com.neo.sk.tank.core

import akka.actor.Terminated
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Flow
import akka.stream.typed.scaladsl.{ActorSink, ActorSource}
import com.neo.sk.tank.common.Constants
import com.neo.sk.tank.models.TankGameUserInfo
import com.neo.sk.tank.protocol.EsheepProtocol.{GetRecordFrameRsp, GetUserInRecordRsp, PlayerList, RecordFrameInfo}
import com.neo.sk.tank.shared.model.Constants.GameState
import org.seekloud.byteobject.MiddleBufferInJvm
import com.neo.sk.tank.shared.protocol.TankGameEvent.{CompleteMsgServer, ReplayFrameData}
import org.slf4j.LoggerFactory
//import com.neo.sk.tank.Boot.roomActor
import com.neo.sk.tank.Boot.{roomManager,esheepSyncClient}
import com.neo.sk.tank.core.game.TankServerImpl
import com.neo.sk.tank.shared.config.TankGameConfigImpl
import com.neo.sk.tank.shared.protocol.TankGameEvent

import scala.concurrent.duration._
import scala.language.implicitConversions
import org.seekloud.byteobject.ByteObject._
import com.neo.sk.tank.protocol.ReplayProtocol.{GetUserInRecordMsg,GetRecordFrameMsg}
/**
  * Created by hongruying on 2018/7/9
  *
  */
object UserActor {

  private val log = LoggerFactory.getLogger(this.getClass)

  private final val InitTime = Some(5.minutes)
  private final case object BehaviorChangeKey

  trait Command

  case class WebSocketMsg(reqOpt:Option[TankGameEvent.WsMsgFront]) extends Command

  case object CompleteMsgFront extends Command
  case class FailMsgFront(ex: Throwable) extends Command

  //fixme 等待变更
  /**此消息用于外部控制状态转入初始状态，以便于重建WebSocket*/
  case object ChangeBehaviorToInit extends Command

  case class UserFrontActor(actor:ActorRef[TankGameEvent.WsMsgSource]) extends Command

  case class DispatchMsg(msg:TankGameEvent.WsMsgSource) extends Command

  case class StartGame(roomId:Option[Long]) extends Command
  case class JoinRoom(uid:String,tankIdOpt:Option[Int],gameStateOpt:Option[Int],name:String,startTime:Long,userActor:ActorRef[UserActor.Command], roomIdOpt:Option[Long] = None) extends Command with RoomManager.Command

  case class JoinRoomSuccess(tank:TankServerImpl,config:TankGameConfigImpl,uId:String,roomActor: ActorRef[RoomActor.Command]) extends Command with RoomManager.Command

  case class UserLeft[U](actorRef:ActorRef[U]) extends Command

  case class StartReplay(rid:Long, wid:String, f:Int) extends Command

  final case class StartObserve(roomId:Long, watchedUserId:String) extends Command

  case class JoinRoomFail4Watch(msg:String) extends Command

  final case class JoinRoomSuccess4Watch(
                                          tank:TankServerImpl,
                                          config:TankGameConfigImpl,
                                          roomActor:ActorRef[RoomActor.Command],
                                          gameState:TankGameEvent.SyncGameAllState
                                        ) extends Command

  case class  InputRecordByDead(killTankNum:Int,lives:Int,damageStatistics:Int) extends Command

  case class InputRecordByLeft(killTankNum:Int,lives:Int,damageStatistics:Int) extends Command

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
  def create(uId:String, userInfo:TankGameUserInfo):Behavior[Command] = {
    Behaviors.setup[Command]{ctx =>
      log.debug(s"${ctx.self.path} is starting...")
      implicit val stashBuffer = StashBuffer[Command](Int.MaxValue)
      Behaviors.withTimers[Command] { implicit timer =>
        implicit val sendBuffer = new MiddleBufferInJvm(8192)
        switchBehavior(ctx,"init",init(uId, userInfo),InitTime,TimeOut("init"))
      }
    }
  }

  private def init(uId:String,userInfo:TankGameUserInfo)(
    implicit stashBuffer:StashBuffer[Command],
    sendBuffer:MiddleBufferInJvm,
    timer:TimerScheduler[Command]
  ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case UserFrontActor(frontActor) =>
          ctx.watchWith(frontActor,UserLeft(frontActor))
          switchBehavior(ctx,"idle",idle(uId, userInfo,System.currentTimeMillis(), frontActor))

        case UserLeft(actor) =>
          log.info("webSocket--error in init")
          ctx.unwatch(actor)
          Behaviors.stopped

        case msg:GetUserInRecordMsg=>
          getGameReplay(ctx,msg.recordId) ! msg
          Behaviors.same

        case msg:GetRecordFrameMsg=>
          getGameReplay(ctx,msg.recordId) ! msg
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



  private def idle(uId:String, userInfo: TankGameUserInfo,startTime:Long, frontActor:ActorRef[TankGameEvent.WsMsgSource])(
    implicit stashBuffer:StashBuffer[Command],
    timer:TimerScheduler[Command],
    sendBuffer:MiddleBufferInJvm
  ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case StartGame(roomIdOpt) =>
          /**换成给roomManager发消息,告知uId,name
            * 还要给userActor发送回带roomId的数据
            * */
          roomManager ! JoinRoom(uId,None,None,userInfo.name,startTime,ctx.self, roomIdOpt)
          Behaviors.same

        case StartReplay(rid,uid,f) =>
          getGameReplay(ctx,rid) ! GamePlayer.InitReplay(frontActor,uid,f)
          Behaviors.same

        case JoinRoomSuccess(tank,config,uId,roomActor) =>
          //获取坦克数据和当前游戏桢数据
          //给前端Actor同步当前桢数据，然后进入游戏Actor
//          println("渲染数据")
          frontActor ! TankGameEvent.Wrap(TankGameEvent.YourInfo(uId,tank.tankId, userInfo.name, config).asInstanceOf[TankGameEvent.WsMsgServer].fillMiddleBuffer(sendBuffer).result())
          switchBehavior(ctx,"play",play(uId, userInfo,tank,startTime,frontActor,roomActor))


        case StartObserve(roomId, watchedUserId) =>
          roomManager ! RoomActor.JoinRoom4Watch(uId,roomId,watchedUserId,ctx.self)
          switchBehavior(ctx, "observeInit", observeInit(uId, userInfo, frontActor))


        case WebSocketMsg(reqOpt) =>
          reqOpt match {
            case Some(t:TankGameEvent.RestartGame) =>
              if(t.gameState == GameState.stop){
                log.debug("dead 3--------------")
                val newStartTime = System.currentTimeMillis()
                roomManager ! JoinRoom(uId,t.tankIdOpt,Some(GameState.stop),t.name,newStartTime,ctx.self)
                idle(uId,userInfo.copy(name = t.name),newStartTime,frontActor)
              }else{
                log.debug(s"tank game state${t.gameState}")
                roomManager ! JoinRoom(uId,t.tankIdOpt,Some(t.gameState),t.name,startTime,ctx.self)
                idle(uId,userInfo.copy(name = t.name),startTime,frontActor)
              }
            case _ =>
              Behaviors.same
          }

        case ChangeBehaviorToInit=>
          switchBehavior(ctx,"init",init(uId, userInfo),InitTime,TimeOut("init"))

        case UserLeft(actor) =>
          log.info("webSocket--error in idle")
          ctx.unwatch(actor)
          Behaviors.stopped

        case msg:GetUserInRecordMsg=>
          getGameReplay(ctx,msg.recordId) ! msg
          Behaviors.same

        case msg:GetRecordFrameMsg=>
          getGameReplay(ctx,msg.recordId) ! msg
          Behaviors.same

        case unknowMsg =>
//          log.warn(s"got unknown msg: $unknowMsg")
          Behavior.same
      }
    }


  private def observeInit(uId:String, userInfo: TankGameUserInfo, frontActor:ActorRef[TankGameEvent.WsMsgSource])(
    implicit stashBuffer:StashBuffer[Command],
    timer:TimerScheduler[Command],
    sendBuffer:MiddleBufferInJvm
  ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case JoinRoomSuccess4Watch(tank, config, roomActor, state) =>
          log.debug(s"${ctx.self.path} first sync gameContainerState")
          frontActor ! TankGameEvent.Wrap(TankGameEvent.YourInfo(uId,tank.tankId, tank.name, config).asInstanceOf[TankGameEvent.WsMsgServer].fillMiddleBuffer(sendBuffer).result())
          frontActor ! TankGameEvent.Wrap(state.asInstanceOf[TankGameEvent.WsMsgServer].fillMiddleBuffer(sendBuffer).result())
          switchBehavior(ctx, "observe", observe(uId, userInfo, tank, frontActor, roomActor))

        case JoinRoomFail4Watch(error) =>
          frontActor ! TankGameEvent.Wrap(TankGameEvent.WsMsgErrorRsp(1, error).asInstanceOf[TankGameEvent.WsMsgServer].fillMiddleBuffer(sendBuffer).result())
          frontActor ! TankGameEvent.CompleteMsgServer
          Behaviors.stopped

        case DispatchMsg(m) =>
          if(m.asInstanceOf[TankGameEvent.Wrap].isKillMsg) {
//            frontActor ! m
//            switchBehavior(ctx,"observeInit",observeInit(uId, userInfo, frontActor))
          }else{
            frontActor ! m
          }
          Behaviors.same

        case UserLeft(actor) =>
          ctx.unwatch(actor)
          Behaviors.stopped

        case unknowMsg =>
          stashBuffer.stash(unknowMsg)
          Behavior.same
      }

    }

  private def observe(
                       uId:String,
                       userInfo: TankGameUserInfo,
                       tank:TankServerImpl,
                       frontActor:ActorRef[TankGameEvent.WsMsgSource],
                       roomActor: ActorRef[RoomActor.Command])(
    implicit stashBuffer:StashBuffer[Command],
    timer:TimerScheduler[Command],
    sendBuffer:MiddleBufferInJvm
  ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case DispatchMsg(m) =>
          if(m.asInstanceOf[TankGameEvent.Wrap].isKillMsg) {
            frontActor ! m
            switchBehavior(ctx,"observeInit",observeInit(uId, userInfo, frontActor))
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
          roomActor ! RoomActor.LeftRoom4Watch(uId, tank.userId)
          Behaviors.stopped


        case unknowMsg =>
          log.warn(s"${ctx.self.path} recv an unknown msg=${msg}")
          Behavior.same

      }

    }


  private def play(
                    uId:String,
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
            frontActor ! m
            roomManager ! RoomActor.LeftRoomByKilled(uId,tank.tankId,userInfo.name)
            switchBehavior(ctx,"idle",idle(uId,userInfo,startTime,frontActor))
          }else{
              frontActor ! m
              Behaviors.same
          }

        case UserLeft(actor) =>
          log.info("webSocket--error in play")
          ctx.unwatch(actor)
          roomManager ! RoomManager.LeftRoom(uId,tank.tankId,userInfo.name,Some(uId))
          Behaviors.stopped

        case k:InputRecordByDead =>
          log.debug(s"input record by dead msg")
          if(tank.lives -1 <= 0 && !uId.contains(Constants.TankGameUserIdPrefix)){
            val endTime = System.currentTimeMillis()
            log.debug(s"input record ${EsheepSyncClient.InputRecord(uId,userInfo.nickName,k.killTankNum,tank.config.getTankLivesLimit,k.damageStatistics, startTime, endTime)}")
            esheepSyncClient ! EsheepSyncClient.InputRecord(uId,userInfo.nickName,k.killTankNum,tank.config.getTankLivesLimit,k.damageStatistics, startTime, endTime)
          }
          Behaviors.same

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
