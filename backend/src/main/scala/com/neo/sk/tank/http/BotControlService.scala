package com.neo.sk.tank.http

import org.slf4j.LoggerFactory
import akka.http.scaladsl.server.Directives.{complete, _}
import akka.http.scaladsl.server.Route
import scala.language.postfixOps
import com.neo.sk.tank.Boot.{botManager}
import com.neo.sk.tank.core.BotManager.DeleteChild
import com.neo.sk.tank.core.{BotManager}
import com.neo.sk.tank.shared.ptcl.{CommonRsp, ErrorRsp, SuccessRsp}


trait BotControlService extends ServiceUtils {

  import io.circe._
  import io.circe.generic.auto._

  private val log = LoggerFactory.getLogger(getClass)

  private val createBots = (path("createBots") & get) {
    botManager ! BotManager.CreateABot(6, 3)
    complete(SuccessRsp(0, "create Bot successfully"))
  }

  private val delBots = (path("delBots") & get){
    botManager ! DeleteChild
    complete(SuccessRsp(0, "delete Bot successfully"))
  }

  val BotRoutes: Route = createBots ~ delBots
}
