package com.neo.sk.tank.front.tankClient

import org.scalajs.dom
import org.scalajs.dom.raw._

/**
  * Created by hongruying on 2018/7/9
  */
class WebSocketClient(
                       url:String,
                       connectSuccessCallback: Event => Unit,
                       connectErrorCallback:Event => Unit,
                       messageHandler:MessageEvent => Unit,
                       closeCallback:Event => Unit
                     ) {

  private var wsSetup = false

  private var websocketStreamOpt : Option[WebSocket] = None

  def getWsState = wsSetup

  def getWebSocketUri: String = {
    val wsProtocol = if (dom.document.location.protocol == "https:") "wss" else "ws"
    s"$wsProtocol://${dom.document.location.host}/${url}"
  }

  def sendMsg(msg:String) = {
    websocketStreamOpt.foreach(_.send(msg))
  }


  def setup():Unit = {
    if(wsSetup){
      println(s"websocket已经启动")
    }else{
      val websocketStream = new WebSocket(getWebSocketUri)
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
