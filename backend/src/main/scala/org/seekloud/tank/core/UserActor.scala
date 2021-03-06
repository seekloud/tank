/*
 * Copyright 2018 seekloud (https://github.com/seekloud)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.seekloud.tank.core

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Flow
import akka.stream.typed.scaladsl.{ActorSink, ActorSource}
import org.seekloud.tank.models.TankGameUserInfo
import org.seekloud.tank.protocol.ActorProtocol.{ChangeRecordMsg, GetRecordFrameMsg, GetUserInRecordMsg, JoinRoom}
import org.seekloud.byteobject.MiddleBufferInJvm
import org.seekloud.tank.Boot.{esheepSyncClient, roomManager}
import org.seekloud.tank.common.Constants
import org.slf4j.LoggerFactory

import concurrent.duration._
import org.seekloud.tank.Boot.executor
import org.seekloud.tank.core.game.TankServerImpl
import org.seekloud.tank.shared.config.{RiverParameters, SteelParameters, TankGameConfigImpl}
import org.seekloud.tank.shared.protocol.TankGameEvent

import scala.concurrent.duration.FiniteDuration
import org.seekloud.byteobject.ByteObject._
import org.seekloud.byteobject.MiddleBufferInJvm
import org.seekloud.tank.shared.protocol.TankGameEvent.ReplayFrameData
import org.seekloud.tank.shared.ptcl.ErrorRsp
import org.seekloud.tank.shared.model.Constants.frameDurationDefault


/**
  * Created by hongruying on 2018/7/9
  *
  */
object UserActor {

  private val log = LoggerFactory.getLogger(this.getClass)

  private final val InitTime = Some(5.minutes)

  private final case object BehaviorChangeKey

  trait Command

  case class WebSocketMsg(reqOpt: Option[TankGameEvent.WsMsgFront]) extends Command

  case object CompleteMsgFront extends Command

  case class FailMsgFront(ex: Throwable) extends Command

  /** 此消息用于外部控制状态转入初始状态，以便于重建WebSocket */
  case object ChangeBehaviorToInit extends Command

  case class UserFrontActor(actor: ActorRef[TankGameEvent.WsMsgSource]) extends Command

  case class DispatchMsg(msg: TankGameEvent.WsMsgSource) extends Command

  case class WsSuccess(roomId: Option[Long]) extends Command

  case class JoinRoomSuccess(tank: TankServerImpl, config: TankGameConfigImpl, uId: String, roomId: Long, roomActor: ActorRef[RoomActor.Command]) extends Command with RoomManager.Command

  case class JoinRoomFail(msg: String) extends Command

  case class TankRelive4UserActor(tank: TankServerImpl, userId: String, name: String, roomActor: ActorRef[RoomActor.Command], config: TankGameConfigImpl) extends Command with UserManager.Command

  case class UserLeft[U](actorRef: ActorRef[U]) extends Command

  case class StartReplay(rid: Long, wid: String, f: Int) extends Command

  case class ChangeUserInfo(info: TankGameUserInfo) extends Command

  final case class StartObserve(roomId: Long, watchedUserId: String) extends Command

  case class JoinRoomFail4Watch(msg: String) extends Command

  final case class JoinRoomSuccess4Watch(
                                          tank: TankServerImpl,
                                          config: TankGameConfigImpl,
                                          roomActor: ActorRef[RoomActor.Command],
                                          gameState: TankGameEvent.SyncGameAllState
                                        ) extends Command

  case class InputRecordByDead(killTankNum: Int, lives: Int, damageStatistics: Int) extends Command

  case class InputRecordByLeft(killTankNum: Int, lives: Int, damageStatistics: Int) extends Command

  case class ChangeWatchedPlayerId(playerInfo: TankGameUserInfo, watchedPlayerId: String) extends Command with UserManager.Command

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


  private def sink(actor: ActorRef[Command]) = ActorSink.actorRef[Command](
    ref = actor,
    onCompleteMessage = CompleteMsgFront,
    onFailureMessage = FailMsgFront.apply
  )

  def flow(actor: ActorRef[UserActor.Command]): Flow[WebSocketMsg, TankGameEvent.WsMsgSource, Any] = {
    val in = Flow[WebSocketMsg].to(sink(actor))
    val out =
      ActorSource.actorRef[TankGameEvent.WsMsgSource](
        completionMatcher = {
          case TankGameEvent.CompleteMsgServer ⇒
        },
        failureMatcher = {
          case TankGameEvent.FailMsgServer(e) ⇒ e
        },
        bufferSize = 128,
        overflowStrategy = OverflowStrategy.dropHead
      ).mapMaterializedValue(outActor => actor ! UserFrontActor(outActor))
    Flow.fromSinkAndSource(in, out)
  }

  //
  def create(uId: String, userInfo: TankGameUserInfo): Behavior[Command] = {
    Behaviors.setup[Command] { ctx =>
      log.debug(s"${ctx.self.path} is starting...")
      implicit val stashBuffer = StashBuffer[Command](Int.MaxValue)
      Behaviors.withTimers[Command] { implicit timer =>
        implicit val sendBuffer = new MiddleBufferInJvm(8192)
        switchBehavior(ctx, "init", init(uId, userInfo), InitTime, TimeOut("init"))
      }
    }
  }

  private def init(uId: String, userInfo: TankGameUserInfo)(
    implicit stashBuffer: StashBuffer[Command],
    sendBuffer: MiddleBufferInJvm,
    timer: TimerScheduler[Command]
  ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case UserFrontActor(frontActor) =>
          ctx.watchWith(frontActor, UserLeft(frontActor))
          switchBehavior(ctx, "idle", idle(uId, userInfo, System.currentTimeMillis(), frontActor))

        case ChangeUserInfo(info) =>
          init(uId, info)

        case UserLeft(actor) =>
          ctx.unwatch(actor)
          Behaviors.stopped

        case msg: GetUserInRecordMsg =>
          log.debug(s"--------------------$userInfo")
          getGameReplay(ctx, msg.recordId) ! msg
          Behaviors.same


        case ChangeBehaviorToInit =>
          log.debug(s"------------000${userInfo}")
          Behaviors.same

        case msg: GetRecordFrameMsg =>
          getGameReplay(ctx, msg.recordId) ! msg
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


  private def idle(uId: String, userInfo: TankGameUserInfo, startTime: Long, frontActor: ActorRef[TankGameEvent.WsMsgSource])(
    implicit stashBuffer: StashBuffer[Command],
    timer: TimerScheduler[Command],
    sendBuffer: MiddleBufferInJvm
  ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case ChangeUserInfo(info) =>
          idle(uId, info, startTime, frontActor)

        case WsSuccess(roomIdOpt) =>
          frontActor ! TankGameEvent.Wrap(TankGameEvent.WsSuccess(roomIdOpt).asInstanceOf[TankGameEvent.WsMsgServer].fillMiddleBuffer(sendBuffer).result())
          Behaviors.same

        case JoinRoomFail(msg) =>
          frontActor ! TankGameEvent.Wrap(TankGameEvent.WsMsgErrorRsp(10001, msg).asInstanceOf[TankGameEvent.WsMsgServer].fillMiddleBuffer(sendBuffer).result())
          Behaviors.same


        case StartReplay(rid, uid, f) =>
          getGameReplay(ctx, rid) ! GamePlayer.InitReplay(frontActor, uid, f)
          switchBehavior(ctx, "replay", replay(uid, rid, userInfo, startTime, frontActor))
        //          Behaviors.same

        case JoinRoomSuccess(tank, config, `uId`, roomId, roomActor) =>
          //获取坦克数据和当前游戏桢数据
          //给前端Actor同步当前桢数据，然后进入游戏Actor
          //          println("渲染数据")'
          val cig = config.copy(obstacleParameters = config.obstacleParameters.copy(riverParameters = RiverParameters(Nil, Nil), steelParameters = SteelParameters(Nil, Nil)))
          frontActor ! TankGameEvent.Wrap(TankGameEvent.YourInfo(uId, tank.tankId, userInfo.name, roomId, cig).asInstanceOf[TankGameEvent.WsMsgServer].fillMiddleBuffer(sendBuffer).result())
          switchBehavior(ctx, "play", play(uId, userInfo, tank, startTime, frontActor, roomActor))


        case StartObserve(roomId, watchedUserId) =>
          roomManager ! RoomActor.JoinRoom4Watch(uId, roomId, watchedUserId, ctx.self)
          switchBehavior(ctx, "observeInit", observeInit(uId, userInfo, roomId, frontActor))


        case WebSocketMsg(reqOpt) =>
          reqOpt match {
            case Some(t: TankGameEvent.RestartGame) =>
              val newStartTime = System.currentTimeMillis()
              roomManager ! JoinRoom(uId, t.tankIdOpt, t.name, newStartTime, ctx.self)
              idle(uId, userInfo.copy(name = t.name), newStartTime, frontActor)

            case Some(t: TankGameEvent.JoinRoom) =>
              log.info("get ws msg startGame")
              /** 换成给roomManager发消息,告知uId,name
                * 还要给userActor发送回带roomId的数据
                * */
              roomManager ! JoinRoom(uId, None, userInfo.name, startTime, ctx.self, t.roomId, t.password)
              Behaviors.same

            case Some(t: TankGameEvent.CreateRoom) =>
              log.info(s"cerate room msg")
              roomManager ! RoomManager.CreateRoom(uId, None, userInfo.name, startTime, ctx.self, t.roomId, t.password, t.frameDuration.getOrElse(frameDurationDefault))
              Behaviors.same

            case _ =>
              Behaviors.same
          }

        /**
          * 本消息内转换为初始状态并给前端发送异地登录消息*/
        case ChangeBehaviorToInit =>
          dispatchTo(frontActor, TankGameEvent.RebuildWebSocket)
          ctx.unwatch(frontActor)
          switchBehavior(ctx, "init", init(uId, userInfo), InitTime, TimeOut("init"))

        case UserLeft(actor) =>
          ctx.unwatch(actor)
          switchBehavior(ctx, "init", init(uId, userInfo), InitTime, TimeOut("init"))

        //        case msg:GetUserInRecordMsg=>
        //          getGameReplay(ctx,msg.recordId) ! msg
        //          Behaviors.same
        //
        //        case msg:GetRecordFrameMsg=>
        //          getGameReplay(ctx,msg.recordId) ! msg
        //          Behaviors.same

        case unknowMsg =>
          //          stashBuffer.stash(unknowMsg)
          log.warn(s"got unknown msg: $unknowMsg")
          Behavior.same
      }
    }

  private def replay(uId: String,
                     recordId: Long,
                     userInfo: TankGameUserInfo,
                     startTime: Long,
                     frontActor: ActorRef[TankGameEvent.WsMsgSource])(
                      implicit stashBuffer: StashBuffer[Command],
                      timer: TimerScheduler[Command],
                      sendBuffer: MiddleBufferInJvm
                    ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        /**
          * 本消息内转换为初始状态并给前端发送异地登录消息*/
        case ChangeBehaviorToInit =>
          dispatchTo(frontActor, TankGameEvent.RebuildWebSocket)
          ctx.unwatch(frontActor)
          switchBehavior(ctx, "init", init(uId, userInfo), InitTime, TimeOut("init"))

        case ChangeUserInfo(info) =>
          replay(uId, recordId, info, startTime, frontActor)

        case UserLeft(actor) =>
          ctx.unwatch(actor)
          switchBehavior(ctx, "init", init(uId, userInfo), InitTime, TimeOut("init"))

        case msg: GetUserInRecordMsg =>
          log.debug(s"${ctx.self.path} recv a msg=${msg}")
          if (msg.recordId != recordId) {
            msg.replyTo ! ErrorRsp(10002, "you are watching the other record")
          } else {
            getGameReplay(ctx, msg.recordId) ! msg
          }

          Behaviors.same

        case msg: ChangeRecordMsg =>
          ctx.self ! UserActor.StartReplay(msg.rid, msg.playerId, msg.f)
          switchBehavior(ctx, "idle", idle(uId, userInfo, startTime, frontActor))

        case msg: GetRecordFrameMsg =>
          log.debug(s"${ctx.self.path} recv a msg=${msg}")
          if (msg.recordId != recordId) {
            msg.replyTo ! ErrorRsp(10002, "you are watching the other record")
          } else {
            getGameReplay(ctx, msg.recordId) ! msg
          }
          Behaviors.same

        case unknowMsg =>
          Behavior.same
      }
    }


  private def observeInit(uId: String, userInfo: TankGameUserInfo, roomId: Long, frontActor: ActorRef[TankGameEvent.WsMsgSource])(
    implicit stashBuffer: StashBuffer[Command],
    timer: TimerScheduler[Command],
    sendBuffer: MiddleBufferInJvm
  ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case ChangeUserInfo(info) =>
          observeInit(uId, info, roomId, frontActor)

        case JoinRoomSuccess4Watch(tank, config, roomActor, state) =>
          log.debug(s"${ctx.self.path} first sync gameContainerState")
          frontActor ! TankGameEvent.Wrap(TankGameEvent.YourInfo(uId, tank.tankId, tank.name, roomId, config).asInstanceOf[TankGameEvent.WsMsgServer].fillMiddleBuffer(sendBuffer).result())
          frontActor ! TankGameEvent.Wrap(state.asInstanceOf[TankGameEvent.WsMsgServer].fillMiddleBuffer(sendBuffer).result())
          switchBehavior(ctx, "observe", observe(uId, userInfo, roomId, tank, frontActor, roomActor))

        case JoinRoomFail4Watch(error) =>
          log.debug(s"${ctx.self.path} recv a msg=${msg}")
          frontActor ! TankGameEvent.Wrap(TankGameEvent.WsMsgErrorRsp(1, error).asInstanceOf[TankGameEvent.WsMsgServer].fillMiddleBuffer(sendBuffer).result())
          frontActor ! TankGameEvent.CompleteMsgServer
          Behaviors.stopped

        case TankRelive4UserActor(tank, userId, name, roomActor, config) =>
          switchBehavior(ctx, "observe", observe(uId, userInfo, roomId, tank, frontActor, roomActor))

        case DispatchMsg(m) =>
          if (m.asInstanceOf[TankGameEvent.Wrap].isKillMsg) {
            //            frontActor ! m
            //            switchBehavior(ctx,"observeInit",observeInit(uId, userInfo, frontActor))
          } else {
            frontActor ! m
          }
          Behaviors.same

        case ChangeBehaviorToInit =>
          frontActor ! TankGameEvent.Wrap(TankGameEvent.RebuildWebSocket.asInstanceOf[TankGameEvent.WsMsgServer].fillMiddleBuffer(sendBuffer).result())
          ctx.unwatch(frontActor)
          switchBehavior(ctx, "init", init(uId, userInfo), InitTime, TimeOut("init"))

        case UserLeft(actor) =>
          ctx.unwatch(actor)
          switchBehavior(ctx, "init", init(uId, userInfo), InitTime, TimeOut("init"))

        case unknowMsg =>
          stashBuffer.stash(unknowMsg)
          Behavior.same
      }

    }

  private def observe(
                       uId: String,
                       userInfo: TankGameUserInfo,
                       roomId: Long,
                       tank: TankServerImpl,
                       frontActor: ActorRef[TankGameEvent.WsMsgSource],
                       roomActor: ActorRef[RoomActor.Command])(
                       implicit stashBuffer: StashBuffer[Command],
                       timer: TimerScheduler[Command],
                       sendBuffer: MiddleBufferInJvm
                     ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case ChangeUserInfo(info) =>
          observe(uId, info, roomId, tank, frontActor, roomActor)

        case DispatchMsg(m) =>
          if (m.asInstanceOf[TankGameEvent.Wrap].isKillMsg) {
            frontActor ! m
            switchBehavior(ctx, "observeInit", observeInit(uId, userInfo, roomId, frontActor))
          } else {
            frontActor ! m
            Behaviors.same
          }

        case WebSocketMsg(reqOpt) =>
          reqOpt.foreach {
            case t: TankGameEvent.UserActionEvent =>
              //分发数据给roomActor
              //              println(s"${ctx.self.path} websocketmsg---------------${t}")
              roomActor ! RoomActor.WebSocketMsg(uId, tank.tankId, t)
            case t: TankGameEvent.PingPackage =>
              frontActor ! TankGameEvent.Wrap(t.asInstanceOf[TankGameEvent.WsMsgServer].fillMiddleBuffer(sendBuffer).result())

            case TankGameEvent.GetSyncGameState =>
              roomActor ! RoomActor.GetSyncState(uId)
            case _ =>
          }
          Behaviors.same

        case msg: ChangeWatchedPlayerId =>
          ctx.self ! StartObserve(roomId, msg.watchedPlayerId)
          switchBehavior(ctx, "idle", idle(uId, userInfo, System.currentTimeMillis(), frontActor))

        case ChangeBehaviorToInit =>
          frontActor ! TankGameEvent.Wrap(TankGameEvent.RebuildWebSocket.asInstanceOf[TankGameEvent.WsMsgServer].fillMiddleBuffer(sendBuffer).result())
          roomActor ! RoomActor.LeftRoom4Watch(uId, tank.userId)
          ctx.unwatch(frontActor)
          switchBehavior(ctx, "init", init(uId, userInfo), InitTime, TimeOut("init"))

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
                    uId: String,
                    userInfo: TankGameUserInfo,
                    tank: TankServerImpl,
                    startTime: Long,
                    frontActor: ActorRef[TankGameEvent.WsMsgSource],
                    roomActor: ActorRef[RoomActor.Command])(
                    implicit stashBuffer: StashBuffer[Command],
                    timer: TimerScheduler[Command],
                    sendBuffer: MiddleBufferInJvm
                  ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case ChangeUserInfo(info) =>
          play(uId, info, tank, startTime, frontActor, roomActor)

        case WebSocketMsg(reqOpt) =>
          if (reqOpt.nonEmpty) {
            reqOpt.get match {
              case t: TankGameEvent.UserActionEvent =>
                roomActor ! RoomActor.WebSocketMsg(uId, tank.tankId, t)
                Behaviors.same

              case t: TankGameEvent.PingPackage =>
                frontActor ! TankGameEvent.Wrap(t.asInstanceOf[TankGameEvent.WsMsgServer].fillMiddleBuffer(sendBuffer).result())
                Behaviors.same

              case TankGameEvent.GetSyncGameState =>
                roomActor ! RoomActor.GetSyncState(uId)
                Behaviors.same

              case t: TankGameEvent.RestartGame =>
                roomManager ! RoomActor.LeftRoomByKilled(uId, tank.tankId, tank.getTankState().lives, userInfo.name)
                val newStartTime = System.currentTimeMillis()
                roomManager ! JoinRoom(uId, t.tankIdOpt, t.name, newStartTime, ctx.self)
                switchBehavior(ctx, "idle", idle(uId, userInfo.copy(name = t.name), newStartTime, frontActor))

              case _ =>
                Behaviors.same
            }
          } else {
            Behaviors.same
          }

        case DispatchMsg(m) =>
          if (m.asInstanceOf[TankGameEvent.Wrap].isKillMsg) {
            frontActor ! m
            println(s"${ctx.self.path} tank 当前生命值${tank.getTankState().lives}")
            if (tank.lives > 1) {
              //玩家进入复活状态
              //              roomManager ! RoomActor.LeftRoomByKilled(uId,tank.tankId,tank.getTankState().lives,userInfo.name)
              switchBehavior(ctx, "waitRestartWhenPlay", waitRestartWhenPlay(uId, userInfo, startTime, frontActor, tank))
            } else {
              Behaviors.same
            }
          } else {
            frontActor ! m
            Behaviors.same
          }

        case ChangeBehaviorToInit =>
          frontActor ! TankGameEvent.Wrap(TankGameEvent.RebuildWebSocket.asInstanceOf[TankGameEvent.WsMsgServer].fillMiddleBuffer(sendBuffer).result())
          roomManager ! RoomManager.LeftRoom(uId, tank.tankId, userInfo.name, Some(uId))
          ctx.unwatch(frontActor)
          switchBehavior(ctx, "init", init(uId, userInfo), InitTime, TimeOut("init"))

        case UserLeft(actor) =>
          ctx.unwatch(actor)
          roomManager ! RoomManager.LeftRoom(uId, tank.tankId, userInfo.name, Some(uId))
          Behaviors.stopped

        case k: InputRecordByDead =>
          log.debug(s"input record by dead msg")
          if (tank.lives - 1 <= 0 && !uId.contains(Constants.TankGameUserIdPrefix)) {
            val endTime = System.currentTimeMillis()
            log.debug(s"input record ${EsheepSyncClient.InputRecord(uId, userInfo.nickName, k.killTankNum, tank.config.getTankLivesLimit, k.damageStatistics, startTime, endTime)}")
            esheepSyncClient ! EsheepSyncClient.InputRecord(uId, userInfo.nickName, k.killTankNum, tank.config.getTankLivesLimit, k.damageStatistics, startTime, endTime)
          }
          Behaviors.same

        case unknowMsg =>
          //          log.warn(s"got unknown msg: $unknowMsg")
          Behavior.same
      }
    }


  /**
    * 死亡重玩状态
    **/
  private def waitRestartWhenPlay(uId: String,
                                  userInfo: TankGameUserInfo,
                                  startTime: Long,
                                  frontActor: ActorRef[TankGameEvent.WsMsgSource],
                                  tank: TankServerImpl)(
                                   implicit stashBuffer: StashBuffer[Command],
                                   timer: TimerScheduler[Command],
                                   sendBuffer: MiddleBufferInJvm
                                 ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case TankRelive4UserActor(t, userId, name, roomActor, config) =>
          switchBehavior(ctx, "play", play(uId, userInfo, t, startTime, frontActor, roomActor))

        case DispatchMsg(m) =>
          frontActor ! m
          Behaviors.same

        /**
          * 本消息内转换为初始状态并给前端发送异地登录消息*/
        case ChangeBehaviorToInit =>
          frontActor ! TankGameEvent.Wrap(TankGameEvent.RebuildWebSocket.asInstanceOf[TankGameEvent.WsMsgServer].fillMiddleBuffer(sendBuffer).result())
          roomManager ! RoomManager.LeftRoom(uId, tank.tankId, userInfo.name, Some(uId))
          ctx.unwatch(frontActor)
          switchBehavior(ctx, "init", init(uId, userInfo), InitTime, TimeOut("init"))

        case UserLeft(actor) =>
          ctx.unwatch(actor)
          roomManager ! RoomManager.LeftRoom(uId, tank.tankId, userInfo.name, Some(uId))
          switchBehavior(ctx, "init", init(uId, userInfo), InitTime, TimeOut("init"))

        case JoinRoomSuccess(t, config, `uId`, roomId, roomActor) =>
          frontActor ! TankGameEvent.Wrap(TankGameEvent.YourInfo(uId, t.tankId, userInfo.name, roomId, config).asInstanceOf[TankGameEvent.WsMsgServer].fillMiddleBuffer(sendBuffer).result())
          switchBehavior(ctx, "play", play(uId, userInfo, t, startTime, frontActor, roomActor))


        case unknowMsg =>
          log.warn(s"got unknown msg: $unknowMsg")
          Behavior.same
      }
    }

  import org.seekloud.byteobject.ByteObject._

  private def dispatchTo(subscriber: ActorRef[TankGameEvent.WsMsgSource], msg: TankGameEvent.WsMsgServer)(implicit sendBuffer: MiddleBufferInJvm) = {
    subscriber ! ReplayFrameData(List(msg).fillMiddleBuffer(sendBuffer).result())
  }


  private def busy()(
    implicit stashBuffer: StashBuffer[Command],
    timer: TimerScheduler[Command]
  ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case SwitchBehavior(name, behavior, durationOpt, timeOut) =>
          switchBehavior(ctx, name, behavior, durationOpt, timeOut)

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
  private def getGameReplay(ctx: ActorContext[Command], recordId: Long): ActorRef[GamePlayer.Command] = {
    val childName = s"gameReplay--$recordId"
    ctx.child(childName).getOrElse {
      val actor = ctx.spawn(GamePlayer.create(recordId), childName)
      actor
    }.upcast[GamePlayer.Command]
  }

}
