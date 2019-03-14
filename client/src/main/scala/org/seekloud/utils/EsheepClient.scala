/*
 * Copyright 2018 seekloud (https://github.com/seekloud)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.seekloud.utils

import org.seekloud.tank.model._
import org.seekloud.tank.shared.ptcl.ErrorRsp
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import org.seekloud.tank.ClientApp._
import org.seekloud.tank.common.AppSettings

/**
  * Created by hongruying on 2018/10/16
  */
object EsheepClient extends HttpUtil {

  import io.circe.generic.auto._
  import io.circe.parser.decode
  import io.circe.syntax._

  private val log = LoggerFactory.getLogger(this.getClass)

  //  private val baseUrl = s"${AppSettings.esheepProtocol}://${AppSettings.esheepHost}:${AppSettings.esheepPort}"
  private val baseUrl = s"${AppSettings.esheepProtocol}://${AppSettings.esheepDomain}"

  private val gameId = AppSettings.esheepGameId

  private val gameServerKey = AppSettings.esheepGameKey

  def getLoginInfo(): Future[Either[ErrorRsp, LoginInfo]] = {
    val methodName = s"login"
    val url = s"${baseUrl}/esheep/api/gameAgent/login"

    getRequestSend(methodName, url, Nil).map {
      case Right(jsonStr) =>
        decode[LoginResponse](jsonStr) match {
          case Right(rsp) =>
            if (rsp.errCode == 0) {
              Right(rsp.data)
            } else {
              println(s"${methodName} failed,error:${rsp.msg}")
              Left(ErrorRsp(rsp.errCode, rsp.msg))
            }
          case Left(error) =>
            log.info(s"${methodName} parse json error:${error.getMessage}")
            Left(ErrorRsp(-1, error.getMessage))
        }
      case Left(error) =>
        log.info(s"${methodName}  failed,error:${error.getMessage}")
        Left(ErrorRsp(-1, error.getMessage))
    }
  }

  def validateByEmail(mail: String, pwd: String) = {
    val methodName = s"validate email"
    val url = s"$baseUrl/esheep/rambler/login"
    val data = LoginReq(mail, pwd).asJson.noSpaces

    postJsonRequestSend(methodName, url, Nil, data).map {
      case Right(jsonStr) =>
        decode[ESheepUserInfoRsp](jsonStr) match {
          case Right(rsp) =>
            if (rsp.errCode == 0) {
              Right(rsp)
            } else {
              log.debug(s"$methodName failed,error:${rsp.msg}")
              Left(ErrorRsp(rsp.errCode, rsp.msg))
            }
          case Left(error) =>
            println(s"$methodName parse json error:${error.getMessage}")
            Left(ErrorRsp(-1, error.getMessage))
        }
      case Left(error) =>
        println(s"$methodName  failed,error:${error.getMessage}")
        Left(ErrorRsp(-1, error.getMessage))
    }
  }


  def linkGameAgent(token: String, playerId: String): Future[Either[ErrorRsp, GameServerData]] = {
    val methodName = s"joinGame"
    val url = s"${baseUrl}/esheep/api/gameAgent/joinGame?token=$token"

    val data = JoinGameReq(gameId, playerId).asJson.noSpaces


    postJsonRequestSend(methodName, url, Nil, data).map {
      case Right(jsonStr) =>
        println(jsonStr)
        decode[JoinGameRsp](jsonStr) match {
          case Right(rsp) =>
            if (rsp.errCode == 0) {
              Right(rsp.data)
            } else {
              log.debug(s"${methodName} failed,error:${rsp.msg}")
              Left(ErrorRsp(rsp.errCode, rsp.msg))
            }
          case Left(error) =>
            println(s"${methodName} parse json error:${error.getMessage}")
            Left(ErrorRsp(-1, error.getMessage))
        }
      case Left(error) =>
        println(s"${methodName}  failed,error:${error.getMessage}")
        Left(ErrorRsp(-1, error.getMessage))
    }
  }

  def getBotToken(botId:String,botKey:String): Future[Either[String, BotInfo]] ={
    val url = baseUrl + "/esheep/api/sdk/botKey2Token"
    val data = BotKeyReq(botId,botKey).asJson.noSpaces
    postJsonRequestSend("post",url,Nil,data).map{
      case Right(r)=>
        decode[BotKeyRes](r) match {
          case Right(res)=>
            if(res.errCode==0) Right(res.data)
            else Left(res.msg)
          case Left(e)=>
            Left(s"decode error: $e")
        }
      case Left(e)=>
        Left(s"getBotToken error: $e")
    }
  }

  def refreshToken(token: String, playerId: String): Future[Either[ErrorRsp, TokenInfo]] = {
    val methodName = s"gaRefreshToken"
    val url = s"${baseUrl}/esheep/api/gameAgent/gaRefreshToken?token=$token"

    val data = gaRefreshTokenReq(playerId).asJson.noSpaces


    postJsonRequestSend(methodName, url, Nil, data).map {
      case Right(jsonStr) =>
        println(jsonStr)
        decode[gaRefreshTokenRsp](jsonStr) match {
          case Right(rsp) =>
            if (rsp.errCode == 0) {
              Right(rsp.data)
            } else {
              log.debug(s"${methodName} failed,error:${rsp.msg}")
              Left(ErrorRsp(rsp.errCode, rsp.msg))
            }
          case Left(error) =>
            println(s"${methodName} parse json error:${error.getMessage}")
            Left(ErrorRsp(-1, error.getMessage))
        }
      case Left(error) =>
        println(s"${methodName}  failed,error:${error.getMessage}")
        Left(ErrorRsp(-1, error.getMessage))
    }
  }


}
