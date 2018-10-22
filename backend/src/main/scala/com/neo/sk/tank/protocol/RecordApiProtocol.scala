package com.neo.sk.tank.protocol

import com.neo.sk.tank.shared.ptcl.CommonRsp

object RecordApiProtocol {
  //全量获取
  case class getGameRecReq(
                            lastRecordId:Long,
                            count:Int
                          )

  //根据时间获取
  case class getGameRecByTimeReq(
                                  startTime:Long,
                                  endTime:Long,
                                  lastRecordId:Long,
                                  count:Int
                                )

  //根据用户获取
  case class getGameRecByPlayerReq(
                                    playerId:Long,
                                    lastRecordId:Long,
                                    count:Int
                                  )

  //请求下载录像
  case class downloadRecordReq(
                              recordId:Long
                              )

  //列表
  case class gameRec(
                    recordId:Long,
                    roomId:Long,
                    startTime:Long,
                    endTime:Long,
                    userCounts:Int,
                    userList:Seq[Long]
                    )

  //列表回复
  case class getGameRecRsp(
                            data:Option[List[gameRec]],
                            errCode:Int = 0,
                            msg:String = "ok"
                          ) extends CommonRsp

}
