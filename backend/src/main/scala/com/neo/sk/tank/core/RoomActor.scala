package com.neo.sk.tank.core

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import akka.http.scaladsl.model.ws.Message
import akka.stream.scaladsl.Flow
import com.neo.sk.tank.common.AppSettings
import com.neo.sk.tank.core.RoomManager.Command
import com.neo.sk.tank.core.game.GameContainerServerImpl
import com.neo.sk.tank.shared.protocol.TankGameEvent
import org.slf4j.LoggerFactory
//import com.neo.sk.tank.core.game.GameContainerServerImpl

import concurrent.duration._
import scala.collection.mutable
import com.neo.sk.tank.Boot.roomManager
import org.seekloud.byteobject.MiddleBufferInJvm

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
  private final case object BehaviorChangeKey
  private final case object GameLoopKey

  sealed trait Command

  case class JoinRoom(uid:Long,tankIdOpt:Option[Int],name:String,userActor:ActorRef[UserActor.Command],roomId:Long) extends Command

  case class WebSocketMsg(uid:Long,tankId:Int,req:TankGameEvent.UserActionEvent) extends Command with RoomManager.Command

  case class LeftRoom(uid:Long,tankId:Int,name:String,uidSet:mutable.HashSet[(Long,Boolean)],roomId:Long) extends Command with RoomManager.Command
  case class LeftRoomByKilled(uid:Long,tankId:Int,name:String) extends Command with RoomManager.Command
  case class LeftRoom4Watch(uid:Long) extends Command with RoomManager.Command
  case class JoinRoom4Watch(uid:Long,roomId:Int,playerId:Long,userActor4Watch: ActorRef[UserActor4WatchGame.Command]) extends Command with  RoomManager.Command with UserActor4WatchGame.Command
  final case class ChildDead[U](name:String,childRef:ActorRef[U]) extends Command with RoomManager.Command
  case object GameLoop extends Command
  case class ShotgunExpire(tId:Int) extends Command
  case class TankFillABullet(tId:Int) extends Command
  case class TankInvincible(tId:Int)extends  Command


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

  def create(roomId:Long):Behavior[Command] ={
    log.debug(s"Room Actor-${roomId} start...")
    Behaviors.setup[Command]{
      ctx =>
        Behaviors.withTimers[Command]{
          implicit timer =>
            val subscribersMap = mutable.HashMap[Long,ActorRef[UserActor.Command]]()
            val observersMap = mutable.HashMap[Long,ActorRef[UserActor4WatchGame.Command]]()
            implicit val sendBuffer = new MiddleBufferInJvm(81920)
            val gameContainer = GameContainerServerImpl(AppSettings.tankGameConfig, ctx.self, timer, log,
              dispatch(subscribersMap,observersMap),
              dispatchTo(subscribersMap,observersMap)
            )
            if(AppSettings.gameRecordIsWork){
              getGameRecorder(ctx,gameContainer)
            }
            timer.startPeriodicTimer(GameLoopKey,GameLoop,gameContainer.config.frameDuration.millis)
            idle(Nil,subscribersMap,observersMap,gameContainer,0L)
        }
    }
  }

  def idle(
            justJoinUser:List[(Long,Option[Int],ActorRef[UserActor.Command])],
            subscribersMap:mutable.HashMap[Long,ActorRef[UserActor.Command]],
            observersMap:mutable.HashMap[Long,ActorRef[UserActor4WatchGame.Command]],
            gameContainer:GameContainerServerImpl,
            tickCount:Long
          )(
            implicit timer:TimerScheduler[Command],
            sendBuffer:MiddleBufferInJvm
          ):Behavior[Command] = {
    Behaviors.receive{(ctx,msg) =>
      msg match {
        case JoinRoom(uid,tankIdOpt,name,userActor,roomId) =>
          gameContainer.joinGame(uid,tankIdOpt,name,userActor)
          //这一桢结束时会告诉所有新加入用户的tank信息以及地图全量数据
          idle((uid,tankIdOpt,userActor) :: justJoinUser, subscribersMap, observersMap,gameContainer, tickCount)

        case JoinRoom4Watch(uid,roomId,playerId,userActor4Watch) =>
          observersMap.put(uid,userActor4Watch)
          val gameContainerAllState = gameContainer.getGameContainerAllState()
          gameContainer.handleJoinRoom4Watch(userActor4Watch,playerId,gameContainerAllState)
          Behaviors.same

        case WebSocketMsg(uid,tankId,req) =>
          gameContainer.receiveUserAction(req)
          Behaviors.same

        case LeftRoom(uid,tankId,name,uidSet,roomId) =>
          subscribersMap.remove(uid)
          gameContainer.leftGame(uid,name,tankId)
          if(uidSet.isEmpty){
            if(roomId > 1l) {
              Behaviors.stopped
            }else{
              idle(justJoinUser.filter(_._1 != uid),subscribersMap,observersMap,gameContainer,tickCount)
            }
          }else{
            idle(justJoinUser.filter(_._1 != uid),subscribersMap,observersMap,gameContainer,tickCount)
          }
        case LeftRoom4Watch(uid) =>
          observersMap.remove(uid)
          Behaviors.same

        case LeftRoomByKilled(uid,tankId,name) =>
//          gameContainer.tankL
          subscribersMap.remove(uid)
          idle(justJoinUser.filter(_._1 != uid),subscribersMap,observersMap,gameContainer,tickCount)

        case GameLoop =>
          val startTime = System.currentTimeMillis()


          val record = gameContainer.getGameEventAndSnapshot()
          if(AppSettings.gameRecordIsWork){
            getGameRecorder(ctx,gameContainer) ! GameRecorder.GameRecord(record)
          }
          //生成坦克
          gameContainer.update()



          if (tickCount % 20 == 5) {
            val state = gameContainer.getGameContainerState()
            dispatch(subscribersMap,observersMap)(TankGameEvent.SyncGameState(state))
          }
          if(tickCount % 20 == 1){
            dispatch(subscribersMap,observersMap)(TankGameEvent.Ranks(gameContainer.currentRank,gameContainer.historyRank))
          }
          //分发新加入坦克的地图全量数据
          justJoinUser.foreach(t => subscribersMap.put(t._1,t._3))
          val gameContainerAllState = gameContainer.getGameContainerAllState()
          justJoinUser.foreach{t =>
            dispatchTo(subscribersMap,observersMap)(t._1,TankGameEvent.SyncGameAllState(gameContainerAllState))
          }
          val endTime = System.currentTimeMillis()
          if(tickCount % 100 == 2){
            log.debug(s"${ctx.self.path} curFrame=${gameContainer.systemFrame} use time=${endTime-startTime}")
          }
          idle(Nil,subscribersMap,observersMap,gameContainer,tickCount+1)

        case TankFillABullet(tId) =>
          //          log.debug(s"${ctx.self.path} recv a msg=${msg}")
          gameContainer.receiveGameEvent(TankGameEvent.TankFillBullet(tId,gameContainer.systemFrame))
          Behaviors.same


        case ChildDead(name, childRef) =>
          log.debug(s"${ctx.self.path} recv a msg:${msg}")
          ctx.unwatch(childRef)
          Behaviors.same


        case TankInvincible(tId) =>
          gameContainer.receiveGameEvent(TankGameEvent.TankInvincible(tId,gameContainer.systemFrame))
          Behaviors.same

        case ShotgunExpire(tId) =>
          gameContainer.receiveGameEvent(TankGameEvent.TankShotgunExpire(tId,gameContainer.systemFrame))
          Behaviors.same


        case _ =>
          log.warn(s"${ctx.self.path} recv a unknow msg=${msg}")
          Behaviors.same


      }
    }

  }
  import scala.language.implicitConversions
  import org.seekloud.byteobject.ByteObject._

  def dispatch(subscribers:mutable.HashMap[Long,ActorRef[UserActor.Command]],observers:mutable.HashMap[Long,ActorRef[UserActor4WatchGame.Command]])( msg:TankGameEvent.WsMsgServer)(implicit sendBuffer:MiddleBufferInJvm) = {
//    println(s"+++++++++++++++++$msg")
    val isKillMsg = msg.isInstanceOf[TankGameEvent.YouAreKilled]
    subscribers.values.foreach( _ ! UserActor.DispatchMsg(TankGameEvent.Wrap(msg.asInstanceOf[TankGameEvent.WsMsgServer].fillMiddleBuffer(sendBuffer).result(),isKillMsg)))
    observers.values.foreach(_ ! UserActor4WatchGame.DispatchMsg(TankGameEvent.Wrap(msg.asInstanceOf[TankGameEvent.WsMsgServer].fillMiddleBuffer(sendBuffer).result(),isKillMsg)))
  }

  def dispatchTo(subscribers:mutable.HashMap[Long,ActorRef[UserActor.Command]],observers:mutable.HashMap[Long,ActorRef[UserActor4WatchGame.Command]])( id:Long,msg:TankGameEvent.WsMsgServer)(implicit sendBuffer:MiddleBufferInJvm) = {
//    println(s"$id--------------$msg")

    val isKillMsg = msg.isInstanceOf[TankGameEvent.YouAreKilled]
    subscribers.get(id).foreach( _ ! UserActor.DispatchMsg(TankGameEvent.Wrap(msg.asInstanceOf[TankGameEvent.WsMsgServer].fillMiddleBuffer(sendBuffer).result(),isKillMsg)))
    observers.get(id).foreach(_ ! UserActor4WatchGame.DispatchMsg(TankGameEvent.Wrap(msg.asInstanceOf[TankGameEvent.WsMsgServer].fillMiddleBuffer(sendBuffer).result(),isKillMsg)))
  }


    private def getGameRecorder(ctx: ActorContext[Command],gameContainer:GameContainerServerImpl):ActorRef[GameRecorder.Command] = {
      val childName = s"gameRecorder"
      ctx.child(childName).getOrElse{
        val curTime = System.currentTimeMillis()
        val fileName = s"tankGame_${curTime}"
        val gameInformation = TankGameEvent.GameInformation(curTime)
        val initStateOpt = Some(gameContainer.getCurGameSnapshot())
        val actor = ctx.spawn(GameRecorder.create(fileName,gameInformation,initStateOpt),childName)
        ctx.watchWith(actor,ChildDead(childName,actor))
        actor
      }.upcast[GameRecorder.Command]
    }







}
