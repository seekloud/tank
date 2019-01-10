package com.neo.sk.tank.view

//import java.awt.TextField

import com.neo.sk.tank.common.{Constants, Context}
import com.neo.sk.tank.model
import com.neo.sk.tank.model.PlayerInfo
import javafx.collections.{FXCollections, ObservableArray, ObservableList}
import javafx.geometry.{HPos, Insets, Pos, VPos}
import javafx.scene.{Group, Scene}
import javafx.scene.control._
import javafx.scene.image.{Image, ImageView}
import javafx.scene.layout._
import javafx.scene.paint.{Color, CycleMethod, LinearGradient, Stop}
import javafx.scene.shape.Rectangle
import javafx.scene.text.{Font, FontWeight, Text}

/**
  * created by benyafang on 2018/10/26
  * */
class GameHallScreen(context:Context,playerInfo: PlayerInfo){
  //房间列表
  private val group = new Group()
  private val scene = new Scene(group,Constants.SceneBound.weight,Constants.SceneBound.height)
  private val borderPane = new BorderPane()

  private val vBox4Info = new VBox(10)//放个人信息的两个label
  private val vBox4Btn = new VBox(10)//放两个button
  private val vBox4Center = new VBox(10)
  private val hBox = new HBox(10)
  private val hBox4Add = new HBox(5)

  private val nicknameLabel = new Label(s"昵称：${playerInfo.nickName}")
  private val playerIdLabel = new Label(s"玩家id：${playerInfo.playerId}")
  private val roomListLabel = new Label("房间列表")

  private val confirmBtn = new Button("确定")
  confirmBtn.setOnAction(e => listener.confirmBtnListener(listView.getSelectionModel.selectedItemProperty().get(), roomIdTextField.getText(), judgeTheRoomId(roomIdTextField.getText())))

  private val randomBtn = new Button("随机进入")
  randomBtn.setOnAction(e => listener.randomBtnListener())

  private var listener:GameHallListener = _
  private var roomList:List[Long] = List.empty[Long]

  private val observableList:ObservableList[String] = FXCollections.observableArrayList()
  private val listView = new ListView[String](observableList)

  private val roomIdTextField = new TextField()
  roomIdTextField.setPromptText("请输入指定房间号")

  private val addRoomBtn = new Button()
  addRoomBtn.setOnAction(e => listener.addSelfDefinedRoom())
  private val imageView = new ImageView(new Image(getClass.getResourceAsStream("/img/add.png")))
  imageView.setFitHeight(20)
  imageView.setFitWidth(20)
  addRoomBtn.setGraphic(imageView)

  add()

  //创建房间
  private val sceneTitle = new Label("房间ID")
  private val roomField = new TextField()

  private val pwdLabel = new Label("密码")
  private val pwBox = new PasswordField

  private val enLink = new Hyperlink()
  enLink.setText("带密码创建")
  enLink.setFont(Font.font("Cambria", FontWeight.NORMAL, 10))
  enLink.setOnAction(_ => listener.change2Encrypt())

  private val plainLink = new Hyperlink()
  plainLink.setText("不带密码创建")
  plainLink.setFont(Font.font("Cambria", FontWeight.NORMAL, 10))
  plainLink.setOnAction(_ => listener.change2Plain())

//  private val btnBack = new Button()
//  private val imageView4back = new ImageView(new Image(getClass.getResourceAsStream("/img/back_1.png")))
//  imageView4back.setFitHeight(20)
//  imageView4back.setFitWidth(20)
//  btnBack.setGraphic(imageView4back)
//  btnBack.setPrefSize(20,20)
//  btnBack.setStyle("-fx-background-color:transparent;")
//  btnBack.setOnAction(_ => listener.backToRoomList())

  private val btn4plain = new Button("开始游戏")
  btn4plain.setPrefSize(80,20)
  btn4plain.setOnAction(_ => listener.createPlain(roomField.getText))

  private val btn4Encry = new Button("开始游戏")
  btn4Encry.setPrefSize(80,20)
  btn4Encry.setOnAction(_ => listener.createEncrypt(roomField.getText, pwBox.getText))

  private val hBox4Plain = new HBox(10)
  hBox4Plain.setAlignment(Pos.BOTTOM_RIGHT)
  hBox4Plain.getChildren.addAll(enLink, btn4plain)

  private val hBox4Encry = new HBox(10)
  hBox4Encry.setAlignment(Pos.BOTTOM_RIGHT)
  hBox4Encry.getChildren.addAll(plainLink, btn4Encry)

//  private val hBox4Back = new HBox()
//  hBox4Back.setAlignment(Pos.BOTTOM_RIGHT)
//  hBox4Back.getChildren.add(btnBack)


  def setFont() = {
    playerIdLabel.setTextFill(Color.WHITE)
    nicknameLabel.setTextFill(Color.WHITE)
    playerIdLabel.setFont(Font.font("",FontWeight.BOLD,14))
    nicknameLabel.setFont(Font.font("",FontWeight.BOLD,14))
    roomListLabel.setFont(Font.font("",FontWeight.BOLD,14))
    roomListLabel.setTextFill(Color.rgb(33,66,99))
    roomListLabel.setPadding(new Insets(6,0,0,0))

    confirmBtn.setPrefSize(100,20)
    randomBtn.setPrefSize(100,20)

    vBox4Info.setPadding(new Insets(15,12,15,12))
    vBox4Btn.setPadding(new Insets(150,20,15,20))

    vBox4Info.setSpacing(10)
    vBox4Info.setStyle("-fx-background-color:#336699;")

    vBox4Btn.setSpacing(10)
    vBox4Center.setMaxWidth(300)
    vBox4Center.setMaxHeight(500)
    vBox4Center.setPrefWidth(200)
    vBox4Center.setPrefHeight(200)

    vBox4Center.setPadding(new Insets(30,30,30,30))
    VBox.setMargin(hBox4Add,new Insets(0,0,5,8))
    VBox.setMargin(listView,new Insets(0,0,5,8))
    VBox.setMargin(roomIdTextField,new Insets(0,0,5,8))
//    hBox.setPadding(new Insets(20,20,20,100))
    hBox.setAlignment(Pos.CENTER)
    HBox.setHgrow(vBox4Center,Priority.ALWAYS)

    borderPane.prefHeightProperty().bind(scene.heightProperty())
    borderPane.prefWidthProperty().bind(scene.widthProperty())

    addRoomBtn.setPrefSize(20,20)
    addRoomBtn.setStyle("-fx-background-color:transparent;")

  }

  def add():Unit = {
    setFont()
    hBox4Add.getChildren.addAll(roomListLabel,addRoomBtn)
    vBox4Info.getChildren.addAll(playerIdLabel,nicknameLabel)
    vBox4Btn.getChildren.addAll(confirmBtn,randomBtn)
    vBox4Center.getChildren.addAll(hBox4Add,listView,roomIdTextField)
    hBox.getChildren.addAll(vBox4Center,vBox4Btn)
    borderPane.setTop(vBox4Info)
    borderPane.setCenter(hBox)
    group.getChildren.addAll(borderPane)
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

  def judgeTheRoomId(id:String) = {
    if(id != "" && roomList.contains(id.toLong)) true else false
  }

  def plainScreen = {
    val grid = new GridPane()
    grid.setAlignment(Pos.CENTER)
    grid.setHgap(10)
    grid.setVgap(10)
    grid.setPadding(new Insets(30, 30, 30, 30))
    grid.add(sceneTitle, 0, 0)
    grid.add(roomField, 1, 0)
    grid.add(hBox4Plain, 1, 1)
    val scene = new Scene(grid,Constants.SceneBound.weight,Constants.SceneBound.height)
    context.switchScene(scene)
  }

  def encryptScreen = {
    val grid = new GridPane()
    grid.setAlignment(Pos.CENTER)
    grid.setHgap(10)
    grid.setVgap(10)
    grid.setPadding(new Insets(30, 30, 30, 30))
    grid.add(sceneTitle, 0, 0)
    grid.add(roomField, 1, 0)
    grid.add(pwdLabel, 0, 1)
    grid.add(pwBox, 1, 1)
    grid.add(hBox4Encry, 1, 2)
    val scene = new Scene(grid,Constants.SceneBound.weight,Constants.SceneBound.height)
    context.switchScene(scene)
  }

}

abstract class GameHallListener{
  def confirmBtnListener(roomIdListView:String, roomIdTextFiled:String, roomIdExist:Boolean)
  def randomBtnListener()
  def addSelfDefinedRoom()
  def createEncrypt(roomId:String,salt:String)
  def createPlain(roomId:String)
  def change2Encrypt()
  def change2Plain()
}
