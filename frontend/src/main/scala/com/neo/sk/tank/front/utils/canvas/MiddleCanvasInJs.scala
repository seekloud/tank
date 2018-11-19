package com.neo.sk.tank.front.utils.canvas

import com.neo.sk.tank.shared.util.canvas.MiddleCanvas
import org.scalajs.dom.html.Canvas
import org.scalajs.dom
import org.scalajs.dom.html

/**
  * Created by sky
  * Date on 2018/11/16
  * Time at 下午3:18
  */
object MiddleCanvasInJs {
  def apply(width: Double, height: Double): MiddleCanvasInJs = new MiddleCanvasInJs(width, height)
}

class MiddleCanvasInJs private() extends MiddleCanvas {
  private[this] var canvas: Canvas = _

  def this(width: Double, height: Double) = {
    this()
    canvas = dom.document.createElement("canvas").asInstanceOf[html.Canvas]
//    setWidth(width)
//    setHeight(height)
  }

  private def getCtx = canvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]

  override def getWidth = canvas.width

  override def getHeight = canvas.height

  override def setWidth(h: Int) = canvas.width = h

  override def setHeight(h: Int) = canvas.height = h

  override def setGlobalAlpha(alpha: Double): Unit = getCtx.globalAlpha = alpha

  override def setLineWidth(h: Double) = getCtx.lineWidth = h

  override def setStrokeStyle(s: String) = getCtx.strokeStyle = s

  override def arc(x: Double, y: Double, radius: Double, startAngle: Double,
                   endAngle: Double) = getCtx.arc(x, y, radius, startAngle, endAngle)

  override def fill = getCtx.fill()

  override def closePath = getCtx.closePath()

  override def setFill(color: String) = getCtx.fillStyle = color

  override def moveTo(x: Double, y: Double): Unit = getCtx.moveTo(x, y)

  override def change2Image = canvas

  override def drawImage(image: Any, offsetX: Double, offsetY: Double): Unit = getCtx.drawImage(image.asInstanceOf[Canvas],offsetX,offsetY)
}