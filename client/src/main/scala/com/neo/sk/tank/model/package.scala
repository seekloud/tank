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
                           port:Long,
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
  case class UserInfo(
                     userId:Long,
                     nickname: String,
                     token: String,
                     tokenExpireTime: Long
                     )
  case class UserInfoResponse(
                        data: UserInfo,
                        errCode: Int = 0,
                        msg: String = "ok"
                        )
  case class Ws4AgentRsp(
                          Ws4AgentRsp:UserInfoResponse
                        )

  sealed trait WsMsgSource
  case object CompleteMsgServer extends WsMsgSource
  case class FailMsgServer(ex: Exception) extends WsMsgSource

  sealed trait WsSendMsg
  case object WsSendComplete extends WsSendMsg
  case class WsSendFailed(ex: Throwable) extends WsSendMsg

  //esheep连接游戏
  case class GameServerData(
                             accessCode: String,
                             gsPrimaryInfo: GameServerInfo
                           )
  case class JoinGameRsp(
                        data: GameServerData,
                        errCode: Int = 0,
                        msg: String = "ok"
                        ) extends CommonRsp
  case class JoinGameReq(
                          gameId: Long,
                          playerId: String
                        )

  case class RoomList(roomList:List[Long])
  case class RoomListRsp(data:RoomList,
                         errCode:Int = 0,
                         msg:String = "ok")


}
