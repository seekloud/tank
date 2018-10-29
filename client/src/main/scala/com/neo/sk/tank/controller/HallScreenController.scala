package com.neo.sk.tank.controller

import akka.http.scaladsl.Http
import com.neo.sk.tank.common.Context
import com.neo.sk.tank.{App, model}
import com.neo.sk.tank.view.{GameHallListener, GameHallScreen, PlayGameScreen}
import javafx.geometry.Pos
import javafx.scene.Group
import javafx.scene.control.{Label, ListView}
import com.neo.sk.utils.HttpUtil.Imports.postJsonRequestSend
import com.neo.sk.utils.SecureUtil._
import io.circe.Json
import io.circe.parser.decode
import io.circe.syntax._
import io.circe.generic.auto._
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success}
/**
  * created by benyafang on 2018/10/26
  * 获取房间列表，指定房间进入和随机进入房间
  * 实例化完成即向服务器请求房间列表
  * */
class HallScreenController(val context:Context,val gameHall:GameHallScreen){
  private val log = LoggerFactory.getLogger(this.getClass)

  private def getRoomListInit() = {
    val url = s"https://localhost:30369/tank/getRoomList"
    val jsonData = genPostEnvelope("esheep",System.nanoTime().toString,{}.asJson.noSpaces,"").asJson.noSpaces
    postJsonRequestSend("post",url,List(),jsonData,timeOut = 60 * 1000,needLogRsp = false).map{
      case Right(value) =>
        decode[model.RoomListRsp](value) match {
          case Right(data) =>
            if(data.errCode == 0){
              Right(data)
            }else{
              log.debug(s"获取列表失败，errCode:${data.errCode},msg:${data.msg}")
              Left("Error")
            }
          case Left(error) =>
            log.debug(s"444")
            Left("Error")

        }
      case Left(error) =>
        log.debug(s"555")
        Left("Error")
    }
  }
  updateRoomList()

  private def updateRoomList() = {
    getRoomListInit().onComplete{
      case Success(res) =>
        res match {
          case Right(roomListRsp) =>
            gameHall.updateRoomList(roomListRsp.data.roomList)
          case Left(e) =>
            log.error(s"获取房间列表失败，error：${e}")
        }
      case Failure(e) =>
        log.error(s"failure:${e}")
    }
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
