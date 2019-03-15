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

  val config = ConfigFactory.parseResources("product.conf").withFallback(ConfigFactory.load())

  val appConfig = config.getConfig("app")
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

  val isGray = appConfig.getBoolean("isGray")

  val botServerPort = appConfig.getInt("botServerPort")

  val botId = appConfig.getString("botInfo.botId")
  val botKey = appConfig.getString("botInfo.botKey")

  val viewWidth = appConfig.getInt("viewSize.width")
  val viewHeight = appConfig.getInt("viewSize.height")

}
