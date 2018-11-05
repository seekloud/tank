package com.neo.sk.tank.common

import com.neo.sk.tank.model.PlayerInfo

/**
  * Created by sky
  * Date on 2018/11/2
  * Time at 上午11:25
  */
object Route {
  def getJoinGameWebSocketUri(name:String, domain:String, playerInfo:PlayerInfo,roomIdOpt:Option[String]): String = {
    val wsProtocol = "ws"
//    s"$wsProtocol://${domain}/tank${wsJoinGameUrl(name,playerInfo.playerId, playerInfo.nickName, playerInfo.accessCode, roomIdOpt)}"
    s"$wsProtocol://localhost:30369/tank${wsJoinGameUrl(name,playerInfo.playerId, playerInfo.nickName, playerInfo.accessCode, roomIdOpt)}"
  }

  def wsJoinGameUrl(name:String, userId:String, userName:String, accessCode:String, roomIdOpt:Option[String]): String = {
    s"/game/userJoin?name=$name&userId=$userId&userName=$userName&accessCode=$accessCode" +
      (roomIdOpt match {
        case Some(roomId) =>
          s"&roomId=$roomId"
        case None =>
          ""
      })
  }
}
