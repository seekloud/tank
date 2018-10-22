package com.neo.sk.tank.front.common

import com.neo.sk.tank.front.model.PlayerInfo
import org.scalajs.dom
import com.neo.sk.tank.front.model.ReplayInfo

/**
  * User: Taoz
  * Date: 2/24/2017
  * Time: 10:59 AM
  */
object Routes {


  val base = "/tank"
  val getRoomListRoute = base + "/getRoomIdList"

  def wsJoinGameUrl(name:String) = base + s"/game/join?name=${name}"

  def wsWatchGameUrl(roomId:Long, accessCode:String, playerId:Option[Long]) = base + s"/game/watchGame?roomId=$roomId&accessCode=${accessCode}" + playerId.map(t => s"&playerId=$t").getOrElse("")



  def wsJoinGameUrl(name:String, userId:Long, userName:String, accessCode:String, roomIdOpt:Option[Long]): String = {
    base + s"/game/userJoin?name=$name&userId=$userId&userName=$userName&accessCode=$accessCode" +
      (roomIdOpt match {
        case Some(roomId) =>
          s"&roomId=$roomId"
        case None =>
          ""
      })
  }

  def wsReplayGameUrl(info:ReplayInfo) = base + s"/game/replay?rid=${info.recordId}&wid=${info.playerId}&f=${info.frame}&accessCode=${info.accessCode}"



  def getJoinGameWebSocketUri(name:String, playerInfoOpt: Option[PlayerInfo]): String = {
    val wsProtocol = if (dom.document.location.protocol == "https:") "wss" else "ws"
    playerInfoOpt match {
      case Some(playerInfo) =>
        s"$wsProtocol://${dom.document.location.host}${Routes.wsJoinGameUrl(name,playerInfo.userId, playerInfo.userName, playerInfo.accessCode, playerInfo.roomIdOpt)}"
      case None =>
        s"$wsProtocol://${dom.document.location.host}${Routes.wsJoinGameUrl(name)}"
    }
  }


  def getReplaySocketUri(info:ReplayInfo): String = {
    val wsProtocol = if (dom.document.location.protocol == "https:") "wss" else "ws"
    s"$wsProtocol://${dom.document.location.host}${Routes.wsReplayGameUrl(info)}"
  }

  def getWsSocketUri(roomId:Long, accessCode:String, playerId:Option[Long]): String = {
    val wsProtocol = if (dom.document.location.protocol == "https:") "wss" else "ws"
    s"$wsProtocol://${dom.document.location.host}${Routes.wsWatchGameUrl(roomId, accessCode, playerId)}"
  }










}
