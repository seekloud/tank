package com.neo.sk.tank.controller

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
import io.circe.Json

import scala.concurrent.duration._
import io.circe.parser.decode
import com.neo.sk.tank.App.{executor, materializer, scheduler, system}
import com.neo.sk.tank.actor.PlayGameActor
import io.circe.syntax._
import io.circe.generic.auto._
import org.slf4j.LoggerFactory
import com.neo.sk.tank.actor.PlayGameActor.ConnectGame
import com.neo.sk.tank.model.{GameServerInfo, PlayerInfo}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}
/**
  * created by benyafang on 2018/10/26
  * 获取房间列表，指定房间进入和随机进入房间
  * 实例化完成即向服务器请求房间列表
  * */
class HallScreenController(val context:Context, val gameHall:GameHallScreen, gameServerInfo: GameServerInfo, playerInfo:PlayerInfo){
  private val log = LoggerFactory.getLogger(this.getClass)
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
            log.debug(s"444")
            Left("Error")

        }
      case Left(error) =>
        log.debug(s"555")
        Left("Error")
    }
  }

  App.pushStack2AppThread{
    scheduler.schedule(1.millis,1.minutes){
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
        context.switchScene(playGameScreen.getScene())
        new PlayScreenController(playerInfo,gameServerInfo,context,playGameScreen).start
        close()
      }

    }

    override def confirmBtnListener(roomIdListView: String, roomIdTextField:String): Unit = {
      App.pushStack2AppThread{
        println(roomIdListView)
        if(roomIdListView != null || roomIdTextField != ""){
          val roomId = roomIdTextField match{
            case "" => roomIdListView
            case _ => roomIdTextField
          }
          val playGameScreen:PlayGameScreen = new PlayGameScreen(context)
          context.switchScene(playGameScreen.getScene())
          new PlayScreenController(playerInfo, gameServerInfo, context, playGameScreen, Some(roomId)).start
          close()
        }else{
          val warn = new Alert(Alert.AlertType.WARNING,"还没有选择房间哦",new ButtonType("确定",ButtonBar.ButtonData.YES))
          warn.setTitle("警示")
          val buttonType = warn.showAndWait()
          if(buttonType.get().getButtonData.equals(ButtonBar.ButtonData.YES))warn.close()
        }
      }



    }

  })

  private def close() = {


  }

}
