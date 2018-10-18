package com.neo.sk.tank.shared.ptcl

object TankGameProtocol {
   case class GameRecordReq(
                             userId:Long,
                             recordId:Long,
                             roomId:Long,
                             page:Int
                           )

   case class GameRecordRsp(
                             lst:Option[List[(Long, Long, Long, Long,List[Long])]],
                             errCode:Int=0,
                             msg:String="Ok"
                           ) extends CommonRsp

}
