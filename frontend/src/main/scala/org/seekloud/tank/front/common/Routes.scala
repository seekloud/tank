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

package org.seekloud.tank.front.common

import org.scalajs.dom
import org.seekloud.tank.front.model._

/**
  * User: Taoz
  * Date: 2/24/2017
  * Time: 10:59 AM
  */
object Routes {

  val base = "/tank"
  val getRoomListRoute = base + "/getRoomIdList"

  val getRecordListUrl = base + s"/getGameRec"
  val getRecordListByPlayerUrl = base + s"/getGameRecByPlayer"
  val getRecordListByRoomUrl = base + s"/getGameRecByRoom"
  val getRecordListByIdUrl = base + s"/getGameRecById"

  def wsJoinGameUrl(name:String,roomIdOpt:Option[Long]) = {
    base + s"/game/join?name=${name}"+
      (roomIdOpt match{
      case Some(roomId) =>s"&roomId=$roomId"
      case None =>""
      })
  }

  def wsWatchGameUrl(roomId:Long, accessCode:String, playerId:Option[String]) = base + s"/game/watchGame?roomId=$roomId&accessCode=${accessCode}" + playerId.map(t => s"&playerId=$t").getOrElse("")



  def wsJoinGameUrl(name:String, userId:String, userName:String, accessCode:String, roomIdOpt:Option[Long]): String = {
    base + s"/game/userJoin?name=$name&userId=$userId&userName=$userName&accessCode=$accessCode" +
      (roomIdOpt match {
        case Some(roomId) =>
          s"&roomId=$roomId"
        case None =>
          ""
      })
  }

  def wsReplayGameUrl(info:ReplayInfo) = base + s"/game/replay?rid=${info.recordId}&wid=${info.playerId}&f=${info.frame}&accessCode=${info.accessCode}"
  def wsReplayGameUrl(name:String,uid:String,rid:Long,wid:String,f:Int) = base + s"/game/replay?name=$name&uid=$uid&rid=$rid&wid=$wid&f=$f"



  def getJoinGameWebSocketUri(name:String, playerInfoOpt: Option[PlayerInfo],roomIdOpt:Option[Long]): String = {
    val wsProtocol = if (dom.document.location.protocol == "https:") "wss" else "ws"
    playerInfoOpt match {
      case Some(playerInfo) =>
        s"$wsProtocol://${dom.document.location.host}${Routes.wsJoinGameUrl(name,playerInfo.userId, playerInfo.userName, playerInfo.accessCode, playerInfo.roomIdOpt)}"
      case None =>
        s"$wsProtocol://${dom.document.location.host}${Routes.wsJoinGameUrl(name,roomIdOpt)}"
    }
  }


  def getReplaySocketUri(info:ReplayInfo): String = {
    val wsProtocol = if (dom.document.location.protocol == "https:") "wss" else "ws"
    s"$wsProtocol://${dom.document.location.host}${Routes.wsReplayGameUrl(info)}"
  }

  def getWsSocketUri(roomId:Long, accessCode:String, playerId:Option[String]): String = {
    val wsProtocol = if (dom.document.location.protocol == "https:") "wss" else "ws"
    s"$wsProtocol://${dom.document.location.host}${Routes.wsWatchGameUrl(roomId, accessCode, playerId)}"
  }










}
