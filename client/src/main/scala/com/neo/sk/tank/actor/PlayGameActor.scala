package com.neo.sk.tank.actor

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import akka.http.scaladsl.model.ws.{WebSocketRequest, _}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.stream.scaladsl.{Flow, Keep, Sink}
import akka.stream.OverflowStrategy
import akka.stream.typed.scaladsl.{ActorSink, ActorSource, _}
import akka.util.ByteString
import com.neo.sk.tank.controller.PlayScreenController
import com.neo.sk.tank.shared.protocol.TankGameEvent
import com.neo.sk.tank.shared.protocol.TankGameEvent.{CompleteMsgServer, FailMsgServer, WsMsgSource}
import org.seekloud.byteobject.ByteObject.bytesDecode
import org.seekloud.byteobject.MiddleBufferInJvm
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.Future
import com.neo.sk.tank.App.{executor, materializer, scheduler, system, timeout}
import com.neo.sk.tank.model.{GameServerInfo, PlayerInfo}

/**
  * Created by hongruying on 2018/10/23
  * 连接游戏服务器的websocket Actor
  *
  * @author sky
  */
object PlayGameActor {
  private val log = LoggerFactory.getLogger(this.getClass)
  private final val InitTime = Some(5.minutes)

  sealed trait Command

  final case class ConnectGame(playInfo:PlayerInfo,gameInfo:GameServerInfo,roomInfo:Option[String]) extends Command

  final case object ConnectTimerKey

  private final case object BehaviorChangeKey

  final case class SwitchBehavior(
                                   name: String,
                                   behavior: Behavior[Command],
                                   durationOpt: Option[FiniteDuration] = None,
                                   timeOut: TimeOut = TimeOut("busy time error")
                                 ) extends Command

  case class TimeOut(msg: String) extends Command

  case class DispatchMsg(msg:TankGameEvent.WsMsgFront) extends Command

  private[this] def switchBehavior(ctx: ActorContext[Command],
                                   behaviorName: String, behavior: Behavior[Command], durationOpt: Option[FiniteDuration] = None, timeOut: TimeOut = TimeOut("busy time error"))
                                  (implicit stashBuffer: StashBuffer[Command],
                                   timer: TimerScheduler[Command]) = {
    println(s"${ctx.self.path} becomes $behaviorName behavior.")
    timer.cancel(BehaviorChangeKey)
    durationOpt.foreach(timer.startSingleTimer(BehaviorChangeKey, timeOut, _))
    stashBuffer.unstashAll(ctx, behavior)
  }

  /** 进入游戏连接参数 */
  def create(control: PlayScreenController) = {
    Behaviors.setup[Command] { ctx =>
      implicit val stashBuffer = StashBuffer[Command](Int.MaxValue)
      Behaviors.withTimers[Command] { implicit timer =>
        init(control)
      }
    }
  }

  def init(control: PlayScreenController)(
    implicit stashBuffer: StashBuffer[Command],
    timer: TimerScheduler[Command]): Behavior[Command] = {
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case msg: ConnectGame =>
          val url = getWebSocketUri(msg)
          println(s"url---$url")
          val webSocketFlow = Http().webSocketClientFlow(WebSocketRequest(url))
          val source = getSource
          val sink = getSink(control)
          val ((stream, response), closed) =
            source
              .viaMat(webSocketFlow)(Keep.both)
              .toMat(sink)(Keep.both)
              .run()

          val connected = response.flatMap { upgrade =>
            if (upgrade.response.status == StatusCodes.SwitchingProtocols) {
              ctx.self ! SwitchBehavior("play", play(stream))
              Future.successful(s"${ctx.self.path} connect success.")
            } else {
              throw new RuntimeException(s"${ctx.self.path} connection failed: ${upgrade.response.status}")
            }
          } //链接建立时
          connected.onComplete { i => println(i.toString) }
          closed.onComplete { i =>
            println(s"${ctx.self.path} connect closed! try again 1 minutes later")
            //remind 此处存在失败重试
            switchBehavior(ctx, "init", init(control), InitTime)
            timer.startSingleTimer(ConnectTimerKey, msg, 1.minutes)
          } //链接断开时
          switchBehavior(ctx, "busy", busy(), InitTime)

        case x =>
          println(s"get unKnow msg $x")
          Behaviors.unhandled
      }
    }
  }

  def play(frontActor: ActorRef[TankGameEvent.WsMsgFront])(implicit stashBuffer: StashBuffer[Command],
                                                           timer: TimerScheduler[Command]) = {
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case msg:DispatchMsg=>
          frontActor ! msg.msg
          Behaviors.same

        case x =>
          Behaviors.unhandled
      }
    }
  }

  private def busy()(
    implicit stashBuffer: StashBuffer[Command],
    timer: TimerScheduler[Command]
  ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case SwitchBehavior(name, behavior, durationOpt, timeOut) =>
          switchBehavior(ctx, name, behavior, durationOpt, timeOut)

        case TimeOut(m) =>
          println(s"${ctx.self.path} is time out when busy,msg=${m}")
          Behaviors.stopped

        case unknowMsg =>
          stashBuffer.stash(unknowMsg)
          Behavior.same
      }
    }

  import org.seekloud.byteobject.ByteObject._

  def getSink(control: PlayScreenController) = {
    import scala.language.implicitConversions

    implicit def parseJsonString2WsMsgFront(s: String): TankGameEvent.WsMsgServer = {
      import io.circe.generic.auto._
      import io.circe.parser._
      try {
        val wsMsg = decode[TankGameEvent.WsMsgServer](s).right.get
        wsMsg
      } catch {
        case e: Exception =>
          println(s"parse front msg failed when json parse,s=${s}")
          TankGameEvent.DecodeError()
      }
    }

    Sink.foreach[Message] {
      case TextMessage.Strict(m) =>
        control.wsMessageHandler(m)

      case BinaryMessage.Strict(m) =>
        val buffer = new MiddleBufferInJvm(m.asByteBuffer)
        bytesDecode[TankGameEvent.WsMsgServer](buffer) match {
          case Right(req) =>
            control.wsMessageHandler(req)
          case Left(e) =>
            println(s"decode binaryMessage failed,error:${e.message}")
            control.wsMessageHandler(TankGameEvent.DecodeError())
        }

      case _ =>

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
    * 链接由从平台获得IP和端口后拼接*/
  def getWebSocketUri(info:ConnectGame): String = {
    val wsProtocol = "ws"
    //todo 更改为目标端口
    val host = "10.1.29.250:30369"
//    val host = info.gameInfo.domain
    s"$wsProtocol://$host/tank/game/join?name=${info.playInfo.nickName}" + {info.roomInfo match {
      case Some(r)=>s"&roomId=$r"
      case None=>""
    }}
  }
}
