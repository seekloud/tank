package com.neo.sk.utils

import java.io.File

import com.neo.sk.tank.common.AppSettings
import com.neo.sk.tank.shared.protocol.TankGameEvent
import com.neo.sk.tank.shared.protocol.TankGameEvent.{GameEvent, GameInformation, UserActionEvent}
import org.seekloud.byteobject.encoder.BytesEncoder
import org.seekloud.byteobject.{MiddleBuffer, MiddleBufferInJvm}
import org.seekloud.essf.io.{FrameData, FrameInputStream, FrameOutputStream}
import org.slf4j.LoggerFactory

import scala.collection.mutable
/**
  * User: sky
  * Date: 2018/10/11
  * Time: 14:39
  * 本部分实现tank支持ESSF存储文件IO接口
  */
object ESSFSupport {
  import org.seekloud.byteobject.ByteObject._
  private final val log = LoggerFactory.getLogger(this.getClass)

  /**
    * 存储
    * @author hongruying on 2018/8/14
    * */
  def initFileRecorder(fileName:String,index:Int,gameInformation: GameInformation,initStateOpt:Option[TankGameEvent.GameSnapshot] = None)
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
    log.debug(s"file-$fileName-$index init success")
    recorder
  }

  /**
    * 读取*/

  def initFileReader(fileName:String)={
    val input = new FrameInputStream(fileName)
    val info = input.init()
    info
  }

  def readData(input: FrameInputStream)= {
    val info = input.init()
    val name = info.simulatorId
    println(name)
    val version = info.simulatorVersion
    println(version)
    val buffer = new MiddleBufferInJvm(info.simulatorMetadata)
    bytesDecode[GameInformation](buffer) match {
      case Right(req) =>
        println(req)
      case Left(e) =>
        log.error(s"decode binaryMessage failed,error:${e.message}")
    }
    val buffer1 = new MiddleBufferInJvm(info.simulatorInitState)
    bytesDecode[TankGameEvent.GameSnapshot](buffer1) match {
      case Right(req) =>
        println(req)
      case Left(e) =>
        log.error(s"decode binaryMessage failed,error:${e.message}")
    }
    val gameEventMap = mutable.HashMap[Long,List[GameEvent]]() //frame -> List[GameEvent] 待处理的事件 frame >= curFrame
    val actionEventMap = mutable.HashMap[Long,List[UserActionEvent]]() //frame -> List[UserActionEvent]
    while (input.hasMoreFrame) {
      input.readFrame() match {
        case Some(FrameData(idx, ev, stOp)) =>
          val data = if (ev.length > 0) {
            println(idx)
            val buffer = new MiddleBufferInJvm(ev)
            bytesDecode[List[TankGameEvent.WsMsgServer]](buffer) match {
              case Right(req) =>
                println(req)
              case Left(e) =>
                log.error(s"decode binaryMessage failed,error:${e.message}")
            }
            stOp.foreach{r=>
              val buffer = new MiddleBufferInJvm(r)
              bytesDecode[TankGameEvent.GameSnapshot](buffer) match {
                case Right(req) =>
                  println(req)
                case Left(e) =>
                  log.error(s"decode binaryMessage failed,error:${e.message}")
              }
            }
          } else {
            if (stOp.isEmpty) {
              println(None)
            } else {
              throw new RuntimeException("this game can not go to here.")
            }
          }
        case None =>
          println("get to the end, no more frame.")
      }
    }
  }


  def main(args: Array[String]): Unit = {
    initFileReader("C:\\Users\\sky\\IdeaProjects\\tank\\backend\\gameDataDirectoryPath\\tankGame_1539309693971_2")
  }

}
