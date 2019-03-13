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

import akka.actor.Cancellable
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._
import javafx.scene.control._
import org.seekloud.tank.App.{executor, scheduler}
import org.seekloud.tank.common.Context
import org.seekloud.tank.game.control.UserPlayController
import org.seekloud.tank.model.{GameServerInfo, PlayerInfo}
import org.seekloud.tank.view.{GameHallListener, GameHallScreen, PlayGameScreen}
import org.seekloud.tank.{App, model}
import org.seekloud.utils.HttpUtil.Imports.postJsonRequestSend
import org.seekloud.utils.SecureUtil._
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.concurrent.duration._
import scala.util.{Failure, Success}
/**
  * created by benyafang on 2018/10/26
  * 获取房间列表，指定房间进入和随机进入房间
  * 实例化完成即向服务器请求房间列表
  * */
class HallScreenController(val context:Context, val gameHall:GameHallScreen, gameServerInfo: GameServerInfo, playerInfo:PlayerInfo){
  private val log = LoggerFactory.getLogger(this.getClass)
  private var timer:Cancellable = _
  private var roomMap = mutable.HashMap[Long,Boolean]()

  private def getRoomListInit() = {
    //需要起一个定时器，定时刷新请求
    val url = s"https://${gameServerInfo.domain}/tank/getRoomList"
//    val url = s"http://localhost:30369/tank/getRoomList"
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
            log.debug(s"获取房间列表失败，${error}")
            Left("Error")

        }
      case Left(error) =>
        log.debug(s"获取房间列表失败，${error}")
        Left("Error")
    }
  }

  App.pushStack2AppThread{
    timer = scheduler.schedule(1.millis,1.minutes){
      updateRoomList()
    }
  }

  private def updateRoomList() = {
    getRoomListInit().onComplete{
      case Success(res) =>
        res match {
          case Right(roomListRsp) =>
            roomMap = roomListRsp.data.roomList
            App.pushStack2AppThread(
              gameHall.updateRoomList(roomMap.keys.toList)
            )
          case Left(e) =>
            log.error(s"获取房间列表失败，error：${e}")
        }
      case Failure(e) =>
        log.error(s"failure:${e}")
    }
  }

  gameHall.setListener(new GameHallListener{
    override def randomBtnListener(): Unit = {
      App.pushStack2AppThread{
        val playGameScreen:PlayGameScreen = new PlayGameScreen(context)
        context.switchScene(playGameScreen.getScene(),resize = true,fullScreen = true)
        new UserPlayController(playerInfo,gameServerInfo,context,playGameScreen,None,None,false).start
        playGameScreen.setCursor
        close()
      }
    }

    override def confirmBtnListener(roomIdListView:String, roomIdTextField:String): Unit = {
      App.pushStack2AppThread{
        if(roomIdTextField != "" && roomMap.contains(roomIdTextField.toLong)){
          if(!roomMap(roomIdTextField.toLong)){
            val playGameScreen: PlayGameScreen = new PlayGameScreen(context)
            context.switchScene(playGameScreen.getScene(), resize = true, fullScreen = true)
            new UserPlayController(playerInfo, gameServerInfo, context, playGameScreen, Some(roomIdTextField), None, false).start
            playGameScreen.setCursor
            close()
          }
          else{
            val pwdInput = new TextInputDialog()
            pwdInput.setHeaderText("请输入房间密码")
            val pwdResult = pwdInput.showAndWait()
            if(pwdResult.isPresent){
              val playGameScreen:PlayGameScreen = new PlayGameScreen(context)
              context.switchScene(playGameScreen.getScene(),resize = true,fullScreen = true)
              new UserPlayController(playerInfo, gameServerInfo, context, playGameScreen, Some(roomIdTextField), Some(pwdResult.get()),false).start
              playGameScreen.setCursor
              close()
            }
          }
        }else if(roomIdListView != ""){
          if(!roomMap(roomIdListView.toLong)){
            val playGameScreen: PlayGameScreen = new PlayGameScreen(context)
            context.switchScene(playGameScreen.getScene(), resize = true, fullScreen = true)
            new UserPlayController(playerInfo, gameServerInfo, context, playGameScreen, Some(roomIdListView), None, false).start
            playGameScreen.setCursor
            close()
          }
          else{
            val pwdInput = new TextInputDialog()
            pwdInput.setHeaderText("请输入房间密码")
            val pwdResult = pwdInput.showAndWait()
            if(pwdResult.isPresent){
              val playGameScreen:PlayGameScreen = new PlayGameScreen(context)
              context.switchScene(playGameScreen.getScene(),resize = true,fullScreen = true)
              new UserPlayController(playerInfo, gameServerInfo, context, playGameScreen, Some(roomIdListView), Some(pwdResult.get()),false).start
              playGameScreen.setCursor
              close()
            }
          }
        }else{
          val warn = new Alert(Alert.AlertType.WARNING,"您还未选择房间或选择的房间不存在",new ButtonType("确定",ButtonBar.ButtonData.YES))
          warn.setTitle("警示")
          val buttonType = warn.showAndWait()
          if(buttonType.get().getButtonData.equals(ButtonBar.ButtonData.YES))warn.close()
        }
      }

    }

    override def addSelfDefinedRoom(): Unit = {
      App.pushStack2AppThread(gameHall.plainScreen)
    }

    override def createRoom(roomId: String, pwd: Option[String]): Unit = {
      App.pushStack2AppThread{
        if(!roomMap.contains(if(roomId == null) -1L else roomId.toLong)){
          val playGameScreen:PlayGameScreen = new PlayGameScreen(context)
          context.switchScene(playGameScreen.getScene(),resize = true,fullScreen = true)
          new UserPlayController(playerInfo, gameServerInfo, context, playGameScreen, Option(roomId), pwd, true).start
          playGameScreen.setCursor
          close()
        }
        else{
          val warn = new Alert(Alert.AlertType.WARNING,"您创建的房间已存在，请重新输入房间ID",new ButtonType("确定",ButtonBar.ButtonData.YES))
          warn.setTitle("警示")
          val buttonType = warn.showAndWait()
          if(buttonType.get().getButtonData.equals(ButtonBar.ButtonData.YES))warn.close()
        }
      }
    }

    override def change2Encrypt(): Unit = {
      App.pushStack2AppThread(gameHall.encryptScreen)
    }

    override def change2Plain(): Unit = {
      App.pushStack2AppThread(gameHall.plainScreen)
    }

    override def backToRoomList(): Unit = {
      App.pushStack2AppThread{
        val gameHallScreen = new GameHallScreen(context, playerInfo)
        context.switchScene(gameHallScreen.getScene,resize = true)
        new HallScreenController(context, gameHallScreen, gameServerInfo, playerInfo)
        close()
      }
    }
    
  })

  private def close() = {
    timer.cancel()
  }

}
