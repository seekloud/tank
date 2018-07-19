package com.neo.sk.tank.front.tankClient

import com.neo.sk.tank.front.common.Routes
import com.neo.sk.tank.front.utils.byteObject.MiddleBufferInJs
import com.neo.sk.tank.shared.ptcl.protocol.{WsFrontProtocol, WsProtocol}
import org.scalajs.dom
import org.scalajs.dom.raw._


/**
  * Created by hongruying on 2018/7/9
  */
class WebSocketClient(
                       connectSuccessCallback: Event => Unit,
                       connectErrorCallback:Event => Unit,
                       messageHandler:MessageEvent => Unit,
                       closeCallback:Event => Unit
                     ) {

  import io.circe.generic.auto._
  import io.circe.syntax._

  private var wsSetup = false

  private var websocketStreamOpt : Option[WebSocket] = None

  def getWsState = wsSetup

  def getWebSocketUri(name:String): String = {
    val wsProtocol = if (dom.document.location.protocol == "https:") "wss" else "ws"
    s"$wsProtocol://${dom.document.location.host}${Routes.wsJoinGameUrl(name)}"
  }

  private val sendBuffer:MiddleBufferInJs = new MiddleBufferInJs(2048)

  def sendMsg(msg:WsFrontProtocol.WsMsgFront) = {
    import com.neo.sk.tank.front.utils.byteObject.ByteObject._
    websocketStreamOpt.foreach{s =>
      s.send(msg.fillMiddleBuffer(sendBuffer).result())
    }
  }


  def setup(name:String):Unit = {
    if(wsSetup){
      println(s"websocket已经启动")
    }else{
      val websocketStream = new WebSocket(getWebSocketUri(name))
      websocketStreamOpt = Some(websocketStream)
      websocketStream.onopen = { (event: Event) =>
        wsSetup = true
        connectSuccessCallback(event)
      }
      websocketStream.onerror = { (event: Event) =>
        wsSetup = false
        websocketStreamOpt = None
        connectErrorCallback(event)
      }

      websocketStream.onmessage = { (event: MessageEvent) =>
//        println(s"recv msg:${event.data.toString}")
        messageHandler(event)
      }

      websocketStream.onclose = { (event: Event) =>
        wsSetup = false
        websocketStreamOpt = None
        closeCallback(event)
      }
    }
  }


}
