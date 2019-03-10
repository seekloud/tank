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
import org.seekloud.tank.App
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
    println(App.getClass)
    val image = new Image(App.getClass.getResourceAsStream("/img/瞄准_2.png"))
    scene.setCursor(new ImageCursor(image, image.getWidth / 10, image.getHeight / 10))
  }

  setCursor

  def getScene(): Scene = scene

  //todo 考虑本部分代码移到shared
  /*def drawCombatGains(killNum: Int, damageNum: Int, killerList: List[String]): Unit = {
    getCanvasContext.setFont("楷体", "normal", 5 * canvasUnit)
    getCanvasContext.setFill("rgb(0,0,0)")
    getCanvasContext.fillRec(0, 0, canvasBoundary.x * canvasUnit, canvasBoundary.y * canvasUnit)
    val img = drawFrame.createImage("/img/dead.png")
    getCanvasContext.drawImage(img, 450, 250, Some(370, 2700))
    getCanvasContext.setTextAlign("left")
    getCanvasContext.setFill("#FFFFFF")
    getCanvasContext.fillText(s"击杀者：", 500, 300)
    getCanvasContext.fillText(s"伤害量：", 500, 350)
    getCanvasContext.fillText(s"击杀者ID：", 500, 400)
    getCanvasContext.setFill("rgb(255,0,0)")
    getCanvasContext.fillText(s"${killNum}", 650, 300)
    getCanvasContext.fillText(s"${damageNum}", 650, 350)
    var pos = 700
    killerList.foreach { r =>
      getCanvasContext.fillText(s"${r}", pos, 400)
      pos = pos + 4 * canvasUnit * r.length
    }
  }*/

}
