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

package org.seekloud.tank


import akka.actor.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.scaladsl.AskPattern._
import akka.dispatch.MessageDispatcher
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.{getFromResource, pathPrefix}
import akka.stream.ActorMaterializer
import akka.util.Timeout
import javafx.application.{Application, Platform}
import javafx.stage.Stage
import org.seekloud.tank.ClientApp.loginActor
import org.seekloud.tank.core.{LoginActor, TokenActor}
import org.seekloud.tank.common.{AppSettings, Context}
import org.seekloud.tank.controller.EnterScreenController
import org.seekloud.tank.game.control.BotViewController
import org.seekloud.tank.model.{BotKeyReq, GameServerInfo, PlayerInfo}
import org.seekloud.tank.view.EnterScreen

import scala.concurrent.Future
import scala.util.{Failure, Success}
/**
  * Created by hongruying on 2018/10/22
  */
class  ClientApp extends Application {

  override def start(primaryStage: Stage): Unit = {
    val context = new Context(primaryStage)
    val enterScreen = new EnterScreen(context)
    context.switchScene(enterScreen.getScene,resize = true)
    new EnterScreenController(context, enterScreen)
//    loginActor ! LoginActor.BotLogin(BotKeyReq(AppSettings.botId, AppSettings.botKey))
  }

}

object ClientApp {

  import concurrent.duration._
  import scala.language.postfixOps

  implicit val system = ActorSystem("tankDemoSystem")
  // the executor should not be the default dispatcher.
  implicit val executor: MessageDispatcher =
    system.dispatchers.lookup("akka.actor.default-dispatcher")

  implicit val materializer = ActorMaterializer()

  implicit val scheduler = system.scheduler

  val tokenActor:ActorRef[TokenActor.Command] = system.spawn(TokenActor.create(),"esheepSyncClient")

  val loginActor: ActorRef[LoginActor.Command] = system.spawn(LoginActor.create(),"LoginActor")

  val botServer: ActorRef[BotServer.Command] = system.spawn(BotServer.create(), "botServer")

  implicit val timeout:Timeout = Timeout(20 seconds) // for actor asks

  val log: LoggingAdapter = Logging(system, getClass)

  def pushStack2AppThread(fun: => Unit) = {
    Platform.runLater(() => fun)
  }

 /* def main(args: Array[String]): Unit = {
    loginActor ! LoginActor.BotLogin(BotKeyReq(AppSettings.botId, AppSettings.botKey))
  }*/
}
