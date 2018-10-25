package com.neo.sk.tank.core

import java.util.concurrent.atomic.{AtomicInteger, AtomicLong}

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import com.neo.sk.tank.core.RoomActor.{LeftRoomByKilled, WebSocketMsg}
import com.neo.sk.tank.core.UserActor.{JoinRoom, TimeOut}
import com.neo.sk.tank.core.game.TankServerImpl
import com.neo.sk.tank.shared.config.TankGameConfigImpl
import org.slf4j.LoggerFactory
import com.neo.sk.tank.common.AppSettings.{leftTimeLimit, personLimit}

import scala.concurrent.duration._
import scala.collection.immutable.HashMap
import scala.collection.mutable
import scala.concurrent.duration.{Duration, FiniteDuration}
import akka.actor.typed.Behavior
import com.neo.sk.tank.protocol.WatchGameProtocol
import com.neo.sk.tank.protocol.WatchGameProtocol._
import com.neo.sk.tank.shared.model.Constants.GameState
import com.neo.sk.tank.shared.ptcl.CommonRsp
import com.neo.sk.utils.TimeUtil

import scala.concurrent.duration._
import scala.util.{Failure, Success}

object RoomManager {
  private val log = LoggerFactory.getLogger(this.getClass)
  private final val leftTime = leftTimeLimit.minutes
  private case object LeftRoomKey

  trait Command
  private final case object BehaviorChangeKey
  private case class TimeOut(msg:String) extends Command
  private case class ChildDead[U](name:String,childRef:ActorRef[U]) extends Command

  case class LeftRoom(uid:String,tankId:Int,name:String,userOpt: Option[String]) extends Command

  def create():Behavior[Command] = {
    log.debug(s"RoomManager start...")
    Behaviors.setup[Command]{
      ctx =>
        implicit val stashBuffer = StashBuffer[Command](Int.MaxValue)
        Behaviors.withTimers[Command]{implicit timer =>
          val roomIdGenerator = new AtomicLong(1L)
          val roomInUse = mutable.HashMap((1l,List.empty[(String,String)]))
          idle(roomIdGenerator,roomInUse)
        }
    }
  }

  def idle(roomIdGenerator:AtomicLong,
           roomInUse:mutable.HashMap[Long,List[(String,String)]]) // roomId => List[userId, userName]
          (implicit stashBuffer: StashBuffer[Command],timer:TimerScheduler[Command]) = {
    Behaviors.receive[Command]{(ctx,msg) =>
      msg match {
        case JoinRoom(uid,tankIdOpt,gameStateOpt,name,startTime,userActor, roomIdOpt) =>
          roomIdOpt match{
            case Some(roomId) =>
              roomInUse.get(roomId) match{
                case Some(ls) => roomInUse.put(roomId,(uid,name) :: ls)
                case None => roomInUse.put(roomId,List((uid,name)))
              }
              getRoomActor(ctx,roomId) ! RoomActor.JoinRoom(uid,tankIdOpt,name,startTime,userActor,roomId)
            case None =>
              gameStateOpt match{
                case Some(GameState.relive) =>
                  roomInUse.find(_._2.exists(_._1 == uid)) match{
                    case Some(t) =>getRoomActor(ctx,t._1) ! RoomActor.JoinRoom(uid,tankIdOpt,name,startTime,userActor,t._1)
                    case None =>log.debug(s"${ctx.self.path} error:tank relives, but find no room")
                  }
                case _ =>
                  roomInUse.find(p => p._2.length < personLimit).toList.sortBy(_._1).headOption match{
                    case Some(t) =>
                      roomInUse.put(t._1,(uid,name) :: t._2)
                      getRoomActor(ctx,t._1) ! RoomActor.JoinRoom(uid,tankIdOpt,name,startTime,userActor,t._1)
                    case None =>
                      var roomId = roomIdGenerator.getAndIncrement()
                      while(roomInUse.exists(_._1 == roomId))roomId = roomIdGenerator.getAndIncrement()
                      roomInUse.put(roomId,List((uid,name)))
                      getRoomActor(ctx,roomId) ! RoomActor.JoinRoom(uid,tankIdOpt,name,startTime,userActor,roomId)
                  }
              }

          }
          log.debug(s"now roomInUse:$roomInUse")
          Behaviors.same


        case RoomActor.JoinRoom4Watch(uid,roomId,playerId,userActor4Watch) =>
          log.debug(s"${ctx.self.path} recv a msg=${msg}")
          roomInUse.get(roomId) match {
            case Some(set) =>
              set.exists(p => p._1 == playerId) match {
                case false =>
                  log.debug(s"玩家不在房间中 ${roomId},${playerId}")
                  userActor4Watch ! UserActor.JoinRoomFail4Watch("您所观察的用户不在房间里")
                case _ => getRoomActor(ctx,roomId) ! RoomActor.JoinRoom4Watch(uid,roomId,playerId,userActor4Watch)
              }
            case None => userActor4Watch ! UserActor.JoinRoomFail4Watch("您所观察的房间不存在")
          }
          Behaviors.same

        case LeftRoom(uid,tankId,name,userOpt) =>
          roomInUse.find(_._2.exists(_._1 == uid)) match{
            case Some(t) =>
              roomInUse.put(t._1,t._2.filterNot(_._1 == uid))
              getRoomActor(ctx,t._1) ! RoomActor.LeftRoom(uid,tankId,name,roomInUse(t._1),t._1)
              if(roomInUse(t._1).isEmpty && t._1 > 1l)roomInUse.remove(t._1)
              log.debug(s"玩家：${uid}--$name remember to come back!!!$roomInUse")
            case None => log.debug(s"该玩家不在任何房间")
          }
          Behaviors.same

        case LeftRoomByKilled(uid,tankId,tankLives,name) =>
          roomInUse.find(_._2.exists(_._1 == uid)) match{
            case Some(t) =>
              log.debug(s"${ctx.self.path} name:${name} lives ${tankLives}")
              if(tankLives <= 0){
                roomInUse.put(t._1,t._2.filterNot(_._1 == uid))
              }
              getRoomActor(ctx,t._1) ! LeftRoomByKilled(uid,tankId,tankLives,name)
            case None =>log.debug(s"this user doesn't exist")
          }
          Behaviors.same

        case WatchGameProtocol.GetRoomId(playerId,replyTo) =>
          roomInUse.map{p =>(p._1,p._2.exists(t => t._1 == playerId))}
            .find(_._2 == true) match{
            case Some((roomId,playerIsIn)) =>replyTo ! WatchGameProtocol.RoomIdRsp(WatchGameProtocol.RoomInfo(roomId))
            case None =>replyTo ! WatchGameProtocol.RoomIdRsp(WatchGameProtocol.RoomInfo(-1),errCode = 100005,msg = "this player exists no room")
          }
          Behaviors.same

        case GetUserInfoList(roomId,replyTo) =>
          roomInUse.find(_._1 == roomId) match{
            case Some(tuple) => replyTo ! UserInfoListByRoomIdRsp(UserInfoList(tuple._2.map{ t => UserInfo(t._1,t._2)}.toList))
            case None => replyTo ! UserInfoListByRoomIdRsp(UserInfoList(Nil),errCode = 100006,msg = "该房间未被创建")
          }
          Behaviors.same

        case GetRoomListReq(replyTo) =>
          replyTo ! RoomListRsp(RoomList(roomInUse.keys.toList))
          Behaviors.same

        case ChildDead(child,childRef) =>
          log.debug(s"roomManager 不再监管room:$child,$childRef")
          ctx.unwatch(childRef)
          Behaviors.same

        case unknow =>
          Behaviors.same
      }
    }
  }

  private def getRoomActor(ctx:ActorContext[Command],roomId:Long) = {
    val childName = s"room_$roomId"
    ctx.child(childName).getOrElse{
      val actor = ctx.spawn(RoomActor.create(roomId),childName)
      ctx.watchWith(actor,ChildDead(childName,actor))
      actor

    }.upcast[RoomActor.Command]
  }

}
