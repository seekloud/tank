package org.seekloud.tank.protocol

import org.seekloud.tank.shared.ptcl.CommonRsp

/**
  * Created by hongruying on 2019/2/3
  */
object RecordApiProtocol {
  //全量获取
  case class GetGameRecReq(
                            lastRecordId:Long,
                            count:Int
                          )

  //根据时间获取
  case class GetGameRecByTimeReq(
                                  startTime:Long,
                                  endTime:Long,
                                  lastRecordId:Long,
                                  count:Int
                                )

  //根据用户获取
  case class GetGameRecByPlayerReq(
                                    playerId:String,
                                    lastRecordId:Long,
                                    count:Int
                                  )

  //请求下载录像
  case class DownloadRecordReq(
                              recordId:Long
                              )

  //列表
  case class GameRec(
                    recordId:Long,
                    roomId:Long,
                    startTime:Long,
                    endTime:Long,
                    userCounts:Int,
                    userList:Seq[(String, String)]
                    )

  //列表回复
  case class GetGameRecRsp(
                            data:Option[List[GameRec]],
                            errCode:Int = 0,
                            msg:String = "ok"
                          ) extends CommonRsp

}
