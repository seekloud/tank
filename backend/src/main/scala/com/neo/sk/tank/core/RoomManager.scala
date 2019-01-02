package com.neo.sk.tank.core

import java.util.concurrent.atomic.{AtomicInteger, AtomicLong}

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import com.neo.sk.tank.core.RoomActor.{LeftRoomByKilled, WebSocketMsg}
import com.neo.sk.tank.core.UserActor.{JoinRoom, TimeOut}
import com.neo.sk.tank.core.game.TankServerImpl
import com.neo.sk.tank.shared.config.TankGameConfigImpl
import org.slf4j.LoggerFactory
import com.neo.sk.tank.common.AppSettings.personLimit

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
          val password:Option[String] = None
          val roomInUse = mutable.HashMap((1l,(password,List.empty[(String,String)])))
          idle(roomIdGenerator,roomInUse)
        }
    }
  }

  def idle(roomIdGenerator:AtomicLong,
           roomInUse:mutable.HashMap[Long,(Option[String],List[(String,String)])]) // roomId => (password,List[userId, userName])
          (implicit stashBuffer: StashBuffer[Command],timer:TimerScheduler[Command]) = {
    Behaviors.receive[Command]{(ctx,msg) =>
      msg match {
        case JoinRoom(uid,tankIdOpt,name,startTime,userActor, roomIdOpt,passwordOpt) =>
          roomIdOpt match{
            case Some(roomId) =>
              roomInUse.get(roomId) match{
                case Some(ls) =>
                 if(passwordOpt == ls._1){
                   roomInUse.put(roomId,(passwordOpt,(uid,name):: ls._2))
                 } else{
                   //密码不正确
                 }

                case None => roomInUse.put(roomId,(None,List((uid,name))))
              }
              getRoomActor(ctx,roomId) ! RoomActor.JoinRoom(uid,tankIdOpt,name,startTime,userActor,roomId)
            case None =>
              roomInUse.find(p => p._2._2.length < personLimit).toList.sortBy(_._1).headOption match{
                case Some(t) =>
                  roomInUse.put(t._1,(t._2._1,(uid,name) :: t._2._2))
                  getRoomActor(ctx,t._1) ! RoomActor.JoinRoom(uid,tankIdOpt,name,startTime,userActor,t._1)
                case None =>
                  var roomId = roomIdGenerator.getAndIncrement()
                  while(roomInUse.exists(_._1 == roomId))roomId = roomIdGenerator.getAndIncrement()
                  roomInUse.put(roomId,(None,List((uid,name))))
                  getRoomActor(ctx,roomId) ! RoomActor.JoinRoom(uid,tankIdOpt,name,startTime,userActor,roomId)
              }
          }
          log.debug(s"now roomInUse:$roomInUse")
          Behaviors.same


        case RoomActor.JoinRoom4Watch(uid,roomId,playerId,userActor4Watch) =>
          log.debug(s"${ctx.self.path} recv a msg=${msg}")
          roomInUse.get(roomId) match {
            case Some(set) =>
              set._2.exists(p => p._1 == playerId) match {
                case false => userActor4Watch ! UserActor.JoinRoomFail4Watch("您所观察的用户不在房间里")
                case _ => getRoomActor(ctx,roomId) ! RoomActor.JoinRoom4Watch(uid,roomId,playerId,userActor4Watch)
              }
            case None => userActor4Watch ! UserActor.JoinRoomFail4Watch("您所观察的房间不存在")
          }
          Behaviors.same

        case LeftRoom(uid,tankId,name,userOpt) =>
          roomInUse.find(_._2._2.exists(_._1 == uid)) match{
            case Some(t) =>
              roomInUse.put(t._1,(t._2._1,t._2._2.filterNot(_._1 == uid)))
              getRoomActor(ctx,t._1) ! RoomActor.LeftRoom(uid,tankId,name,roomInUse(t._1)._2,t._1)
              if(roomInUse(t._1)._2.isEmpty && t._1 > 1l)roomInUse.remove(t._1)
              log.debug(s"玩家：${uid}--$name remember to come back!!!$roomInUse")
            case None => log.debug(s"该玩家不在任何房间")
          }
          Behaviors.same

        case LeftRoomByKilled(uid,tankId,tankLives,name) =>
          roomInUse.find(_._2._2.exists(_._1 == uid)) match{
            case Some(t) =>
              roomInUse.put(t._1,(t._2._1,t._2._2.filterNot(_._1 == uid)))
              getRoomActor(ctx,t._1) ! LeftRoomByKilled(uid,tankId,tankLives,name)
            case None =>log.debug(s"this user doesn't exist")
          }
          Behaviors.same

        case WatchGameProtocol.GetRoomId(playerId,replyTo) =>
          log.debug(s"请求房间id，${roomInUse}")
          roomInUse.map{p =>(p._1,p._2._2.exists(t => t._1 == playerId))}
            .find(_._2 == true) match{
            case Some((roomId,playerIsIn)) =>replyTo ! WatchGameProtocol.RoomIdRsp(WatchGameProtocol.RoomInfo(roomId))
            case None =>replyTo ! WatchGameProtocol.RoomIdRsp(WatchGameProtocol.RoomInfo(-1),errCode = 100005,msg = "this player exists no room")
          }
          Behaviors.same

        case GetUserInfoList(roomId,replyTo) =>
          roomInUse.find(_._1 == roomId) match{
            case Some(tuple) => replyTo ! UserInfoListByRoomIdRsp(UserInfoList(tuple._2._2.map{ t => UserInfo(t._1,t._2)}.toList))
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
