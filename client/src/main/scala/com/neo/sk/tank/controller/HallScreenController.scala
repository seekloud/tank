package com.neo.sk.tank.controller

import akka.actor.Cancellable
import akka.actor.typed.ActorRef
import akka.http.scaladsl.Http
import akka.actor.typed.scaladsl.adapter._
import com.neo.sk.tank.common.Context
import com.neo.sk.tank.{App, model}
import com.neo.sk.tank.view.{GameHallListener, GameHallScreen, PlayGameScreen}
import javafx.geometry.Pos
import javafx.scene.Group
import javafx.scene.control._
import com.neo.sk.utils.HttpUtil.Imports.postJsonRequestSend
import com.neo.sk.utils.SecureUtil._

import scala.concurrent.duration._
import io.circe.parser.decode
import com.neo.sk.tank.App.{executor, materializer, scheduler, system, tokenActor}
import com.neo.sk.tank.actor.{PlayGameActor, TokenActor}
import io.circe.syntax._
import io.circe.generic.auto._
import org.slf4j.LoggerFactory
import com.neo.sk.tank.model.{GameServerInfo, PlayerInfo}
import scala.util.{Failure, Success}
/**
  * created by benyafang on 2018/10/26
  * 获取房间列表，指定房间进入和随机进入房间
  * 实例化完成即向服务器请求房间列表
  * */
class HallScreenController(val context:Context, val gameHall:GameHallScreen, gameServerInfo: GameServerInfo, playerInfo:PlayerInfo){
  private val log = LoggerFactory.getLogger(this.getClass)

  private var timer:Cancellable = _
  private def getRoomListInit() = {
    //需要起一个定时器，定时刷新请求
    val url = s"http://${gameServerInfo.domain}/tank/getRoomList"
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
            gameHall.updateRoomList(roomListRsp.data.roomList)
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
        new PlayScreenController(playerInfo,gameServerInfo,context,playGameScreen,None,None,false).start
        playGameScreen.setCursor
        close()
      }
    }

    override def confirmBtnListener(roomIdListView:String, roomIdTextField:String, roomIdExist:Boolean): Unit = {
      App.pushStack2AppThread{
        if(roomIdExist){
          val playGameScreen:PlayGameScreen = new PlayGameScreen(context)
          context.switchScene(playGameScreen.getScene(),resize = true,fullScreen = true)
          new PlayScreenController(playerInfo, gameServerInfo, context, playGameScreen, Some(roomIdTextField), None,false).start
          playGameScreen.setCursor
          close()
        }else if(roomIdListView != null){
          val playGameScreen:PlayGameScreen = new PlayGameScreen(context)
          context.switchScene(playGameScreen.getScene(),resize = true,fullScreen = true)
          new PlayScreenController(playerInfo, gameServerInfo, context, playGameScreen, Some(roomIdListView), None,false ).start
          playGameScreen.setCursor
          close()
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

    override def createRoom(roomId: Option[String], salt: Option[String]): Unit = {
      App.pushStack2AppThread{
        val playGameScreen:PlayGameScreen = new PlayGameScreen(context)
        context.switchScene(playGameScreen.getScene(),resize = true,fullScreen = true)
        new PlayScreenController(playerInfo, gameServerInfo, context, playGameScreen, roomId, salt).start
        playGameScreen.setCursor
        close()
      }
    }

    override def change2Encrypt(): Unit = {
      App.pushStack2AppThread(gameHall.encryptScreen)
    }

    override def change2Plain(): Unit = {
      App.pushStack2AppThread(gameHall.plainScreen)
    }
    
  })

  private def close() = {
    timer.cancel()
  }

}
