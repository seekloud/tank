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

import java.util.concurrent.atomic.AtomicLong

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import org.seekloud.tank.protocol.WatchGameProtocol._
import org.seekloud.tank.protocol.WatchGameProtocol
import org.slf4j.LoggerFactory

import concurrent.duration._
import org.seekloud.tank.Boot.executor
import org.seekloud.tank.common.AppSettings
import org.seekloud.tank.core.RoomActor.{BotJoinRoom, BotLeftRoom, LeftRoomByKilled}
import org.seekloud.tank.protocol.ActorProtocol.{JoinRoom}

import scala.collection.mutable

/**
  * Created by hongruying on 2019/2/3
  */
object RoomManager {
  private val log = LoggerFactory.getLogger(this.getClass)

  trait Command
  private case class TimeOut(msg:String) extends Command
  private case class ChildDead[U](name:String,childRef:ActorRef[U]) extends Command
  case class CreateRoom(uid:String,tankIdOpt:Option[Int],name:String,startTime:Long,userActor:ActorRef[UserActor.Command],roomId:Option[Long],password:Option[String]) extends Command
  case class LeftRoom(uid:String,tankId:Int,name:String,userOpt: Option[String]) extends Command


  def create():Behavior[Command] = {
    log.debug(s"RoomManager start...")
    Behaviors.setup[Command]{
      ctx =>
        implicit val stashBuffer = StashBuffer[Command](Int.MaxValue)
        Behaviors.withTimers[Command]{implicit timer =>
          val roomIdGenerator = new AtomicLong(1L)
          val roomInUse = mutable.HashMap((1l,("",List.empty[(String,String)])))
          idle(roomIdGenerator,roomInUse)
        }
    }
  }

  def idle(roomIdGenerator:AtomicLong,
           roomInUse:mutable.HashMap[Long,(String,List[(String,String)])]) // roomId => (password,List[userId, userName])
          (implicit stashBuffer: StashBuffer[Command],timer:TimerScheduler[Command]) = {
    Behaviors.receive[Command]{(ctx,msg) =>
      msg match {
        case JoinRoom(uid,tankIdOpt,name,startTime,userActor, roomIdOpt,passwordOpt) =>
          val pw = passwordOpt.getOrElse("")
          roomIdOpt match{
            case Some(roomId) =>
              roomInUse.get(roomId) match{
                case Some(ls) =>
                 if(pw == ls._1){
                   roomInUse.put(roomId,(pw,(uid,name):: ls._2))
                   getRoomActor(ctx,roomId) ! JoinRoom(uid,tankIdOpt,name,startTime,userActor,Some(roomId),passwordOpt)
                 } else{
                   //密码不正确
                   userActor ! UserActor.JoinRoomFail("密码错误！")
                 }
                case None => userActor ! UserActor.JoinRoomFail("房间未被创建！")
              }
            case None =>
              roomInUse.find(p => p._2._2.length < AppSettings.personLimit).toList.sortBy(_._1).headOption match{
                case Some(t) =>
                  roomInUse.put(t._1,(t._2._1,(uid,name) :: t._2._2))
                  getRoomActor(ctx,t._1) ! JoinRoom(uid,tankIdOpt,name,startTime,userActor,Some(t._1))
                case None =>
                  var roomId = roomIdGenerator.getAndIncrement()
                  while(roomInUse.exists(_._1 == roomId))roomId = roomIdGenerator.getAndIncrement()
                  roomInUse.put(roomId,("",List((uid,name))))
                  getRoomActor(ctx,roomId) ! JoinRoom(uid,tankIdOpt,name,startTime,userActor,Some(roomId))
              }
          }
          log.debug(s"now roomInUse:$roomInUse")
          Behaviors.same

        case CreateRoom(uid,tankIdOpt,name,startTime,userActor,roomIdOpt,passwordOpt) =>
          val roomId = if(roomIdOpt.nonEmpty) roomIdOpt.get else roomIdGenerator.getAndIncrement()
          roomInUse.put(roomId,(passwordOpt.getOrElse(""),List((uid,name))))
          getRoomActor(ctx,roomId) ! JoinRoom(uid,tankIdOpt,name,startTime,userActor,Some(roomId))
          Behaviors.same

        case msg:BotJoinRoom=>
          log.debug(s"before botJoin roomInUse:$roomInUse")
          roomInUse.get(msg.roomId) match{
            case Some(ls) => roomInUse.put(msg.roomId,(ls._1,(msg.bid,msg.name):: ls._2))
            case None => roomInUse.put(msg.roomId,("",List((msg.bid,msg.name))))
          }

          getRoomActor(ctx,msg.roomId) ! msg
          log.debug(s"${ctx.self.path}新加入bot${msg.bid}--${msg.name},now roomInUse:$roomInUse")
          Behaviors.same

        case msg:BotLeftRoom=>
          roomInUse.find(_._2._2.exists(_._1 == msg.uid)) match{
            case Some(t) =>
              roomInUse.put(t._1,(t._2._1,t._2._2.filterNot(_._1 == msg.uid)))
              getRoomActor(ctx,t._1) ! msg
              if(roomInUse(t._1)._2.isEmpty && t._1 > 1l)
                roomInUse.remove(t._1)
              log.debug(s"Bot：${msg.uid}--${msg.name} remember to come back!!!$roomInUse")
            case None => log.debug(s"该bot不在任何房间")
          }
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
              log.debug(s"${ctx.self.path}房间管理正在维护的信息${roomInUse}")
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
          replyTo ! RoomListRsp(RoomList(roomInUse.map{r =>
            r._1->(if(r._2._1 == "") false else true)
          }))
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
      val actor = ctx.spawn(RoomActor.create(roomId,50),childName)
      ctx.watchWith(actor,ChildDead(childName,actor))
      actor

    }.upcast[RoomActor.Command]
  }

}
