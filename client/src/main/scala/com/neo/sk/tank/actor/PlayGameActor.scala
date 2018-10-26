package com.neo.sk.tank.actor

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{Behaviors, StashBuffer, TimerScheduler}
import akka.http.javadsl.model.ws.WebSocketRequest
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.stream.scaladsl.Flow
import akka.stream.typed.scaladsl.ActorSink
import com.neo.sk.tank.shared.protocol.TankGameEvent.{CompleteMsgServer, FailMsgServer, WsMsgSource}
import org.seekloud.byteobject.ByteObject.bytesDecode
import org.seekloud.byteobject.MiddleBufferInJvm
import org.slf4j.LoggerFactory
/**
  * Created by hongruying on 2018/10/23
  * 连接游戏服务器的websocket Actor
  */
object PlayGameActor {
  private val log = LoggerFactory.getLogger(this.getClass)
  sealed trait Command
  final case class ConnectGame(name:String) extends Command

  /**进入游戏连接参数*/
  def create()={
    Behaviors.setup[Command]{ctx=>
      implicit val stashBuffer = StashBuffer[Command](Int.MaxValue)
      Behaviors.withTimers[Command]{timer=>
        init()
      }

    }
  }

  def init()(
    implicit stashBuffer: StashBuffer[Command],
    timer:TimerScheduler[Command])={
    Behaviors.receive[Command]{(ctx,msg)=>
      msg match {
        case msg:ConnectGame=>
          val url=getWebSocketUri(msg.name)
          val webSocketFlow=Http().webSocketClientFlow(WebSocketRequest(url))

          Behaviors.same
        case x=>
          Behaviors.unhandled
      }
    }
  }

  def play(implicit stashBuffer: StashBuffer[Command],
           timer:TimerScheduler[Command])={
    Behaviors.receive[Command]{(ctx,msg)=>
      msg match {
        case x=>
          Behaviors.unhandled
      }
    }
  }

  def getSink =
    Flow[Message].collect {
      case TextMessage.Strict(msg) =>
        log.debug(s"msg from webSocket: $msg")

      case BinaryMessage.Strict(bMsg) =>
        //decode process.
        val buffer = new MiddleBufferInJvm(bMsg.asByteBuffer)
        val msg =
          bytesDecode[](buffer) match {
            case Right(v) => v
            case Left(e) =>
              println(s"decode error: ${e.message}")
          }
        msg
    }.to(ActorSink.actorRef[WsMsgSource](actor, CompleteMsgServer, FailMsgServer))

  def getWebSocketUri(name: String): String = {
    val wsProtocol = "ws"
    val host ="localhost:30369"
    s"$wsProtocol://$host/tank/game/join?name=$name"
  }
}
