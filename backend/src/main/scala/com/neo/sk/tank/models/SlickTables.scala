package com.neo.sk.tank.models
// AUTO-GENERATED Slick data model
/** Stand-alone Slick data model for immediate use */
object SlickTables extends {
  val profile = slick.jdbc.PostgresProfile
} with SlickTables

/** Slick data model trait for extension, choice of backend or usage in the cake pattern. (Make sure to initialize this late.) */
trait SlickTables {
  val profile: slick.jdbc.JdbcProfile
  import profile.api._
  import slick.model.ForeignKeyAction
  import com.neo.sk.utils.MyPostgresDriver.api.circeJsonTypeMapper
  // NOTE: GetResult mappers for plain SQL are only generated for tables where Slick knows how to map the types of all columns.
  import slick.jdbc.{GetResult => GR}

  /** DDL for all tables. Call .create to execute. */
  lazy val schema: profile.SchemaDescription = Array(tAccountInfo.schema, tApp.schema, tAppChannel.schema, tAppCountData.schema, tAppUserFirstVisit.schema, tAppWhiteUser.schema, tCollectEventData.schema, tCollectEventDataWithFirst.schema, tCollectUserInfo.schema, tCustomEventDefinition.schema, tEventDefinition.schema, tPowerKey.schema, tSegment.schema, tWxBindApp.schema, tWxUser.schema).reduceLeft(_ ++ _)
  @deprecated("Use .schema instead of .ddl", "3.0")
  def ddl = schema

  /** Entity class storing rows of table tAccountInfo
    *  @param accountId Database column account_id SqlType(bigserial), AutoInc, PrimaryKey
    *  @param email Database column email SqlType(varchar), Length(64,true)
    *  @param secure Database column secure SqlType(varchar), Length(128,true)
    *  @param accountType Database column account_type SqlType(int4)
    *  @param createTime Database column create_time SqlType(int8), Default(0)
    *  @param nickname Database column nickname SqlType(varchar), Length(32,true), Default()
    *  @param companyName Database column company_name SqlType(varchar), Length(64,true), Default()
    *  @param companyDesc Database column company_desc SqlType(varchar), Length(128,true), Default() */
  case class rAccountInfo(accountId: Long, email: String, secure: String, accountType: Int, createTime: Long = 0L, nickname: String = "", companyName: String = "", companyDesc: String = "")
  /** GetResult implicit for fetching rAccountInfo objects using plain SQL queries */
  implicit def GetResultrAccountInfo(implicit e0: GR[Long], e1: GR[String], e2: GR[Int]): GR[rAccountInfo] = GR{
    prs => import prs._
      rAccountInfo.tupled((<<[Long], <<[String], <<[String], <<[Int], <<[Long], <<[String], <<[String], <<[String]))
  }
  /** Table description of table account_info. Objects of this class serve as prototypes for rows in queries. */
  class tAccountInfo(_tableTag: Tag) extends profile.api.Table[rAccountInfo](_tableTag, "account_info") {
    def * = (accountId, email, secure, accountType, createTime, nickname, companyName, companyDesc) <> (rAccountInfo.tupled, rAccountInfo.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(accountId), Rep.Some(email), Rep.Some(secure), Rep.Some(accountType), Rep.Some(createTime), Rep.Some(nickname), Rep.Some(companyName), Rep.Some(companyDesc)).shaped.<>({r=>import r._; _1.map(_=> rAccountInfo.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6.get, _7.get, _8.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column account_id SqlType(bigserial), AutoInc, PrimaryKey */
    val accountId: Rep[Long] = column[Long]("account_id", O.AutoInc, O.PrimaryKey)
    /** Database column email SqlType(varchar), Length(64,true) */
    val email: Rep[String] = column[String]("email", O.Length(64,varying=true))
    /** Database column secure SqlType(varchar), Length(128,true) */
    val secure: Rep[String] = column[String]("secure", O.Length(128,varying=true))
    /** Database column account_type SqlType(int4) */
    val accountType: Rep[Int] = column[Int]("account_type")
    /** Database column create_time SqlType(int8), Default(0) */
    val createTime: Rep[Long] = column[Long]("create_time", O.Default(0L))
    /** Database column nickname SqlType(varchar), Length(32,true), Default() */
    val nickname: Rep[String] = column[String]("nickname", O.Length(32,varying=true), O.Default(""))
    /** Database column company_name SqlType(varchar), Length(64,true), Default() */
    val companyName: Rep[String] = column[String]("company_name", O.Length(64,varying=true), O.Default(""))
    /** Database column company_desc SqlType(varchar), Length(128,true), Default() */
    val companyDesc: Rep[String] = column[String]("company_desc", O.Length(128,varying=true), O.Default(""))
  }
  /** Collection-like TableQuery object for table tAccountInfo */
  lazy val tAccountInfo = new TableQuery(tag => new tAccountInfo(tag))

  /** Entity class storing rows of table tApp
    *  @param appId Database column app_id SqlType(bigserial), AutoInc, PrimaryKey
    *  @param accountId Database column account_id SqlType(int8)
    *  @param appName Database column app_name SqlType(varchar), Length(32,true)
    *  @param appDesc Database column app_desc SqlType(varchar), Length(256,true), Default()
    *  @param appImg Database column app_img SqlType(varchar), Length(64,true), Default()
    *  @param createTime Database column create_time SqlType(int8), Default(0) */
  case class rApp(appId: Long, accountId: Long, appName: String, appDesc: String = "", appImg: String = "", createTime: Long = 0L)
  /** GetResult implicit for fetching rApp objects using plain SQL queries */
  implicit def GetResultrApp(implicit e0: GR[Long], e1: GR[String]): GR[rApp] = GR{
    prs => import prs._
      rApp.tupled((<<[Long], <<[Long], <<[String], <<[String], <<[String], <<[Long]))
  }
  /** Table description of table app. Objects of this class serve as prototypes for rows in queries. */
  class tApp(_tableTag: Tag) extends profile.api.Table[rApp](_tableTag, "app") {
    def * = (appId, accountId, appName, appDesc, appImg, createTime) <> (rApp.tupled, rApp.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(appId), Rep.Some(accountId), Rep.Some(appName), Rep.Some(appDesc), Rep.Some(appImg), Rep.Some(createTime)).shaped.<>({r=>import r._; _1.map(_=> rApp.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column app_id SqlType(bigserial), AutoInc, PrimaryKey */
    val appId: Rep[Long] = column[Long]("app_id", O.AutoInc, O.PrimaryKey)
    /** Database column account_id SqlType(int8) */
    val accountId: Rep[Long] = column[Long]("account_id")
    /** Database column app_name SqlType(varchar), Length(32,true) */
    val appName: Rep[String] = column[String]("app_name", O.Length(32,varying=true))
    /** Database column app_desc SqlType(varchar), Length(256,true), Default() */
    val appDesc: Rep[String] = column[String]("app_desc", O.Length(256,varying=true), O.Default(""))
    /** Database column app_img SqlType(varchar), Length(64,true), Default() */
    val appImg: Rep[String] = column[String]("app_img", O.Length(64,varying=true), O.Default(""))
    /** Database column create_time SqlType(int8), Default(0) */
    val createTime: Rep[Long] = column[Long]("create_time", O.Default(0L))
  }
  /** Collection-like TableQuery object for table tApp */
  lazy val tApp = new TableQuery(tag => new tApp(tag))

  /** Entity class storing rows of table tAppChannel
    *  @param appId Database column app_id SqlType(int8)
    *  @param channel Database column channel SqlType(varchar), Length(16,true)
    *  @param channelNameCn Database column channel_name_cn SqlType(varchar), Length(32,true)
    *  @param createTime Database column create_time SqlType(int8), Default(0) */
  case class rAppChannel(appId: Long, channel: String, channelNameCn: String, createTime: Long = 0L)
  /** GetResult implicit for fetching rAppChannel objects using plain SQL queries */
  implicit def GetResultrAppChannel(implicit e0: GR[Long], e1: GR[String]): GR[rAppChannel] = GR{
    prs => import prs._
      rAppChannel.tupled((<<[Long], <<[String], <<[String], <<[Long]))
  }
  /** Table description of table app_channel. Objects of this class serve as prototypes for rows in queries. */
  class tAppChannel(_tableTag: Tag) extends profile.api.Table[rAppChannel](_tableTag, "app_channel") {
    def * = (appId, channel, channelNameCn, createTime) <> (rAppChannel.tupled, rAppChannel.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(appId), Rep.Some(channel), Rep.Some(channelNameCn), Rep.Some(createTime)).shaped.<>({r=>import r._; _1.map(_=> rAppChannel.tupled((_1.get, _2.get, _3.get, _4.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column app_id SqlType(int8) */
    val appId: Rep[Long] = column[Long]("app_id")
    /** Database column channel SqlType(varchar), Length(16,true) */
    val channel: Rep[String] = column[String]("channel", O.Length(16,varying=true))
    /** Database column channel_name_cn SqlType(varchar), Length(32,true) */
    val channelNameCn: Rep[String] = column[String]("channel_name_cn", O.Length(32,varying=true))
    /** Database column create_time SqlType(int8), Default(0) */
    val createTime: Rep[Long] = column[Long]("create_time", O.Default(0L))

    /** Primary key of tAppChannel (database name app_channel_pkey) */
    val pk = primaryKey("app_channel_pkey", (appId, channel))
  }
  /** Collection-like TableQuery object for table tAppChannel */
  lazy val tAppChannel = new TableQuery(tag => new tAppChannel(tag))

  /** Entity class storing rows of table tAppCountData
    *  @param id Database column id SqlType(bigserial), AutoInc, PrimaryKey
    *  @param appId Database column app_id SqlType(int8)
    *  @param channel Database column channel SqlType(varchar), Length(16,true)
    *  @param intervalType Database column interval_type SqlType(varchar), Length(32,true)
    *  @param lastDataId Database column last_data_id SqlType(int8)
    *  @param timestamp Database column timestamp SqlType(int8), Default(0)
    *  @param newUsers Database column new_users SqlType(int8), Default(0)
    *  @param visitTimes Database column visit_times SqlType(int8), Default(0)
    *  @param visitUsers Database column visit_users SqlType(int8), Default(0)
    *  @param allUsers Database column all_users SqlType(int8), Default(0)
    *  @param userIds Database column user_ids SqlType(jsonb) */
  case class rAppCountData(id: Long, appId: Long, channel: String, intervalType: String, lastDataId: Long, timestamp: Long = 0L, newUsers: Long = 0L, visitTimes: Long = 0L, visitUsers: Long = 0L, allUsers: Long = 0L, userIds: io.circe.Json)
  /** GetResult implicit for fetching rAppCountData objects using plain SQL queries */
  implicit def GetResultrAppCountData(implicit e0: GR[Long], e1: GR[String], e2: GR[io.circe.Json]): GR[rAppCountData] = GR{
    prs => import prs._
      rAppCountData.tupled((<<[Long], <<[Long], <<[String], <<[String], <<[Long], <<[Long], <<[Long], <<[Long], <<[Long], <<[Long], <<[io.circe.Json]))
  }
  /** Table description of table app_count_data. Objects of this class serve as prototypes for rows in queries. */
  class tAppCountData(_tableTag: Tag) extends profile.api.Table[rAppCountData](_tableTag, "app_count_data") {
    def * = (id, appId, channel, intervalType, lastDataId, timestamp, newUsers, visitTimes, visitUsers, allUsers, userIds) <> (rAppCountData.tupled, rAppCountData.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(id), Rep.Some(appId), Rep.Some(channel), Rep.Some(intervalType), Rep.Some(lastDataId), Rep.Some(timestamp), Rep.Some(newUsers), Rep.Some(visitTimes), Rep.Some(visitUsers), Rep.Some(allUsers), Rep.Some(userIds)).shaped.<>({r=>import r._; _1.map(_=> rAppCountData.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6.get, _7.get, _8.get, _9.get, _10.get, _11.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(bigserial), AutoInc, PrimaryKey */
    val id: Rep[Long] = column[Long]("id", O.AutoInc, O.PrimaryKey)
    /** Database column app_id SqlType(int8) */
    val appId: Rep[Long] = column[Long]("app_id")
    /** Database column channel SqlType(varchar), Length(16,true) */
    val channel: Rep[String] = column[String]("channel", O.Length(16,varying=true))
    /** Database column interval_type SqlType(varchar), Length(32,true) */
    val intervalType: Rep[String] = column[String]("interval_type", O.Length(32,varying=true))
    /** Database column last_data_id SqlType(int8) */
    val lastDataId: Rep[Long] = column[Long]("last_data_id")
    /** Database column timestamp SqlType(int8), Default(0) */
    val timestamp: Rep[Long] = column[Long]("timestamp", O.Default(0L))
    /** Database column new_users SqlType(int8), Default(0) */
    val newUsers: Rep[Long] = column[Long]("new_users", O.Default(0L))
    /** Database column visit_times SqlType(int8), Default(0) */
    val visitTimes: Rep[Long] = column[Long]("visit_times", O.Default(0L))
    /** Database column visit_users SqlType(int8), Default(0) */
    val visitUsers: Rep[Long] = column[Long]("visit_users", O.Default(0L))
    /** Database column all_users SqlType(int8), Default(0) */
    val allUsers: Rep[Long] = column[Long]("all_users", O.Default(0L))
    /** Database column user_ids SqlType(jsonb) */
    val userIds: Rep[io.circe.Json] = column[io.circe.Json]("user_ids")
  }
  /** Collection-like TableQuery object for table tAppCountData */
  lazy val tAppCountData = new TableQuery(tag => new tAppCountData(tag))

  /** Entity class storing rows of table tAppUserFirstVisit
    *  @param appId Database column app_id SqlType(int8)
    *  @param userId Database column user_id SqlType(varchar), Length(256,true)
    *  @param channel Database column channel SqlType(varchar), Length(16,true), Default(ALL)
    *  @param firstVisitTime Database column first_visit_time SqlType(int8), Default(0) */
  case class rAppUserFirstVisit(appId: Long, userId: String, channel: String = "ALL", firstVisitTime: Long = 0L)
  /** GetResult implicit for fetching rAppUserFirstVisit objects using plain SQL queries */
  implicit def GetResultrAppUserFirstVisit(implicit e0: GR[Long], e1: GR[String]): GR[rAppUserFirstVisit] = GR{
    prs => import prs._
      rAppUserFirstVisit.tupled((<<[Long], <<[String], <<[String], <<[Long]))
  }
  /** Table description of table app_user_first_visit. Objects of this class serve as prototypes for rows in queries. */
  class tAppUserFirstVisit(_tableTag: Tag) extends profile.api.Table[rAppUserFirstVisit](_tableTag, "app_user_first_visit") {
    def * = (appId, userId, channel, firstVisitTime) <> (rAppUserFirstVisit.tupled, rAppUserFirstVisit.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(appId), Rep.Some(userId), Rep.Some(channel), Rep.Some(firstVisitTime)).shaped.<>({r=>import r._; _1.map(_=> rAppUserFirstVisit.tupled((_1.get, _2.get, _3.get, _4.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column app_id SqlType(int8) */
    val appId: Rep[Long] = column[Long]("app_id")
    /** Database column user_id SqlType(varchar), Length(256,true) */
    val userId: Rep[String] = column[String]("user_id", O.Length(256,varying=true))
    /** Database column channel SqlType(varchar), Length(16,true), Default(ALL) */
    val channel: Rep[String] = column[String]("channel", O.Length(16,varying=true), O.Default("ALL"))
    /** Database column first_visit_time SqlType(int8), Default(0) */
    val firstVisitTime: Rep[Long] = column[Long]("first_visit_time", O.Default(0L))

    /** Primary key of tAppUserFirstVisit (database name app_user_first_visit_pkey) */
    val pk = primaryKey("app_user_first_visit_pkey", (appId, userId))
  }
  /** Collection-like TableQuery object for table tAppUserFirstVisit */
  lazy val tAppUserFirstVisit = new TableQuery(tag => new tAppUserFirstVisit(tag))

  /** Entity class storing rows of table tAppWhiteUser
    *  @param id Database column id SqlType(bigserial), AutoInc, PrimaryKey
    *  @param appId Database column app_id SqlType(int8)
    *  @param userId Database column user_id SqlType(varchar), Length(256,true), Default() */
  case class rAppWhiteUser(id: Long, appId: Long, userId: String = "")
  /** GetResult implicit for fetching rAppWhiteUser objects using plain SQL queries */
  implicit def GetResultrAppWhiteUser(implicit e0: GR[Long], e1: GR[String]): GR[rAppWhiteUser] = GR{
    prs => import prs._
      rAppWhiteUser.tupled((<<[Long], <<[Long], <<[String]))
  }
  /** Table description of table app_white_user. Objects of this class serve as prototypes for rows in queries. */
  class tAppWhiteUser(_tableTag: Tag) extends profile.api.Table[rAppWhiteUser](_tableTag, "app_white_user") {
    def * = (id, appId, userId) <> (rAppWhiteUser.tupled, rAppWhiteUser.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(id), Rep.Some(appId), Rep.Some(userId)).shaped.<>({r=>import r._; _1.map(_=> rAppWhiteUser.tupled((_1.get, _2.get, _3.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(bigserial), AutoInc, PrimaryKey */
    val id: Rep[Long] = column[Long]("id", O.AutoInc, O.PrimaryKey)
    /** Database column app_id SqlType(int8) */
    val appId: Rep[Long] = column[Long]("app_id")
    /** Database column user_id SqlType(varchar), Length(256,true), Default() */
    val userId: Rep[String] = column[String]("user_id", O.Length(256,varying=true), O.Default(""))
  }
  /** Collection-like TableQuery object for table tAppWhiteUser */
  lazy val tAppWhiteUser = new TableQuery(tag => new tAppWhiteUser(tag))

  /** Entity class storing rows of table tCollectEventData
    *  @param dataId Database column data_id SqlType(int8), PrimaryKey
    *  @param appId Database column app_id SqlType(int8)
    *  @param eventType Database column event_type SqlType(int8)
    *  @param userId Database column user_id SqlType(varchar), Length(256,true)
    *  @param channel Database column channel SqlType(varchar), Length(16,true), Default(ALL)
    *  @param timestamp Database column timestamp SqlType(int8), Default(0)
    *  @param data Database column data SqlType(jsonb) */
  case class rCollectEventData(dataId: Long, appId: Long, eventType: Long, userId: String, channel: String = "ALL", timestamp: Long = 0L, data: io.circe.Json)
  /** GetResult implicit for fetching rCollectEventData objects using plain SQL queries */
  implicit def GetResultrCollectEventData(implicit e0: GR[Long], e1: GR[String], e2: GR[io.circe.Json]): GR[rCollectEventData] = GR{
    prs => import prs._
      rCollectEventData.tupled((<<[Long], <<[Long], <<[Long], <<[String], <<[String], <<[Long], <<[io.circe.Json]))
  }
  /** Table description of table collect_event_data. Objects of this class serve as prototypes for rows in queries. */
  class tCollectEventData(_tableTag: Tag) extends profile.api.Table[rCollectEventData](_tableTag, "collect_event_data") {
    def * = (dataId, appId, eventType, userId, channel, timestamp, data) <> (rCollectEventData.tupled, rCollectEventData.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(dataId), Rep.Some(appId), Rep.Some(eventType), Rep.Some(userId), Rep.Some(channel), Rep.Some(timestamp), Rep.Some(data)).shaped.<>({r=>import r._; _1.map(_=> rCollectEventData.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6.get, _7.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column data_id SqlType(int8), PrimaryKey */
    val dataId: Rep[Long] = column[Long]("data_id", O.PrimaryKey)
    /** Database column app_id SqlType(int8) */
    val appId: Rep[Long] = column[Long]("app_id")
    /** Database column event_type SqlType(int8) */
    val eventType: Rep[Long] = column[Long]("event_type")
    /** Database column user_id SqlType(varchar), Length(256,true) */
    val userId: Rep[String] = column[String]("user_id", O.Length(256,varying=true))
    /** Database column channel SqlType(varchar), Length(16,true), Default(ALL) */
    val channel: Rep[String] = column[String]("channel", O.Length(16,varying=true), O.Default("ALL"))
    /** Database column timestamp SqlType(int8), Default(0) */
    val timestamp: Rep[Long] = column[Long]("timestamp", O.Default(0L))
    /** Database column data SqlType(jsonb) */
    val data: Rep[io.circe.Json] = column[io.circe.Json]("data")
  }
  /** Collection-like TableQuery object for table tCollectEventData */
  lazy val tCollectEventData = new TableQuery(tag => new tCollectEventData(tag))

  /** Entity class storing rows of table tCollectEventDataWithFirst
    *  @param dataId Database column data_id SqlType(bigserial), AutoInc, PrimaryKey
    *  @param appId Database column app_id SqlType(int8)
    *  @param eventType Database column event_type SqlType(int8)
    *  @param userId Database column user_id SqlType(varchar), Length(256,true)
    *  @param channel Database column channel SqlType(varchar), Length(16,true), Default(ALL)
    *  @param isFirstVisit Database column is_first_visit SqlType(int4), Default(0)
    *  @param timestamp Database column timestamp SqlType(int8), Default(0)
    *  @param isFirstVisitChannel Database column is_first_visit_channel SqlType(int4), Default(0) */
  case class rCollectEventDataWithFirst(dataId: Long, appId: Long, eventType: Long, userId: String, channel: String = "ALL", isFirstVisit: Int = 0, timestamp: Long = 0L, isFirstVisitChannel: Int = 0)
  /** GetResult implicit for fetching rCollectEventDataWithFirst objects using plain SQL queries */
  implicit def GetResultrCollectEventDataWithFirst(implicit e0: GR[Long], e1: GR[String], e2: GR[Int]): GR[rCollectEventDataWithFirst] = GR{
    prs => import prs._
      rCollectEventDataWithFirst.tupled((<<[Long], <<[Long], <<[Long], <<[String], <<[String], <<[Int], <<[Long], <<[Int]))
  }
  /** Table description of table collect_event_data_with_first. Objects of this class serve as prototypes for rows in queries. */
  class tCollectEventDataWithFirst(_tableTag: Tag) extends profile.api.Table[rCollectEventDataWithFirst](_tableTag, "collect_event_data_with_first") {
    def * = (dataId, appId, eventType, userId, channel, isFirstVisit, timestamp, isFirstVisitChannel) <> (rCollectEventDataWithFirst.tupled, rCollectEventDataWithFirst.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(dataId), Rep.Some(appId), Rep.Some(eventType), Rep.Some(userId), Rep.Some(channel), Rep.Some(isFirstVisit), Rep.Some(timestamp), Rep.Some(isFirstVisitChannel)).shaped.<>({r=>import r._; _1.map(_=> rCollectEventDataWithFirst.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6.get, _7.get, _8.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column data_id SqlType(bigserial), AutoInc, PrimaryKey */
    val dataId: Rep[Long] = column[Long]("data_id", O.AutoInc, O.PrimaryKey)
    /** Database column app_id SqlType(int8) */
    val appId: Rep[Long] = column[Long]("app_id")
    /** Database column event_type SqlType(int8) */
    val eventType: Rep[Long] = column[Long]("event_type")
    /** Database column user_id SqlType(varchar), Length(256,true) */
    val userId: Rep[String] = column[String]("user_id", O.Length(256,varying=true))
    /** Database column channel SqlType(varchar), Length(16,true), Default(ALL) */
    val channel: Rep[String] = column[String]("channel", O.Length(16,varying=true), O.Default("ALL"))
    /** Database column is_first_visit SqlType(int4), Default(0) */
    val isFirstVisit: Rep[Int] = column[Int]("is_first_visit", O.Default(0))
    /** Database column timestamp SqlType(int8), Default(0) */
    val timestamp: Rep[Long] = column[Long]("timestamp", O.Default(0L))
    /** Database column is_first_visit_channel SqlType(int4), Default(0) */
    val isFirstVisitChannel: Rep[Int] = column[Int]("is_first_visit_channel", O.Default(0))
  }
  /** Collection-like TableQuery object for table tCollectEventDataWithFirst */
  lazy val tCollectEventDataWithFirst = new TableQuery(tag => new tCollectEventDataWithFirst(tag))

  /** Entity class storing rows of table tCollectUserInfo
    *  @param id Database column id SqlType(int8), PrimaryKey
    *  @param appId Database column app_id SqlType(int8)
    *  @param userId Database column user_id SqlType(varchar), Length(256,true)
    *  @param nickname Database column nickname SqlType(varchar), Length(256,true), Default()
    *  @param headImg Database column head_img SqlType(varchar), Length(256,true), Default()
    *  @param bbsId Database column bbs_id SqlType(varchar), Length(256,true), Default() */
  case class rCollectUserInfo(id: Long, appId: Long, userId: String, nickname: String = "", headImg: String = "", bbsId: String = "")
  /** GetResult implicit for fetching rCollectUserInfo objects using plain SQL queries */
  implicit def GetResultrCollectUserInfo(implicit e0: GR[Long], e1: GR[String]): GR[rCollectUserInfo] = GR{
    prs => import prs._
      rCollectUserInfo.tupled((<<[Long], <<[Long], <<[String], <<[String], <<[String], <<[String]))
  }
  /** Table description of table collect_user_info. Objects of this class serve as prototypes for rows in queries. */
  class tCollectUserInfo(_tableTag: Tag) extends profile.api.Table[rCollectUserInfo](_tableTag, "collect_user_info") {
    def * = (id, appId, userId, nickname, headImg, bbsId) <> (rCollectUserInfo.tupled, rCollectUserInfo.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(id), Rep.Some(appId), Rep.Some(userId), Rep.Some(nickname), Rep.Some(headImg), Rep.Some(bbsId)).shaped.<>({r=>import r._; _1.map(_=> rCollectUserInfo.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(int8), PrimaryKey */
    val id: Rep[Long] = column[Long]("id", O.PrimaryKey)
    /** Database column app_id SqlType(int8) */
    val appId: Rep[Long] = column[Long]("app_id")
    /** Database column user_id SqlType(varchar), Length(256,true) */
    val userId: Rep[String] = column[String]("user_id", O.Length(256,varying=true))
    /** Database column nickname SqlType(varchar), Length(256,true), Default() */
    val nickname: Rep[String] = column[String]("nickname", O.Length(256,varying=true), O.Default(""))
    /** Database column head_img SqlType(varchar), Length(256,true), Default() */
    val headImg: Rep[String] = column[String]("head_img", O.Length(256,varying=true), O.Default(""))
    /** Database column bbs_id SqlType(varchar), Length(256,true), Default() */
    val bbsId: Rep[String] = column[String]("bbs_id", O.Length(256,varying=true), O.Default(""))
  }
  /** Collection-like TableQuery object for table tCollectUserInfo */
  lazy val tCollectUserInfo = new TableQuery(tag => new tCollectUserInfo(tag))

  /** Entity class storing rows of table tCustomEventDefinition
    *  @param customId Database column custom_id SqlType(bigserial), AutoInc, PrimaryKey
    *  @param appId Database column app_id SqlType(int8)
    *  @param eventName Database column event_name SqlType(varchar), Length(255,true), Default()
    *  @param property Database column property SqlType(jsonb)
    *  @param timestamp Database column timestamp SqlType(int8), Default(0) */
  case class rCustomEventDefinition(customId: Long, appId: Long, eventName: String = "", property: io.circe.Json, timestamp: Long = 0L)
  /** GetResult implicit for fetching rCustomEventDefinition objects using plain SQL queries */
  implicit def GetResultrCustomEventDefinition(implicit e0: GR[Long], e1: GR[String], e2: GR[io.circe.Json]): GR[rCustomEventDefinition] = GR{
    prs => import prs._
      rCustomEventDefinition.tupled((<<[Long], <<[Long], <<[String], <<[io.circe.Json], <<[Long]))
  }
  /** Table description of table custom_event_definition. Objects of this class serve as prototypes for rows in queries. */
  class tCustomEventDefinition(_tableTag: Tag) extends profile.api.Table[rCustomEventDefinition](_tableTag, "custom_event_definition") {
    def * = (customId, appId, eventName, property, timestamp) <> (rCustomEventDefinition.tupled, rCustomEventDefinition.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(customId), Rep.Some(appId), Rep.Some(eventName), Rep.Some(property), Rep.Some(timestamp)).shaped.<>({r=>import r._; _1.map(_=> rCustomEventDefinition.tupled((_1.get, _2.get, _3.get, _4.get, _5.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column custom_id SqlType(bigserial), AutoInc, PrimaryKey */
    val customId: Rep[Long] = column[Long]("custom_id", O.AutoInc, O.PrimaryKey)
    /** Database column app_id SqlType(int8) */
    val appId: Rep[Long] = column[Long]("app_id")
    /** Database column event_name SqlType(varchar), Length(255,true), Default() */
    val eventName: Rep[String] = column[String]("event_name", O.Length(255,varying=true), O.Default(""))
    /** Database column property SqlType(jsonb) */
    val property: Rep[io.circe.Json] = column[io.circe.Json]("property")
    /** Database column timestamp SqlType(int8), Default(0) */
    val timestamp: Rep[Long] = column[Long]("timestamp", O.Default(0L))
  }
  /** Collection-like TableQuery object for table tCustomEventDefinition */
  lazy val tCustomEventDefinition = new TableQuery(tag => new tCustomEventDefinition(tag))

  /** Entity class storing rows of table tEventDefinition
    *  @param eventId Database column event_id SqlType(bigserial), AutoInc, PrimaryKey
    *  @param appId Database column app_id SqlType(int8)
    *  @param eventName Database column event_name SqlType(varchar), Length(255,true), Default()
    *  @param eventType Database column event_type SqlType(int8), Default(0)
    *  @param filterCondition Database column filter_condition SqlType(jsonb)
    *  @param timestamp Database column timestamp SqlType(int8), Default(0) */
  case class rEventDefinition(eventId: Long, appId: Long, eventName: String = "", eventType: Long = 0L, filterCondition: io.circe.Json, timestamp: Long = 0L)
  /** GetResult implicit for fetching rEventDefinition objects using plain SQL queries */
  implicit def GetResultrEventDefinition(implicit e0: GR[Long], e1: GR[String], e2: GR[io.circe.Json]): GR[rEventDefinition] = GR{
    prs => import prs._
      rEventDefinition.tupled((<<[Long], <<[Long], <<[String], <<[Long], <<[io.circe.Json], <<[Long]))
  }
  /** Table description of table event_definition. Objects of this class serve as prototypes for rows in queries. */
  class tEventDefinition(_tableTag: Tag) extends profile.api.Table[rEventDefinition](_tableTag, "event_definition") {
    def * = (eventId, appId, eventName, eventType, filterCondition, timestamp) <> (rEventDefinition.tupled, rEventDefinition.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(eventId), Rep.Some(appId), Rep.Some(eventName), Rep.Some(eventType), Rep.Some(filterCondition), Rep.Some(timestamp)).shaped.<>({r=>import r._; _1.map(_=> rEventDefinition.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column event_id SqlType(bigserial), AutoInc, PrimaryKey */
    val eventId: Rep[Long] = column[Long]("event_id", O.AutoInc, O.PrimaryKey)
    /** Database column app_id SqlType(int8) */
    val appId: Rep[Long] = column[Long]("app_id")
    /** Database column event_name SqlType(varchar), Length(255,true), Default() */
    val eventName: Rep[String] = column[String]("event_name", O.Length(255,varying=true), O.Default(""))
    /** Database column event_type SqlType(int8), Default(0) */
    val eventType: Rep[Long] = column[Long]("event_type", O.Default(0L))
    /** Database column filter_condition SqlType(jsonb) */
    val filterCondition: Rep[io.circe.Json] = column[io.circe.Json]("filter_condition")
    /** Database column timestamp SqlType(int8), Default(0) */
    val timestamp: Rep[Long] = column[Long]("timestamp", O.Default(0L))
  }
  /** Collection-like TableQuery object for table tEventDefinition */
  lazy val tEventDefinition = new TableQuery(tag => new tEventDefinition(tag))

  /** Entity class storing rows of table tPowerKey
    *  @param key Database column key SqlType(varchar), PrimaryKey, Length(100,true)
    *  @param appId Database column app_id SqlType(int8)
    *  @param createTime Database column create_time SqlType(int8), Default(0) */
  case class rPowerKey(key: String, appId: Long, createTime: Long = 0L)
  /** GetResult implicit for fetching rPowerKey objects using plain SQL queries */
  implicit def GetResultrPowerKey(implicit e0: GR[String], e1: GR[Long]): GR[rPowerKey] = GR{
    prs => import prs._
      rPowerKey.tupled((<<[String], <<[Long], <<[Long]))
  }
  /** Table description of table power_key. Objects of this class serve as prototypes for rows in queries. */
  class tPowerKey(_tableTag: Tag) extends profile.api.Table[rPowerKey](_tableTag, "power_key") {
    def * = (key, appId, createTime) <> (rPowerKey.tupled, rPowerKey.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(key), Rep.Some(appId), Rep.Some(createTime)).shaped.<>({r=>import r._; _1.map(_=> rPowerKey.tupled((_1.get, _2.get, _3.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column key SqlType(varchar), PrimaryKey, Length(100,true) */
    val key: Rep[String] = column[String]("key", O.PrimaryKey, O.Length(100,varying=true))
    /** Database column app_id SqlType(int8) */
    val appId: Rep[Long] = column[Long]("app_id")
    /** Database column create_time SqlType(int8), Default(0) */
    val createTime: Rep[Long] = column[Long]("create_time", O.Default(0L))
  }
  /** Collection-like TableQuery object for table tPowerKey */
  lazy val tPowerKey = new TableQuery(tag => new tPowerKey(tag))

  /** Entity class storing rows of table tSegment
    *  @param segId Database column seg_id SqlType(bigserial), AutoInc, PrimaryKey
    *  @param appId Database column app_id SqlType(int8)
    *  @param segName Database column seg_name SqlType(varchar), Length(255,true), Default()
    *  @param filterCondition Database column filter_condition SqlType(jsonb)
    *  @param timestamp Database column timestamp SqlType(int8), Default(0)
    *  @param grain Database column grain SqlType(varchar), Length(20,true), Default() */
  case class rSegment(segId: Long, appId: Long, segName: String = "", filterCondition: io.circe.Json, timestamp: Long = 0L, grain: String = "")
  /** GetResult implicit for fetching rSegment objects using plain SQL queries */
  implicit def GetResultrSegment(implicit e0: GR[Long], e1: GR[String], e2: GR[io.circe.Json]): GR[rSegment] = GR{
    prs => import prs._
      rSegment.tupled((<<[Long], <<[Long], <<[String], <<[io.circe.Json], <<[Long], <<[String]))
  }
  /** Table description of table segment. Objects of this class serve as prototypes for rows in queries. */
  class tSegment(_tableTag: Tag) extends profile.api.Table[rSegment](_tableTag, "segment") {
    def * = (segId, appId, segName, filterCondition, timestamp, grain) <> (rSegment.tupled, rSegment.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(segId), Rep.Some(appId), Rep.Some(segName), Rep.Some(filterCondition), Rep.Some(timestamp), Rep.Some(grain)).shaped.<>({r=>import r._; _1.map(_=> rSegment.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column seg_id SqlType(bigserial), AutoInc, PrimaryKey */
    val segId: Rep[Long] = column[Long]("seg_id", O.AutoInc, O.PrimaryKey)
    /** Database column app_id SqlType(int8) */
    val appId: Rep[Long] = column[Long]("app_id")
    /** Database column seg_name SqlType(varchar), Length(255,true), Default() */
    val segName: Rep[String] = column[String]("seg_name", O.Length(255,varying=true), O.Default(""))
    /** Database column filter_condition SqlType(jsonb) */
    val filterCondition: Rep[io.circe.Json] = column[io.circe.Json]("filter_condition")
    /** Database column timestamp SqlType(int8), Default(0) */
    val timestamp: Rep[Long] = column[Long]("timestamp", O.Default(0L))
    /** Database column grain SqlType(varchar), Length(20,true), Default() */
    val grain: Rep[String] = column[String]("grain", O.Length(20,varying=true), O.Default(""))
  }
  /** Collection-like TableQuery object for table tSegment */
  lazy val tSegment = new TableQuery(tag => new tSegment(tag))

  /** Entity class storing rows of table tWxBindApp
    *  @param id Database column id SqlType(bigserial), AutoInc, PrimaryKey
    *  @param openid Database column openid SqlType(varchar), Length(256,true), Default()
    *  @param appId Database column app_id SqlType(int8), Default(0) */
  case class rWxBindApp(id: Long, openid: String = "", appId: Long = 0L)
  /** GetResult implicit for fetching rWxBindApp objects using plain SQL queries */
  implicit def GetResultrWxBindApp(implicit e0: GR[Long], e1: GR[String]): GR[rWxBindApp] = GR{
    prs => import prs._
      rWxBindApp.tupled((<<[Long], <<[String], <<[Long]))
  }
  /** Table description of table wx_bind_app. Objects of this class serve as prototypes for rows in queries. */
  class tWxBindApp(_tableTag: Tag) extends profile.api.Table[rWxBindApp](_tableTag, "wx_bind_app") {
    def * = (id, openid, appId) <> (rWxBindApp.tupled, rWxBindApp.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(id), Rep.Some(openid), Rep.Some(appId)).shaped.<>({r=>import r._; _1.map(_=> rWxBindApp.tupled((_1.get, _2.get, _3.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(bigserial), AutoInc, PrimaryKey */
    val id: Rep[Long] = column[Long]("id", O.AutoInc, O.PrimaryKey)
    /** Database column openid SqlType(varchar), Length(256,true), Default() */
    val openid: Rep[String] = column[String]("openid", O.Length(256,varying=true), O.Default(""))
    /** Database column app_id SqlType(int8), Default(0) */
    val appId: Rep[Long] = column[Long]("app_id", O.Default(0L))
  }
  /** Collection-like TableQuery object for table tWxBindApp */
  lazy val tWxBindApp = new TableQuery(tag => new tWxBindApp(tag))

  /** Entity class storing rows of table tWxUser
    *  @param id Database column id SqlType(bigserial), AutoInc, PrimaryKey
    *  @param openId Database column open_id SqlType(varchar), Length(256,true), Default()
    *  @param nickname Database column nickname SqlType(varchar), Length(256,true), Default()
    *  @param headImg Database column head_img SqlType(varchar), Length(256,true), Default()
    *  @param createTime Database column create_time SqlType(int8), Default(0)
    *  @param lastLoginTime Database column last_login_time SqlType(int8), Default(0) */
  case class rWxUser(id: Long, openId: String = "", nickname: String = "", headImg: String = "", createTime: Long = 0L, lastLoginTime: Long = 0L)
  /** GetResult implicit for fetching rWxUser objects using plain SQL queries */
  implicit def GetResultrWxUser(implicit e0: GR[Long], e1: GR[String]): GR[rWxUser] = GR{
    prs => import prs._
      rWxUser.tupled((<<[Long], <<[String], <<[String], <<[String], <<[Long], <<[Long]))
  }
  /** Table description of table wx_user. Objects of this class serve as prototypes for rows in queries. */
  class tWxUser(_tableTag: Tag) extends profile.api.Table[rWxUser](_tableTag, "wx_user") {
    def * = (id, openId, nickname, headImg, createTime, lastLoginTime) <> (rWxUser.tupled, rWxUser.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(id), Rep.Some(openId), Rep.Some(nickname), Rep.Some(headImg), Rep.Some(createTime), Rep.Some(lastLoginTime)).shaped.<>({r=>import r._; _1.map(_=> rWxUser.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(bigserial), AutoInc, PrimaryKey */
    val id: Rep[Long] = column[Long]("id", O.AutoInc, O.PrimaryKey)
    /** Database column open_id SqlType(varchar), Length(256,true), Default() */
    val openId: Rep[String] = column[String]("open_id", O.Length(256,varying=true), O.Default(""))
    /** Database column nickname SqlType(varchar), Length(256,true), Default() */
    val nickname: Rep[String] = column[String]("nickname", O.Length(256,varying=true), O.Default(""))
    /** Database column head_img SqlType(varchar), Length(256,true), Default() */
    val headImg: Rep[String] = column[String]("head_img", O.Length(256,varying=true), O.Default(""))
    /** Database column create_time SqlType(int8), Default(0) */
    val createTime: Rep[Long] = column[Long]("create_time", O.Default(0L))
    /** Database column last_login_time SqlType(int8), Default(0) */
    val lastLoginTime: Rep[Long] = column[Long]("last_login_time", O.Default(0L))
  }
  /** Collection-like TableQuery object for table tWxUser */
  lazy val tWxUser = new TableQuery(tag => new tWxUser(tag))
}
