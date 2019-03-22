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

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.adapter._
import org.seekloud.tank.ClientApp
import org.seekloud.tank.core.LoginActor
import org.seekloud.tank.common.Context
import org.seekloud.tank.model.{GameServerInfo, PlayerInfo}
import org.seekloud.tank.view._
import org.seekloud.tank.BotServer
import org.seekloud.tank.common.AppSettings
import akka.actor.typed.scaladsl.AskPattern._
import org.seekloud.tank.game.control.BotViewController
import org.seekloud.tank.model.BotKeyReq

import scala.concurrent.Future
import scala.util.{Failure, Success}
/**
  * Created by hongruying on 2018/10/23
  */
object LoginScreenController{
  var loginScreenController:LoginScreenController=_
}
class LoginScreenController(val context: Context, val loginScreen: LoginScreen) {

  import org.seekloud.tank.ClientApp._

  def start={}

  loginScreen.setLoginSceneListener(new LoginSceneListener {
    override def onButtonConnect(): Unit = {
      loginActor ! LoginActor.QrLogin
    }

    override def onButtonEmail(mail: String, pwd: String): Unit = {
      loginActor ! LoginActor.EmailLogin(mail,pwd)
    }

    override def onLinkToEmail(): Unit = {
      loginActor ! LoginActor.EmailLogin
    }

    override def onLinkToQr(): Unit = {
      loginActor ! LoginActor.QrLogin
    }

    override def onButtonBot(id: String, key: String, frame: Int): Unit = {
      val rspFuture: Future[(PlayerInfo, GameServerInfo)] = loginActor ? (LoginActor.BotLogin(BotKeyReq(id, key), _))
      rspFuture.onComplete {
        case Success(value) =>
          log.info("botView start")
          ClientApp.pushStack2AppThread {
            /** bot启动 */
            val playGameScreen: PlayGameScreen = new PlayGameScreen(context)
            context.switchScene(playGameScreen.getScene(), resize = true, fullScreen = true)
            playGameScreen.setCursor
            val c = new BotViewController(value._1, value._2, true, Some(playGameScreen))
            c.startGame
            botServer ! BotServer.BuildServer(AppSettings.botServerPort, executor, c)
          }
        case Failure(exception) =>
          log.error(exception.getMessage)
      }
    }
  })

  /**
    * 显示扫码图片
    * */
  def showScanUrl(scanUrl:String):Unit = {
    ClientApp.pushStack2AppThread(loginScreen.showScanUrl(scanUrl))
  }

  def showSuccess()={
    ClientApp.pushStack2AppThread(loginScreen.loginSuccess())
  }


  def showLoginError(error: String)={
    ClientApp.pushStack2AppThread(loginScreen.getImgError(error))
  }

  //显示邮箱登录
  def showEmailLogin() = {
    ClientApp.pushStack2AppThread(loginScreen.emailLogin())
  }


  /**
    * 切换到游戏页面
    * */
  def joinGame(playerInfo:PlayerInfo, gameServerInfo: GameServerInfo) = {
    println("joinGame----------")
    loginActor ! LoginActor.StopWs
    ClientApp.pushStack2AppThread{
      val gameHallScreen = new GameHallScreen(context, playerInfo)
      context.switchScene(gameHallScreen.getScene,resize = true)
      new HallScreenController(context, gameHallScreen, gameServerInfo, playerInfo)
      close()
    }
  }


  /**
    * 资源回收
    * eg.关闭Actor
    * */
  private def close():Unit = {

  }




}
