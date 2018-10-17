package com.neo.sk.tank.front.common

/**
  * User: Taoz
  * Date: 2/24/2017
  * Time: 10:59 AM
  */
object Routes {


  val base = "/tank"
  val getRoomListRoute = base + "/getRoomIdList"

  def wsJoinGameUrl(name:String) = base + s"/game/join?name=${name}"

  def wsWatchGameUrl(roomId:Int,playerId:Long) = base + s"/game/watchGame?roomId=$roomId&playerId=$playerId"










}
