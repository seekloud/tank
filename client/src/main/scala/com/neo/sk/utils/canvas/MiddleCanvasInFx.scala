package com.neo.sk.utils.canvas

import com.neo.sk.tank.shared.util.canvas.MiddleCanvas
import javafx.scene.SnapshotParameters
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import javafx.scene.image.Image

/**
  * Created by sky
  * Date on 2018/11/16
  * Time at 下午2:54
  */
object MiddleCanvasInFx {
  def apply(width: Double, height: Double): MiddleCanvasInFx = new MiddleCanvasInFx(width, height)
}

class MiddleCanvasInFx private() extends MiddleCanvas {

  private[this] var canvas: Canvas = _

  def this(width: Double, height: Double) = {
    this()
    canvas = new Canvas(width, height)
  }

  private def getCtx = canvas.getGraphicsContext2D

  override def getWidth = canvas.getWidth

  override def getHeight = canvas.getHeight

  override def setWidth(h: Int) = canvas.setWidth(h)

  override def setHeight(h: Int) = canvas.setHeight(h)

  override def setGlobalAlpha(alpha: Double): Unit = getCtx.setGlobalAlpha(alpha)

  override def setLineWidth(h: Int) = getCtx.setLineWidth(h)

  override def setStrokeStyle(s: String) = getCtx.setStroke(Color.web(s))

  override def arc(x: Double, y: Double, radius: Double, startAngle: Double,
                   endAngle: Double) = getCtx.arc(x, y, radius, radius, startAngle, endAngle)

  override def fill = getCtx.fill()

  override def closePath = getCtx.closePath()

  override def setFill(color: String) = getCtx.setFill(Color.web(color))

  override def change2Image = {
    val params = new SnapshotParameters
    params.setFill(Color.TRANSPARENT)
    canvas.snapshot(params, null)
  }

  override def drawImage(image: Any, offsetX: Double, offsetY: Double) = getCtx.drawImage(image.asInstanceOf[Image], offsetX, offsetY)

  override def moveTo(x: Int, y: Int): Unit = getCtx.moveTo(x, y)
}
