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
import com.neo.sk.tank.shared.ptcl.ErrorRsp

/**
  * Created by hongruying on 2018/3/11
  */
trait HttpService
  extends ResourceService
    with ServiceUtils
    with PlayService
    with RoomInfoService{

  import akka.actor.typed.scaladsl.AskPattern._
  import com.neo.sk.utils.CirceSupport._
  import io.circe.generic.auto._
  import io.circe._

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



  private def watchGamePath = (path("watchGame") & get & pathEndOrSingleSlash){
    parameter(
      'roomId.as[Long],
      'accessCode.as[String],
      'playerId.as[Long].?
    ){
      (roomId, accessCode, watchedUserIdOpt) =>
//        authPlatUser(accessCode) { user =>
        if (watchedUserIdOpt.nonEmpty){
          val flowFuture:Future[Flow[Message,Message,Any]] = userManager ? (UserManager.GetWebSocketFlow4WatchGame(roomId, watchedUserIdOpt.get, _))
          dealFutureResult(flowFuture.map(handleWebSocketMessages))
        } else {
          complete("暂时不支持随机用户视角观战")
        }

//        }

    }
  }



  lazy val routes: Route = pathPrefix(AppSettings.rootPath) {
    resourceRoutes ~ roomInfoRoute~
      pathPrefix("game") {
        pathEndOrSingleSlash{
          getFromResource("html/admin.html")
        } ~ watchGamePath ~
//          getRoomPlayerList~
        path("join"){
          parameter('name){ name =>
            val flowFuture:Future[Flow[Message,Message,Any]] = userManager ? (UserManager.GetWebSocketFlow(name,_))
            dealFutureResult(
              flowFuture.map(t => handleWebSocketMessages(t))
            )
          }
        } ~ path("replay"){
          parameter(
            'rid.as[Long],
            'wid.as[Long],
            'f.as[Int],
            'accessCode.as[String]
          ){ (rid,wid,f,accessCode) =>
            authPlatUser(accessCode){player=>
              val flowFuture:Future[Flow[Message,Message,Any]] = userManager ? (UserManager.GetReplaySocketFlow(player.nickname,player.playerId,rid,wid,f,_))
              dealFutureResult(
                flowFuture.map(t => handleWebSocketMessages(t))
              )
            }
          }
        } ~ playRoute
      }
  }




}
