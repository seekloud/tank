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
import org.seekloud.tank.protocol.EsheepProtocol._
import org.seekloud.tank.protocol.ReplayProtocol.{EssfMapJoinLeftInfo, EssfMapKey, GetRecordFrameMsg, GetUserInRecordMsg}
import org.seekloud.byteobject.MiddleBufferInJvm
import org.seekloud.essf.io.{FrameData, FrameInputStream}
import org.seekloud.tank.common.AppSettings
import org.seekloud.tank.models.DAO.RecordDAO
import org.seekloud.utils.ESSFSupport.{initFileReader, initStateDecode, metaDataDecode, userMapDecode}
import org.slf4j.LoggerFactory

import concurrent.duration._
import org.seekloud.tank.Boot.executor
import org.seekloud.tank.shared.protocol.TankGameEvent
import org.seekloud.tank.shared.protocol.TankGameEvent.{GameInformation, ReplayFrameData, YourInfo}
import org.seekloud.tank.shared.ptcl.ErrorRsp

import scala.concurrent.duration.FiniteDuration

/**
  * User: sky
  * Date: 2018/10/12
  * Time: 16:06
  */
object GamePlayer {
  private final val log = LoggerFactory.getLogger(this.getClass)

  private val waitTime=10.minutes
  trait Command
  private final case object BehaviorChangeKey
  private final case object BehaviorWaitKey
  private final case object GameLoopKey

  final case class SwitchBehavior(
                                   name: String,
                                   behavior: Behavior[Command],
                                   durationOpt: Option[FiniteDuration] = None,
                                   timeOut: TimeOut = TimeOut("busy time error")
                                 ) extends Command

  case class TimeOut(msg:String) extends Command
  case object GameLoop extends Command

  private[this] def switchBehavior(ctx: ActorContext[Command],
                                   behaviorName: String, behavior: Behavior[Command], durationOpt: Option[FiniteDuration] = None,timeOut: TimeOut  = TimeOut("busy time error"))
                                  (implicit stashBuffer: StashBuffer[Command],
                                   timer:TimerScheduler[Command]) = {
    log.debug(s"${ctx.self.path} becomes $behaviorName behavior.")
    timer.cancel(BehaviorChangeKey)
    durationOpt.foreach(timer.startSingleTimer(BehaviorChangeKey,timeOut,_))
    stashBuffer.unstashAll(ctx,behavior)
  }

  /**actor内部消息*/
  case class InitReplay(userActor: ActorRef[TankGameEvent.WsMsgSource],userId:String,f:Int) extends Command
  case object GetUserListInRecord extends Command

  def create(recordId:Long):Behavior[Command] = {
    Behaviors.setup[Command]{ctx=>
      log.info(s"${ctx.self.path} is starting..")
      implicit val stashBuffer = StashBuffer[Command](Int.MaxValue)
      implicit val sendBuffer = new MiddleBufferInJvm(81920)
      Behaviors.withTimers[Command] { implicit timer =>
        RecordDAO.getRecordById(recordId).map {
          case Some(r)=>
            try{
              val replay=initFileReader(r.filePath)
              val info=replay.init()
              ctx.self ! SwitchBehavior("work",
                work(
                  replay,
                  metaDataDecode(info.simulatorMetadata).right.get,
                  initStateDecode(info.simulatorInitState).right.get.asInstanceOf[TankGameEvent.TankGameSnapshot].state.f,
                  info.frameCount,
                  userMapDecode(replay.getMutableInfo(AppSettings.essfMapKeyName).getOrElse(Array[Byte]())).right.get.m
                ))
            }catch {
              case e:Throwable=>
                log.error("error---"+e.getMessage)
                ctx.self ! SwitchBehavior("initError",initError)
            }
          case None=>
            log.debug(s"record--$recordId didn't exist!!")
            ctx.self ! SwitchBehavior("initError",initError)
        }
        switchBehavior(ctx,"busy",busy())
      }
    }
  }

  def work(fileReader:FrameInputStream,
           metaData:GameInformation,
           initStateFrame:Long,
           frameCount:Int,
           userMap:List[(EssfMapKey,EssfMapJoinLeftInfo)],
           userOpt:Option[ActorRef[TankGameEvent.WsMsgSource]]=None
          )(
            implicit stashBuffer:StashBuffer[Command],
            timer:TimerScheduler[Command],
            sendBuffer: MiddleBufferInJvm
          ):Behavior[Command]={
    Behaviors.receive[Command]{(ctx,msg)=>
      msg match {
        case msg:InitReplay=>
          log.info("start new replay!")
          timer.cancel(GameLoopKey)
          userMap.filter(t => t._1.userId == msg.userId && t._2.leftF >= msg.f).sortBy(_._2.joinF).headOption match {
            case Some(u)=>
              //val tankCofig = metaData.tankConfig.copy(frameDuration = 50L)
              val replayRate = AppSettings.tankGameConfig.getTankGameConfigImpl().replayRate
              val tankConfig = metaData.tankConfig.copy(playRate = replayRate)
              //dispatchTo(msg.userActor,YourInfo(u._1.userId,u._1.tankId,u._1.name,metaData.tankConfig))
              dispatchTo(msg.userActor,YourInfo(u._1.userId,u._1.tankId,u._1.name,tankConfig))

              log.info(s" set replay from frame=${msg.f}")
              fileReader.gotoSnapshot(msg.f)
              log.info(s"replay from frame=${fileReader.getFramePosition}")
              //快速播放
              for(_ <- 0 until (msg.f - fileReader.getFramePosition)){
                if(fileReader.hasMoreFrame){
                  fileReader.readFrame().foreach { f => dispatchByteTo(msg.userActor, f)}
                }else{
                  log.debug(s"${ctx.self.path} file reader has no frame, reply finish")
                  dispatchTo(msg.userActor,TankGameEvent.ReplayFinish())
                }
              }
              dispatchTo(msg.userActor, TankGameEvent.StartReplay)

              if(fileReader.hasMoreFrame){
                timer.startPeriodicTimer(GameLoopKey, GameLoop, (metaData.tankConfig.frameDuration / replayRate).millis)
                work(fileReader,metaData,initStateFrame,frameCount,userMap,Some(msg.userActor))
              }else{
                Behaviors.stopped
              }
            case None=>
              dispatchTo(msg.userActor,TankGameEvent.InitReplayError("本局游戏中不存在该用户！！"))
              Behaviors.stopped
          }

        case GameLoop=>
          if(fileReader.hasMoreFrame){
            userOpt.foreach(u=>
              fileReader.readFrame().foreach { f =>
                dispatchByteTo(u, f)
              }
            )
            Behaviors.same
          }else{
            userOpt.foreach(u=>
              dispatchTo(u,TankGameEvent.ReplayFinish())
            )
            timer.cancel(GameLoopKey)
            Behaviors.stopped
          }

        case msg:GetRecordFrameMsg=>
          msg.replyTo ! GetRecordFrameRsp(RecordFrameInfo(fileReader.getFramePosition,frameCount))
          Behaviors.same

        case msg:GetUserInRecordMsg=>
          val data=userMap.groupBy(r=>(r._1.userId,r._1.name)).map{r=>
            val fList=r._2.map(f=>ExistTimeInfo(f._2.joinF-initStateFrame,f._2.leftF-initStateFrame))
            PlayerInRecordInfo(r._1._1,r._1._2,fList)
          }.toList
          msg.replyTo ! GetUserInRecordRsp(PlayerList(frameCount,data))
          Behaviors.same

        case unKnowMsg =>
          stashBuffer.stash(unKnowMsg)
          Behavior.same
      }
    }
  }

  private def initError(
                         implicit sendBuffer: MiddleBufferInJvm
                       ):Behavior[Command]={
    Behaviors.receive[Command]{(ctx,msg)=>
      msg match {
        case msg:InitReplay =>
          dispatchTo(msg.userActor,TankGameEvent.InitReplayError("游戏文件不存在或者已损坏！！"))
          Behaviors.stopped

        case msg:GetRecordFrameMsg=>
          msg.replyTo ! ErrorRsp(10001,"init error")
          Behaviors.stopped

        case msg:GetUserInRecordMsg=>
          msg.replyTo ! ErrorRsp(10001,"init error")
          Behaviors.stopped
      }
    }
  }

  import org.seekloud.byteobject.ByteObject._
  private def dispatchTo(subscriber: ActorRef[TankGameEvent.WsMsgSource],msg: TankGameEvent.WsMsgServer)(implicit sendBuffer: MiddleBufferInJvm)= {
//    subscriber ! ReplayFrameData(msg.asInstanceOf[TankGameEvent.WsMsgServer].fillMiddleBuffer(sendBuffer).result())
    subscriber ! ReplayFrameData(List(msg).fillMiddleBuffer(sendBuffer).result())
  }

  private def dispatchByteTo(subscriber: ActorRef[TankGameEvent.WsMsgSource], msg:FrameData)(implicit sendBuffer: MiddleBufferInJvm) = {
//    subscriber ! ReplayFrameData(replayEventDecode(msg.eventsData).fillMiddleBuffer(sendBuffer).result())
//    msg.stateData.foreach(s=>subscriber ! ReplayFrameData(replayStateDecode(s).fillMiddleBuffer(sendBuffer).result()))
    msg.stateData.foreach(s=>subscriber ! ReplayFrameData(s))
    if(msg.eventsData.length>0){
      subscriber ! ReplayFrameData(msg.eventsData)
    }

  }

  private def busy()(
    implicit stashBuffer:StashBuffer[Command],
    timer:TimerScheduler[Command]
  ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case SwitchBehavior(name, behavior,durationOpt,timeOut) =>
          switchBehavior(ctx,name,behavior,durationOpt,timeOut)

        case TimeOut(m) =>
          log.debug(s"${ctx.self.path} is time out when busy,msg=${m}")
          Behaviors.stopped

        case unknowMsg =>
          stashBuffer.stash(unknowMsg)
          Behavior.same
      }
    }
}
