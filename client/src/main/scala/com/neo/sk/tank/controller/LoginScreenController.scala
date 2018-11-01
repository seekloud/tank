package com.neo.sk.tank.controller

import akka.actor.typed.ActorRef
import com.neo.sk.tank.actor.LoginActor
import com.neo.sk.tank.common.Context
import com.neo.sk.tank.view.{GameHallScreen, LoginScreen, PlayGameScreen}
import akka.actor.typed.scaladsl.adapter._
import com.neo.sk.tank.App
import com.neo.sk.tank.actor.LoginActor.Request
import com.neo.sk.tank.model.{GameServerInfo, PlayerInfo}
import javafx.animation.{AnimationTimer, KeyFrame, Timeline}
import javafx.application.Platform
import javafx.util.Duration

/**
  * Created by hongruying on 2018/10/23
  */
class LoginScreenController(val context: Context, val loginScreen: LoginScreen) {

  import com.neo.sk.tank.App._

  private val loginActor: ActorRef[LoginActor.Command] = system.spawn(LoginActor.create(this),"LoginManager")

  loginActor ! LoginActor.Login


  def start={
   // joinGame(PlayerInfo("test","test","sgadga"),GameServerInfo("","",""))
  }

  /**
    * 显示扫码图片
    * */
  def showScanUrl(scanUrl:String):Unit = {
    App.pushStack2AppThread(loginScreen.showScanUrl(scanUrl))
  }



  /**
    * 切换到游戏页面
    * */
  def joinGame(playerInfo:PlayerInfo, gameServerInfo: GameServerInfo) = {
    println("joinGame----------")
    App.pushStack2AppThread{
      val playGameScreen = new PlayGameScreen(context)
      //new PlayScreenController(playerInfo, gameServerInfo, context, playGameScreen).start
      val gameHallScreen = new GameHallScreen(context, playerInfo)
      context.switchScene(gameHallScreen.getScene())
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
