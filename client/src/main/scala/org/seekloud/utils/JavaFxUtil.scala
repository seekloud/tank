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

  def getMoveStateByKeySet(actionSet:Set[KeyCode]):Byte = {
    if(actionSet.contains(KeyCode.LEFT) && actionSet.contains(KeyCode.UP)){
      5
    }else if(actionSet.contains(KeyCode.RIGHT) && actionSet.contains(KeyCode.UP)){
      7
    }else if(actionSet.contains(KeyCode.LEFT) && actionSet.contains(KeyCode.DOWN)){
      3
    }else if(actionSet.contains(KeyCode.RIGHT) && actionSet.contains(KeyCode.DOWN)){
      1
    }else if(actionSet.contains(KeyCode.RIGHT)){
      0
    }else if(actionSet.contains(KeyCode.LEFT)){
      4
    }else if(actionSet.contains(KeyCode.UP) ){
      6
    }else if(actionSet.contains(KeyCode.DOWN)){
      2
    }else 8
  }

}
