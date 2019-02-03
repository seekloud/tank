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

package org.seekloud.utils

import java.awt.event.KeyEvent

import javafx.scene.input.KeyCode
import org.seekloud.tank.shared.model.Constants

/**
  * User: sky
  * Date: 2018/10/30
  * Time: 10:24
  * 本文件内针对Javafx的一些通用操作
  */
object JavaFxUtil {
  def getCanvasUnit(canvasWidth:Float):Int = (canvasWidth / Constants.WindowView.x).toInt

  def changeKeys(k: KeyCode) = k match {
    case KeyCode.W => KeyCode.UP
    case KeyCode.S => KeyCode.DOWN
    case KeyCode.A => KeyCode.LEFT
    case KeyCode.D => KeyCode.RIGHT
    case origin => origin
  }

  def keyCode2Int(c: KeyCode) = {
    c match {
      case KeyCode.SPACE => KeyEvent.VK_SPACE
      case KeyCode.LEFT => KeyEvent.VK_LEFT
      case KeyCode.UP => KeyEvent.VK_UP
      case KeyCode.RIGHT => KeyEvent.VK_RIGHT
      case KeyCode.DOWN => KeyEvent.VK_DOWN
      case KeyCode.K => KeyEvent.VK_K
      case KeyCode.L => KeyEvent.VK_L
      case KeyCode.E => KeyEvent.VK_E
      case _ => KeyEvent.VK_F2
    }
  }
}
