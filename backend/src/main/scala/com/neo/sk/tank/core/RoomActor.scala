package com.neo.sk.tank.core

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import akka.http.scaladsl.model.ws.Message
import akka.stream.scaladsl.Flow
import com.neo.sk.tank.core.tank.GridServerImpl
import com.neo.sk.tank.shared.ptcl.protocol.{WsFrontProtocol, WsProtocol}
import org.slf4j.LoggerFactory

import concurrent.duration._
import scala.collection.mutable
import com.neo.sk.tank.shared.ptcl

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

  case class JoinRoom(uid:Long,name:String,userActor:ActorRef[UserActor.Command]) extends Command

  case class WebSocketMsg(uid:Long,tankId:Int,req:WsFrontProtocol.TankAction) extends Command

  case class LeftRoom(uid:Long,tankId:Int,name:String) extends Command


  case object GameLoop extends Command

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

  def create():Behavior[Command] ={
    log.debug(s"RoomManager start...")
    Behaviors.setup[Command]{
      ctx =>
        Behaviors.withTimers[Command]{
          implicit timer =>
            val subscribersMap = mutable.HashMap[Long,ActorRef[UserActor.Command]]()
            val grid = new GridServerImpl(ctx,log,dispatch(subscribersMap),dispatchTo(subscribersMap),ptcl.model.Boundary.getBoundary)
            grid.obstaclesInit()
            timer.startPeriodicTimer(GameLoopKey,GameLoop,ptcl.model.Frame.millsAServerFrame.millis)
            idle(Nil,subscribersMap,grid,0L)
        }
    }
  }

  def idle(
            justJoinUser:List[(Long,ActorRef[UserActor.Command])],
            subscribersMap:mutable.HashMap[Long,ActorRef[UserActor.Command]],
            grid:GridServerImpl,
            tickCount:Long
          )(
            implicit timer:TimerScheduler[Command]
          ):Behavior[Command] = {
    Behaviors.receive{(ctx,msg) =>
      msg match {
        case JoinRoom(uid,name,userActor) =>
          grid.joinGame(uid,name,userActor)

          //这一桢结束时会告诉所有新加入用户的tank信息以及地图全量数据
          idle((uid,userActor) :: justJoinUser, subscribersMap, grid, tickCount)

        case WebSocketMsg(uid,tankId,req) =>
          grid.addAction(tankId,req)
          req match {
            case r:WsFrontProtocol.MouseMove => dispatch(subscribersMap)(WsProtocol.TankActionFrameMouse(tankId,grid.systemFrame,r))
            case r:WsFrontProtocol.MouseClick =>
            case r:WsFrontProtocol.PressKeyDown => dispatch(subscribersMap)(WsProtocol.TankActionFrameKeyDown(tankId,grid.systemFrame,r))
            case r:WsFrontProtocol.PressKeyUp => dispatch(subscribersMap)(WsProtocol.TankActionFrameKeyUp(tankId,grid.systemFrame,r))
            case r:WsFrontProtocol.GunDirectionOffset => dispatch(subscribersMap)(WsProtocol.TankActionFrameOffset(tankId,grid.systemFrame,r))
            case _ =>
          }



          Behaviors.same

        case LeftRoom(uid,tankId,name) =>
          subscribersMap.remove(uid)
          grid.leftGame(tankId)
          dispatch(subscribersMap)(WsProtocol.UserLeftRoom(tankId,name))
          idle(justJoinUser.filter(_._1 != uid),subscribersMap,grid,tickCount)

        case GameLoop =>
          val startTime = System.currentTimeMillis()

          grid.update()

          if (tickCount % 20 == 5) {
            val gridData = grid.getGridStateWithoutBullet()
            dispatch(subscribersMap)(WsProtocol.GridSyncState(gridData))
          }
          if(tickCount % 20 == 1){
            dispatch(subscribersMap)(WsProtocol.Ranks(grid.currentRank,grid.historyRank))
          }
          //分发新加入坦克的地图全量数据
          justJoinUser.foreach(t => subscribersMap.put(t._1,t._2))
          val gridState = grid.getGridState()
          justJoinUser.foreach{t =>
            dispatchTo(subscribersMap)(t._1,WsProtocol.GridSyncAllState(gridState))
          }
          val endTime = System.currentTimeMillis()
          if(tickCount % 100 == 2){
            log.debug(s"${ctx.self.path} curFrame=${grid.systemFrame} use time=${endTime-startTime}")
          }


          idle(Nil,subscribersMap,grid,tickCount+1)

        case TankFillABullet(tId) =>
//          log.debug(s"${ctx.self.path} recv a msg=${msg}")
          grid.tankFillABullet(tId)
          Behaviors.same

        case TankInvincible(tId) =>
          grid.tankInvincible(tId)
          dispatch(subscribersMap)(WsProtocol.TankInvincible(grid.systemFrame,tId))
          Behaviors.same

        case _ =>
          log.warn(s"${ctx.self.path} recv a unknow msg=${msg}")
          Behaviors.same


      }
    }

  }

  def dispatch(subscribers:mutable.HashMap[Long,ActorRef[UserActor.Command]])(msg:WsProtocol.WsMsgServer) = {
    subscribers.values.foreach( _ ! UserActor.DispatchMsg(msg))
  }

  def dispatchTo(subscribers:mutable.HashMap[Long,ActorRef[UserActor.Command]])(id:Long,msg:WsProtocol.WsMsgServer) = {
    subscribers.get(id).foreach( _ ! UserActor.DispatchMsg(msg))
  }







}
