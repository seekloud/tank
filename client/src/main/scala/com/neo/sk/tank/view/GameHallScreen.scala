package com.neo.sk.tank.view

//import java.awt.TextField

import com.neo.sk.tank.common.Context
import com.neo.sk.tank.model
import com.neo.sk.tank.model.PlayerInfo
import javafx.collections.{FXCollections, ObservableArray, ObservableList}
import javafx.geometry.Insets
import javafx.scene.{Group, Scene}
import javafx.scene.control.{Button, Label, ListView, TextField}
import javafx.scene.layout.{BorderPane, GridPane, HBox, VBox}

/**
  * created by benyafang on 2018/10/26
  * */
class GameHallScreen(context:Context,playerInfo: PlayerInfo){
  private val group = new Group()
  private val scene = new Scene(group)
//  private val playerInfo = PlayerInfo("tank--1","aa","11")
  private val nicknameLabel = new Label(s"昵称：${playerInfo.nickName}")
  private val playerIdLabel = new Label(s"uid:${playerInfo.playerId}")
  private val vBox4Info = new VBox(10)//放个人信息的两个label

  private val confirmBtn = new Button("确定")
  private val randomBtn = new Button("随机进入")
  private val hBox4Btn = new HBox(10)//放两个button

  private var roomList:List[Long] = List.empty[Long]

  private val observableList:ObservableList[String] = FXCollections.observableArrayList()
  private var listView = new ListView[String](observableList)
//  private val roomIdLabel = new Label("请输入房间号")
  private val roomIdTextField = new TextField()
  roomIdTextField.setPromptText("请输入指定房间号")
//  roomIdTextField.textProperty().bind(listView.getSelectionModel.selectedItemProperty())
  private val vBox4Center = new VBox(10)

  private val grid = new GridPane()
  private val borderPane = new BorderPane()
//  borderPane.setMaxWidth(500)
//  borderPane.setMaxSize(1000,600)

  add()
  def add() = {
    confirmBtn.setPrefSize(100,20)
    randomBtn.setPrefSize(100,20)
    vBox4Info.getChildren.addAll(playerIdLabel,nicknameLabel)
    hBox4Btn.getChildren.addAll(confirmBtn,randomBtn)
    vBox4Info.setPadding(new Insets(15,12,15,12))
    hBox4Btn.setPadding(new Insets(10,10,10,10))
    vBox4Info.setSpacing(10)
    hBox4Btn.setSpacing(10)
    listView.setMaxWidth(200)
    listView.setMaxHeight(200)

    vBox4Center.getChildren.addAll(listView,roomIdTextField)


    borderPane.setBottom(hBox4Btn)
    borderPane.setTop(vBox4Info)
    borderPane.setCenter(vBox4Center)
//    borderPane.setRight(new Label(""))
    group.getChildren.addAll(borderPane)

  }

  def getScene() = this.scene
  private var listener:GameHallListener = _

  def setListener(gameHallListener:GameHallListener) = {
    this.listener = gameHallListener
  }

  def updateRoomList(roomList:List[Long]) = {
    this.roomList = roomList
    observableList.clear()
    roomList.map(_.toString) foreach observableList.add
  }
  confirmBtn.setOnAction(e => listener.confirmBtnListener(listView.getSelectionModel.selectedItemProperty().get(),roomIdTextField.getText(),group))
  randomBtn.setOnAction(e => listener.randomBtnListener())



}

abstract class GameHallListener{
  def confirmBtnListener(roomIdListView:String,roomIdTextField:String,root:Group)

  def randomBtnListener()
}
