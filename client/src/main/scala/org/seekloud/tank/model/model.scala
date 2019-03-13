/*
 * Copyright 2018 seekloud (https://github.com/seekloud)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.seekloud.tank

import org.seekloud.tank.shared.ptcl.CommonRsp

import scala.collection.mutable

/**
  * Created by hongruying on 2019/2/1
  */
package object model {
  case class BotInfo(
                    botId:String,
                    botKey:String
                    )

  case class PlayerInfo(
                       userInfo:UserInfo,
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

  //esheep邮箱登录
  final case class LoginReq(
                             email: String,
                             password: String
                           )
  final case class ESheepUserInfoRsp(
                                      userName: String ,
                                      userId: Long ,
                                      headImg: String ,
                                      token: String ,
                                      gender: Int,
                                      errCode: Int = 0,
                                      msg: String = "ok"
                                    )

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
  //刷新tokend
  case class gaRefreshTokenReq(
                                playerId: String
                              )
  case class TokenInfo(
                      token: String,
                      expireTime: Long
                      )
  case class TokenAndAcessCode(
                                token: String,
                                expireTime: Long,
                                accessCode: String
                              )

  case class gaRefreshTokenRsp(
                                data: TokenInfo,
                                errCode: Int = 0,
                                msg: String = "ok"
                              )extends CommonRsp

  case class RoomList(roomList:mutable.HashMap[Long,Boolean])
  case class RoomListRsp(data:RoomList,
                         errCode:Int = 0,
                         msg:String = "ok")

  //bot api 相关 protocol
  case class JoinRoomRsp(
                          roomId: Long,
                          errCode:Int =0,
                          msg:String="ok"
                        )


}
