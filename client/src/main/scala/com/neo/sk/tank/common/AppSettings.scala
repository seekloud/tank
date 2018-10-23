package com.neo.sk.tank.common

import java.util.concurrent.TimeUnit

import com.neo.sk.tank.shared.config.TankGameConfig
import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.LoggerFactory

import scala.collection.mutable.ListBuffer

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











}
