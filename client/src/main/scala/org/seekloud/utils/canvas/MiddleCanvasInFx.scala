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

package org.seekloud.utils.canvas

import java.nio.ByteBuffer

import org.seekloud.tank.shared.util.canvas.MiddleCanvas
import javafx.scene.SnapshotParameters
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import javafx.scene.image.{Image, WritableImage}
import org.seekloud.tank.common.AppSettings

/**
  * Created by sky
  * Date on 2018/11/16
  * Time at 下午2:54
  */
object MiddleCanvasInFx {
  val emptyArray = new Array[Byte](0)

  def apply(width: Float, height: Float): MiddleCanvasInFx = new MiddleCanvasInFx(width, height)
}

class MiddleCanvasInFx private() extends MiddleCanvas {

  import MiddleCanvasInFx.emptyArray

  private[this] var canvas: Canvas = _

  def this(width: Float, height: Float) = {
    this()
    canvas = new Canvas(width, height)
    setWidth(width)
    setHeight(height)
  }

  def getCanvas = canvas

  override def getCtx = MiddleContextInFx(this)

  override def getWidth = canvas.getWidth

  override def getHeight = canvas.getHeight

  override def setWidth(h: Any) = h match {
    case d: Float => canvas.setWidth(d)
    case _ => canvas.setWidth(h.asInstanceOf[Int].toFloat)
  }

  override def setHeight(h: Any) = h match {
    case d: Float => canvas.setHeight(d)
    case _ => canvas.setHeight(h.asInstanceOf[Int].toFloat)
  }

  override def change2Image = {
    val params = new SnapshotParameters
    params.setFill(Color.TRANSPARENT)
    canvas.snapshot(params, null)
  }

  /**
    * copy from medusa
    * @author sky
    * */
  def canvas2byteArray: Array[Byte] = {
    try {
      val params = new SnapshotParameters
      val w = canvas.getWidth.toInt
      val h = canvas.getHeight.toInt
      val wi = new WritableImage(w, h)
      params.setFill(Color.TRANSPARENT)
      canvas.snapshot(params, wi) //从画布中复制绘图并复制到writableImage
      val reader = wi.getPixelReader
      if (!AppSettings.isGray) {
        val byteBuffer = ByteBuffer.allocate(4 * w * h)
        for (y <- 0 until h; x <- 0 until w) {
          val color = reader.getArgb(x, y)
          byteBuffer.putInt(color)
        }
        byteBuffer.flip()
        byteBuffer.array().take(byteBuffer.limit)
      } else {
        //获取灰度图，每个像素点1Byte
        val byteArray = new Array[Byte](1 * w * h)
        for (y <- 0 until h; x <- 0 until w) {
          val color = reader.getColor(x, y).grayscale()
          val gray = (color.getRed * 255).toByte
          byteArray(y * w + x) = gray
        }
        byteArray
      }
    } catch {
      case e: Exception =>
        emptyArray
    }
  }
}
