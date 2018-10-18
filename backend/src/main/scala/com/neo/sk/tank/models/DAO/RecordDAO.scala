package com.neo.sk.tank.models.DAO

import com.neo.sk.utils.DBUtil.db
import slick.jdbc.PostgresProfile.api._
import com.neo.sk.tank.models.SlickTables._

/**
  * Created by hongruying on 2018/10/12
  */
object RecordDAO {

  //选择所有的录像
  def queryAllRec(page:Int) = {
    val leftOuterJoin = for{
      (a,b) <- tGameRecord joinLeft tUserRecordMap on (_.recordId === _.recordId)
    } yield (a.recordId, a.roomId, a.startTime, a.endTime, b.map(_.userId))
    val q1 = leftOuterJoin.filter(r => r._1 >= (1 + page * 10).toLong && r._1 <= (10 + page * 10).toLong).result
    db.run(q1)
  }

  //选择某用户对应的记录
  def queryRecByUser(userId:Long) = {
//    val explicitLeftOuterJoin = (for{
//      (a,b) <- tGameRecord joinLeft tUserRecordMap on (_.recordId === _.recordId)
//    } yield (a.recordId, a.roomId, a.startTime, a.endTime, b.map(_.userId))).filter(_._5.getOrElse(0L) === userId).result
    val q = tUserRecordMap.filter(_.userId === userId).map(_.recordId).result
    db.run(q)
  }

  //根据id选择录像
  def queryRecByRec(recordId:Long) = {
    val leftOuterJoin = (for{
      (a,b) <- tGameRecord joinLeft tUserRecordMap on (_.recordId === _.recordId)
    } yield (a.recordId, a.roomId, a.startTime, a.endTime, b.map(_.userId))).filter(_._1 === recordId).result
    db.run(leftOuterJoin)
  }

  //根据房间选择录像
  def queryRecByRoom(roomId:Long) = {
    val leftOuterJoin = (for{
      (a,b) <- tGameRecord joinLeft tUserRecordMap on (_.recordId === _.recordId)
    } yield (a.recordId, a.roomId, a.startTime, a.endTime, b.map(_.userId))).filter(_._2 === roomId).result
    db.run(leftOuterJoin)
  }

}
