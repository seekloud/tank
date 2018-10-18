package com.neo.sk.tank.front.common

/**
  * User: Taoz
  * Date: 2/24/2017
  * Time: 10:59 AM
  */
object Routes {

  val base = "/tank"

  val getGameRecordUrl = base + s"/game/getGameRec"

  def wsJoinGameUrl(name:String) = base + s"/game/join?name=${name}"

}
