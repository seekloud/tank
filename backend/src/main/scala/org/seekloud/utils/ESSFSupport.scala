package org.seekloud.utils

import java.io.File

import org.seekloud.tank.protocol.ReplayProtocol.{EssfMapInfo, EssfMapJoinLeftInfo, EssfMapKey}
import org.seekloud.byteobject.MiddleBufferInJvm
import org.seekloud.essf.io.{FrameData, FrameInputStream, FrameOutputStream}
import org.seekloud.tank.common.AppSettings
import org.seekloud.tank.shared.protocol.TankGameEvent
import org.seekloud.tank.shared.protocol.TankGameEvent.GameInformation
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.concurrent.Future

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
    log.debug(s" init success")
    recorder
  }

  /**
    * 读取
    * @author sky*/

  def initFileReader(fileName:String)={
    val input = new FrameInputStream(fileName)
    input
  }

  /**解码*/

  def metaDataDecode(a:Array[Byte])={
    val buffer = new MiddleBufferInJvm(a)
    bytesDecode[GameInformation](buffer)
  }

  def initStateDecode(a:Array[Byte]) ={
    val buffer = new MiddleBufferInJvm(a)
    bytesDecode[TankGameEvent.GameSnapshot](buffer)
  }

  def userMapDecode(a:Array[Byte])={
    val buffer = new MiddleBufferInJvm(a)
    bytesDecode[EssfMapInfo](buffer)
  }

  def userMapEncode(u:mutable.HashMap[EssfMapKey,EssfMapJoinLeftInfo])(implicit middleBuffer: MiddleBufferInJvm)={
    EssfMapInfo(u.toList).fillMiddleBuffer(middleBuffer).result()
  }

  /**用于后端先解码数据然后再进行编码传输*/
  def replayEventDecode(a:Array[Byte]):TankGameEvent.WsMsgServer={
    if (a.length > 0) {
      val buffer = new MiddleBufferInJvm(a)
      bytesDecode[List[TankGameEvent.WsMsgServer]](buffer) match {
        case Right(r) =>
          TankGameEvent.EventData(r)
        case Left(e) =>
          TankGameEvent.DecodeError()
      }
    }else{
      TankGameEvent.DecodeError()
    }
  }

  def replayStateDecode(a:Array[Byte]):TankGameEvent.WsMsgServer={
    val buffer = new MiddleBufferInJvm(a)
    bytesDecode[TankGameEvent.GameSnapshot](buffer) match {
      case Right(r) =>
        TankGameEvent.SyncGameAllState(r.asInstanceOf[TankGameEvent.TankGameSnapshot].state)
      case Left(e) =>
        TankGameEvent.DecodeError()
    }
  }




  def readData(input: FrameInputStream, i : Int = 0)= {
    val info=input.init()
    val a=metaDataDecode(info.simulatorMetadata)
//    println(a)
//    println(input.getMutableInfo(AppSettings.essfMapKeyName))
//    println(userMapDecode(input.getMutableInfo(AppSettings.essfMapKeyName).get))
    println(s"all frame=${info.frameCount}")
    while (input.hasMoreFrame) {
      input.readFrame() match {
        case Some(FrameData(idx, ev, stOp)) =>
          val event = replayEventDecode(ev)
          stOp.foreach{r=>
            replayStateDecode(r)
          }
          val len = if(event.isInstanceOf[TankGameEvent.EventData]){
            event.asInstanceOf[TankGameEvent.EventData].list.length
          } else 0

          println(s"frame=${input.getFramePosition}, event=${len}")
          /*if (ev.length > 0) {
            println(idx)
            val buffer = new MiddleBufferInJvm(ev)
            bytesDecode[List[TankGameEvent.WsMsgServer]](buffer) match {
              case Right(req) =>
                println(req)
              case Left(e) =>
                log.error(s"decode binaryMessage failed,error:${e.message}")
            }
//            replayEventDecode(ev)
            stOp.foreach{r=>
              /*val buffer = new MiddleBufferInJvm(r)
              bytesDecode[TankGameEvent.GameSnapshot](buffer) match {
                case Right(req) =>
                  println(req)
                case Left(e) =>
                  log.error(s"decode binaryMessage failed,error:${e.message}")
              }*/
              replayStateDecode(r)
            }
          } else {
            if (stOp.isEmpty) {
              println(None)
            } else {
              throw new RuntimeException("this game can not go to here.")
            }
          }*/
        case None =>
          println("get to the end, no more frame.")
      }
    }

    println(s"finsih=${i}")
  }


  def main(args: Array[String]): Unit = {
    import concurrent.ExecutionContext.Implicits.global
    for(i <- 1 to 20)
    Future{
      readData(initFileReader("D:\\software_data\\ideaProject\\tank\\tankGame_1541561905459_2"), i)
    }

    Thread.sleep(100000000)

//    readData(initFileReader("D:\\software_data\\ideaProject\\tank\\tankGame_1541561905459_2"))
//    readData(initFileReader("D:\\software_data\\ideaProject\\tank\\tankGame_1541561905459_2"))
//    readData(initFileReader("D:\\software_data\\ideaProject\\tank\\tankGame_1541561905459_2"))
//    readData(initFileReader("D:\\software_data\\ideaProject\\tank\\tankGame_1541561905459_2"))
//    readData(initFileReader("D:\\software_data\\ideaProject\\tank\\tankGame_1541561905459_2"))
  }

}
