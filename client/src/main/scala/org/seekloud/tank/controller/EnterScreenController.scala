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

package org.seekloud.tank.controller

import org.seekloud.tank.{BotServer, ClientApp}
import org.seekloud.tank.ClientApp.{botServer, executor, loginActor, scheduler, timeout}
import org.seekloud.tank.common.{AppSettings, Context}
import org.seekloud.tank.core.LoginActor
import akka.actor.typed.scaladsl.AskPattern._
import org.seekloud.tank.game.control.BotViewController
import org.seekloud.tank.model.{BotKeyReq, GameServerInfo, PlayerInfo}
import org.seekloud.tank.view.{EnterSceneListener, EnterScreen, LoginScreen, PlayGameScreen}
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.util.{Failure, Success}

class EnterScreenController(val context: Context, val enter: EnterScreen) {
  private val log = LoggerFactory.getLogger(this.getClass)
  enter.setListener(new EnterSceneListener {
    override def onBtnForMan(): Unit = {
      ClientApp.pushStack2AppThread {
        val loginScreen = new LoginScreen(context)
        LoginScreenController.loginScreenController = new LoginScreenController(context, loginScreen)
        LoginScreenController.loginScreenController.start
      }
    }

    override def onBtnForBot(): Unit = {
      ClientApp.pushStack2AppThread{
        val loginScreen = new LoginScreen(context)
        loginScreen.botLogin()
        LoginScreenController.loginScreenController = new LoginScreenController(context,loginScreen)
      }
//      val rspFuture: Future[(PlayerInfo, GameServerInfo)] = loginActor ? (LoginActor.BotLogin(BotKeyReq(AppSettings.botId, AppSettings.botKey), _))
//      rspFuture.onComplete {
//        case Success(value) =>
//          log.info("botView start")
//          ClientApp.pushStack2AppThread {
//            /** bot启动 */
//            val playGameScreen: PlayGameScreen = new PlayGameScreen(context)
//            context.switchScene(playGameScreen.getScene(), resize = true, fullScreen = true)
//            playGameScreen.setCursor
//            val c = new BotViewController(value._1, value._2, true, Some(playGameScreen))
//            c.startGame
//            botServer ! BotServer.BuildServer(AppSettings.botServerPort, executor, c)
//          }
//        case Failure(exception) =>
//          log.error(exception.getMessage)
//      }
    }
  })


}
