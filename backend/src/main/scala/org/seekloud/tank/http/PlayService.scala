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

package org.seekloud.tank.http

import akka.http.scaladsl.model.ws.Message
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.Flow
import org.seekloud.tank.Boot.userManager
import org.seekloud.tank.core.UserManager
import org.slf4j.LoggerFactory
import concurrent.duration._
import org.seekloud.tank.Boot.{executor, timeout, scheduler}
import scala.concurrent.Future
import akka.actor.typed.scaladsl.AskPattern._
import scala.concurrent.Future

/**
  * Created by hongruying on 2018/10/18
  */
trait PlayService extends AuthService{

  private val log = LoggerFactory.getLogger(this.getClass)


  private def userJoin = path("userJoin") {
    parameter(
      'name.as[String],
      'userId.as[String],
      'userName.as[String],
      'accessCode.as[String],
      'roomId.as[Long].?
    ){ case (name, userId, nickName, accessCode, roomIdOpt) =>
      authPlatUser(accessCode){ user =>
//        complete("error")
        val flowFuture:Future[Flow[Message,Message,Any]] = userManager ? (UserManager.GetWebSocketFlow(name,_, Some(user),roomIdOpt))
        dealFutureResult(
          flowFuture.map(t => handleWebSocketMessages(t))
        )
      }
    }
  }


  protected val playRoute:Route = userJoin

}
