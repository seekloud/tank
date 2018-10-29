package com.neo.sk.tank.actor

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import akka.http.javadsl.model.ws.WebSocketRequest
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
/**
  * Created by hongruying on 2018/10/23
  * 连接游戏服务器的websocket Actor
  */
object PlayGameActor {
  private val log = LoggerFactory.getLogger(this.getClass)
  sealed trait Command
  final case class ConnectGame(name:String) extends Command
  final case object ConnectTimerKey
  private final case object BehaviorChangeKey

  final case class SwitchBehavior(
                                   name: String,
                                   behavior: Behavior[Command],
                                   durationOpt: Option[FiniteDuration] = None,
                                   timeOut: TimeOut = TimeOut("busy time error")
                                 ) extends Command

  case class TimeOut(msg:String) extends Command

  private[this] def switchBehavior(ctx: ActorContext[Command],
                                   behaviorName: String, behavior: Behavior[Command], durationOpt: Option[FiniteDuration] = None,timeOut: TimeOut  = TimeOut("busy time error"))
                                  (implicit stashBuffer: StashBuffer[Command],
                                   timer:TimerScheduler[Command]) = {
    log.debug(s"${ctx.self.path} becomes $behaviorName behavior.")
    timer.cancel(BehaviorChangeKey)
    durationOpt.foreach(timer.startSingleTimer(BehaviorChangeKey,timeOut,_))
    stashBuffer.unstashAll(ctx,behavior)
  }

  /**进入游戏连接参数*/
  def create={
    Behaviors.setup[Command]{ctx=>
      implicit val stashBuffer = StashBuffer[Command](Int.MaxValue)
      Behaviors.withTimers[Command]{timer=>
        init
      }

    }
  }

  def init(
    implicit stashBuffer: StashBuffer[Command],
    timer:TimerScheduler[Command])={
    Behaviors.receive[Command]{(ctx,msg)=>
      msg match {
        case msg:ConnectGame=>
          val url=getWebSocketUri(msg.name)
          val webSocketFlow=Http().webSocketClientFlow(WebSocketRequest(url))
          val source = getSource
          val sink = getSink
          val ((stream, response), closed) =
            source
              .viaMat(webSocketFlow)(Keep.both)
              .toMat(sink)(Keep.both)
              .run()

          val connected = response.flatMap { upgrade =>
            if (upgrade.response.status == StatusCodes.SwitchingProtocols) {
              ctx.schedule(10.seconds, stream, TankGameEvent.PingPackage(System.currentTimeMillis()))
              Future.successful(s"${ctx.self.path} connect success.")
            } else {
              throw new RuntimeException(s"${ctx.self.path} connection failed: ${upgrade.response.status}")
            }
          } //链接建立时
          connected.onComplete{i => log.info(i.toString)}
          closed.onComplete { i =>
            log.error(s"${ctx.self.path} connect closed! try again 1 minutes later")
            timer.startSingleTimer(ConnectTimerKey,msg,1 minute)
          } //链接断开时
          busy()

        case x=>
          log.info(s"get unKnow msg $x")
          Behaviors.unhandled
      }
    }
  }

  def play(frontActor:ActorRef[TankGameEvent.WsMsgFront])(implicit stashBuffer: StashBuffer[Command],
           timer:TimerScheduler[Command])={
    Behaviors.receive[Command]{(ctx,msg)=>
      msg match {
        case x=>
          Behaviors.unhandled
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
