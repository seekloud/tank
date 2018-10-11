package com.neo.sk.tank.core

import java.io.File

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.{ActorContext, StashBuffer, TimerScheduler}
import com.neo.sk.tank.common.AppSettings
import com.neo.sk.tank.shared.protocol.TankGameEvent
import com.neo.sk.tank.shared.protocol.TankGameEvent.GameInformation
import org.seekloud.byteobject.MiddleBufferInJvm
import org.seekloud.essf.io.FrameOutputStream
import org.slf4j.LoggerFactory

import scala.language.implicitConversions
import scala.concurrent.duration._



/**
  * Created by hongruying on 2018/8/14
  * 采用essf工具进行记录
  * 每次玩家进入游戏，死亡效果。
  * 主要记录每帧的用户事件，环境事件，还有游戏状态
  */


object GameRecorder {

  import org.seekloud.byteobject.ByteObject._

  sealed trait Command

  final case class GameRecord(event:(List[TankGameEvent.WsMsgServer],Option[TankGameEvent.GameSnapshot])) extends Command

  private final val InitTime = Some(5.minutes)
  private final case object BehaviorChangeKey

  final case class SwitchBehavior(
                                   name: String,
                                   behavior: Behavior[Command],
                                   durationOpt: Option[FiniteDuration] = None,
                                   timeOut: TimeOut = TimeOut("busy time error")
                                 ) extends Command

  case class TimeOut(msg:String) extends Command

  final case class GameRecorderData(
                                     fileName: String,
                                     fileIndex:Int,
                                     gameInformation: GameInformation,
                                     initStateOpt: Option[TankGameEvent.GameSnapshot],
                                     recorder:FrameOutputStream,
                                     var gameRecordBuffer:List[GameRecord],
                                     var fileRecordNum:Int = 0
                                   )

  private[this] def switchBehavior(ctx: ActorContext[Command],
                                   behaviorName: String, behavior: Behavior[Command], durationOpt: Option[FiniteDuration] = None,timeOut: TimeOut  = TimeOut("busy time error"))
                                  (implicit stashBuffer: StashBuffer[Command],
                                   timer:TimerScheduler[Command]) = {
    log.debug(s"${ctx.self.path} becomes $behaviorName behavior.")
    timer.cancel(BehaviorChangeKey)
    durationOpt.foreach(timer.startSingleTimer(BehaviorChangeKey,timeOut,_))
    stashBuffer.unstashAll(ctx,behavior)
  }


  private final val maxRecordNum = 100
  private final val fileMaxRecordNum = 100000000
  private final val log = LoggerFactory.getLogger(this.getClass)


  def create(fileName:String, gameInformation: GameInformation, initStateOpt:Option[TankGameEvent.GameSnapshot] = None):Behavior[Command] = {
    Behaviors.setup{ ctx =>
      log.info(s"${ctx.self.path} is starting..")
      implicit val stashBuffer = StashBuffer[Command](Int.MaxValue)
      implicit val middleBuffer = new MiddleBufferInJvm(10 * 4096)
      Behaviors.withTimers[Command] { implicit timer =>
        val fileRecorder = initFileRecorder(fileName,0,gameInformation,initStateOpt)
        val gameRecordBuffer:List[GameRecord] = List[GameRecord]()
        val data = GameRecorderData(fileName,0,gameInformation,initStateOpt,fileRecorder,gameRecordBuffer)
        switchBehavior(ctx,"work",work(data))
      }
    }
  }

  private def work(gameRecordData: GameRecorderData
                  )(
                    implicit stashBuffer:StashBuffer[Command],
                    timer:TimerScheduler[Command],
                    middleBuffer: MiddleBufferInJvm
                  ) : Behavior[Command] = {
    import gameRecordData._
    Behaviors.receive{ (ctx,msg) =>
      msg match {
        case t:GameRecord =>
          gameRecordBuffer = t :: gameRecordBuffer
          if(gameRecordBuffer.size > maxRecordNum){
            val rs = gameRecordBuffer.reverse
            rs.headOption.foreach{ e =>
//              e.event._1.fillMiddleBuffer(middleBuffer).result()
              recorder.writeFrame(e.event._1.fillMiddleBuffer(middleBuffer).result(),e.event._2.map(_.fillMiddleBuffer(middleBuffer).result()))
              rs.tail.foreach{e =>
                if(e.event._1.nonEmpty){
                  recorder.writeFrame(e.event._1.fillMiddleBuffer(middleBuffer).result())
                }else{
                  recorder.writeEmptyFrame()
                }
              }
            }
            fileRecordNum += rs.size
            if(fileRecordNum > fileMaxRecordNum){
              recorder.finish()
              log.info(s"${ctx.self.path} has save game data to file=${fileName}_$fileIndex")
              val newRecorder = initFileRecorder(fileName,fileIndex + 1, gameInformation, initStateOpt)
              work(gameRecordData.copy(fileIndex = gameRecordData.fileIndex + 1, recorder = newRecorder, gameRecordBuffer = List[GameRecord](),fileRecordNum = 0))
            }else{
              work(gameRecordData.copy(gameRecordBuffer = List[GameRecord]()))
            }
          }else{
            Behaviors.same
          }

        case unknow =>
          log.warn(s"${ctx.self.path} recv an unknown msg:${unknow}")
          Behaviors.same
      }


    }
  }

  private def initFileRecorder(fileName:String,index:Int,gameInformation: GameInformation,initStateOpt:Option[TankGameEvent.GameSnapshot] = None)
                              (implicit middleBuffer: MiddleBufferInJvm):FrameOutputStream = {
    val dir = new File(AppSettings.gameDataDirectoryPath)
    if(!dir.exists()){
      dir.mkdir()
    }
    val file = AppSettings.gameDataDirectoryPath + fileName + s"_$index"
    val name = "tank"
    val version = "0.1"
    val gameInformationBytes = gameInformation.fillMiddleBuffer(middleBuffer).result()
    val initStateBytes = initStateOpt.map{
      case t:TankGameEvent.GameSnapshot =>
        t.fillMiddleBuffer(middleBuffer).result()
    }.getOrElse(Array[Byte]())
    val recorder = new FrameOutputStream(file)
    recorder.init(name,version,gameInformationBytes,initStateBytes)
    log.debug(s" init success")
    recorder
  }





}
