package com.neo.sk.tank.core

import java.io.File

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.{ActorContext, StashBuffer, TimerScheduler}
import com.neo.sk.tank.common.AppSettings
import com.neo.sk.tank.core.UserActor.DispatchReplayMsg
import org.slf4j.LoggerFactory

import scala.language.implicitConversions
import scala.concurrent.duration._
import com.neo.sk.utils.ESSFSupport._
import org.seekloud.essf.io.{EpisodeInfo, FrameData, FrameInputStream}
import com.neo.sk.tank.models.DAO.RecordDAO._
import com.neo.sk.tank.shared.protocol.TankGameEvent.{ReplayData, ReplayFrameData}

import scala.concurrent.Future
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
  case class InitReplay(userActor: ActorRef[UserActor.Command]) extends Command
  case class InitDownload(userActor: ActorRef[UserActor.Command]) extends Command


  def create(recordId:Long):Behavior[Command] = {
    Behaviors.setup[Command]{ctx=>
      log.info(s"${ctx.self.path} is starting..")
      implicit val stashBuffer = StashBuffer[Command](Int.MaxValue)
      Behaviors.withTimers[Command] { implicit timer =>
        /*getRecordById(recordId).map {
          case Some(r)=>
            val replay=initFileReader(r.filePath)
            switchBehavior(ctx,"work",work(replay))
          case None=>
        }*/
        Future.successful{
          val replay=initFileReader("C:\\Users\\sky\\IdeaProjects\\tank\\backend\\gameDataDirectoryPath\\tankGame_1539309693971_1")
          switchBehavior(ctx,"work",work(replay))
        }
        busy()
      }
    }
  }

  def work(fileReader:FrameInputStream,userOpt:Option[ActorRef[UserActor.Command]]=None)(implicit stashBuffer:StashBuffer[Command],
                                        timer:TimerScheduler[Command]):Behavior[Command]={
    Behaviors.receive[Command]{(ctx,msg)=>
      msg match {
        case msg:InitReplay=>
          for(i <- 0 to loadFrame){
            if(fileReader.hasMoreFrame){
              dispatchTo(msg.userActor,fileReader.readFrame())
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
            userOpt.foreach(r=>
              dispatchTo(r,fileReader.readFrame())
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

  def dispatchTo(subscriber: ActorRef[UserActor.Command],msg:Option[FrameData]) = {
    val data=msg.map(r=>ReplayData(r.frameIndex,r.eventsData,r.stateData))
    subscriber ! DispatchReplayMsg(ReplayFrameData(data))
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
