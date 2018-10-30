package com.neo.sk.utils

import java.awt.event.KeyEvent

import com.neo.sk.tank.shared.model.Constants
import javafx.scene.input.KeyCode

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
      case _ => KeyEvent.VK_F2
    }
  }
}
