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
import com.neo.sk.tank.protocol.EsheepProtocol._
import com.neo.sk.tank.protocol.ReplayProtocol.{EssfMapJoinLeftInfo, EssfMapKey}
import com.neo.sk.tank.shared.protocol.TankGameEvent
import com.neo.sk.tank.shared.protocol.TankGameEvent.{GameInformation, ReplayFrameData, SyncGameAllState, YourInfo}
import org.seekloud.byteobject.MiddleBufferInJvm

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import com.neo.sk.utils.ESSFSupport.{initFileReader => _, metaDataDecode => _, userMapDecode => _, _}
import com.neo.sk.tank.protocol.ReplayProtocol.{GetRecordFrameMsg, GetUserInRecordMsg}
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
          timer.cancel(BehaviorWaitKey)
          userMap.find(_._1.userId == msg.userId) match {
            case Some(u)=>
              dispatchTo(msg.userActor,YourInfo(u._1.userId,u._1.tankId,u._1.name,metaData.tankConfig))
              log.info(s" set replay from frame=${msg.f}")
              fileReader.gotoSnapshot(msg.f)
              log.info(s"replay from frame=${fileReader.getFramePosition}")
              if(fileReader.hasMoreFrame){
                timer.startPeriodicTimer(GameLoopKey, GameLoop, 100.millis)
                work(fileReader,metaData,userMap,Some(msg.userActor))
              }else{
                timer.startSingleTimer(BehaviorWaitKey,TimeOut("wait time out"),waitTime)
                Behaviors.same
              }
            case None=>
              dispatchTo(msg.userActor,TankGameEvent.InitReplayError("本局游戏中不存在该用户！！"))
              timer.startSingleTimer(BehaviorWaitKey,TimeOut("wait time out"),waitTime)
              Behaviors.same
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
            timer.startSingleTimer(BehaviorWaitKey,TimeOut("wait time out"),waitTime)
            Behaviors.same
          }

        case msg:GetRecordFrameMsg=>
          msg.replyTo ! GetRecordFrameRsp(RecordFrameInfo(fileReader.getFramePosition))
          Behaviors.same

        case msg:GetUserInRecordMsg=>
          val data=userMap.map{r=>PlayerInfo(r._1.userId,r._1.name)}
          msg.replyTo ! GetUserInRecordRsp(PlayerList(data))
          Behaviors.same

        case msg:TimeOut=>
          Behaviors.stopped

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
