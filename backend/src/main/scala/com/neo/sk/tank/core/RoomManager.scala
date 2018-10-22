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

  case class LeftRoom(uid:Long,tankId:Int,name:String,userOpt: Option[Long]) extends Command

  def create():Behavior[Command] = {
    log.debug(s"RoomManager start...")
    Behaviors.setup[Command]{
      ctx =>
        implicit val stashBuffer = StashBuffer[Command](Int.MaxValue)
        Behaviors.withTimers[Command]{implicit timer =>
          val roomIdGenerator = new AtomicLong(1L)
          idle(roomIdGenerator,mutable.HashMap.empty[Long,List[(Long,String,Boolean)]])
        }
    }
  }

  def idle(roomIdGenerator:AtomicLong,
           roomInUse:mutable.HashMap[Long,List[(Long,String,Boolean)]])
          (implicit stashBuffer: StashBuffer[Command],timer:TimerScheduler[Command]) = {
    Behaviors.receive[Command]{(ctx,msg) =>
      msg match {
        case JoinRoom(uid,tankIdOpt,name,userActor) =>
          roomInUse.map{p =>(p._1,p._2.exists(t => t._1 == uid),p._2)}
            .find(_._2 == true) match{
            case Some(tuple) =>
              //(roomId,isExist,(uid,name,isLived))
              //该玩家存在tuple._1房间中
              roomInUse.put(tuple._1,(uid,name,false) :: roomInUse(tuple._1).filterNot(_._1 == uid))
//              log.debug(s"enter repeatedly !!! user$uid :$name is already in ${tuple._1}")
              getRoomActor(ctx,tuple._1) ! RoomActor.JoinRoom(uid,tankIdOpt,name,userActor,tuple._1)
            case None =>
              //该玩家不在任何房间中
              roomInUse.filter(_._2.size < personLimit).toList.sortBy(_._1).headOption match{
                case Some(v) =>
                  //有可分发的房间
                  roomInUse.put(v._1,(uid,name,false)::roomInUse(v._1))
                  getRoomActor(ctx,v._1) ! RoomActor.JoinRoom(uid,tankIdOpt,name,userActor,v._1)
                case None =>
                  //新建房间
                  val roomId = roomIdGenerator.getAndIncrement()
                  roomInUse.put(roomId,List((uid,name,false)))
                  getRoomActor(ctx,roomId) ! RoomActor.JoinRoom(uid,tankIdOpt,name,userActor,roomId)
              }
          }
          log.debug(s"now roomInUse:$roomInUse")
          Behaviors.same


        case RoomActor.JoinRoom4Watch(uid,roomId,playerId,userActor4Watch) =>
          log.debug(s"${ctx.self.path} recv a msg=${msg}")
          roomInUse.get(roomId) match {
            case Some(set) =>
              set.exists(p => p._1 == playerId) match {
                case false => userActor4Watch ! UserActor.JoinRoomFail4Watch("您所观察的用户不在房间里")
                case _ => getRoomActor(ctx,roomId) ! RoomActor.JoinRoom4Watch(uid,roomId,playerId,userActor4Watch)
              }
            case None => userActor4Watch ! UserActor.JoinRoomFail4Watch("您所观察的房间不存在")
          }
          Behaviors.same

        case LeftRoom(uid,tankId,name,userOpt) =>
          roomInUse.map{p =>(p._1,p._2.exists(t => t._1 == uid),p._2)}
            .find(_._2 == true) match {
            case Some(v) =>
              //玩家断掉websocket
              userOpt match{
                case Some(uid) =>
                  roomInUse.put(v._1,v._3.filterNot(_._1 == uid))
                  getRoomActor(ctx,v._1) ! RoomActor.LeftRoom(uid,tankId,name,roomInUse(v._1),v._1)
                  if(roomInUse(v._1).isEmpty && v._1 > 1l)roomInUse.remove(v._1)
                  log.debug(s"玩家：${uid}--$name remember to come back!!!$roomInUse")
                case None =>
                //死亡时间超过房间预留限制时间
                  v._3.find(_._1 == uid) match{
                    case Some((uId,nickname,isDead)) =>
                      if(isDead){
                        roomInUse.put(v._1,v._3.filterNot(_._1 == uid))
                        getRoomActor(ctx,v._1) ! RoomActor.LeftRoom(uid,tankId,name,roomInUse(v._1),v._1)
                        if(roomInUse(v._1).isEmpty && v._1 > 1l) roomInUse.remove(v._1)
                        log.debug(s"$name $uid remember to come back!!!$roomInUse")
                      }else{
                        log.debug(s"$name $uid has been relive")
                      }
                    case None =>log.debug(s"表中没有存储该用户的信息")
                  }
              }
            case None =>log.debug(s"该玩家不存在房间中")
          }
          Behaviors.same

        case LeftRoomByKilled(uid,tankId,name) =>
          roomInUse.map{p =>(p._1,p._2.exists(t => t._1 == uid),p._2)}
            .find(_._2 == true) match{
            case Some(v) =>
              roomInUse.put(v._1,(uid,name,true) :: v._3.filterNot(_._1 == uid))
              getRoomActor(ctx,v._1) ! LeftRoomByKilled(uid,tankId,name)
              log.debug(s"I am so sorry that you $name $uid are killed, the timer is beginning....")
              timer.startSingleTimer("room_"+v._1+"uid"+uid,LeftRoom(uid,tankId,name,None),leftTime)
            case None => log.debug(s"this user doesn't exist")
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
