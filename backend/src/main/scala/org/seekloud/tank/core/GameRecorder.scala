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

import java.io.File

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import akka.actor.typed.{Behavior, PostStop}
import org.seekloud.tank.protocol.ReplayProtocol.{EssfMapJoinLeftInfo, EssfMapKey}
import org.seekloud.byteobject.MiddleBufferInJvm
import org.seekloud.essf.io.FrameOutputStream
import org.seekloud.tank.common.AppSettings
import org.seekloud.tank.models.DAO.RecordDAO
import org.seekloud.utils.ESSFSupport.userMapEncode
import org.slf4j.LoggerFactory
import org.seekloud.tank.models.SlickTables.{rGameRecord, rUserRecordMap}
import concurrent.duration._
import org.seekloud.tank.Boot.executor
import org.seekloud.tank.shared.protocol.TankGameEvent
import org.seekloud.tank.shared.protocol.TankGameEvent._

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.Await
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}

/**
  * Created by hongruying on 2018/8/14
  * 采用essf工具进行记录
  * 每次玩家进入游戏，死亡效果。
  * 主要记录每帧的用户事件，环境事件，还有游戏状态
  */


object GameRecorder {

  import org.seekloud.byteobject.ByteObject._
  import org.seekloud.utils.ESSFSupport.initFileRecorder
  sealed trait Command

  final case class GameRecord(event:(List[TankGameEvent.WsMsgServer],Option[TankGameEvent.GameSnapshot])) extends Command
  final case class SaveDate(stop:Int) extends Command
  final case class SaveEmpty(stop: Int, fileName: String) extends Command
  final case object Save extends Command
  final case object RoomClose extends Command
  final case object StopRecord extends Command

  private final val InitTime = Some(5.minutes)
  private final case object BehaviorChangeKey
  private final case object SaveDateKey
  private final val saveTime = AppSettings.gameRecordTime.minute

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



  private[this] def switchBehavior(ctx: ActorContext[Command],
                                   behaviorName: String, behavior: Behavior[Command], durationOpt: Option[FiniteDuration] = None,timeOut: TimeOut  = TimeOut("busy time error"))
                                  (implicit stashBuffer: StashBuffer[Command],
                                   timer:TimerScheduler[Command]) = {
    //log.debug(s"${ctx.self.path} becomes $behaviorName behavior.")
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
        timer.startSingleTimer(SaveDateKey, Save, saveTime)
        val startFrame = initStateOpt.map(_.asInstanceOf[TankGameSnapshot].state.f).getOrElse(0L)
        switchBehavior(ctx,"work",work(data,mutable.HashMap.empty[EssfMapKey,EssfMapJoinLeftInfo],mutable.HashMap.empty[String,(Int,String)],mutable.HashMap.empty[String,(Int,String)], startFrame, -1L))
      }
    }
  }

  private def work(gameRecordData: GameRecorderData,
                   essfMap: mutable.HashMap[EssfMapKey,EssfMapJoinLeftInfo],
                   userAllMap: mutable.HashMap[String,(Int,String)], //userId,(tankId,name)
                   userMap: mutable.HashMap[String,(Int,String)],
                   startF: Long,
                   endF: Long
                  )(
                    implicit stashBuffer:StashBuffer[Command],
                    timer:TimerScheduler[Command],
                    middleBuffer: MiddleBufferInJvm
                  ) : Behavior[Command] = {
    import gameRecordData._
    Behaviors.receive[Command] { (ctx,msg) =>
      msg match {
        case t:GameRecord =>
          //log.info(s"${ctx.self.path} work get msg gameRecord")
          val wsMsg = t.event._1
          wsMsg.foreach{
                 case UserJoinRoom(userId, name, tankState,frame) =>
                   if(tankState.lives >= gameRecordData.gameInformation.tankConfig.getTankLivesLimit){
                     userAllMap.put(userId, (tankState.tankId,name))
                     userMap.put(userId, (tankState.tankId,name))
                     essfMap.put(EssfMapKey(tankState.tankId, userId, name), EssfMapJoinLeftInfo(frame, -1l))
                   }


                 case UserLeftRoom(userId, name, tankId,frame) =>
                   userMap.remove(userId)
                   if(essfMap.get(EssfMapKey(tankId, userId, name)).isDefined){
                     val startF = essfMap(EssfMapKey(tankId, userId, name)).joinF
                     essfMap.put(EssfMapKey(tankId, userId,name), EssfMapJoinLeftInfo(startF,frame))
                   }

                 case UserLeftRoomByKill(userId, name, tankId,frame) =>
                   userMap.remove(userId)
                   if(essfMap.get(EssfMapKey(tankId, userId, name)).isDefined){
                     val startF = essfMap(EssfMapKey(tankId, userId, name)).joinF
                     essfMap.put(EssfMapKey(tankId, userId,name), EssfMapJoinLeftInfo(startF,frame))
                   }


                 case _ =>

          }

          gameRecordBuffer = t.copy(event = (wsMsg.filterNot(_.isInstanceOf[UserLeftRoomByKill]), t.event._2)) :: gameRecordBuffer
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
          val file = AppSettings.gameDataDirectoryPath + fileName + s"_$fileIndex"
          log.info(s"${ctx.self.path} work get msg save")
          timer.startSingleTimer(SaveDateKey, Save, saveTime)
          if(userAllMap.nonEmpty){
            ctx.self ! SaveDate(0)
          }else{
            ctx.self ! SaveEmpty(0,file)
          }
          switchBehavior(ctx,"save",save(gameRecordData,essfMap,userAllMap,userMap,startF,endF))

        case RoomClose =>
          val file = AppSettings.gameDataDirectoryPath + fileName + s"_$fileIndex"
          log.info(s"${ctx.self.path} work get msg save, room close")
          if(userAllMap.nonEmpty){
            ctx.self ! SaveDate(1)
          }else{
            ctx.self ! SaveEmpty(1,file)
          }
          switchBehavior(ctx,"save",save(gameRecordData,essfMap,userAllMap,userMap,startF,endF))

        case unknow =>
          log.warn(s"${ctx.self.path} recv an unknown msg:${unknow}")
          Behaviors.same
      }
    }.receiveSignal{
      case (ctx,PostStop) =>
        timer.cancel(SaveDateKey)
        log.info(s"${ctx.self.path} stopping....")

        val gameRecorderBuffer = gameRecordData.gameRecordBuffer
        //保存剩余gameRecorderBuffer中数据
        val rs = gameRecorderBuffer.reverse
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
        val mapInfo = essfMap.map{
          essf=>
            if(essf._2.leftF == -1L){
              (essf._1,EssfMapJoinLeftInfo(essf._2.joinF,endF))
            }else{
              essf
            }
        }
        recorder.putMutableInfo(AppSettings.essfMapKeyName,userMapEncode(mapInfo))
        recorder.finish()
        val endTime = System.currentTimeMillis()
        val filePath = AppSettings.gameDataDirectoryPath + fileName + s"_$fileIndex"
        val recordInfo = rGameRecord(-1L, gameRecordData.roomId, gameRecordData.gameInformation.gameStartTime, endTime,filePath)
        val recordId =Await.result(RecordDAO.insertGameRecord(recordInfo), 1.minute)
        val list = ListBuffer[rUserRecordMap]()
        userAllMap.foreach{
          userRecord =>
            list.append(rUserRecordMap(userRecord._1, recordId, roomId, userRecord._2._2))
        }
        Await.result(RecordDAO.insertUserRecordList(list.toList), 2.minute)
        Behaviors.stopped
    }
  }


  private def save(
                    gameRecordData: GameRecorderData,
                    essfMap: mutable.HashMap[EssfMapKey,EssfMapJoinLeftInfo],
                    userAllMap: mutable.HashMap[String,(Int,String)],
                    userMap: mutable.HashMap[String,(Int,String)],
                    startF: Long,
                    endF: Long
                  )(
                    implicit stashBuffer:StashBuffer[Command],
                    timer:TimerScheduler[Command],
                    middleBuffer: MiddleBufferInJvm
                  ): Behavior[Command] = {
    import gameRecordData._
    Behaviors.receive{(ctx,msg) =>
      msg match {
          //fixme 这里存储文件的时候，gameRecordData的buffer数据没存，导致数据丢失
        case s:SaveDate =>
          log.info(s"${ctx.self.path} save get msg saveDate")
          val gameRecorderBuffer = gameRecordData.gameRecordBuffer
          //保存剩余gameRecorderBuffer中数据
          val rs = gameRecorderBuffer.reverse
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
          val mapInfo = essfMap.map{
            essf=>
              if(essf._2.leftF == -1L){
                (essf._1,EssfMapJoinLeftInfo(essf._2.joinF,endF))
              }else{
                essf
              }
          }
          recorder.putMutableInfo(AppSettings.essfMapKeyName,userMapEncode(mapInfo))

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
                  list.append(rUserRecordMap(userRecord._1, recordId, roomId, userRecord._2._2))
              }
              RecordDAO.insertUserRecordList(list.toList).onComplete{
                case Success(_) =>
                  log.info(s"insert user record success")
                  ctx.self !  SwitchBehavior("initRecorder",initRecorder(roomId,gameRecordData.fileName,fileIndex,gameInformation, userMap))
                  if(s.stop == 1) ctx.self ! StopRecord
                case Failure(e) =>
                  log.error(s"insert user record fail, error: $e")
                  ctx.self !  SwitchBehavior("initRecorder",initRecorder(roomId,gameRecordData.fileName,fileIndex,gameInformation, userMap))
                  if(s.stop == 1) ctx.self ! StopRecord
              }

            case Failure(e) =>
              log.error(s"insert geme record fail, error: $e")
              ctx.self !  SwitchBehavior("initRecorder",initRecorder(roomId,gameRecordData.fileName,fileIndex,gameInformation, userMap))
              if(s.stop == 1) ctx.self ! StopRecord

          }
            switchBehavior(ctx,"busy",busy())

        case s:SaveEmpty =>
          log.info(s"${ctx.self.path} save get msg SaveEmpty")
          val mapInfo = essfMap.map{
            essf=>
              if(essf._2.leftF == -1L){
                (essf._1,EssfMapJoinLeftInfo(essf._2.joinF,endF))
              }else{
                essf
              }
          }
          recorder.putMutableInfo(AppSettings.essfMapKeyName,userMapEncode(mapInfo))
          recorder.finish()
          val deleteFile = new File(s.fileName)
          if(deleteFile.isFile && deleteFile.exists()){
            deleteFile.delete()
          }else{
            log.error(s"delete file error, file is ${s.fileName}")
          }
          if(s.stop == 1) ctx.self ! StopRecord
          initRecorder(roomId,gameRecordData.fileName,fileIndex,gameInformation, userMap)

        case unknow =>
          log.warn(s"${ctx} save got unknow msg ${unknow.getClass}")
          stashBuffer.stash(unknow)
          Behaviors.same
      }

    }

  }


  private def initRecorder(
                            roomId: Long,
                            fileName: String,
                            fileIndex:Int,
                            gameInformation: GameInformation,
                            userMap: mutable.HashMap[String,(Int,String)]
                          )(
                            implicit stashBuffer:StashBuffer[Command],
                            timer:TimerScheduler[Command],
                            middleBuffer: MiddleBufferInJvm
                          ):Behavior[Command] = {
    Behaviors.receive{(ctx,msg) =>
      msg match {
        case t:GameRecord =>
          log.info(s"${ctx.self.path} init get msg gameRecord")
          val startF = t.event._2.get match {
            case tank:TankGameSnapshot =>
              tank.state.f
          }
          val startTime = System.currentTimeMillis()
          val newInitStateOpt =t.event._2
          val newRecorder = initFileRecorder(fileName,fileIndex + 1, gameInformation, newInitStateOpt)
          val newGameInformation = GameInformation(startTime,gameInformation.tankConfig)
          val newGameRecorderData = GameRecorderData(roomId, fileName, fileIndex + 1, newGameInformation, newInitStateOpt, newRecorder, gameRecordBuffer = List[GameRecord]())
          val newEssfMap = mutable.HashMap.empty[EssfMapKey, EssfMapJoinLeftInfo]
          val newUserAllMap = mutable.HashMap.empty[String,(Int,String)]
          userMap.foreach{
            user=>
              newEssfMap.put(EssfMapKey(user._2._1,user._1,user._2._2), EssfMapJoinLeftInfo( startF, -1L))
              newUserAllMap.put(user._1, user._2)
          }
          switchBehavior(ctx,"work",work(newGameRecorderData, newEssfMap, newUserAllMap, userMap, startF, -1L))

        case StopRecord=>
          log.info(s"${ctx.self.path} room close, stop record ")
          Behaviors.stopped

        case unknow =>
          log.warn(s"${ctx} initRecorder got unknow msg ${unknow}")
          Behaviors.same
      }
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
