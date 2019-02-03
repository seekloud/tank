/*
 * Copyright 2018 seekloud (https://github.com/seekloud)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
