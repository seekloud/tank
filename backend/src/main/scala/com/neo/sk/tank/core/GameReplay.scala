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
import com.neo.sk.tank.models.DAO.RecordDAO._
import com.neo.sk.tank.shared.protocol.TankGameEvent
import com.neo.sk.tank.shared.protocol.TankGameEvent.{ReplayFrameData, YourInfo}
import org.seekloud.byteobject.MiddleBufferInJvm

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
/**
  * User: sky
  * Date: 2018/10/12
  * Time: 16:06
  */
object GameReplay {
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

  val loadFrame=100

  /**actor内部消息*/
  case class InitReplay(userActor: ActorRef[TankGameEvent.WsMsgSource]) extends Command
  case class InitDownload(userActor: ActorRef[TankGameEvent.WsMsgSource]) extends Command


  def create(recordId:Long):Behavior[Command] = {
    Behaviors.setup[Command]{ctx=>
      log.info(s"${ctx.self.path} is starting..")
      implicit val stashBuffer = StashBuffer[Command](Int.MaxValue)
      implicit val sendBuffer = new MiddleBufferInJvm(81920)
      Behaviors.withTimers[Command] { implicit timer =>
        //todo 此处替换从数据库中读取
        /*getRecordById(recordId).map {
          case Some(r)=>
            val replay=initFileReader(r.filePath)
            switchBehavior(ctx,"work",work(replay))
          case None=>
        }*/
        println("----5")
        Future{
          val replay=initFileReader(AppSettings.gameDataDirectoryPath+"tankGame_1539309693971_5")
          switchBehavior(ctx,"work",work(replay))
        }
        switchBehavior(ctx,"busy",busy())
      }
    }
  }

  def work(fileReader:FrameInputStream,userOpt:Option[ActorRef[TankGameEvent.WsMsgSource]]=None)
          (
            implicit stashBuffer:StashBuffer[Command],
            timer:TimerScheduler[Command],
            sendBuffer: MiddleBufferInJvm
          ):Behavior[Command]={
    Behaviors.receive[Command]{(ctx,msg)=>
      msg match {
        case msg:InitReplay=>
          //todo 此处从文件中读取相关数据传送给前端
          println("----2")
          dispatchTo(msg.userActor,YourInfo(0l,0,"test",AppSettings.tankGameConfig.getTankGameConfigImpl()))
          for(i <- 0 to loadFrame){
            if(fileReader.hasMoreFrame){
              fileReader.readFrame().foreach { r =>
                //                dispatchByteTo(msg.userActor,r)
                dispatchTo(msg.userActor, TankGameEvent.FrameData(r.eventsData))
                r.stateData.foreach(s=>dispatchTo(msg.userActor, TankGameEvent.FrameData(s)))
              }
            }
          }
          if(fileReader.hasMoreFrame){
            timer.startPeriodicTimer(GameLoopKey, GameLoop, 100.millis)
            work(fileReader,Some(msg.userActor))
          }else{
            Behaviors.stopped
          }

        case msg:InitDownload=>
          Behaviors.stopped

        case GameLoop=>
          if(fileReader.hasMoreFrame){
            userOpt.foreach(u=>
              fileReader.readFrame().foreach(f=>
                dispatchByteTo(u,f)
              )
            )
            Behaviors.same
          }else{
            timer.cancel(GameLoopKey)
            Behaviors.stopped
          }

        case unKnowMsg =>
          stashBuffer.stash(unKnowMsg)
          Behavior.same
      }
    }
  }

  import org.seekloud.byteobject.ByteObject._
  def dispatchTo(subscriber: ActorRef[TankGameEvent.WsMsgSource],msg: TankGameEvent.WsMsgServer)(implicit sendBuffer: MiddleBufferInJvm)= {
    subscriber ! ReplayFrameData(msg.asInstanceOf[TankGameEvent.WsMsgServer].fillMiddleBuffer(sendBuffer).result())
  }

  def dispatchByteTo(subscriber: ActorRef[TankGameEvent.WsMsgSource], msg:FrameData) = {
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
