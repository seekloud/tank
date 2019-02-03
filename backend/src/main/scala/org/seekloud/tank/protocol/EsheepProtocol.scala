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

package org.seekloud.tank.protocol

import org.seekloud.tank.shared.ptcl.CommonRsp

/**
  * Created by hongruying on 2018/10/16
  */
object EsheepProtocol {

  case class GameServerKey2TokenReq(
                                gameId:Long,
                                gsKey:String
                                )

  case class GameServerKey2TokenInfo(
                                      token:String,
                                      expireTime:Long
                                    )

  case class GameServerKey2TokenRsp(
                                     data: Option[GameServerKey2TokenInfo],
                                     errCode: Int = 0,
                                     msg: String = "ok"
                                   ) extends CommonRsp


  case class PlayerInfo(
                         playerId:String,
                         nickname:String
                       )


  case class VerifyAccessCodeInfo(
                                   playerInfo:PlayerInfo
                                 )

  case class VerifyAccessCodeRsp(
                                  data: Option[PlayerInfo],
                                  errCode: Int = 0,
                                  msg: String = "ok"
                                ) extends CommonRsp

  /**获取录像内玩家列表*/
  case class GetUserInRecordReq(
                            recordId:Long,
                            playerId:String
                            )

  case class GetUserInRecordRsp(
                               data:PlayerList,
                               errCode: Int = 0,
                               msg: String = "ok"
                               ) extends CommonRsp

  case class PlayerInRecordInfo(
                                 playerId:String,
                                 nickname:String,
                                 existTime:List[ExistTimeInfo]
                               )

  case class ExistTimeInfo(
                          startFrame:Long,
                          endFrame:Long
                          )

  case class PlayerList(totalFrame:Int,
                        playerList:List[PlayerInRecordInfo])

  /**获取录像播放进度*/
  case class GetRecordFrameReq(
                              recordId:Long,
                              playerId:String  //观看者
                              )

  case class GetRecordFrameRsp(
                              data:RecordFrameInfo,
                              errCode: Int = 0,
                              msg: String = "ok"
                              ) extends CommonRsp

  case class RecordFrameInfo(frame:Int,frameNum:Long)

  /**更改观看内容*/
  case class ChangeRecordReq(
                            recordId:Long,
                            watchId:String,
                            playerId:String,
                            frame:Int
                            )

//战绩信息
  case class BatRecordInfo(
                           playerId: String,
                           gameId: Long,
                           nickname: String,
                           killing: Int,
                           killed: Int,
                           score: Int,
                           gameExtent: String,
                           startTime: Long,
                           endTime: Long
                           )
  case class BatRecord(
                       playerRecord: BatRecordInfo
                       )

  //code for download
  case class CodeForDownloadRsp(
                               code: String
                               )


}
