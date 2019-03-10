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

package org.seekloud.tank.common

import java.net.URLEncoder

import org.seekloud.tank.model.PlayerInfo

/**
  * Created by sky
  * Date on 2018/11/2
  * Time at 上午11:25
  */
object Route {
  def getUserJoinGameWebSocketUri(name:String, domain:String, playerInfo:PlayerInfo, roomIdOpt:Option[String]): String = {
    val wsProtocol = if (AppSettings.esheepProtocol == "https") "wss" else "ws"
    s"$wsProtocol://${domain}/tank${wsUserJoinGameUrl(URLEncoder.encode(name, "utf-8"),playerInfo.playerId, URLEncoder.encode(playerInfo.nickName, "utf-8"), playerInfo.accessCode, roomIdOpt)}"
//        s"$wsProtocol://$domain/tank${wsUserJoinGameUrl(name,playerInfo.playerId, playerInfo.nickName, playerInfo.accessCode, roomIdOpt)}"
  }

  def getJoinGameWebSocketUri(name:String, domain:String, roomIdOpt:Option[String]): String = {
    val wsProtocol = if (AppSettings.esheepProtocol == "https") "wss" else "ws"
    s"$wsProtocol://${domain}/tank${wsJoinGameUrl(URLEncoder.encode(name, "utf-8"), roomIdOpt)}"
//        s"$wsProtocol://$domain/tank${wsJoinGameUrl(name,roomIdOpt)}"
  }

  def wsUserJoinGameUrl(name:String, userId:String, userName:String, accessCode:String, roomIdOpt:Option[String]): String = {
    s"/game/userJoin?name=$name&userId=$userId&userName=$userName&accessCode=$accessCode" +
      (roomIdOpt match {
        case Some(roomId) =>
          s"&roomId=$roomId"
        case None =>
          ""
      })
  }

  def wsJoinGameUrl(name:String, roomIdOpt:Option[String]): String = {
    s"/game/join?name=$name" +
      (roomIdOpt match {
        case Some(roomId) =>
          s"&roomId=$roomId"
        case None =>
          ""
      })
  }
}
