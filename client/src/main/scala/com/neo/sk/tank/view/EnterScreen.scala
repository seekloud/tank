package com.neo.sk.tank.view

import com.neo.sk.tank.common.{Constants, Context}
import javafx.geometry.{Insets, Pos}
import javafx.scene.{Group, Scene}
import javafx.scene.control.{Button, Label, ListView, TextField}
import javafx.scene.layout.{BorderPane, HBox, Priority, VBox}
import javafx.scene.paint.Color
import javafx.scene.text.{Font, FontWeight}

abstract class EnterSceneListener {
    def onBtnForMan()
    def onBtnForBot()
}


class EnterScreen(context:Context){
  private var enterSceneListener: EnterSceneListener = _
  private val group = new Group()
  private val scene = new Scene(group,Constants.SceneBound.weight,Constants.SceneBound.height)

  private val borderPane = new BorderPane()
  borderPane.prefHeightProperty().bind(scene.heightProperty())
  borderPane.prefWidthProperty().bind(scene.widthProperty())

  private val hBox = new HBox(10)
  hBox.setAlignment(Pos.CENTER)

  private val buttonForMan = new Button()
  private val buttonForBot = new Button()
  buttonForMan.setText("用户登录")
  buttonForBot.setText("Bot登录")
  buttonForMan.setOnAction(_ => enterSceneListener.onBtnForMan())
  buttonForBot.setOnAction(_ => enterSceneListener.onBtnForBot())

  hBox.getChildren.addAll(buttonForMan, buttonForBot)
  borderPane.setCenter(hBox)
  group.getChildren.add(borderPane)

  def getScene:Scene = this.scene

  def setListener(listener:EnterSceneListener):Unit = {
    enterSceneListener = listener
  }

}

