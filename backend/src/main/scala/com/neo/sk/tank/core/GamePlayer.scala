package com.neo.sk.tank.core

import java.io.File

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.{ActorContext, StashBuffer, TimerScheduler}
import com.neo.sk.tank.common.AppSettings
import org.slf4j.LoggerFactory

import scala.language.implicitConversions
import scala.concurrent.duration._
import com.neo.sk.utils.ESSFSupport._
import org.seekloud.essf.io.{EpisodeInfo, FrameData, FrameInputStream}
import com.neo.sk.tank.models.DAO.RecordDAO
import com.neo.sk.tank.protocol.ReplayProtocol.{EssfMapJoinLeftInfo, EssfMapKey}
import com.neo.sk.tank.shared.protocol.TankGameEvent
import com.neo.sk.tank.shared.protocol.TankGameEvent.{GameInformation, ReplayFrameData, YourInfo}
import org.seekloud.byteobject.MiddleBufferInJvm

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import com.neo.sk.utils.ESSFSupport.{initStateDecode, metaDataDecode, replayEventDecode, replayStateDecode, userMapDecode}
/**
  * User: sky
  * Date: 2018/10/12
  * Time: 16:06
  */
object GamePlayer {
  private final val log = LoggerFactory.getLogger(this.getClass)

  sealed trait Command
  private final case object BehaviorChangeKey
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
  case class InitReplay(userActor: ActorRef[TankGameEvent.WsMsgSource],userId:Long,f:Int) extends Command
  case class InitDownload(userActor: ActorRef[TankGameEvent.WsMsgSource]) extends Command


  def create(recordId:Long):Behavior[Command] = {
    Behaviors.setup[Command]{ctx=>
      log.info(s"${ctx.self.path} is starting..")
      implicit val stashBuffer = StashBuffer[Command](Int.MaxValue)
      implicit val sendBuffer = new MiddleBufferInJvm(81920)
      Behaviors.withTimers[Command] { implicit timer =>
        RecordDAO.getRecordById(recordId).map {
          case Some(r)=>
            val replay=initFileReader(r.filePath)
            val info=replay.init()
            try{
              ctx.self ! SwitchBehavior("work",
                work(
                  replay,
                  metaDataDecode(info.simulatorMetadata).right.get,
                  userMapDecode(replay.getMutableInfo(AppSettings.essfMapKeyName).getOrElse(Array[Byte]())).right.get.m
                ))
            }catch {
              case e:Throwable=>
                log.error("error---"+e.getMessage)
            }
          case None=>
            log.debug(s"record--$recordId didn't exist!!")
        }
        switchBehavior(ctx,"busy",busy())
      }
    }
  }

  def work(fileReader:FrameInputStream,
           metaData:GameInformation,
//           initState:TankGameEvent.TankGameSnapshot,
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
          //todo 此处从文件中读取相关数据传送给前端
          timer.cancel(GameLoopKey)
          userMap.find(_._1.userId == msg.userId) match {
            case Some(u)=>
              dispatchTo(msg.userActor,YourInfo(u._1.userId,u._1.tankId,u._1.name,metaData.tankConfig))
              if(fileReader.hasMoreFrame){
                timer.startPeriodicTimer(GameLoopKey, GameLoop, 100.millis)
                work(fileReader,metaData,userMap,Some(msg.userActor))
              }else{
                switchBehavior(ctx,"busy",busy(),Some(10.seconds))
              }
            case None=>
              switchBehavior(ctx,"busy",busy(),Some(10.seconds))
          }


        case msg:InitDownload=>
          switchBehavior(ctx,"busy",busy(),Some(10.seconds))

        case GameLoop=>
          if(fileReader.hasMoreFrame){
            userOpt.foreach(u=>
              fileReader.readFrame().foreach { f =>
                dispatchByteTo(u, f)
              }
            )
            Behaviors.same
          }else{
            timer.cancel(GameLoopKey)
            switchBehavior(ctx,"busy",busy(),Some(10.seconds))
          }

        case unKnowMsg =>
          stashBuffer.stash(unKnowMsg)
          Behavior.same
      }
    }
  }

  import org.seekloud.byteobject.ByteObject._
  def dispatchTo(subscriber: ActorRef[TankGameEvent.WsMsgSource],msg: TankGameEvent.WsMsgServer)(implicit sendBuffer: MiddleBufferInJvm)= {
//    subscriber ! ReplayFrameData(msg.asInstanceOf[TankGameEvent.WsMsgServer].fillMiddleBuffer(sendBuffer).result())
    subscriber ! ReplayFrameData(List(msg).fillMiddleBuffer(sendBuffer).result())
  }

  def dispatchByteTo(subscriber: ActorRef[TankGameEvent.WsMsgSource], msg:FrameData)(implicit sendBuffer: MiddleBufferInJvm) = {
//    subscriber ! ReplayFrameData(replayEventDecode(msg.eventsData).fillMiddleBuffer(sendBuffer).result())
//    msg.stateData.foreach(s=>subscriber ! ReplayFrameData(replayStateDecode(s).fillMiddleBuffer(sendBuffer).result()))
    subscriber ! ReplayFrameData(msg.eventsData)
    msg.stateData.foreach(s=>subscriber ! ReplayFrameData(s))
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
