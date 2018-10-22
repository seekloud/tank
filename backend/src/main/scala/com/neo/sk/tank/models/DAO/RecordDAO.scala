package com.neo.sk.tank.models.DAO

import com.neo.sk.utils.DBUtil.db
import slick.jdbc.PostgresProfile.api._
import com.neo.sk.tank.models.SlickTables._
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by hongruying on 2018/10/12
  */
object RecordDAO {

  //选择所有的录像
  def queryAllRec(lastId: Long, count: Int) = {
    val q = for {
      rst <- tGameRecord.filter(r => r.recordId > lastId).sortBy(_.recordId).take(count).joinLeft(tUserRecordMap).on(_.recordId === _.recordId).result
    } yield rst
    db.run(q)
  }

  //根据时间选择录像
  def queryRecByTime(startTime: Long, endTime: Long, lastId: Long, count: Int) = {
    val q = for {
      rst <- tGameRecord.filter(r => r.recordId > lastId && r.startTime >= startTime && r.endTime <= endTime).sortBy(_.recordId).take(count).joinLeft(tUserRecordMap).on(_.recordId === _.recordId).result
    } yield rst
    db.run(q)
  }

  def getRecByUserId(userId: Long, lastId: Long, count: Int) = {
    val action = for{
      recordIds <- tUserRecordMap.filter(t => t.userId === userId && t.recordId > lastId).sortBy(_.recordId).map(_.recordId).take(count).result
      rst <- tGameRecord.filter(_.recordId.inSet(recordIds)).joinLeft(tUserRecordMap).on(_.recordId === _.recordId).result
    } yield {
      rst
    }
    db.run(action.transactionally)
  }

  def getFilePath(recordId:Long) = {
    val q = tGameRecord.filter(_.recordId === recordId).map(_.filePath).result
    db.run(q)
  }
}
