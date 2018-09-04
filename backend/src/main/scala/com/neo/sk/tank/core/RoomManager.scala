package com.neo.sk.tank.core

import java.util.concurrent.atomic.{AtomicInteger, AtomicLong}

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import com.neo.sk.tank.core.RoomActor.{LeftRoomByKilled, WebSocketMsg}
import com.neo.sk.tank.core.UserActor.{JoinRoom, TimeOut}
import com.neo.sk.tank.core.game.TankServerImpl
import com.neo.sk.tank.shared.config.TankGameConfigImpl
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.collection.immutable.HashMap
import scala.collection.mutable
import scala.concurrent.duration.{Duration, FiniteDuration}
import akka.actor.typed.Behavior
import com.neo.sk.utils.TimeUtil

import scala.concurrent.duration._
import scala.util.{Failure, Success}

object RoomManager {
  private val log = LoggerFactory.getLogger(this.getClass)
  private val personLimit = 10
  private final val leftTime = 5.minutes
  private case object LeftRoomKey
  private val roomInUse = mutable.HashMap[Long,mutable.HashSet[Long]]()//roomId->Set(uid)


  trait Command
  private final case object BehaviorChangeKey
  private case class TimeOut(msg:String) extends Command
  private case class ChildDead[U](name:String,childRef:ActorRef[U]) extends Command

//  case class LeftRoomByKilled(uid:Long,tankId:Int,name:String) extends Command
  case class LeftRoom(uid:Long,tankId:Int,name:String) extends Command
  case class LeftRoomSuccess(uidSet:mutable.HashSet[Long], name:String, actorRef: ActorRef[RoomActor.Command],roomId:Long) extends Command

  def create():Behavior[Command] = {
    Behaviors.setup[Command]{
      ctx =>
        implicit val stashBuffer = StashBuffer[Command](Int.MaxValue)
        Behaviors.withTimers[Command]{implicit timer =>
          val roomIdGenerator = new AtomicLong(1L)
          idle(roomIdGenerator)
        }
    }
  }

  def idle(roomIdGenerator:AtomicLong)(implicit stashBuffer: StashBuffer[Command],timer:TimerScheduler[Command]) = {
    Behaviors.receive[Command]{(ctx,msg) =>
      msg match {
        case JoinRoom(uid,name,userActor) =>
          /**
            * 1.新加入等待分配roomId
            *     --- 有可容纳的房间
            *     --- 没有可容纳的房间
            * 2. 重新回到原来游戏房间,更新定时器
            * 3. 等收到成功加入房间的消息再更新统计数据
            * */
          val roomExistUidMap = roomInUse.filter(p => p._2.exists(_ == uid))
          if(roomExistUidMap.size == 0){
            val roomCanBeUse = roomInUse.filter(_._2.size < personLimit)
            if(roomCanBeUse.size > 0){
              println(s"room already exists !!! user$uid :$name enter into ${roomCanBeUse.keys.min}")
              getRoomActor(ctx,roomCanBeUse.keys.min) ! RoomActor.JoinRoom(uid,name,userActor,roomCanBeUse.keys.min)
            }else{
              val roomId = roomIdGenerator.getAndIncrement()
              println(s"new room !!!! user$uid :$name enter into $roomId")
              getRoomActor(ctx,roomId) ! RoomActor.JoinRoom(uid,name,userActor,roomId)
            }
          }else{
            println(s"enter repeatedly !!! user$uid :$name is already in ${roomExistUidMap.head._1}")
            getRoomActor(ctx,roomExistUidMap.head._1) ! RoomActor.JoinRoom(uid,name,userActor,roomExistUidMap.head._1)
          }

          Behaviors.same

        case UserActor.JoinRoomSuccess(tank,config,userActor,uId,roomId) =>
          val roomExist = roomInUse.filter{u => u._1 == roomId}
          if(roomExist.exists(u => u._1 == roomId)){
            roomInUse(roomId).filter(u => u == uId).size match {
              case 0 =>
                roomInUse(roomId).add(uId)
              case _ =>
            }
          }else{
            roomInUse.put(roomId,mutable.HashSet(uId))
          }
          println(s"enter into success!!!now the rooms:$roomInUse")
          userActor ! UserActor.JoinRoomSuccess(tank,config,userActor,uId,roomId)
          Behaviors.same
        case WebSocketMsg(uid,tankId,req) =>
          val roomExist = roomInUse.filter(p => p._2.exists(_ == uid))
          if(roomExist.size > 1){
            getRoomActor(ctx,roomExist.head._1) ! WebSocketMsg(uid,tankId,req)
          }
          Behaviors.same

        case LeftRoom(uid,tankId,name) =>
          val roomExist = roomInUse.filter(p => p._2.exists(_ == uid))
          if(roomExist.size > 1){
            roomInUse.update(roomExist.head._1,roomInUse(roomExist.head._1).-(uid))
            getRoomActor(ctx,roomExist.head._1) ! RoomActor.LeftRoom(uid,tankId,name,roomInUse(roomExist.head._1),roomExist.head._1)
            if(roomInUse(roomExist.head._1).isEmpty){
              if(roomExist.head._1 > 1l) roomInUse.remove(roomExist.head._1)
            }
            println(s"remember to come back!!!$roomInUse")
          }

          Behaviors.same

        case LeftRoomSuccess(uidSet,name,actorRef,roomId) =>
          if(uidSet.isEmpty){
            println(s"left room success room_$roomId",actorRef)
            if(roomId > 1l) ctx.self ! ChildDead(s"room_$roomId",actorRef)
          }
          Behaviors.same

        case LeftRoomByKilled(uid,tankId,name) =>
          val roomExist = roomInUse.filter(p => p._2.exists(_ == uid)).head
          getRoomActor(ctx,roomExist._1) ! LeftRoomByKilled(uid,tankId,name)
          println(s"I am so sorry that you $uid are killed, the timer is beginning....")
          timer.startSingleTimer(LeftRoomKey,LeftRoom(uid,tankId,name),leftTime)
          Behaviors.same

        case ChildDead(child,childRef) =>
          println(s"不再监管room:$child,$childRef")
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
      println(childName,actor)
      ctx.watchWith(actor,ChildDead(childName,actor))
      actor

    }.upcast[RoomActor.Command]
  }

}
