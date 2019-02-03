package org.seekloud.tank

/**
  * Created by hongruying on 2019/2/3
  */
package object models {

  case class TankGameUserInfo(
                     userId:String,
                     nickName:String,
                     name:String,
                     isFlatUser:Boolean
                     )

}
