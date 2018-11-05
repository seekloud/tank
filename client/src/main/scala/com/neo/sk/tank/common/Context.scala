package com.neo.sk.tank.common

import javafx.scene.Scene
import javafx.stage.Stage

/**
  * Created by hongruying on 2018/10/23
  */
class Context(stage: Stage) {

  def switchScene(scene: Scene, title:String = "Tank Game",resize:Boolean = false) = {
    stage.setScene(scene)
    stage.sizeToScene()
    stage.setResizable(resize)
    stage.setTitle(title)
    println(scene.getWidth)

//    stage.setMaximized(true)
//    stage.setFullScreen(true)
    stage.show()
  }



}
