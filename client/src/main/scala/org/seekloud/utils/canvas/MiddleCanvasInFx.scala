package org.seekloud.utils.canvas

import org.seekloud.tank.shared.util.canvas.MiddleCanvas
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
  def apply(width: Float, height: Float): MiddleCanvasInFx = new MiddleCanvasInFx(width, height)
}

class MiddleCanvasInFx private() extends MiddleCanvas {

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
}
