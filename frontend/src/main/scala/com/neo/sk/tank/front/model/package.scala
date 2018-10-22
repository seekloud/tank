package com.neo.sk.tank.front

/**
  * Created by hongruying on 2018/10/18
  */
package object model {
  case class PlayerInfo(userId:String, userName:String, accessCode:String, roomIdOpt:Option[Long])

}
