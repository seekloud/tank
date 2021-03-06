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

import org.seekloud.tank.shared.util.canvas.MiddleContext
import javafx.geometry.VPos
import javafx.scene.image.WritableImage
import javafx.scene.paint.Color
import javafx.scene.canvas.GraphicsContext
import javafx.scene.shape.{StrokeLineCap, StrokeLineJoin}
import javafx.scene.text.{Font, FontWeight, TextAlignment}
import scala.language.implicitConversions
/**
  * Created by sky
  * Date on 2018/11/19
  * Time at 下午12:53
  */
object MiddleContextInFx {
  def apply(canvas: MiddleCanvasInFx): MiddleContextInFx = new MiddleContextInFx(canvas)

  def string2FontWeight(s: String): FontWeight = {
    s match {
      case "bold" => FontWeight.BOLD
      case "normal" => FontWeight.NORMAL
      case "black" => FontWeight.BLACK
      case "extra_bold" => FontWeight.EXTRA_BOLD
      case "extra_light" => FontWeight.EXTRA_LIGHT
      case "light" => FontWeight.LIGHT
      case "medium" => FontWeight.MEDIUM
      case "semi_bold" => FontWeight.SEMI_BOLD
      case "thin" => FontWeight.THIN
      case _ => FontWeight.NORMAL
    }
  }

  implicit def string2TextAlignment(s: String): TextAlignment = {
    s match {
      case "center" => TextAlignment.CENTER
      case "left" => TextAlignment.LEFT
      case "right" => TextAlignment.RIGHT
      case "justify" => TextAlignment.JUSTIFY
      case _ => TextAlignment.CENTER
    }
  }

  implicit def string2TextBaseline(s: String): VPos = {
    s match {
      case "middle" => VPos.CENTER
      case "top" => VPos.TOP
      case "center" => VPos.CENTER
      case "bottom" => VPos.BOTTOM
      case _ => VPos.CENTER //设置默认值
    }
  }

  implicit def string2StrokeLineCap(s: String): StrokeLineCap = {
    s match {
      case "round" => StrokeLineCap.ROUND
      case "butt" => StrokeLineCap.BUTT
      case "square" => StrokeLineCap.SQUARE
      case _ => StrokeLineCap.ROUND //设置默认值
    }
  }

  implicit def string2StrokeLineJoin(s: String): StrokeLineJoin = {
    s match {
      case "round" => StrokeLineJoin.ROUND
      case "miter" => StrokeLineJoin.MITER
      case "revel" => StrokeLineJoin.BEVEL
      case _ => StrokeLineJoin.ROUND
    }
  }
}

class MiddleContextInFx extends MiddleContext {

  import MiddleContextInFx._

  private[this] var context: GraphicsContext = _

  def this(canvas: MiddleCanvasInFx) = {
    this()
    context = canvas.getCanvas.getGraphicsContext2D
  }

  def getContext = context

  override def setGlobalAlpha(alpha: Double): Unit = context.setGlobalAlpha(alpha)

  override def setLineWidth(h: Double) = context.setLineWidth(h)

  override def setStrokeStyle(color: String) = {
    context.setStroke(Color.web(color))
  }

  override def arc(x: Double, y: Double, radius: Double, startAngle: Double,
                   endAngle: Double) = context.arc(x, y, radius, radius, startAngle, endAngle)

  override def fill = context.fill()

  override def closePath = context.closePath()

  override def setFill(color: String) = context.setFill(Color.web(color))

  override def moveTo(x: Double, y: Double): Unit = context.moveTo(x, y)

  override def drawImage(image: Any, offsetX: Double, offsetY: Double, size: Option[(Double, Double)]): Unit = {
    image match {
      case js: MiddleImageInFx =>
        if (size.isEmpty) {
          context.drawImage(js.getImage, offsetX, offsetY)
        } else {
          context.drawImage(js.getImage, offsetX, offsetY, size.get._1, size.get._2)
        }
      case js: WritableImage =>
        if (size.isEmpty) {
          context.drawImage(js, offsetX, offsetY)
        } else {
          context.drawImage(js, offsetX, offsetY, size.get._1, size.get._2)
        }
    }
  }

  override def fillRec(x: Double, y: Double, w: Double, h: Double) = context.fillRect(x, y, w, h)

  override def clearRect(x: Double, y: Double, w: Double, h: Double) = context.clearRect(x, y, w, h)

  override def beginPath() = context.beginPath()

  override def lineTo(x1: Double, y1: Double) = context.lineTo(x1, y1)

  override def stroke() = context.stroke()

  override def fillText(text: String, x: Double, y: Double, z: Double = 500) = context.fillText(text, x, y)

  override def setFont(f: String, fw: String, s: Double) = context.setFont(Font.font(f, string2FontWeight(fw), s))

  override def setTextAlign(s: String) = context.setTextAlign(s)

  override def setTextBaseline(s: String) = context.setTextBaseline(s)

  override def setLineCap(s: String) = context.setLineCap(s)

  override def setLineJoin(s: String) = context.setLineJoin(s)

  override def rect(x: Double, y: Double, w: Double, h: Double) = context.rect(x, y, w, h)

  override def strokeText(text: String, x: Double, y: Double, maxWidth: Double) = context.strokeText(text, x, y, maxWidth)

  override def save(): Unit = context.save()

  override def restore(): Unit = context.restore()
}
