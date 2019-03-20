/*
 * Copyright 2018 seekloud (https://github.com/seekloud)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.seekloud.tank.common

import java.util.concurrent.TimeUnit

import com.typesafe.config.{Config, ConfigFactory}
import org.seekloud.tank.core.game.TankGameConfigServerImpl
import org.seekloud.tank.shared.config.{TankGameConfigImpl}
import org.seekloud.utils.SessionSupport.SessionConfig
import org.slf4j.LoggerFactory

/**
  * Created by hongruying on 2018/3/11
  */
object AppSettings {

  private implicit class RichConfig(config: Config) {
    val noneValue = "none"

    def getOptionalString(path: String): Option[String] =
      if (config.getAnyRef(path) == noneValue) None
      else Some(config.getString(path))

    def getOptionalLong(path: String): Option[Long] =
      if (config.getAnyRef(path) == noneValue) None
      else Some(config.getLong(path))

    def getOptionalDurationSeconds(path: String): Option[Long] =
      if (config.getAnyRef(path) == noneValue) None
      else Some(config.getDuration(path, TimeUnit.SECONDS))
  }


  val log = LoggerFactory.getLogger(this.getClass)
  val config = ConfigFactory.parseResources("product.conf").withFallback(ConfigFactory.load())
  val tankGameConfig:TankGameConfigImpl = TankGameConfigServerImpl(ConfigFactory.parseResources("tankGame.conf")).tankGameConfig



  val appConfig = config.getConfig("app")
  val dependence = config.getConfig("dependence")

  val personLimit = appConfig.getInt("tankGameRoomManager.personLimit")
  val supportLiveLimit = appConfig.getBoolean("tankGameRoomManager.supportLiveLimit")
  val botLimit = appConfig.getInt("tankGameRoomManager.botLimit")
  val botSupport = appConfig.getBoolean("tankGameRoomManager.botSupport")

  val nameList = appConfig.getStringList("botManager.nameList")
  val needSpecialName = appConfig.getBoolean("botManager.needSpecialName")



  val appSecureMap = {
    import collection.JavaConverters._
    val appIdList = collectionAsScalaIterable(appConfig.getStringList("client.appIds"))
    val secureKeys = collectionAsScalaIterable(appConfig.getStringList("client.secureKeys"))
    require(appIdList.size == secureKeys.size, "appIdList.length and secureKeys.length not equal.")
    appIdList.zip(secureKeys).toMap
  }

  val httpInterface = appConfig.getString("http.interface")
  val httpPort = appConfig.getInt("http.port")

  val serverProtocol = appConfig.getString("server.protocol")
  val serverHost = appConfig.getString("server.host")
  val rootPath = appConfig.getString("server.rootPath")
  val baseUrl = serverProtocol + "://" + serverHost


  val authCheck = appConfig.getBoolean("authCheck")
  val ramblerAuthCheck = appConfig.getBoolean("ramblerAuthCheck")

  val gameDataDirectoryPath = appConfig.getString("gameDataDirectoryPath")
  val gameRecordIsWork = appConfig.getBoolean("gameRecordIsWork")
  val gameRecordTime = appConfig.getInt("gameRecordTime")


  val slickConfig = config.getConfig("slick.db")
  val slickUrl = slickConfig.getString("url")
  val slickUser = slickConfig.getString("user")
  val slickPassword = slickConfig.getString("password")
  val slickMaximumPoolSize = slickConfig.getInt("maximumPoolSize")
  val slickConnectTimeout = slickConfig.getInt("connectTimeout")
  val slickIdleTimeout = slickConfig.getInt("idleTimeout")
  val slickMaxLifetime = slickConfig.getInt("maxLifetime")




  private val esheepConfig = appConfig.getConfig("esheep")
  val esheepAppId = esheepConfig.getString("appId")
  val esheepSecureKey = esheepConfig.getString("secureKey")
  val esheepProtocol = esheepConfig.getString("protocol")
  val esheepHost = esheepConfig.getString("host")
  val esheepPort = esheepConfig.getInt("port")
  val esheepDomain = esheepConfig.getString("domain")
  val esheepGameId = esheepConfig.getLong("gameId")
  val esheepGameKey = esheepConfig.getString("gsKey")
  val esheepAuthToken = esheepConfig.getBoolean("authToken")





  val sessionConfig = {
    val sConf = config.getConfig("session")
    SessionConfig(
      cookieName = sConf.getString("cookie.name"),
      serverSecret = sConf.getString("serverSecret"),
      domain = sConf.getOptionalString("cookie.domain"),
      path = sConf.getOptionalString("cookie.path"),
      secure = sConf.getBoolean("cookie.secure"),
      httpOnly = sConf.getBoolean("cookie.httpOnly"),
      maxAge = sConf.getOptionalDurationSeconds("cookie.maxAge"),
      sessionEncryptData = sConf.getBoolean("encryptData")
    )
  }



  val essfMapKeyName = "essfMap"






}
