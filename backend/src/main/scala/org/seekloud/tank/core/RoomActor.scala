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
import org.seekloud.tank.core.bot.BotManager
import org.seekloud.byteobject.MiddleBufferInJvm
import org.seekloud.tank.Boot.{botManager, esheepSyncClient}
import org.seekloud.tank.common.{AppSettings, Constants}
import org.seekloud.tank.core.bot.{BotActor, BotManager}
import org.seekloud.tank.core.game.GameContainerServerImpl
import org.slf4j.LoggerFactory

import concurrent.duration._
import org.seekloud.tank.Boot.executor
import org.seekloud.tank.shared.protocol.TankGameEvent

import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration
import org.seekloud.tank.protocol.ActorProtocol.JoinRoom
/**
  * Created by hongruying on 2018/7/9
  * 管理房间的地图数据以及分发操作
  *
  *
  *
  */
object RoomActor {

  private val log = LoggerFactory.getLogger(this.getClass)

  private final val InitTime = Some(5.minutes)

  private final val classify= 100 // 10s同步一次状态

  private final val syncFrameOnly = 20 // 2s同步一次状态

  private final case object BehaviorChangeKey

  private final case object GameLoopKey

  trait Command

  case class GetSyncState(uid:String) extends Command

  case class BotJoinRoom(bid: String, tankIdOpt: Option[Int], name: String, startTime: Long, botActor: ActorRef[BotActor.Command], roomId: Long) extends Command with RoomManager.Command

  case class WebSocketMsg(uid: String, tankId: Int, req: TankGameEvent.UserActionEvent) extends Command with RoomManager.Command

  case class BotLeftRoom(uid: String, tankId: Int, name: String,roomId: Long) extends Command with RoomManager.Command

  case class LeftRoom(uid: String, tankId: Int, name: String, uidSet: List[(String, String)], roomId: Long) extends Command with RoomManager.Command

  case class LeftRoomByKilled(uid: String, tankId: Int, tankLives: Int, name: String) extends Command with RoomManager.Command

  case class LeftRoom4Watch(uid: String, playerId: String) extends Command with RoomManager.Command

  case class JoinRoom4Watch(uid: String, roomId: Long, playerId: String, userActor4Watch: ActorRef[UserActor.Command]) extends Command with RoomManager.Command

  final case class ChildDead[U](name: String, childRef: ActorRef[U]) extends Command with RoomManager.Command

  case object GameLoop extends Command

  case class TankRelive(userId: String, tankIdOpt: Option[Int], name: String) extends Command

  private final case object SyncFrame4NewComerKey extends  Command
  final case class SyncFrame4NewComer(userId:String,tickCount:Int) extends Command


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

  def create(roomId: Long): Behavior[Command] = {
    log.debug(s"Room Actor-${roomId} start...")
    Behaviors.setup[Command] {
      ctx =>
        Behaviors.withTimers[Command] {
          implicit timer =>
            val subscribersMap = mutable.HashMap[String, ActorRef[UserActor.Command]]()
            val observersMap = mutable.HashMap[String, ActorRef[UserActor.Command]]()
            implicit val sendBuffer = new MiddleBufferInJvm(81920)
            val gameContainer = game.GameContainerServerImpl(AppSettings.tankGameConfig, ctx.self, timer, log, roomId,
              dispatch(subscribersMap, observersMap),
              dispatchTo(subscribersMap, observersMap)
            )
            if (AppSettings.gameRecordIsWork) {
              getGameRecorder(ctx, gameContainer, roomId, gameContainer.systemFrame)
            }
            timer.startPeriodicTimer(GameLoopKey, GameLoop, (gameContainer.config.frameDuration / gameContainer.config.playRate).millis)
            idle(0l, roomId, Nil, Nil, mutable.HashMap[Long, Set[String]](), subscribersMap, observersMap, gameContainer, 0L)
        }
    }
  }

  def idle(
            index: Long,
            roomId: Long,
            justJoinUser: List[(String, Option[Int], Long, ActorRef[UserActor.Command])],
            userMap: List[(String, Option[Int], String, Long, Long)],
            userGroup: mutable.HashMap[Long, Set[String]],
            subscribersMap: mutable.HashMap[String, ActorRef[UserActor.Command]],
            observersMap: mutable.HashMap[String, ActorRef[UserActor.Command]],
            gameContainer: GameContainerServerImpl,
            tickCount: Long
          )(
            implicit timer: TimerScheduler[Command],
            sendBuffer: MiddleBufferInJvm
          ): Behavior[Command] = {
    Behaviors.receive { (ctx, msg) =>
      msg match {
        case JoinRoom(uid, tankIdOpt, name, startTime, userActor, roomIdOpt, passwordOpt) =>
          log.debug(s"joinRoom ${tankIdOpt}")
          gameContainer.joinGame(uid, tankIdOpt, name, userActor)
          //这一桢结束时会告诉所有新加入用户的tank信息以及地图全量数据
          userGroup.get(index%classify) match {
            case Some(s)=> userGroup.update(index%classify,s+uid)
            case None => userGroup.put(index%classify,Set(uid))
          }
          idle(index + 1, roomId, (uid, tankIdOpt, startTime, userActor) :: justJoinUser, (uid, tankIdOpt, name, startTime, index%classify) :: userMap,userGroup, subscribersMap, observersMap, gameContainer, tickCount)

        case BotJoinRoom(bid, tankIdOpt, name, startTime, botActor, roomId)=>
          gameContainer.botJoinGame(bid, tankIdOpt, name, botActor, ctx.self)
          idle(index,roomId,justJoinUser,userMap,userGroup,subscribersMap,observersMap,gameContainer,tickCount)

        case JoinRoom4Watch(uid, _, playerId, userActor4Watch) =>
          log.debug(s"${ctx.self.path} recv a msg=${msg}")
          observersMap.put(uid, userActor4Watch)
          gameContainer.handleJoinRoom4Watch(userActor4Watch, uid, playerId)
          userGroup.get(index%classify) match {
            case Some(s)=> userGroup.update(index%classify,s+uid)
            case None => userGroup.put(index%classify,Set(uid))
          }
          idle(index+1,roomId,justJoinUser,userMap,userGroup,subscribersMap,observersMap,gameContainer,tickCount)

        case WebSocketMsg(uid, tankId, req) =>
          gameContainer.receiveUserAction(req)
          Behaviors.same

        case LeftRoom(uid, tankId, name, uidSet, roomId) =>
          log.debug(s"roomActor left room:${uid}")
          //上报战绩
            if (userMap.exists(_._1 == uid) && gameContainer.tankMap.exists(_._2.userId == uid)) {
              val startTime = userMap.filter(_._1 == uid).head._4
              val tank = gameContainer.tankMap.filter(_._2.userId == uid).head._2
              if (!uid.contains(Constants.TankGameUserIdPrefix)) {
                val endTime = System.currentTimeMillis()
                val killed = gameContainer.config.getTankLivesLimit - tank.lives
                log.debug(s"input record ${EsheepSyncClient.InputRecord(uid, name, tank.killTankNum, killed, tank.damageStatistics, startTime, endTime)}")
                esheepSyncClient ! EsheepSyncClient.InputRecord(uid, name, tank.killTankNum, killed, tank.damageStatistics, startTime, endTime)
              }
            }
          subscribersMap.remove(uid)
          gameContainer.leftGame(uid, name, tankId)
          userMap.filter(_._1==uid).foreach{u=>
            userGroup.get(u._5) match {
              case Some(s)=> userGroup.update(u._5,s-u._1)
              case None=> userGroup.put(u._5,Set.empty)
            }
          }
          if (uidSet.isEmpty) {
            if (roomId > 1l) {
              Behaviors.stopped
            } else {
              idle(index,roomId, justJoinUser.filter(_._1 != uid), userMap.filter(_._1 != uid),userGroup, subscribersMap, observersMap, gameContainer, tickCount)
            }
          } else {
            idle(index,roomId, justJoinUser.filter(_._1 != uid), userMap.filter(_._1 != uid), userGroup,subscribersMap, observersMap, gameContainer, tickCount)
          }

        case BotLeftRoom(uid, tankId, name, roomId)=>
          gameContainer.leftGame(uid, name, tankId)
          Behaviors.same

        case LeftRoom4Watch(uid, playerId) =>
          gameContainer.leftWatchGame(uid, playerId)
          observersMap.remove(uid)
          userMap.filter(_._1==uid).foreach{u=>
            userGroup.get(u._5) match {
              case Some(s)=> userGroup.update(u._5,s-u._1)
              case None=> userGroup.put(u._5,Set.empty)
            }
          }
          Behaviors.same

        case LeftRoomByKilled(uid, tankId, tankLives, name) =>
          log.debug("LeftRoomByKilled")
          subscribersMap.remove(uid)
          userMap.filter(_._1==uid).foreach{u=>
            userGroup.get(u._5) match {
              case Some(s)=> userGroup.update(u._5,s-u._1)
              case None=> userGroup.put(u._5,Set.empty)
            }
          }
          idle(index,roomId, justJoinUser.filter(_._1 != uid), userMap.filter(_._1 != uid), userGroup,subscribersMap, observersMap, gameContainer, tickCount)


        case GameLoop =>
          val snapshotOpt = gameContainer.getCurSnapshot()

          //生成坦克
          gameContainer.update()

          val gameEvents = gameContainer.getLastGameEvent()
          if (AppSettings.gameRecordIsWork) {
            getGameRecorder(ctx, gameContainer, roomId, gameContainer.systemFrame) ! GameRecorder.GameRecord(gameEvents, snapshotOpt)
          }
          //remind 错峰发送
          val state = gameContainer.getGameContainerState()
          userGroup.get(tickCount%classify).foreach{s=>
            if(s.nonEmpty){
              dispatch(subscribersMap.filter(r=>s.contains(r._1)), observersMap.filter(r=>s.contains(r._1)))(TankGameEvent.SyncGameState(state))
            }
          }
          for(i <- (tickCount % syncFrameOnly) * (classify / syncFrameOnly) until (tickCount % syncFrameOnly + 1) * (classify / syncFrameOnly)){
            userGroup.get(i).foreach{s =>
              if(s.nonEmpty){
                dispatch(subscribersMap.filter(r=>s.contains(r._1)), observersMap.filter(r=>s.contains(r._1)))(TankGameEvent.SyncGameState(gameContainer.getGameContainerState(true)))
              }
            }
          }
          if(justJoinUser.nonEmpty){
            val gameContainerAllState = gameContainer.getGameContainerAllState()
            val tankFollowEventSnap = gameContainer.getFollowEventSnap()
            justJoinUser.foreach { t =>
              subscribersMap.put(t._1, t._4)
              val ls = gameContainer.getUserActor4WatchGameList(t._1)
              dispatchTo(subscribersMap, observersMap)(t._1, tankFollowEventSnap, ls)
              dispatchTo(subscribersMap, observersMap)(t._1, TankGameEvent.SyncGameAllState(gameContainerAllState), ls)
              timer.startSingleTimer(s"newComer${t._1}",SyncFrame4NewComer(t._1,1),100.millis)

            }
          }
          //remind 控制人数
          if(tickCount%20==0){
            if(AppSettings.botSupport)
            botManager ! BotManager.SysUserSize(roomId,gameContainer.tankMap.size,gameContainer)
          }

          idle(index,roomId, Nil, userMap,userGroup, subscribersMap, observersMap, gameContainer, tickCount + 1)

        case SyncFrame4NewComer(userId,tickCount) =>
          if(tickCount < 21){//新加入的玩家前2s高频率同步帧号
            dispatchTo(subscribersMap, observersMap)(userId, TankGameEvent.SyncGameState(gameContainer.getGameContainerState(true)), gameContainer.getUserActor4WatchGameList(userId))
            timer.startSingleTimer(s"newComer${userId}",SyncFrame4NewComer(userId,tickCount + 1),100.millis)
          }

          Behaviors.same

        case ChildDead(name, childRef) =>
          //          log.debug(s"${ctx.self.path} recv a msg:${msg}")
          ctx.unwatch(childRef)
          Behaviors.same

        case TankRelive(userId, tankIdOpt, name) =>
          log.debug(s"${userId}")
          gameContainer.handleTankRelive(userId, tankIdOpt, name)
          val state = gameContainer.getGameContainerState()
          dispatch(subscribersMap, observersMap)(TankGameEvent.SyncGameState(state))
          Behaviors.same

        case m:GetSyncState=>
          val state = gameContainer.getGameContainerState()
          dispatchOnlyTo(subscribersMap, observersMap,m.uid, TankGameEvent.SyncGameState(state))
          Behaviors.same

        case _ =>
          log.warn(s"${ctx.self.path} recv a unknow msg=${msg}")
          Behaviors.same


      }
    }

  }

  import org.seekloud.byteobject.ByteObject._

  import scala.language.implicitConversions

  def dispatch(subscribers: mutable.HashMap[String, ActorRef[UserActor.Command]], observers: mutable.HashMap[String, ActorRef[UserActor.Command]])(msg: TankGameEvent.WsMsgServer)(implicit sendBuffer: MiddleBufferInJvm) = {
    val isKillMsg = msg.isInstanceOf[TankGameEvent.YouAreKilled]
    subscribers.values.foreach(_ ! UserActor.DispatchMsg(TankGameEvent.Wrap(msg.asInstanceOf[TankGameEvent.WsMsgServer].fillMiddleBuffer(sendBuffer).result(), isKillMsg)))
    observers.values.foreach(_ ! UserActor.DispatchMsg(TankGameEvent.Wrap(msg.asInstanceOf[TankGameEvent.WsMsgServer].fillMiddleBuffer(sendBuffer).result(), isKillMsg)))
  }

  def dispatchTo(subscribers: mutable.HashMap[String, ActorRef[UserActor.Command]], observers: mutable.HashMap[String, ActorRef[UserActor.Command]])(id: String, msg: TankGameEvent.WsMsgServer, observersByUserId: Option[mutable.HashMap[String, ActorRef[UserActor.Command]]])(implicit sendBuffer: MiddleBufferInJvm) = {
    //    println(s"$id--------------${msg.getClass}")
    msg match {
      case k: TankGameEvent.YouAreKilled =>
        subscribers.get(id).foreach(_ ! UserActor.InputRecordByDead(k.killTankNum, k.lives, k.damageStatistics))

      case _ =>
    }

    val isKillMsg = msg.isInstanceOf[TankGameEvent.YouAreKilled]
    subscribers.get(id).foreach(_ ! UserActor.DispatchMsg(TankGameEvent.Wrap(msg.asInstanceOf[TankGameEvent.WsMsgServer].fillMiddleBuffer(sendBuffer).result(), isKillMsg)))

    /**
      * 分发数据
      **/

    observersByUserId match {
      case Some(ls) => ls.keys.foreach(uId4WatchGame => observers.get(uId4WatchGame).foreach(t => t ! UserActor.DispatchMsg(TankGameEvent.Wrap(msg.asInstanceOf[TankGameEvent.WsMsgServer].fillMiddleBuffer(sendBuffer).result(), isKillMsg))))
      case None =>
    }
    //    observers.get(id).foreach(_ ! UserActor4WatchGame.DispatchMsg(TankGameEvent.Wrap(msg.asInstanceOf[TankGameEvent.WsMsgServer].fillMiddleBuffer(sendBuffer).result(),isKillMsg)))
  }

  def dispatchOnlyTo(subscribers: mutable.HashMap[String, ActorRef[UserActor.Command]], observers: mutable.HashMap[String, ActorRef[UserActor.Command]], id: String, msg: TankGameEvent.WsMsgServer)(implicit sendBuffer: MiddleBufferInJvm) = {
    //    println(s"$id--------------${msg.getClass}")
    subscribers.get(id).foreach(_ ! UserActor.DispatchMsg(TankGameEvent.Wrap(msg.asInstanceOf[TankGameEvent.WsMsgServer].fillMiddleBuffer(sendBuffer).result(), false)))
    observers.get(id).foreach(_ ! UserActor.DispatchMsg(TankGameEvent.Wrap(msg.asInstanceOf[TankGameEvent.WsMsgServer].fillMiddleBuffer(sendBuffer).result(), false)))
  }


  private def getGameRecorder(ctx: ActorContext[Command], gameContainer: GameContainerServerImpl, roomId: Long, frame: Long): ActorRef[GameRecorder.Command] = {
    val childName = s"gameRecorder"
    ctx.child(childName).getOrElse {
      val curTime = System.currentTimeMillis()
      val fileName = s"tankGame_${curTime}"
      val gameInformation = TankGameEvent.GameInformation(curTime, AppSettings.tankGameConfig.getTankGameConfigImpl())
      val initStateOpt = Some(gameContainer.getCurGameSnapshot())
      val actor = ctx.spawn(GameRecorder.create(fileName, gameInformation, initStateOpt, roomId), childName)
      ctx.watchWith(actor, ChildDead(childName, actor))
      actor
    }.upcast[GameRecorder.Command]
  }


}
