package org.seekloud.utils

/**
  * Created by hongruying on 2018/6/1
  */
import com.github.tminglei.slickpg._
import slick.basic.Capability
import slick.jdbc.JdbcCapabilities

trait MyPostgresDriver extends ExPostgresProfile
  with PgArraySupport
  with PgDate2Support
  with PgRangeSupport
  with PgHStoreSupport
  with PgSearchSupport
  with PgJsonSupport
  with PgNetSupport
  with PgLTreeSupport
  with PgCirceJsonSupport {
  def pgjson = "jsonb" // jsonb support is in postgres 9.4.0 onward; for 9.3.x use "json"

  // Add back `capabilities.insertOrUpdate` to enable native `upsert` support; for postgres 9.5+
  override protected def computeCapabilities: Set[Capability] =
    super.computeCapabilities + JdbcCapabilities.insertOrUpdate

  override val api = MyAPI

  object MyAPI extends API with ArrayImplicits
    with DateTimeImplicits
    with JsonImplicits
    with NetImplicits
    with LTreeImplicits
    with RangeImplicits
    with HStoreImplicits
    with SearchImplicits
    with SearchAssistants
    with CirceImplicits
    with CirceJsonPlainImplicits {
    implicit val strListTypeMapper = new SimpleArrayJdbcType[String]("text").to(_.toList)

  }
}

object MyPostgresDriver extends MyPostgresDriver
