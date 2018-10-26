package com.neo.sk.tank.actor

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.{WebSocketRequest, _}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Flow, Keep}
import akka.stream.typed.scaladsl.{ActorSink, ActorSource}
import akka.util.ByteString
import com.neo.sk.tank.controller.LoginScreenController
import com.neo.sk.tank.model._
import org.seekloud.byteobject.MiddleBufferInJvm
import org.slf4j.LoggerFactory
import utils.EsheepClient

import scala.util.{Failure, Success}

/**
  * Created by hongruying on 2018/10/23
  */
object LoginActor {

  sealed trait Command

  final case object Login extends Command
  final case class WSLogin(url:String) extends Command

  final case class Request(m: String) extends Command
  private val log = LoggerFactory.getLogger(this.getClass)

  def create(controller: LoginScreenController): Behavior[Command] = {
    Behaviors.receive[Command]{ (ctx, msg) =>
      msg match {
        case Login =>
          EsheepClient.getLoginInfo().onComplete{
            case Success(rst) =>
              rst match {
                case Right(value) =>
                  controller.showScanUrl(value.scanUrl)
                  ctx.self ! WSLogin(value.wsUrl)
                  idle(controller)
                case Left(error) =>
                  //异常
                  println(error)
              }
            case Failure(exception) =>
              //异常
              log.warn(s"${ctx.self.path} VerifyAccessCode failed, error:${exception.getMessage}")
          }
          idle(controller)
        case _=>
          Behaviors.same
      }
    }
  }

  def idle(controller: LoginScreenController): Behavior[Command] = {
    Behaviors.receive[Command]{ (ctx, msg) =>
      msg match {
        case WSLogin(m) =>
          val webSocketFlow = Http().webSocketClientFlow(WebSocketRequest(url))
          val source = getSource
          val sink = getSink(gameController)
          val ((stream, response), _) =
            source
              .viaMat(webSocketFlow)(Keep.both)
              .toMat(sink)(Keep.both)
              .run()



          Behaviors.same


        case _ =>
          Behaviors.same

      }
    }
  }


  def getSink(actor: ActorRef[WsMsgSource]) =
    Flow[Message].collect {
      case TextMessage.Strict(msg) =>
        log.debug(s"msg from webSocket: $msg")
        TextMsg(msg)

      case BinaryMessage.Strict(bMsg) =>
        //decode process.
        val buffer = new MiddleBufferInJvm(bMsg.asByteBuffer)
        val msg =
          bytesDecode[GameMessage](buffer) match {
            case Right(v) => v
            case Left(e) =>
              println(s"decode error: ${e.message}")
              TextMsg("decode error")
          }
        msg
    }.to(ActorSink.actorRef[WsMsgSource](actor, CompleteMsgServer, FailMsgServer))

  def getSource = ActorSource.actorRef[WsSendMsg](
    completionMatcher = {
      case WsSendComplete =>
    }, failureMatcher = {
      case WsSendFailed(ex) ⇒ ex
    },
    bufferSize = 8,
    overflowStrategy = OverflowStrategy.fail
  ).collect {
    case message: UserAction =>
      val sendBuffer = new MiddleBufferInJvm(409600)
      BinaryMessage.Strict(ByteString(
        message.fillMiddleBuffer(sendBuffer).result()
      ))
  }



}
