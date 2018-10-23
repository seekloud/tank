package com.neo.sk.tank.models.DAO
import com.neo.sk.tank.models.SlickTables._
import com.neo.sk.utils.DBUtil.db
import slick.jdbc.PostgresProfile.api._
import com.neo.sk.tank.Boot.executor


import com.neo.sk.utils.DBUtil.db
import slick.jdbc.PostgresProfile.api._
import com.neo.sk.tank.models.SlickTables._
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by hongruying on 2018/10/12
  */
object RecordDAO {
  def getRecordById(id:Long)={
    db.run(tGameRecord.filter(_.recordId===id).result.headOption)
  }
  def insertGameRecord(g: rGameRecord) = {
    db.run( tGameRecord.returning(tGameRecord.map(_.recordId)) += g)
  }

  def insertUserRecord(u: rUserRecordMap)={
    db.run(tUserRecordMap += u)
  }

  def insertUserRecordList(list: List[rUserRecordMap])={
    db.run(tUserRecordMap ++= list)
  }

  def updataGameRecord(id:Long, endTime:Long) = {
    db.run(tGameRecord.filter(_.recordId === id).map(_.endTime).update(endTime))
  }

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

  //根据用户选择录像
  def queryRecByPlayer(userId: String, lastId: Long, count: Int) = {
    val action = for{
      recordIds <- tUserRecordMap.filter(t => t.userId === userId && t.recordId > lastId).sortBy(_.recordId).map(_.recordId).take(count).result
      rst <- tGameRecord.filter(_.recordId.inSet(recordIds.toSet)).joinLeft(tUserRecordMap).on((r,m)=>r.recordId===m.recordId).result
    } yield {
      rst
    }
    db.run(action.transactionally)
  }

  //根据房间号选择ID
  def queryRecByRoom(roomId:Long, lastId: Long, count: Int) = {
    val q = for {
      rst <- tGameRecord.filter(r => r.recordId > lastId && r.roomId === roomId).sortBy(_.recordId).take(count).joinLeft(tUserRecordMap).on(_.recordId === _.recordId).result
    } yield rst
    db.run(q)
  }

  //根据录像Id选择录像
  def queryRecById(recordId:Long) = {
    val q = for {
      rst <- tGameRecord.filter(r => r.recordId === recordId).joinLeft(tUserRecordMap).on(_.recordId === _.recordId).result
    } yield rst
    db.run(q)
  }

  def getFilePath(recordId:Long) = {
    val q = tGameRecord.filter(_.recordId === recordId).map(_.filePath).result
    db.run(q)
  }

}
