package com.neo.sk.tank

/**
  * Created by hongruying on 2018/10/18
  */
package object models {

  case class TankGameUserInfo(
                     userId:Long,
                     nickName:String,
                     name:String,
                     isFlatUser:Boolean
                     )

}
