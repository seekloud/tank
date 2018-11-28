package com.neo.sk.tank.http

import java.net.URLEncoder

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
import com.neo.sk.tank.Boot.roomManager

import scala.concurrent.{ExecutionContextExecutor, Future}
import com.neo.sk.tank.Boot.{botManager, executor, scheduler, timeout, userManager}
import com.neo.sk.tank.core.{BotManager, UserManager}
import com.neo.sk.tank.protocol.WatchGameProtocol.{GetUserInfoList, UserInfoListByRoomIdRsp}
import com.neo.sk.tank.shared.ptcl.ErrorRsp

import scala.util.Random

/**
  * Created by hongruying on 2018/3/11
  */
trait HttpService
  extends ResourceService
    with ServiceUtils
    with PlayService
    with RoomInfoService
    with RecordApiService
    with GameRecService
    with BotControlService{

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
      'playerId.as[String].?
    ){
      (roomId, accessCode, watchedUserIdOpt) =>
        authPlatUser(accessCode) { user =>
          if (watchedUserIdOpt.nonEmpty) {
            val flowFuture: Future[Flow[Message, Message, Any]] = userManager ? (UserManager.GetWebSocketFlow4WatchGame(roomId, watchedUserIdOpt.get, _, Some(user)))
            dealFutureResult(flowFuture.map(handleWebSocketMessages))
          } else {
            val resFuture: Future[UserInfoListByRoomIdRsp] = roomManager ? (GetUserInfoList(roomId, _))
            dealFutureResult {
              resFuture.map { res =>
                if (res.errCode == 0) {
                  if (res.data.playerList.nonEmpty) {
                    val random = (new Random).nextInt(res.data.playerList.length)
                    val flowFuture: Future[Flow[Message, Message, Any]] = userManager ? (UserManager.GetWebSocketFlow4WatchGame(roomId, res.data.playerList(random).playerId, _, Some(user)))
                    dealFutureResult(flowFuture.map(handleWebSocketMessages))
                  } else {
                    log.debug(s"该房间没有玩家")
                    complete("该房间没有玩家")
                  }
                } else {
                  log.debug(s"该房间未被创建")
                  complete("该房间未被创建")
                }
              }.recover {
                case e: Exception =>
                  log.debug(s"websocket 建立连接失败${e}")
                  complete(s"websocket 建立连接失败${e}")
              }
            }
          }
        }
    }
  }


  def platEnterRoute: Route = path("playGame"){
    parameter(
      'playerId.as[String],
      'playerName.as[String],
      'accessCode.as[String],
      'roomId.as[Long].?
    ) {
      case (playerId, playerName, accessCode, roomIdOpt) =>
        redirect(s"/tank/game/#/playGame/${playerId}/${URLEncoder.encode(playerName, "utf-8")}" + roomIdOpt.map(s => s"/$s").getOrElse("") + s"/$accessCode",
          StatusCodes.SeeOther
        )

    }
  } ~ path("watchGame") {
    parameter(
      'roomId.as[Long],
      'accessCode.as[String],
      'playerId.as[String].?
    ) {
      case (roomId, accessCode, playerIdOpt) =>
        redirect(s"/tank/game/#/watchGame/${roomId}" + playerIdOpt.map(s => s"/$s").getOrElse("") + s"/$accessCode",
          StatusCodes.SeeOther
        )

    }
  } ~ path("watchRecord") {
    parameter(
      'recordId.as[Long],
      'playerId.as[String],
      'frame.as[Long],
      'accessCode.as[String]
    ) {
      case (recordId, playerId, frame, accessCode) =>
        redirect(s"/tank/game/#/watchRecord/${recordId}/${playerId}/${frame}/${accessCode}",
          StatusCodes.SeeOther
        )
    }
  }




  lazy val routes: Route = pathPrefix(AppSettings.rootPath){
    resourceRoutes ~ GameRecRoutes ~ GameRecRoutesLocal ~roomInfoRoute ~ BotRoutes ~ platEnterRoute ~
      (pathPrefix("game") & get){
        pathEndOrSingleSlash{
          getFromResource("html/admin.html")
        } ~ watchGamePath ~
//          getRoomPlayerList~
        path("join"){
          parameter(
            'name,
            'roomId.as[Long].?
          ){ (name,roomIdOpt) =>
            val flowFuture:Future[Flow[Message,Message,Any]] = userManager ? (UserManager.GetWebSocketFlow(name,_,None,roomIdOpt))
            dealFutureResult(
              flowFuture.map(t => handleWebSocketMessages(t))
            )
          }
        } ~ path("replay"){
          parameter(
            'rid.as[Long],
            'wid.as[String],
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
