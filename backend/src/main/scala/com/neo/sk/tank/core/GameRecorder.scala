package com.neo.sk.tank.core

import java.io.File

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.{ActorContext, StashBuffer, TimerScheduler}
import akka.parboiled2.RuleTrace.Fail
import com.neo.sk.tank.common.AppSettings
import com.neo.sk.tank.shared.protocol.TankGameEvent
import com.neo.sk.tank.shared.protocol.TankGameEvent.{GameInformation, TankGameSnapshot, UserJoinRoom, UserLeftRoom}
import com.neo.sk.tank.models.SlickTables._
import com.neo.sk.tank.models.DAO.RecordDAO
import org.seekloud.byteobject.MiddleBufferInJvm
import org.seekloud.essf.io.FrameOutputStream
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.language.implicitConversions
import scala.concurrent.duration._
import scala.util.{Failure, Success}



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
  final case object SaveDate extends Command
  final case object Save extends Command

  private final val InitTime = Some(5.minutes)
  private final case object BehaviorChangeKey
  private final case object SaveDateKey

  final case class SwitchBehavior(
                                   name: String,
                                   behavior: Behavior[Command],
                                   durationOpt: Option[FiniteDuration] = None,
                                   timeOut: TimeOut = TimeOut("busy time error")
                                 ) extends Command

  case class TimeOut(msg:String) extends Command

  final case class GameRecorderData(
                                     roomId: Long,
                                     fileName: String,
                                     fileIndex:Int,
                                     gameInformation: GameInformation,
                                     initStateOpt: Option[TankGameEvent.GameSnapshot],
                                     recorder:FrameOutputStream,
                                     var gameRecordBuffer:List[GameRecord]
                                   )
  final case class EssfMapInfo(
                              userId: Long,
                              startTime: Long,
                              endTime: Long
                              )

  final case class JoinUserInfo(
                               userId: Long,
                               tankId: Long,
                               joinF: Long
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


  def create(fileName:String, gameInformation: GameInformation, initStateOpt:Option[TankGameEvent.GameSnapshot] = None, roomId: Long):Behavior[Command] = {
    Behaviors.setup{ ctx =>
      log.info(s"${ctx.self.path} is starting..")
      implicit val stashBuffer = StashBuffer[Command](Int.MaxValue)
      implicit val middleBuffer = new MiddleBufferInJvm(10 * 4096)
      Behaviors.withTimers[Command] { implicit timer =>
        val fileRecorder = initFileRecorder(fileName,0,gameInformation,initStateOpt)
        val gameRecordBuffer:List[GameRecord] = List[GameRecord]()
        val data = GameRecorderData(roomId,fileName,0,gameInformation,initStateOpt,fileRecorder,gameRecordBuffer)
        timer.startSingleTimer(SaveDateKey, Save, 1.hour)
        switchBehavior(ctx,"work",work(data,mutable.HashMap.empty[Long, EssfMapInfo],mutable.HashMap.empty[Long,Long],mutable.HashMap.empty[Long,Long], 0L, -1L))
      }
    }
  }

  private def work(gameRecordData: GameRecorderData,
                   essfMap: mutable.HashMap[Long, EssfMapInfo],
                   userAllMap: mutable.HashMap[Long,Long],
                   userMap: mutable.HashMap[Long,Long],
                   startF: Long,
                   endF: Long
                  )(
                    implicit stashBuffer:StashBuffer[Command],
                    timer:TimerScheduler[Command],
                    middleBuffer: MiddleBufferInJvm
                  ) : Behavior[Command] = {
    import gameRecordData._
    Behaviors.receive{ (ctx,msg) =>
      msg match {
        case t:GameRecord =>
          val wsMsg = t.event._1
          wsMsg.foreach{
                 case UserJoinRoom(userId, name, tankState,frame) =>
                   userAllMap.put(userId, tankState.tankId)
                   userMap.put(userId, tankState.tankId)
                   essfMap.put(tankState.tankId, EssfMapInfo(userId, frame, -1l))

                 case UserLeftRoom(userId, name, tankId,frame) =>
                   userMap.remove(userId)
                   val startF = essfMap(tankId).startTime
                   essfMap.put(tankId, EssfMapInfo(userId, startF, frame))

          }

          gameRecordBuffer = t :: gameRecordBuffer
          val newEndF = t.event._2.get match {
            case tank:TankGameSnapshot =>
              tank.state.f
          }
          if(gameRecordBuffer.size > maxRecordNum){
            val rs = gameRecordBuffer.reverse
            rs.headOption.foreach{ e =>
              recorder.writeFrame(e.event._1.fillMiddleBuffer(middleBuffer).result(),e.event._2.map(_.fillMiddleBuffer(middleBuffer).result()))
              rs.tail.foreach{e =>
                if(e.event._1.nonEmpty){
                  recorder.writeFrame(e.event._1.fillMiddleBuffer(middleBuffer).result())
                }else{
                  recorder.writeEmptyFrame()
                }
              }
            }

            gameRecordBuffer = List[GameRecord]()
            switchBehavior(ctx,"work",work(gameRecordData,essfMap,userAllMap,userMap, startF, newEndF))
          }else{
            switchBehavior(ctx,"work",work(gameRecordData,essfMap,userAllMap,userMap, startF, newEndF))
          }

        case Save =>
          timer.startSingleTimer(SaveDateKey, Save, 1.hour)
          ctx.self ! SaveDate
          switchBehavior(ctx,"save",save(gameRecordData,essfMap,userAllMap,userMap,startF,endF))



        case unknow =>
          log.warn(s"${ctx.self.path} recv an unknown msg:${unknow}")
          Behaviors.same
      }


    }
  }


  private def save(
                    gameRecordData: GameRecorderData,
                    essfMap: mutable.HashMap[Long, EssfMapInfo],
                    userAllMap: mutable.HashMap[Long,Long],
                    userMap: mutable.HashMap[Long,Long],
                    startF: Long,
                    endF: Long
                  ): Behavior[Command] = {
    import gameRecordData._
    Behaviors.receive{(ctx,msg) =>
      msg match {
        case SaveDate =>
          essfMap.foreach{
            essf=>
              val info = if(essf._2.endTime == -1L){
                (essf._1, EssfMapInfo(essf._2.userId, essf._2.startTime, endF))
              }else{
                essf
              }
              recorder.putMutableInfo(info._1.toString,info._2.toString.getBytes("utf-8"))
          }
          recorder.finish()
          log.info(s"${ctx.self.path} has save game data to file=${fileName}_$fileIndex")
          val endTime = System.currentTimeMillis()
          val filePath = AppSettings.gameDataDirectoryPath + fileName + s"_$fileIndex"
          val recordInfo = rGameRecord(-1L, gameRecordData.roomId, gameRecordData.gameInformation.gameStartTime, endTime,filePath)
          RecordDAO.insertGameRecord(recordInfo).onComplete{
            case Success(recordId) =>
              val list = ListBuffer[rUserRecordMap]()
              userAllMap.foreach{
                userRecord =>
                  list.append(rUserRecordMap(userRecord._1, recordId, roomId))
              }
              RecordDAO.insertUserRecordList(list.toList).onComplete{
                case Success(_) =>
                  log.info(s"insert user record success")
                case Failure(e) =>
                  log.error(s"insert user record fail, error: $e")
              }

            case Failure(e) =>
              log.error(s"insert geme record fail, error: $e")

          }

          switchBehavior(ctx,"initRecorder",initRecorder(roomId,gameRecordData.fileName,fileIndex,gameInformation, userMap))
        case unknow =>
          log.warn(s"${ctx} save got unknow msg ${unknow}")
          Behaviors.same
      }

    }

  }


  private def initRecorder(
                            roomId: Long,
                            fileName: String,
                            fileIndex:Int,
                            gameInformation: GameInformation,
                            userMap: mutable.HashMap[Long,Long]
                          ):Behavior[Command] = {
    Behaviors.receive{(ctx,msg) =>
      msg match {
        case t:GameRecord =>
          val startF = t.event._2.get match {
            case tank:TankGameSnapshot =>
              tank.state.f
          }
          val startTime = System.currentTimeMillis()
          val newInitStateOpt =t.event._2
          val newRecorder = initFileRecorder(fileName,fileIndex + 1, gameInformation, newInitStateOpt)
          val newGameInformation = GameInformation(startTime, gameInformation.tankConfig)
          val newGameRecorderData = GameRecorderData(roomId, fileName, fileIndex + 1, newGameInformation, newInitStateOpt, newRecorder, gameRecordBuffer = List[GameRecord]())
          val newEssfMap = mutable.HashMap.empty[Long, EssfMapInfo]
          val newUserAllMap = mutable.HashMap.empty[Long,Long]
          userMap.foreach{
            user=>
              newEssfMap.put(user._2, EssfMapInfo(user._1, startF, -1L))
              newUserAllMap.put(user._1, user._2)
          }
          switchBehavior(ctx,"work",work(newGameRecorderData, newEssfMap, newUserAllMap, userMap, startF, -1L))

        case unknow =>
          log.warn(s"${ctx} initRecorder got unknow msg ${unknow}")
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
