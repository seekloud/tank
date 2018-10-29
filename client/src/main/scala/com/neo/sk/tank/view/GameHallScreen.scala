package com.neo.sk.tank.view

import com.neo.sk.tank.common.Context
import com.neo.sk.tank.model
import com.neo.sk.tank.model.PlayerInfo
import javafx.collections.{FXCollections, ObservableArray, ObservableList}
import javafx.scene.{Group, Scene}
import javafx.scene.control.{Button, Label, ListView}
import javafx.scene.layout.GridPane

/**
  * created by benyafang on 2018/10/26
  * */
class GameHallScreen(context:Context,playerInfo: PlayerInfo){
  private val group = new Group()
  private val scene = new Scene(group)
//  private val playerInfo = PlayerInfo("tank--1","aa","11")
  private val nicknameLabel = new Label(s"昵称：${playerInfo.nickName}")
  private val playerIdLabel = new Label(s"uid:${playerInfo.playerId}")

  private val confirmBtn = new Button("确定")
  private val randomBtn = new Button("随机进入")

  private var roomList:List[Long] = List.empty[Long]
  def getRoomListView(rsp:model.RoomListRsp) = {
    rsp.data.roomList
  }

  private val observableList:ObservableList[String] = FXCollections.observableArrayList("")
  roomList.map(_.toString) foreach observableList.add
  private val listView = new ListView[String](observableList)

  private val grid = new GridPane()

  add()
  def add() = {
    grid.add(nicknameLabel,0,0,0,0)
    grid.add(playerIdLabel,1,1,1,1)
    grid.add(listView,2,2,2,2)
    grid.add(confirmBtn,3,3,3,3)
    grid.add(randomBtn,4,4,4,4)
    group.getChildren.add(grid)
  }

  def getScene() = this.scene
  private var listener:GameHallListener = _
  private val gameServerInfo = model.GameServerInfo("localhost","30369","tank/game")

  confirmBtn.setOnAction(e => listener.confirmBtnListener(playerInfo,listView,gameServerInfo,group))
  randomBtn.setOnAction(e => listener.randomBtnListener(playerInfo,gameServerInfo))
  def setListener(gameHallListener:GameHallListener) = {
    this.listener = gameHallListener
  }

  def updateRoomList(roomList:List[Long]) = {
    this.roomList = roomList
  }


}

abstract class GameHallListener{
  def confirmBtnListener(playerInfo:PlayerInfo,select:ListView[String],gameServerInfo:model.GameServerInfo,root:Group)

  def randomBtnListener(playerInfo: PlayerInfo,gameServerInfo:model.GameServerInfo)
}
