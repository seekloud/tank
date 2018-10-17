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
import com.neo.sk.tank.Boot.{executor, roomManager, scheduler, timeout, userManager}
//import com.neo.sk.tank.core.RoomManager.{GetRoomIdListReq, GetRoomListRsp}
import com.neo.sk.tank.core.UserManager
import com.neo.sk.tank.shared.ptcl.ErrorRsp
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.ErrorMsg


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
  case class GetRoomPlayersReq(roomId:Int)
  private def getRoomPlayerListErrorRsp(msg:String) = ErrorRsp(100001,msg)
//  private val getRoomPlayerList = (path("getRoomPlayerList") & post){
//    entity(as[Either[Error,GetRoomPlayersReq]]){
//      case Right(req) =>
//        val resFuture:Future[] = roomManager ? req
//      case Left(error) =>
//
//    }
//    val roomListFutureRsp:Future[GetRoomListRsp] = roomManager ? (GetRoomIdListReq(_))
//    dealFutureResult{
//      roomListFutureRsp.map{roomList =>
//        complete(roomList)
//      }.recover{
//        case e:Exception =>
//          log.debug(s"get room id list error:$e")
//          complete(getRoomPlayerListErrorRsp(s"get room id list error:$e"))
//      }
//    }
//
//  }



  private def watchGamePath = path("watch"){
    val flowFuture:Future[Flow[Message,Message,Any]] = userManager ? (UserManager.GetWebSocketFlow4WatchGame(_))
    dealFutureResult(flowFuture.map(handleWebSocketMessages(_)))
  }



  lazy val routes: Route = pathPrefix(AppSettings.rootPath) {
    resourceRoutes ~
      (pathPrefix("game") & get){
        pathEndOrSingleSlash{
          getFromResource("html/admin.html")
        } ~
        watchGamePath~
//          getRoomPlayerList~
          path("join"){
          parameter('name){ name =>
            log.debug(s"sssssssssname=${name}")
            val flowFuture:Future[Flow[Message,Message,Any]] = userManager ? (UserManager.GetWebSocketFlow(name,_))
            complete("sss")
            dealFutureResult(
              flowFuture.map(t => handleWebSocketMessages(t))
            )
          }
        }

      }
  }




}
