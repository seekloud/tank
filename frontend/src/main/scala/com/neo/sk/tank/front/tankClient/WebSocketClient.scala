package com.neo.sk.tank.front.tankClient

import com.neo.sk.tank.front.common.Routes
import com.neo.sk.tank.shared.game.GameContainerAllState
import com.neo.sk.tank.shared.protocol.TankGameEvent
import com.neo.sk.tank.shared.protocol.TankGameEvent.SyncGameAllState
import org.scalajs.dom
import org.scalajs.dom.Blob
import org.scalajs.dom.raw._
import org.seekloud.byteobject.ByteObject.bytesDecode
import org.seekloud.byteobject.MiddleBufferInJs

import scala.scalajs.js.typedarray.ArrayBuffer


/**
  * Created by hongruying on 2018/7/9
  */
case class WebSocketClient(
                       connectSuccessCallback: Event => Unit,
                       connectErrorCallback:Event => Unit,
                       messageHandler:TankGameEvent.WsMsgServer => Unit,
                       closeCallback:Event => Unit,
                       replay:Boolean = false
                     ) {



  private var wsSetup = false

  private var websocketStreamOpt : Option[WebSocket] = None

  def getWsState = wsSetup

  def getWebSocketUri(name:String): String = {
    val wsProtocol = if (dom.document.location.protocol == "https:") "wss" else "ws"
    s"$wsProtocol://${dom.document.location.host}${Routes.wsJoinGameUrl(name)}"
  }

  def getReplaySocketUri(name:String,uid:Long,rid:Long,wid:Long,f:Int): String = {
    val wsProtocol = if (dom.document.location.protocol == "https:") "wss" else "ws"
    s"$wsProtocol://${dom.document.location.host}${Routes.wsReplayGameUrl(name,uid,rid,wid,f)}"
  }

  private val sendBuffer:MiddleBufferInJs = new MiddleBufferInJs(4096)

  def sendMsg(msg:TankGameEvent.WsMsgFront) = {
    import org.seekloud.byteobject.ByteObject._
    websocketStreamOpt.foreach{s =>
      s.send(msg.fillMiddleBuffer(sendBuffer).result())
    }
  }


  def setup(wsUrl:String):Unit = {
    if(wsSetup){
      println(s"websocket已经启动")
    }else{
      val websocketStream = new WebSocket(wsUrl)

      websocketStreamOpt = Some(websocketStream)
      websocketStream.onopen = { event: Event =>
        wsSetup = true
        connectSuccessCallback(event)
      }
      websocketStream.onerror = { event: Event =>
        wsSetup = false
        websocketStreamOpt = None
        connectErrorCallback(event)
      }

      websocketStream.onmessage = { event: MessageEvent =>
        //        println(s"recv msg:${event.data.toString}")
        event.data match {
          case blobMsg:Blob =>
            val fr = new FileReader()
            fr.readAsArrayBuffer(blobMsg)
            fr.onloadend = { _: Event =>
              val buf = fr.result.asInstanceOf[ArrayBuffer]
              if(replay) messageHandler(replayEventDecode(buf))
              else messageHandler(wsByteDecode(buf))
            }
          case jsonStringMsg:String =>
            import io.circe.generic.auto._
            import io.circe.parser._
            val data = decode[TankGameEvent.WsMsgServer](jsonStringMsg).right.get
            messageHandler(data)
          case unknow =>  println(s"recv unknow msg:${unknow}")
        }

      }

      websocketStream.onclose = { event: Event =>
        wsSetup = false
        websocketStreamOpt = None
        closeCallback(event)
      }
    }
  }

  import org.seekloud.byteobject.ByteObject._

  private def wsByteDecode(a:ArrayBuffer):TankGameEvent.WsMsgServer={
    val middleDataInJs = new MiddleBufferInJs(a)
    bytesDecode[TankGameEvent.WsMsgServer](middleDataInJs) match {
      case Right(r) =>
        r
      case Left(e) =>
        println(e.message)
        TankGameEvent.DecodeError()
    }
  }

  private def replayEventDecode(a:ArrayBuffer):TankGameEvent.WsMsgServer={
    val middleDataInJs = new MiddleBufferInJs(a)
    if (a.byteLength > 0) {
      bytesDecode[List[TankGameEvent.WsMsgServer]](middleDataInJs) match {
        case Right(r) =>
          TankGameEvent.EventData(r)
        case Left(e) =>
          println(e.message)
          replayStateDecode(a)
      }
    }else{
      TankGameEvent.DecodeError()
    }
  }

  private def replayStateDecode(a:ArrayBuffer):TankGameEvent.WsMsgServer={
    val middleDataInJs = new MiddleBufferInJs(a)
    bytesDecode[TankGameEvent.GameSnapshot](middleDataInJs) match {
      case Right(r) =>
        TankGameEvent.SyncGameAllState(r.asInstanceOf[TankGameEvent.TankGameSnapshot].state)
      case Left(e) =>
        println(e.message)
        TankGameEvent.DecodeError()
    }
  }


}
