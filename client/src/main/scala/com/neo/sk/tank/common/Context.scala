package com.neo.sk.tank.common

import java.awt.event.{ActionEvent, ActionListener}

import javafx.event.EventHandler
import javafx.scene.input.{KeyCode, KeyEvent}
//import java.beans.EventHandler

import javafx.beans.Observable
//import java.util.Observable

import javafx.scene.Scene
import javafx.stage.Stage

/**
  * Created by hongruying on 2018/10/23
  */
class Context(stage: Stage) {

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
        if(event.getCode == KeyCode.Z && resize) stage.setFullScreen(fullScreen)
      }
    })

  }



}
