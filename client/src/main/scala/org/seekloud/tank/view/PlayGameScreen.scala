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

package org.seekloud.tank.view

import javafx.scene.image.Image
import javafx.scene.{Group, ImageCursor, Scene}
import org.seekloud.tank.ClientApp
import org.seekloud.tank.common.Context
import org.seekloud.tank.shared.model.Point
import org.seekloud.utils.JavaFxUtil.getCanvasUnit
import org.seekloud.utils.canvas.MiddleFrameInFx

/**
  * Created by hongruying on 2018/10/23
  * 玩游戏的view
  *
  */
class PlayGameScreen(context: Context) {

  import javafx.stage.Screen

  val screen = Screen.getPrimary.getVisualBounds
  println(s"----width--${screen.getMaxX.toFloat}")
  println(s"----height--${screen.getMaxY.toFloat}")
  val group = new Group()
  val scene = new Scene(group)

  def setCursor = {
    val image = new Image(ClientApp.getClass.getResourceAsStream("/img/瞄准_2.png"))
    scene.setCursor(new ImageCursor(image, image.getWidth / 10, image.getHeight / 10))
  }

  setCursor

  def getScene(): Scene = scene

}
