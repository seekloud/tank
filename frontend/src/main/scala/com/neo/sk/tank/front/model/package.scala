package com.neo.sk.tank.front

/**
  * Created by hongruying on 2018/10/18
  */
package object model {
  case class PlayerInfo(userId:Long, userName:String, accessCode:String, roomIdOpt:Option[Long])
  /**发送观看消息链接信息*/
  case class ReplayInfo(recordId:Long,playerId:Long,frame:Int,accessCode:String)
}
