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

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import org.seekloud.tank.models.TankGameUserInfo
import org.seekloud.tank.protocol.EsheepProtocol.PlayerInfo
import org.seekloud.tank.protocol.WatchGameProtocol._
import org.seekloud.tank.Boot.{roomManager, userManager}
import org.seekloud.tank.core.UserActor
import org.seekloud.utils.HttpUtil
import org.slf4j.LoggerFactory

import concurrent.duration._
import org.seekloud.tank.Boot.{executor, scheduler, timeout}

import scala.concurrent.Future
import akka.actor.typed.scaladsl.AskPattern._
import org.seekloud.tank.shared.ptcl.{ErrorRsp, SuccessRsp}

import scala.concurrent.Future

/**
  * Created by hongruying on 2019/2/3
  */
trait RoomInfoService extends ServiceUtils with HttpUtil with AuthService{
  import io.circe.generic.auto._

  private val log = LoggerFactory.getLogger(this.getClass)

//  /{游戏名}/getRoomId
  private def getRoomIdErrorRsp(msg:String) = ErrorRsp(100002,msg)
  private val getRoomId = (path("getRoomId") & post){
//    entity(as[Either[Error,GetRoomIdReq]]){
//      case Right(req) =>
////        log.debug(s"获取用户的信息&&&&${req.playerId}******")
//        dealFutureResult{
//          val resFuture:Future[RoomIdRsp] = roomManager ? (GetRoomId(req.playerId,_))
//          resFuture.map{res =>
//            complete(res)
//          }.recover{
//            case e:Exception =>
//              log.debug(s"获取用户对应的房间号失败，recover error:$e")
//              complete(getRoomIdErrorRsp(s"获取用户对应的房间号失败，recover error:$e"))
//          }
//        }
//      case Left(e) =>
//        log.debug(s"获取用户对应的房间号失败，error:$e")
//        complete(getRoomIdErrorRsp(s"获取用户对应的房间号失败，error:$e"))
//    }
    dealPostReq[GetRoomIdReq]{req =>
      val resFuture:Future[RoomIdRsp] = roomManager ? (GetRoomId(req.playerId,_))
      resFuture.map{res =>
        complete(res)
      }.recover{
        case e:Exception =>
          log.debug(s"获取用户对应的房间号失败，recover error:$e")
          complete(getRoomIdErrorRsp(s"获取用户对应的房间号失败，recover error:$e"))
      }
    }
  }

//  /{游戏名}/getRoomPlayerList
  private def getRoomPlayerListErrorRsp(msg:String) = ErrorRsp(100003,msg)
  private val getRoomPlayerList = (path("getRoomPlayerList") & post){
//    entity(as[Either[Error,GetUserInfoListReq]]){
//      case Right(req) =>
//        dealFutureResult{
//          val resFuture:Future[UserInfoListByRoomIdRsp] = roomManager ? (GetUserInfoList(req.roomId,_))
//          resFuture.map{res =>
//            complete(res)
//          }.recover{
//            case e:Exception =>
//              log.debug(s"获取房间号对应的玩家列表失败，error:$e")
//              complete(getRoomPlayerListErrorRsp(s"获取房间号对应的玩家列表失败，error:$e"))
//          }
//        }
//      case Left(e) =>
//        log.debug(s"获取房间号对应的玩家列表失败，error:$e")
//        complete(getRoomPlayerListErrorRsp(s"获取房间号对应的玩家列表失败，error:$e"))
//    }
    dealPostReq[GetUserInfoListReq]{req =>
      val resFuture:Future[UserInfoListByRoomIdRsp] = roomManager ? (GetUserInfoList(req.roomId,_))
      resFuture.map{res =>
        complete(res)
      }.recover{
        case e:Exception =>
          log.debug(s"获取房间号对应的玩家列表失败，error:$e")
          complete(getRoomPlayerListErrorRsp(s"获取房间号对应的玩家列表失败，error:$e"))
      }
    }
  }


//  url：/{游戏名}/getRoomList
  case class GetRoomList()
  private def getRoomListErrorRsp(msg:String) = ErrorRsp(100001,msg)
  private val getRoomList = (path("getRoomList") & post & pathEndOrSingleSlash){
//    entity(as[Either[Error,GetRoomList]]){
//      case Right(req) =>
//        dealFutureResult{
//          val resFuture:Future[RoomListRsp] = roomManager ? (GetRoomListReq(_))
//          resFuture.map{res =>
//            complete(res)
//          }.recover{
//            case e:Exception =>
//              complete(getRoomListErrorRsp(s"获取进行中房间列表失败,recover error:$e"))
//          }
//        }
//      case Left(e) =>
//        complete(getRoomListErrorRsp(s"获取进行中房间列表失败,error:$e"))
//    }
    dealGetReq{
      val resFuture:Future[RoomListRsp] = roomManager ? (GetRoomListReq(_))
      resFuture.map{res =>
        complete(res)
      }.recover{
        case e:Exception =>
          complete(getRoomListErrorRsp(s"获取进行中房间列表失败,error:$e"))
      }
    }
  }

  private val getRoomList4App = (path("getRoomList4App") & get & pathEndOrSingleSlash){
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

  private val changeWatchedPlayerId = (path("changeWatchedPlayerId") & get & pathEndOrSingleSlash){
    parameter(
      'uId.as[String],
      'watchedPlayerId.as[String],
      'accessCode.as[String]
    ){(uId,watchedPlayerId,accessCode) =>
      authPlatUser(accessCode){playerInfo =>
        val playerInfo = PlayerInfo(uId,uId)
        val userInfo = TankGameUserInfo(playerInfo.playerId, playerInfo.nickname, playerInfo.nickname, true)
        userManager ! UserActor.ChangeWatchedPlayerId(userInfo,watchedPlayerId)
        complete(SuccessRsp())

      }
    }

  }

  val roomInfoRoute :Route = getRoomList4App ~ getRoomList ~ getRoomId ~ getRoomPlayerList ~ changeWatchedPlayerId

}
