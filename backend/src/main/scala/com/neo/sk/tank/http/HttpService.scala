package com.neo.sk.tank.http

import akka.actor.{ActorSystem, Scheduler}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import akka.util.Timeout
import com.neo.sk.tank.common.AppSettings

import scala.concurrent.{ExecutionContextExecutor, Future}

/**
  * Created by hongruying on 2018/3/11
  */
trait HttpService
  extends ResourceService
  with ServiceUtils {

  import akka.actor.typed.scaladsl.AskPattern._
  import com.neo.sk.utils.CirceSupport._
  import io.circe.generic.auto._

  implicit val system: ActorSystem

  implicit val executor: ExecutionContextExecutor

  implicit val materializer: Materializer

  implicit val timeout: Timeout

  implicit val scheduler: Scheduler


  import akka.actor.typed.scaladsl.adapter._






  lazy val routes: Route = pathPrefix(AppSettings.rootPath) {
    resourceRoutes
  }




}
