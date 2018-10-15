package com.neo.sk.tank.front.common

/**
  * User: Taoz
  * Date: 2/24/2017
  * Time: 10:59 AM
  */
object Routes {


  val base = "/tank"

  def wsJoinGameUrl(name:String) = base + s"/game/join?name=${name}"

  def wsReplayGameUrl(name:String,uid:Long,rid:Long) = base + s"game/replay?name=$name&uid=$uid&rid=$rid"


}
