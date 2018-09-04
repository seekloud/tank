package com.neo.sk.tank.core

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import akka.http.scaladsl.model.ws.Message
import akka.stream.scaladsl.Flow
import com.neo.sk.tank.common.AppSettings
import com.neo.sk.tank.core.game.GameContainerServerImpl
import com.neo.sk.tank.shared.protocol.TankGameEvent
import org.slf4j.LoggerFactory

import concurrent.duration._
import scala.collection.mutable
import com.neo.sk.tank.Boot.roomManager

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

  case class JoinRoom(uid:Long,name:String,userActor:ActorRef[UserActor.Command],roomId:Long) extends Command

  case class WebSocketMsg(uid:Long,tankId:Int,req:TankGameEvent.UserActionEvent) extends Command with RoomManager.Command

  case class LeftRoom(uid:Long,tankId:Int,name:String,uidSet:mutable.HashSet[Long],roomId:Long) extends Command with RoomManager.Command
  case class LeftRoomByKilled(uid:Long,tankId:Int,name:String) extends Command with RoomManager.Command

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
    log.debug(s"RoomManager start...")
    Behaviors.setup[Command]{
      ctx =>
        Behaviors.withTimers[Command]{
          implicit timer =>
            log.debug(s"111111111111111")
            val subscribersMap = mutable.HashMap[Long,ActorRef[UserActor.Command]]()
            val gameContainer = GameContainerServerImpl(AppSettings.tankGameConfig, ctx.self, timer, log,
              dispatch(subscribersMap),
              dispatchTo(subscribersMap)
            )
            if(AppSettings.gameRecordIsWork){
              getGameRecorder(ctx,gameContainer)
            }
            timer.startPeriodicTimer(GameLoopKey,GameLoop,gameContainer.config.frameDuration.millis)
            idle(Nil,subscribersMap,gameContainer,0L)
        }
    }
  }

  def idle(
            justJoinUser:List[(Long,ActorRef[UserActor.Command])],
            subscribersMap:mutable.HashMap[Long,ActorRef[UserActor.Command]],
            gameContainer:GameContainerServerImpl,
            tickCount:Long
          )(
            implicit timer:TimerScheduler[Command]
          ):Behavior[Command] = {
    Behaviors.receive{(ctx,msg) =>
      msg match {
        case JoinRoom(uid,name,userActor,roomId) =>
          println(s"join room in roomActor")
          gameContainer.joinGame(uid,name,userActor,roomId)
          //这一桢结束时会告诉所有新加入用户的tank信息以及地图全量数据
          idle((uid,userActor) :: justJoinUser, subscribersMap, gameContainer, tickCount)

        case WebSocketMsg(uid,tankId,req) =>
          gameContainer.receiveUserAction(req)
          Behaviors.same

        case LeftRoom(uid,tankId,name,uidSet,roomId) =>
          subscribersMap.remove(uid)
          gameContainer.leftGame(uid,name,tankId)
          roomManager ! RoomManager.LeftRoomSuccess(uidSet,name,ctx.self,roomId)
          if(uidSet.isEmpty){
            if(roomId > 1l) {
              ctx.unwatch(ctx.self)
              Behaviors.stopped
            }else{
              idle(justJoinUser.filter(_._1 != uid),subscribersMap,gameContainer,tickCount)
            }
          }else{
            idle(justJoinUser.filter(_._1 != uid),subscribersMap,gameContainer,tickCount)
          }

//          idle(justJoinUser.filter(_._1 != uid),subscribersMap,gameContainer,tickCount)

        case LeftRoomByKilled(uid,tankId,name) =>
          subscribersMap.remove(uid)
          idle(justJoinUser.filter(_._1 != uid),subscribersMap,gameContainer,tickCount)

        case GameLoop =>
          val startTime = System.currentTimeMillis()


          val record = gameContainer.getGameEventAndSnapshot()
          if(AppSettings.gameRecordIsWork){
            getGameRecorder(ctx,gameContainer) ! GameRecorder.GameRecord(record)
          }
          gameContainer.update()



          if (tickCount % 20 == 5) {
            val state = gameContainer.getGameContainerState()
            dispatch(subscribersMap)(TankGameEvent.SyncGameState(state))
          }
          if(tickCount % 20 == 1){
            dispatch(subscribersMap)(TankGameEvent.Ranks(gameContainer.currentRank,gameContainer.historyRank))
          }
          //分发新加入坦克的地图全量数据
          if(justJoinUser != Nil) println(justJoinUser)
          justJoinUser.foreach(t => subscribersMap.put(t._1,t._2))
          val gameContainerAllState = gameContainer.getGameContainerAllState()
          justJoinUser.foreach{t =>
            println(s"+++++++++++++++++++++$gameContainerAllState")
            dispatchTo(subscribersMap)(t._1,TankGameEvent.SyncGameAllState(gameContainerAllState))
          }
          val endTime = System.currentTimeMillis()
          if(tickCount % 100 == 2){
            log.debug(s"${ctx.self.path} curFrame=${gameContainer.systemFrame} use time=${endTime-startTime}")
          }
          idle(Nil,subscribersMap,gameContainer,tickCount+1)

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

  def dispatch(subscribers:mutable.HashMap[Long,ActorRef[UserActor.Command]])(msg:TankGameEvent.WsMsgServer) = {
//    println(s"+++++++++++++++++$msg")
    subscribers.values.foreach( _ ! UserActor.DispatchMsg(msg))
  }

  def dispatchTo(subscribers:mutable.HashMap[Long,ActorRef[UserActor.Command]])(id:Long,msg:TankGameEvent.WsMsgServer) = {
//    println(s"$id--------------$msg")
    subscribers.get(id).foreach( _ ! UserActor.DispatchMsg(msg))
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
