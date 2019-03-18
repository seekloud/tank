/*
 *  Copyright 2018 seekloud (https://github.com/seekloud)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.seekloud.tank.core

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage, WebSocketRequest}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Keep, Sink}
import akka.stream.typed.scaladsl.ActorSource
import akka.util.{ByteString, ByteStringBuilder}
import org.seekloud.byteobject.MiddleBufferInJvm
import org.seekloud.tank.common.Route
import org.seekloud.tank.model.{GameServerInfo, JoinRoomRsp, PlayerInfo}
import org.seekloud.tank.shared.protocol.TankGameEvent
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.concurrent.duration._
import org.seekloud.tank.ClientApp._
import io.circe.parser.decode
import io.circe.syntax._
import io.circe._
import io.circe.generic.auto._
import org.seekloud.tank.BotSdkTest
import org.seekloud.tank.game.control.{BotViewController, GameController, UserViewController}

/**
  * Created by hongruying on 2018/10/23
  * 连接游戏服务器的websocket Actor
  * 控制游戏逻辑
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

  case object StopGameActor extends Command

  case class  StartGameLoop(f:Long) extends Command

  case object StopGameLoop extends Command

  case object GameLoopKey

  case object GameLoopTimeOut extends Command

  case class CreateRoomReq(password:String,sender:ActorRef[JoinRoomRsp]) extends Command

  case class JoinRoomReq(roomId:Long,password:String,sender:ActorRef[JoinRoomRsp]) extends Command

  private[this] def switchBehavior(ctx: ActorContext[Command],
                                   behaviorName: String, behavior: Behavior[Command], durationOpt: Option[FiniteDuration] = None, timeOut: TimeOut = TimeOut("busy time error"))
                                  (implicit stashBuffer: StashBuffer[Command],
                                   timer: TimerScheduler[Command]) = {
    log.info(s"${ctx.self.path} becomes $behaviorName behavior.")
    timer.cancel(BehaviorChangeKey)
    durationOpt.foreach(timer.startSingleTimer(BehaviorChangeKey, timeOut, _))
    stashBuffer.unstashAll(ctx, behavior)
  }

  /** 进入游戏连接参数 */
  def create(control: GameController) = {
    Behaviors.setup[Command] { ctx =>
      implicit val stashBuffer = StashBuffer[Command](Int.MaxValue)
      Behaviors.withTimers[Command] { implicit timer =>
        init(control)
      }
    }
  }

  def init(control: GameController)(
    implicit stashBuffer: StashBuffer[Command],
    timer: TimerScheduler[Command]): Behavior[Command] = {
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case msg: ConnectGame =>
          val url = getWebSocketUri(msg)
          log.info(s"url---$url")
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
              ctx.self ! SwitchBehavior("play", play(stream,control))
              Future.successful(s"${ctx.self.path} connect success.")
            } else {
              throw new RuntimeException(s"${ctx.self.path} connection failed: ${upgrade.response.status}")
            }
          } //链接建立时
          connected.onComplete { i => log.info(i.toString) }
          closed.onComplete { i =>
            log.info(s"${ctx.self.path} connect closed! try again 1 minutes later")
            //remind 此处存在失败重试
            ctx.self ! SwitchBehavior("init", init(control), InitTime)
            timer.startSingleTimer(ConnectTimerKey, msg, 1.minutes)
          } //链接断开时
          switchBehavior(ctx, "busy", busy(), InitTime)

        case x =>
          log.info(s"get unKnow msg $x")
          Behaviors.unhandled
      }
    }
  }


  def play(frontActor: ActorRef[TankGameEvent.WsMsgFront],
           control: GameController)(implicit stashBuffer: StashBuffer[Command],
                                                           timer: TimerScheduler[Command]) = {
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case m:CreateRoomReq=>
          log.info("createRoomReq")
          BotViewController.SDKReplyTo = m.sender
          frontActor ! TankGameEvent.CreateRoom(None, Some(m.password))
          Behaviors.same

        case t:JoinRoomReq=>
          BotViewController.SDKReplyTo = t.sender
          frontActor ! TankGameEvent.JoinRoom(Some(t.roomId),Some(t.password))
          Behaviors.same

        case msg:DispatchMsg=>
          frontActor ! msg.msg
          Behaviors.same

        case m:StartGameLoop=>
          timer.startPeriodicTimer(GameLoopKey,GameLoopTimeOut,m.f.millis)
          Behaviors.same

        case StopGameLoop=>
          timer.cancel(GameLoopKey)
          Behaviors.same

        case GameLoopTimeOut=>
          control.logicLoop()
          Behaviors.same

        case StopGameActor=>
          Behaviors.stopped

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
          log.info(s"${ctx.self.path} is time out when busy,msg=${m}")
          Behaviors.stopped

        case unknowMsg =>
          stashBuffer.stash(unknowMsg)
          Behavior.same
      }
    }

  import org.seekloud.byteobject.ByteObject._

  def getSink(control: GameController) = {
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
            log.info(s"decode binaryMessage failed,error:${e.message}")
            control.wsMessageHandler(TankGameEvent.DecodeError())
        }

        //akka http 分片流
      case msg: BinaryMessage.Streamed =>
        log.info(s"ssssssssssss${msg}")
        val f = msg.dataStream.runFold(new ByteStringBuilder().result()) {
          case (s, str) => s.++(str)
        }
        f.map { m =>
          val buffer = new MiddleBufferInJvm(m.asByteBuffer)
          bytesDecode[TankGameEvent.WsMsgServer](buffer) match {
            case Right(req) =>
              control.wsMessageHandler(req)
            case Left(e) =>
              log.info(s"decode binaryMessage failed,error:${e.message}")
              control.wsMessageHandler(TankGameEvent.DecodeError())
          }
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
    bufferSize = 128,
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
    val host = info.gameInfo.domain
    Route.getUserJoinGameWebSocketUri(host,info.playInfo,info.roomInfo)

//    Route.getJoinGameWebSocketUri(info.playInfo.nickName,host,info.roomInfo)
  }
}
