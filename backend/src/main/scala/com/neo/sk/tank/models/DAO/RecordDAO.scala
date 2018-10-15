package com.neo.sk.tank.models.DAO
import com.neo.sk.tank.models.SlickTables._
import com.neo.sk.utils.DBUtil.db
import slick.jdbc.PostgresProfile.api._


/**
  * Created by hongruying on 2018/10/12
  */
object RecordDAO {
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

}
