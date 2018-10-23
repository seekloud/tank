package com.neo.sk.tank

/**
  * Created by hongruying on 2018/10/23
  */
package object model {


  case class PlayerInfo(
                       playerId:String,
                       nickName:String,
                       accessCode:String
                       )

  case class GameServerInfo(
                           ip:String,
                           port:String,
                           domain:String
                           )

}
