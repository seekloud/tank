package com.neo.sk.tank.common

import java.net.URLEncoder

import com.neo.sk.tank.model.PlayerInfo

/**
  * Created by sky
  * Date on 2018/11/2
  * Time at 上午11:25
  */
object Route {
  def getUserJoinGameWebSocketUri(name:String, domain:String, playerInfo:PlayerInfo, roomIdOpt:Option[String]): String = {
    val wsProtocol = "ws"
    s"$wsProtocol://${domain}/tank${wsUserJoinGameUrl(URLEncoder.encode(name, "utf-8"),playerInfo.playerId, playerInfo.nickName, playerInfo.accessCode, roomIdOpt)}"
    //    s"$wsProtocol://localhost:30369/tank${wsJoinGameUrl(name,playerInfo.playerId, playerInfo.nickName, playerInfo.accessCode, roomIdOpt)}"
  }

  def getJoinGameWebSocketUri(name:String, domain:String, roomIdOpt:Option[String]): String = {
    val wsProtocol = "ws"
    s"$wsProtocol://${domain}/tank${wsJoinGameUrl(URLEncoder.encode(name, "utf-8"), roomIdOpt)}"
    //    s"$wsProtocol://localhost:30369/tank${wsJoinGameUrl(name,playerInfo.playerId, playerInfo.nickName, playerInfo.accessCode, roomIdOpt)}"
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
