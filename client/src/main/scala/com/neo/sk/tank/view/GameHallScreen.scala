package com.neo.sk.tank.view

//import java.awt.TextField

import com.neo.sk.tank.common.Context
import com.neo.sk.tank.model
import com.neo.sk.tank.model.PlayerInfo
import javafx.collections.{FXCollections, ObservableArray, ObservableList}
import javafx.geometry.{Insets, Pos}
import javafx.scene.{Group, Scene}
import javafx.scene.control._
import javafx.scene.layout._
import javafx.scene.paint.{Color, CycleMethod, LinearGradient, Stop}
import javafx.scene.shape.Rectangle
import javafx.scene.text.{Font, FontWeight, Text}

/**
  * created by benyafang on 2018/10/26
  * */
class GameHallScreen(context:Context,playerInfo: PlayerInfo){
  private val group = new Group()
  private val scene = new Scene(group)
  private val borderPane = new BorderPane()

  private val vBox4Info = new VBox(10)//放个人信息的两个label
  private val vBox4Btn = new VBox(10)//放两个button
  private val vBox4Center = new VBox(10)

  private val nicknameLabel = new Label(s"昵称：${playerInfo.nickName}")
  private val playerIdLabel = new Label(s"玩家id：${playerInfo.playerId}")
  private val roomListLabel = new Label("房间列表")

  private val confirmBtn = new Button("确定")
  private val randomBtn = new Button("随机进入")

  private var listener:GameHallListener = _
  private var roomList:List[Long] = List.empty[Long]

  private val observableList:ObservableList[String] = FXCollections.observableArrayList()
  private val listView = new ListView[String](observableList)

  private val roomIdTextField = new TextField()
  roomIdTextField.setPromptText("请输入指定房间号")


  add()

  def setFont() = {
    playerIdLabel.setTextFill(Color.WHITE)
    nicknameLabel.setTextFill(Color.WHITE)
    playerIdLabel.setFont(Font.font("",FontWeight.BOLD,14))
    nicknameLabel.setFont(Font.font("",FontWeight.BOLD,14))
    roomListLabel.setFont(Font.font("",FontWeight.BOLD,14))
    roomListLabel.setTextFill(Color.rgb(33,66,99))

    confirmBtn.setPrefSize(100,20)
    randomBtn.setPrefSize(100,20)
    vBox4Info.setPadding(new Insets(15,12,15,40))
    vBox4Btn.setPadding(new Insets(120,20,15,20))
    vBox4Info.setSpacing(10)
    vBox4Info.setStyle("-fx-background-color:#336699;")

    vBox4Btn.setSpacing(10)
    listView.setMaxWidth(200)
    listView.setPrefWidth(150)
    listView.setPrefHeight(200)

    vBox4Center.setPadding(new Insets(30,30,30,30))
    VBox.setMargin(roomListLabel,new Insets(0,0,5,8))
    VBox.setMargin(listView,new Insets(0,0,5,8))
    VBox.setMargin(roomIdTextField,new Insets(0,0,5,8))
  }

  def add():Unit = {
    setFont()
    vBox4Info.getChildren.addAll(playerIdLabel,nicknameLabel)
    vBox4Btn.getChildren.addAll(confirmBtn,randomBtn)
    vBox4Center.getChildren.addAll(roomListLabel,listView,roomIdTextField)
    borderPane.setRight(vBox4Btn)
    borderPane.setTop(vBox4Info)
    borderPane.setCenter(vBox4Center)
    group.getChildren.addAll(borderPane)
    setListenerFunc()
  }

  def getScene:Scene = this.scene

  def setListener(gameHallListener:GameHallListener):Unit = {
    this.listener = gameHallListener
  }

  def updateRoomList(roomList:List[Long]):Unit = {
    this.roomList = roomList
    observableList.clear()
    roomList.sortBy(t => t).map(_.toString) foreach observableList.add
  }

  private def setListenerFunc():Unit = {
    confirmBtn.setOnAction(e => listener.confirmBtnListener(listView.getSelectionModel.selectedItemProperty().get(),roomIdTextField.getText()))
    randomBtn.setOnAction(e => listener.randomBtnListener())
  }

}

abstract class GameHallListener{
  def confirmBtnListener(roomIdListView:String,roomIdTextField:String)

  def randomBtnListener()
}
