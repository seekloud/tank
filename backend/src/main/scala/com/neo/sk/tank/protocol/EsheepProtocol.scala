package com.neo.sk.tank.protocol

/**
  * Created by hongruying on 2018/10/16
  */
object EsheepProtocol {

  import com.neo.sk.tank.shared.ptcl.CommonRsp

  case class GameServerKey2TokenReq(
                                gameId:Long,
                                gsKey:String
                                )

  case class GameServerKey2TokenInfo(
                                      gsToken:String,
                                      expireTime:Long
                                    )

  case class GameServerKey2TokenRsp(
                                     data: Option[GameServerKey2TokenInfo],
                                     errCode: Int = 0,
                                     msg: String = "ok"
                                   ) extends CommonRsp


  case class PlayerInfo(
                         playerId:Long,
                         nickname:String
                       )

  case class VerifyAccessCodeInfo(
                                   playerInfo:PlayerInfo
                                 )

  case class VerifyAccessCodeRsp(
                                  data: Option[VerifyAccessCodeInfo],
                                  errCode: Int = 0,
                                  msg: String = "ok"
                                ) extends CommonRsp


  case class BatRecordeInfo(
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
  case class batRecorde(
                       playerRecord: BatRecordeInfo
                       )


}
