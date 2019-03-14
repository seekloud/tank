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

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.{Message, TextMessage, WebSocketRequest}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Keep, Sink, Source}
import org.seekloud.tank.App.tokenActor
import org.seekloud.tank.controller.LoginScreenController
import org.seekloud.tank.model._
import org.seekloud.utils.EsheepClient
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.util.{Failure, Success}
import concurrent.duration._
import org.seekloud.tank.App._
import io.circe.parser.decode
import io.circe.syntax._
import io.circe._
import io.circe.generic.auto._
import org.seekloud.tank.game.control.BotViewController


/**
  * Created by hongruying on 2018/10/23
  */
object LoginActor {
  import LoginScreenController.loginScreenController
  sealed trait Command

  final case object Login extends Command
  final case class WSLogin(url:String) extends Command
  final case object QrLogin extends Command
  final case object EmailLogin extends Command
  final case class EmailLogin(mail:String, pwd:String) extends Command
  final case class BotLogin(req:BotKeyReq,replyTo:ActorRef[(PlayerInfo,GameServerInfo)]) extends Command
  final case class Request(m: String) extends Command
  final case object StopWs extends Command

  private val log = LoggerFactory.getLogger(this.getClass)
  private final case object RefreshTokenKey
  private final val refreshTime = 1.hour

  def create(): Behavior[Command] = {
    Behaviors.receive[Command]{ (ctx, msg) =>
      idle()
    }
  }


  def idle(): Behavior[Command] = {
    Behaviors.receive[Command]{ (ctx, msg) =>
      msg match {
        case Login=>
          ctx.self ! QrLogin
          Behaviors.same

        case QrLogin =>
          EsheepClient.getLoginInfo().onComplete{
            case Success(rst) =>
              rst match {
                case Right(value) =>
                  loginScreenController.showScanUrl(value.scanUrl)
                  ctx.self ! WSLogin(value.wsUrl)
                case Left(error) =>
                  log.error(s"获取二维码失败$error")
                  loginScreenController.showLoginError("获取二维码失败")
              }
            case Failure(exception) =>
              log.warn(s"${ctx.self.path} VerifyAccessCode failed, error:${exception.getMessage}")
              loginScreenController.showLoginError("获取二维码失败")
          }
          Behaviors.same

        case EmailLogin =>
          loginScreenController.showEmailLogin()
          Behaviors.same

        case EmailLogin(mail, pwd) =>
          EsheepClient.validateByEmail(mail, pwd).map{
            case Right(validateRst) =>
              EsheepClient.linkGameAgent(validateRst.token,s"user${validateRst.userId}").map{
                case Right(linkRst) =>
                  tokenActor ! TokenActor.InitToken(validateRst.token,7200,s"user${validateRst.userId}")
                  val userInfo = UserInfo(validateRst.userId,validateRst.userName, validateRst.token, 7200)
                  val playerInfo= PlayerInfo(userInfo,s"user${validateRst.userId}", validateRst.userName, linkRst.accessCode)
                  val gameServerInfo = GameServerInfo(linkRst.gsPrimaryInfo.ip, linkRst.gsPrimaryInfo.port, linkRst.gsPrimaryInfo.domain)
                  loginScreenController.showSuccess()
                  loginScreenController.joinGame(playerInfo, gameServerInfo)
                case Left(e) =>
                  log.warn(s"${ctx.self.path} VerifyAccessCode failed, error:$e")
                  loginScreenController.showLoginError("邮箱登录失败")
              }
            case Left(e) =>
              log.warn(s"${ctx.self.path} VerifyAccessCode failed, error:$e")
              loginScreenController.showLoginError("邮箱登录失败")
          }
          Behaviors.same

        case BotLogin(req,replyTo)=>
          log.info(req.botId+"==="+req.botKey)
          EsheepClient.getBotToken(req.botId, req.botKey).map{
            case Right(validateRst) =>
              EsheepClient.linkGameAgent(validateRst.token,"bot-" + req.botId).map{
                case Right(linkRst) =>
                  val userInfo = UserInfo(1000l,validateRst.botName, validateRst.token, 7200)
                  val playerInfo= PlayerInfo(userInfo,"bot-" + req.botId, validateRst.botName, linkRst.accessCode)
                  val gameServerInfo = GameServerInfo(linkRst.gsPrimaryInfo.ip, linkRst.gsPrimaryInfo.port, linkRst.gsPrimaryInfo.domain)
                  replyTo ! (playerInfo,gameServerInfo)
                case Left(e) =>
                  log.warn(s"${ctx.self.path} VerifyAccessCode failed, error:$e")
              }
            case Left(e) =>
              log.warn(s"${ctx.self.path} VerifyAccessCode failed, error:$e")
          }
          Behaviors.same

        case WSLogin(url) =>
//          println(s"i got msg $url")
          val webSocketFlow = Http().webSocketClientFlow(WebSocketRequest(url))
          val incoming = getSink()
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
          log.info("ws stop now ")
          Behaviors.stopped


        case _ =>
          Behaviors.same

      }
    }
  }




  def getSink() =
    Sink.foreach[Message] {
      case TextMessage.Strict(msg) =>
        decode[Ws4AgentRsp](msg) match {
          case Right(rsp) =>
            val data = rsp.Ws4AgentRsp.data
            EsheepClient.linkGameAgent(data.token,s"user${data.userId}").onComplete{
              case Success(rst) =>
                rst match {
                  case Right(value) =>
                    tokenActor ! TokenActor.InitToken(data.token,data.tokenExpireTime,s"user${data.userId}")
                    val playerInfo= PlayerInfo(data,s"user${data.userId}", data.nickname,value.accessCode)
                    val gameServerInfo = GameServerInfo(value.gsPrimaryInfo.ip, value.gsPrimaryInfo.port, value.gsPrimaryInfo.domain)
                    loginScreenController.showSuccess()
                    loginScreenController.joinGame(playerInfo, gameServerInfo)
                  case Left(error) =>
                    //异常
                    loginScreenController.showLoginError("登录失败")
                    println(error)
                }
              case Failure(exception) =>
                //异常
                log.warn(s" linkGameAgent failed, error:${exception.getMessage}")
                loginScreenController.showLoginError("登录失败")
            }

          case Left(error) =>
            //controller.showLoginError(error.getMessage)
            log.info(s"not Ws4AgentRsp msg ,msg : ${msg}")
        }


      case unknown =>
        log.error(s"wsclient receive unknown message:$unknown")
    }




}
