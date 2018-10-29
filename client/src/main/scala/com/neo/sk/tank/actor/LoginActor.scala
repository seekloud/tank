package com.neo.sk.tank.actor

import akka.actor.typed._
import akka.actor.typed.scaladsl.{Behaviors, TimerScheduler}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.{WebSocketRequest, _}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.typed.scaladsl.{ActorSink, ActorSource}
import akka.util.{ByteString, ByteStringBuilder}
import com.neo.sk.tank.controller.LoginScreenController
import com.neo.sk.tank.model._
import org.seekloud.byteobject.MiddleBufferInJvm
import org.slf4j.LoggerFactory
import utils.EsheepClient

import scala.concurrent.Future
import scala.concurrent.duration._
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
        case WSLogin(url) =>
          val webSocketFlow = Http().webSocketClientFlow(WebSocketRequest(url))
          //val source = getSource
          //val sink = getSink(controller)
          val ((stream, response), closed) =
            Source.actorRef(10,OverflowStrategy.dropHead)
              .viaMat(webSocketFlow)(Keep.both) // keep the materialized Future[WebSocketUpgradeResponse]
              .toMat(incoming)(Keep.both) // also keep the Future[Done]
              .run()

          val connected = response.flatMap { upgrade =>
            if (upgrade.response.status == StatusCodes.SwitchingProtocols) {
              // ctx.schedule(10.seconds, stream, TextMessage.Strict("hello"))
              //ctx.system.scheduler.schedule(1 seconds,30 minutes,stream,TextMessage.Strict("hello")
              Future.successful(s"$log connect success.")
            } else {
              throw new RuntimeException(s"WSClient connection failed: ${upgrade.response.status}")
            }
          } //链接建立时
          connected.onComplete(i => log.info(i.toString))
          //					closed.onComplete { i =>
          //						log.error(s"$logPrefix connection closed!")
          //					} //链接断开时
          Behaviors.same



          Behaviors.same


        case _ =>
          Behaviors.same

      }
    }
  }




  val incoming =
    Sink.foreach[Message] {
      case msg: WSLoginInfo =>
        if(msg.errCode == 0){
          log.debug(s" ws receive userInfo msg: ${msg}")
          msg.data
        }else{
          log.debug(s" ws receive userInfo error")
        }
      case unknown =>
        log.error(s"wsclient receive unknown message:$unknown")
    }



}
