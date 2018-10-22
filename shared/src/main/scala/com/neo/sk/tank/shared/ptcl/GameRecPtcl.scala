package com.neo.sk.tank.shared.ptcl

object GameRecPtcl {
  //全量获取
  case class getGameRecReq(
                            lastRecordId:Long,
                            count:Int
                          )

  //根据ID获取
  case class getGameRecByIdReq(
                            recordId:Long,
                          )

  //根据房间获取
  case class getGameRecByRoomReq(
                                  roomId:Long,
                                  lastRecordId:Long,
                                  count:Int
                                )

  //根据用户获取
  case class getGameRecByPlayerReq(
                                    playerId:Long,
                                    lastRecordId:Long,
                                    count:Int
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
