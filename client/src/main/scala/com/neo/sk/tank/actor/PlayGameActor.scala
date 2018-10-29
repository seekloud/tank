package com.neo.sk.tank.actor

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{Behaviors, StashBuffer, TimerScheduler}
import akka.http.scaladsl.model.ws.WebSocketRequest
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Flow, Keep, Sink}
import akka.stream.typed.scaladsl.{ActorSink, ActorSource}
import akka.util.ByteString
import com.neo.sk.tank.shared.protocol.TankGameEvent
import com.neo.sk.tank.shared.protocol.TankGameEvent.{CompleteMsgServer, FailMsgServer, WsMsgSource}
import org.seekloud.byteobject.ByteObject.bytesDecode
import org.seekloud.byteobject.MiddleBufferInJvm
import org.slf4j.LoggerFactory
import scala.concurrent.duration._
import scala.concurrent.Future
import com.neo.sk.tank.App.{system, executor, materializer}
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
      Behaviors.withTimers[Command]{implicit timer=>
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
          val source = getSource
          val sink = getSink
          val ((stream, response), _) =
            source
              .viaMat(webSocketFlow)(Keep.both)
              .toMat(sink)(Keep.both)
              .run()

          val connected = response.flatMap { upgrade =>
            if (upgrade.response.status == StatusCodes.SwitchingProtocols) {
//              ctx.schedule(10.seconds, stream, NetTest(id, System.currentTimeMillis()))
//              val gameScene = new GameScene()
//              val gameController = new GameController(id, name, accessCode, stageCtx, gameScene, stream)
//              gameController.connectToGameServer
              Future.successful(s"WebSocket connect success.")
            } else {
              throw new RuntimeException(s"WSClient connection failed: ${upgrade.response.status}")
            }
          } //链接建立时
          connected.map(i => log.info(i.toString))
          //					closed.onComplete { i =>
          //						log.error(s"$logPrefix connection closed!")
          //					} //链接断开时

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

  import org.seekloud.byteobject.ByteObject._
  def getSink ={
    import scala.language.implicitConversions

    implicit def parseJsonString2WsMsgFront(s: String): TankGameEvent.WsMsgServer = {
      import io.circe.generic.auto._
      import io.circe.parser._
      try {
        val wsMsg = decode[TankGameEvent.WsMsgServer](s).right.get
        wsMsg
      } catch {
        case e: Exception =>
          log.warn(s"parse front msg failed when json parse,s=${s}")
          TankGameEvent.DecodeError()
      }
    }

    Sink.foreach[Message] {
      case TextMessage.Strict(m) =>
        wsMessageHandler(m)

      case BinaryMessage.Strict(m) =>
        val buffer = new MiddleBufferInJvm(m.asByteBuffer)
        bytesDecode[TankGameEvent.WsMsgServer](buffer) match {
          case Right(req) =>
            wsMessageHandler(req)
          case Left(e) =>
            log.error(s"decode binaryMessage failed,error:${e.message}")
            wsMessageHandler(TankGameEvent.DecodeError())
        }
    }
  }

  def getSource = ActorSource.actorRef[TankGameEvent.WsMsgFrontSource](
    completionMatcher = {
      case TankGameEvent.CompleteMsgFrontServer =>
    }, failureMatcher = {
      case TankGameEvent.FailMsgFrontServer(ex) ⇒ ex
    },
    bufferSize = 8,
    overflowStrategy = OverflowStrategy.fail
  ).collect {
    case message: TankGameEvent.WsMsgFront =>
      val sendBuffer = new MiddleBufferInJvm(409600)
      BinaryMessage.Strict(ByteString(
        message.fillMiddleBuffer(sendBuffer).result()
      ))
  }


  /**
    * 此处处理消息*/
  def wsMessageHandler(m:TankGameEvent.WsMsgServer)={

  }

  def getWebSocketUri(name: String): String = {
    val wsProtocol = "ws"
    val host ="localhost:30369"
    s"$wsProtocol://$host/tank/game/join?name=$name"
  }
}
