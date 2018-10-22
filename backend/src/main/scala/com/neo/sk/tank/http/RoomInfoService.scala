package com.neo.sk.tank.http

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.actor.typed.scaladsl.AskPattern._

import scala.concurrent.Future
import com.neo.sk.tank.Boot.{roomManager, scheduler, executor, timeout}
import com.neo.sk.tank.protocol.WatchGameProtocol._
import com.neo.sk.tank.shared.ptcl.ErrorRsp
import org.slf4j.LoggerFactory
import akka.util.Timeout


trait RoomInfoService extends ServiceUtils{
  import io.circe._
  import io.circe.generic.auto._

  private val log = LoggerFactory.getLogger(this.getClass)

//  /{游戏名}/getRoomId
  private def getRoomIdErrorRsp(msg:String) = ErrorRsp(100002,msg)
  private val getRoomId = (path("getRoomId") & post){
    entity(as[Either[Error,GetRoomIdReq]]){
      case Right(req) =>
        val resFuture:Future[RoomIdRsp] = roomManager ? (GetRoomId(req.playerId,_))
        dealFutureResult{
          resFuture.map{res =>
            complete(res)
          }.recover{
            case e:Exception =>
              log.debug(s"获取用户对应的房间号失败，recover error:$e")
              complete(getRoomIdErrorRsp(s"获取用户对应的房间号失败，recover error:$e"))
          }
        }
      case Left(error) =>
        log.debug(s"获取用户对应的房间号失败，error:$error")
        complete(getRoomIdErrorRsp(s"获取用户对应的房间号失败，error:$error"))

    }
  }

//  /{游戏名}/getRoomPlayerList
  private def getRoomPlayerListErrorRsp(msg:String) = ErrorRsp(100003,msg)
  private val getRoomPlayerList = (path("getRoomPlayerList") & post){
    entity(as[Either[Error,GetUserInfoListReq]]){
      case Right(req) =>
        val resFuture:Future[UserInfoListByRoomIdRsp] = roomManager ? (GetUserInfoList(req.roomId,_))
        dealFutureResult{
          resFuture.map{res =>
            complete(res)
          }.recover{
            case e:Exception =>
              log.debug(s"获取房间号对应的玩家列表失败，error:$e")
              complete(getRoomPlayerListErrorRsp(s"获取房间号对应的玩家列表失败，error:$e"))
          }
        }
      case Left(error) =>
        log.debug(s"获取房间对应的用户列表失败，error:$error")
        complete(getRoomPlayerListErrorRsp(s"获取房间对应的用户列表失败，error:$error"))
    }
  }


//  url：/{游戏名}/getRoomList
  private def getRoomListErrorRsp(msg:String) = ErrorRsp(100001,msg)
  private val getRoomList = (path("getRoomList") & get & pathEndOrSingleSlash){
    val resFuture:Future[RoomListRsp] = roomManager ? (GetRoomListReq(_))
    dealFutureResult{
      resFuture.map{res =>
        complete(res)
      }.recover{
        case e:Exception =>
          complete(getRoomListErrorRsp(s"获取进行中房间列表失败,error:$e"))
      }
    }
  }
  val roomInfoRoute :Route = getRoomList ~ getRoomId ~ getRoomPlayerList

}
