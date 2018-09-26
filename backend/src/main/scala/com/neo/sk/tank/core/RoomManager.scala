package com.neo.sk.tank.core

import java.util.concurrent.atomic.{AtomicInteger, AtomicLong}

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import com.neo.sk.tank.core.RoomActor.{LeftRoomByKilled, WebSocketMsg}
import com.neo.sk.tank.core.UserActor.{JoinRoom, TimeOut}
import com.neo.sk.tank.core.game.TankServerImpl
import com.neo.sk.tank.shared.config.TankGameConfigImpl
import org.slf4j.LoggerFactory
import com.neo.sk.tank.common.AppSettings.{personLimit,leftTimeLimit}

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
  private final val leftTime = leftTimeLimit.minutes
  private case object LeftRoomKey
  private val roomInUse = mutable.HashMap[Long,mutable.HashSet[(Long,Boolean)]]()//roomId->Set((uid,False))uid-->等待复活


  trait Command
  private final case object BehaviorChangeKey
  private case class TimeOut(msg:String) extends Command
  private case class ChildDead[U](name:String,childRef:ActorRef[U]) extends Command

  case class LeftRoom(uid:Long,tankId:Int,name:String,userOpt: Option[Long]) extends Command

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
        case JoinRoom(uid,tankIdOpt,name,userActor) =>
          /**
            * 1.新加入等待分配roomId
            *     --- 有可容纳的房间
            *     --- 没有可容纳的房间
            * 2. 重新回到原来游戏房间,更新定时器，重新回到房间需要携带原来的tankId
            * 3. 等收到成功加入房间的消息再更新统计数据
            * */
          val roomExistUidMap = roomInUse.filter(p => p._2.exists(u => u._1 == uid))
          if(roomExistUidMap.size == 0){
            val roomCanBeUse = roomInUse.filter(_._2.size < personLimit)
            if(roomCanBeUse.size > 0){
              log.debug(s"room already exists !!! user$uid :$name enter into ${roomCanBeUse.keys.min}")
              roomInUse(roomCanBeUse.keys.min).add((uid,false))
              getRoomActor(ctx,roomCanBeUse.keys.min) ! RoomActor.JoinRoom(uid,tankIdOpt,name,userActor,roomCanBeUse.keys.min)
            }else{
              val roomId = roomIdGenerator.getAndIncrement()
              log.debug(s"new room !!!! user$uid :$name enter into $roomId")
              roomInUse.put(roomId,mutable.HashSet((uid,false)))
              getRoomActor(ctx,roomId) ! RoomActor.JoinRoom(uid,tankIdOpt,name,userActor,roomId)
            }
          }else{
            //重新回到原来房间，说明是reStart game
            roomExistUidMap.head._2.remove((uid,true))
            roomExistUidMap.head._2.add((uid,false))
//            roomExistUidMap.head._2.update((uid,false),true)
            roomInUse.update(roomExistUidMap.head._1,roomExistUidMap.head._2)
            log.debug(s"enter repeatedly !!! user$uid :$name is already in ${roomExistUidMap.head._1}")
            getRoomActor(ctx,roomExistUidMap.head._1) ! RoomActor.JoinRoom(uid,tankIdOpt,name,userActor,roomExistUidMap.head._1)
          }
          log.debug(s"now roomInUse:$roomInUse")

          Behaviors.same

        case LeftRoom(uid,tankId,name,userOpt) =>
          /**
            * --断掉websocket
            * --死亡时间超过5分钟
            * */
          val roomExist = roomInUse.filter(p => p._2.exists(u => u._1 == uid))
          if(roomExist.size >= 1){
            userOpt match {
              case Some(uid) =>
                //断开websocket
                log.debug(s"玩家：$uid--$name 离开房间:${roomExist.head._1}")
                roomInUse(roomExist.head._1).filter(u => u._1 == uid).head match {
                  case (uid,isDead) =>
                    roomInUse.update(roomExist.head._1,roomInUse(roomExist.head._1).-((uid,isDead)))
                    getRoomActor(ctx,roomExist.head._1) ! RoomActor.LeftRoom(uid,tankId,name,roomInUse(roomExist.head._1),roomExist.head._1)
                    if(roomInUse(roomExist.head._1).isEmpty && roomExist.head._1 > 1l){
                      roomInUse.remove(roomExist.head._1)
                    }
                    log.debug(s"$name remember to come back!!!$roomInUse")
                  case _ =>
                }
              case None =>
                //死亡时间超过10分钟
                roomInUse(roomExist.head._1).filter(u => u._1 == uid).head match {
                  case (uid,isDead) =>
                    if(isDead){
                      roomInUse.update(roomExist.head._1,roomInUse(roomExist.head._1).-((uid,isDead)))
                      getRoomActor(ctx,roomExist.head._1) ! RoomActor.LeftRoom(uid,tankId,name,roomInUse(roomExist.head._1),roomExist.head._1)
                      if(roomInUse(roomExist.head._1).isEmpty && roomExist.head._1 > 1l){
                        roomInUse.remove(roomExist.head._1)
                      }
                      log.debug(s"$name $uid remember to come back!!!$roomInUse")
                    }else{
                      log.debug(s"$name $uid has been relive")
                    }
                  case _ =>
                }
            }
          }
          Behaviors.same

        case LeftRoomByKilled(uid,tankId,name) =>
          val roomExist = roomInUse.filter(p => p._2.exists(u => u._1 == uid))
          if(roomExist.size >= 1){
            roomExist.head._2.remove((uid,false))
            roomExist.head._2.add((uid,true))
            getRoomActor(ctx,roomExist.head._1) ! LeftRoomByKilled(uid,tankId,name)
            log.debug(s"I am so sorry that you $name $uid are killed, the timer is beginning....")
            timer.startSingleTimer("room_"+roomExist.head._1+"uid"+uid,LeftRoom(uid,tankId,name,None),leftTime)
          }
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
