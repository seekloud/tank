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
import io.circe.parser.decode
import io.circe.generic.auto._
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.util.{Failure, Success}
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import com.neo.sk.tank.App.{executor, materializer, system, tokenActor}
import com.neo.sk.tank.controller.LoginScreenController
import com.neo.sk.utils.EsheepClient

/**
  * Created by hongruying on 2018/10/23
  */
object LoginActor {

  sealed trait Command

  final case object Login extends Command
  final case class WSLogin(url:String) extends Command
  final case object GetImage extends Command
  final case class Request(m: String) extends Command
  final case object StopWs extends Command
  private val log = LoggerFactory.getLogger(this.getClass)
  private final case object RefreshTokenKey
  private final val refreshTime = 1.hour

  def create(controller: LoginScreenController): Behavior[Command] = {
    Behaviors.receive[Command]{ (ctx, msg) =>
      msg match {
        case Login =>
           ctx.self ! GetImage
           idle(controller)
        case _=>
          Behaviors.same
      }
    }
  }


  def idle(controller: LoginScreenController): Behavior[Command] = {
    Behaviors.receive[Command]{ (ctx, msg) =>
      println("idle")
      msg match {
        case GetImage =>
          EsheepClient.getLoginInfo().onComplete{
            case Success(rst) =>
              rst match {
                case Right(value) =>
                  controller.showScanUrl(value.scanUrl)
                  //controller.showLoginError("获取二维码失败")
                  ctx.self ! WSLogin(value.wsUrl)
                case Left(error) =>
                  //异常
                  println(error)
                  controller.showLoginError("获取二维码失败")
              }
            case Failure(exception) =>
              //异常
              log.warn(s"${ctx.self.path} VerifyAccessCode failed, error:${exception.getMessage}")
              controller.showLoginError("获取二维码失败")
          }
          Behaviors.same

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


        case StopWs =>
           println("ws stop now ")
          Behaviors.stopped


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
                    tokenActor ! TokenActor.InitToken(data.token,data.tokenExpireTime,s"user${data.userId}")
                    val playerInfo= PlayerInfo(data,s"user${data.userId}", data.nickname,value.accessCode)
                    val gameServerInfo = GameServerInfo(value.gsPrimaryInfo.ip, value.gsPrimaryInfo.port, value.gsPrimaryInfo.domain)
                    controller.showSuccess()
                    controller.joinGame(playerInfo, gameServerInfo)
                  case Left(error) =>
                    //异常
                    controller.showLoginError("登录失败")
                    println(error)
                }
              case Failure(exception) =>
                //异常
                log.warn(s" linkGameAgent failed, error:${exception.getMessage}")
                controller.showLoginError("登录失败")
            }

          case Left(error) =>
            //controller.showLoginError(error.getMessage)
            println(s"not Ws4AgentRsp msg ,msg : ${msg}")
        }


      case unknown =>
        log.error(s"wsclient receive unknown message:$unknown")
    }



}
