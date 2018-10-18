package com.neo.sk.tank.http

import akka.actor.{ActorSystem, Scheduler}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.Message
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import akka.stream.scaladsl.Flow
import akka.util.Timeout
import com.neo.sk.tank.common.AppSettings
import akka.actor.typed.scaladsl.AskPattern._

import scala.concurrent.{ExecutionContextExecutor, Future}
import com.neo.sk.tank.Boot.{executor, scheduler, timeout, userManager}
import com.neo.sk.tank.core.UserManager
import org.slf4j.LoggerFactory

/**
  * Created by hongruying on 2018/10/18
  */
trait PlayService extends AuthService{
  import com.neo.sk.utils.CirceSupport._
  import io.circe.generic.auto._

  private val log = LoggerFactory.getLogger(this.getClass)


  private def userJoin = path("userJoin") {
    parameter(
      'name.as[String],
      'userId.as[Long],
      'userName.as[String],
      'accessCode.as[String],
      'roomId.as[Long].?
    ){ case (name, userId, nickName, accessCode, roomIdOpt) =>
      authPlatUser(accessCode){ user =>
//        complete("error")
        val flowFuture:Future[Flow[Message,Message,Any]] = userManager ? (UserManager.GetWebSocketFlow(name,_, Some(user)))
        dealFutureResult(
          flowFuture.map(t => handleWebSocketMessages(t))
        )
      }
    }
  }


  protected val playRoute:Route = userJoin

}