package com.neo.sk.tank.front.common

import com.neo.sk.tank.front.model.PlayerInfo
import org.scalajs.dom

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

  def wsJoinGameUrl(name:String) = base + s"/game/join?name=${name}"

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

  def wsReplayGameUrl(name:String,uid:String,rid:Long,wid:String,f:Int) = base + s"/game/replay?name=$name&uid=$uid&rid=$rid&wid=$wid&f=$f"



  def getJoinGameWebSocketUri(name:String, playerInfoOpt: Option[PlayerInfo]): String = {
    val wsProtocol = if (dom.document.location.protocol == "https:") "wss" else "ws"
    playerInfoOpt match {
      case Some(playerInfo) =>
        s"$wsProtocol://${dom.document.location.host}${Routes.wsJoinGameUrl(name,playerInfo.userId, playerInfo.userName, playerInfo.accessCode, playerInfo.roomIdOpt)}"
      case None =>
        s"$wsProtocol://${dom.document.location.host}${Routes.wsJoinGameUrl(name)}"
    }

  }


  def getReplaySocketUri(name:String,uid:String,rid:Long,wid:String,f:Int): String = {
    val wsProtocol = if (dom.document.location.protocol == "https:") "wss" else "ws"
    s"$wsProtocol://${dom.document.location.host}${Routes.wsReplayGameUrl(name,uid,rid,wid,f)}"
  }

  def getWsSocketUri(roomId:Long, accessCode:String, playerId:Option[String]): String = {
    val wsProtocol = if (dom.document.location.protocol == "https:") "wss" else "ws"
    s"$wsProtocol://${dom.document.location.host}${Routes.wsWatchGameUrl(roomId, accessCode, playerId)}"
  }










}
