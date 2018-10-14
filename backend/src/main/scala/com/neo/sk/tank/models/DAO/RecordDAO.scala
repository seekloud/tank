package com.neo.sk.tank.models.DAO

import com.neo.sk.tank.models.SlickTables
import com.neo.sk.utils.DBUtil.db
import slick.jdbc.PostgresProfile.api._
import com.neo.sk.tank.models.SlickTables._

/**
  * Created by hongruying on 2018/10/12
  */
object RecordDAO {
  def getRecordById(id:Long)={
    db.run(tGameRecord.filter(_.recordId===id).result.headOption)
  }

}
