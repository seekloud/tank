package org.seekloud.tank.common

import javafx.event.EventHandler
import javafx.scene.Scene
import javafx.scene.input.{KeyCode, KeyEvent}
import javafx.stage.Stage

/**
  * Created by hongruying on 2018/10/23
  */
class Context(stage: Stage) {

  def getStageWidth = stage.getWidth
  def getStageHeight = stage.getHeight
  def isFullScreen = stage.isFullScreen

  def switchScene(scene: Scene, title:String = "Tank Game",resize:Boolean = false,fullScreen:Boolean = false) = {
    stage.centerOnScreen()
    stage.setScene(scene)
    stage.sizeToScene()
    stage.setResizable(resize)
    stage.setTitle(title)
    stage.setFullScreen(fullScreen)
    stage.show()
    scene.setOnKeyPressed(new EventHandler[KeyEvent] {
      override def handle(event: KeyEvent): Unit = {
        if(event.getCode == KeyCode.Z) stage.setFullScreen(fullScreen)
//        else  false
      }
    })
  }



}
