package com.neo.sk.tank.controller

import akka.http.scaladsl.Http
import com.neo.sk.tank.common.Context
import com.neo.sk.tank.{App, model}
import com.neo.sk.tank.view.{GameHallListener, GameHallScreen, PlayGameScreen}
import javafx.geometry.Pos
import javafx.scene.Group
import javafx.scene.control.{Label, ListView}
/**
  * created by benyafang on 2018/10/26
  * 获取房间列表，指定房间进入和随机进入房间
  * 实例化完成即向服务器请求房间列表
  * */
class HallScreenController(val context:Context,val gameHall:GameHallScreen){

  private def getRoomList() = {

//    val url =
  }



  gameHall.setListener(new GameHallListener{
    override def randomBtnListener(playerInfo: model.PlayerInfo,gameServerInfo:model.GameServerInfo): Unit = {
      App.pushStack2AppThread{
        val playGameScreen:PlayGameScreen = new PlayGameScreen(context)
        context.switchScene(playGameScreen.getScene())
        new PlayScreenController(playerInfo,gameServerInfo,context,playGameScreen)
        close()
      }

    }

    override def confirmBtnListener(playerInfo: model.PlayerInfo, select: ListView[String], gameServerInfo: model.GameServerInfo,group:Group): Unit = {
      val roomId = select.getSelectionModel().selectedItemProperty().get()
      if(roomId == null){
        val label = new Label("还没有选择房间哦")
        group.getChildren().add(label)
        label.setAlignment(Pos.CENTER)
        label.setLayoutY(200)
      }else{
        val playGameScreen:PlayGameScreen = new PlayGameScreen(context)
        context.switchScene(playGameScreen.getScene())
//        playGameScreen.requestFocus()
        new PlayScreenController(playerInfo,gameServerInfo,context,playGameScreen)
        close()
      }

    }

  })

  private def close() = {


  }

}
