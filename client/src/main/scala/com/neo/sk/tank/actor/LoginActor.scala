package com.neo.sk.tank.actor

import akka.actor.ActorSystem
import akka.actor.typed._
import akka.actor.typed.scaladsl.{Behaviors, TimerScheduler}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.{WebSocketRequest, _}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import com.neo.sk.tank.controller.LoginScreenController
import com.neo.sk.tank.model._
import org.slf4j.LoggerFactory
import utils.EsheepClient
import io.circe.parser.decode
import io.circe.generic.auto._
import scala.concurrent.Future
import scala.util.{Failure, Success}
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import com.neo.sk.tank.App.{system,materializer,executor}
import com.neo.sk.tank.controller.LoginScreenController

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

  def idle(controller: LoginScreenController)(
  ): Behavior[Command] = {
    Behaviors.receive[Command]{ (ctx, msg) =>
      println("idle")
      msg match {
        case WSLogin(url) =>
          println(s"i got msg ${url}")
          val webSocketFlow = Http().webSocketClientFlow(WebSocketRequest(url))
          val incoming = getSink(controller)
          val ((stream, response), closed) =
            Source.actorRef(10,OverflowStrategy.dropHead)
              .viaMat(webSocketFlow)(Keep.both) // keep the materialized Future[WebSocketUpgradeResponse]
              .toMat(incoming)(Keep.both) // also keep the Future[Done]
              .run()

          val connected = response.flatMap { upgrade =>

            if (upgrade.response.status == StatusCodes.SwitchingProtocols) {
             // heartBeat = Some(ctx.system.scheduler.schedule(1 seconds,30 minutes,stream,TextMessage.Strict("hello")))
              Future.successful(s"${ctx.self.path} connect success.")
            } else {
              throw new RuntimeException(s"${ctx.self.path} connection failed: ${upgrade.response.status}")
            }
          } //链接建立时
          connected.onComplete(i => log.info(i.toString))
          closed.onComplete { i =>

          } //链接断开时

          Behaviors.same



        case _ =>
          Behaviors.same

      }
    }
  }




  def getSink(controller: LoginScreenController) =
    Sink.foreach[Message] {
      case TextMessage.Strict(msg) =>
        decode[Ws4AgentRsp](msg) match {
          case Right(rsp) =>
            println(rsp)
            val data = rsp.Ws4AgentRsp.data
            EsheepClient.linkGameAgent(data.token,s"user${data.userId}").onComplete{
              case Success(rst) =>
                rst match {
                  case Right(value) =>
                    val playerInfo= PlayerInfo(s"user${data.userId}", data.nickname, data.token)
                    val gameServerInfo = GameServerInfo(value.ip, value.port, value.domain)
                    controller.joinGame(playerInfo, gameServerInfo)
                  case Left(error) =>
                    //异常
                    println(error)
                }
              case Failure(exception) =>
                //异常
                log.warn(s" linkGameAgent failed, error:${exception.getMessage}")
            }

          case Left(error) =>
            println("decode error")
           // TextMsg("decode error")
        }


      case unknown =>
        log.error(s"wsclient receive unknown message:$unknown")
    }



}
