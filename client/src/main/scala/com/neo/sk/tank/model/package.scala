package com.neo.sk.tank

import com.neo.sk.tank.shared.ptcl.CommonRsp

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
// esheep登录

  case class LoginInfo(
                      wsUrl: String,
                      scanUrl: String
                      )
  case class LoginResponse(
                          data:LoginInfo,
                          errCode: Int = 0,
                          msg: String = "ok"
                          )extends CommonRsp

  sealed trait WsMsgSource
  case object CompleteMsgServer extends WsMsgSource
  case class FailMsgServer(ex: Exception) extends WsMsgSource

  sealed trait WsSendMsg
  case object WsSendComplete extends WsSendMsg
  case class WsSendFailed(ex: Throwable) extends WsSendMsg


}
