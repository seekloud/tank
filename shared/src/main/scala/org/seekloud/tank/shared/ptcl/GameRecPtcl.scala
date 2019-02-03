package org.seekloud.tank.shared.ptcl

/**
  * Created by hongruying on 2019/2/1
  */
object GameRecPtcl {
  //全量获取
  case class GetGameRecReq(
                            lastRecordId:Long,
                            count:Int
                          )

  //根据ID获取
  case class GetGameRecByIdReq(
                            recordId:Long,
                          )

  //根据房间获取
  case class GetGameRecByRoomReq(
                                  roomId:Long,
                                  lastRecordId:Long,
                                  count:Int
                                )

  //根据用户获取
  case class GetGameRecByPlayerReq(
                                    playerId:String,
                                    lastRecordId:Long,
                                    count:Int
                                  )

  //列表
  case class GameRec(
                      recordId:Long,
                      roomId:Long,
                      startTime:Long,
                      endTime:Long,
                      userCounts:Int,
                      userList:Seq[String]
                    )

  //列表回复
  case class GetGameRecRsp(
                            data:Option[List[GameRec]],
                            errCode:Int = 0,
                            msg:String = "ok"
                          ) extends CommonRsp


}
