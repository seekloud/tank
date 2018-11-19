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

  def returnSelf=canvas

  override def getCtx =  MiddleContextInFx(this)

  override def getWidth = canvas.getWidth

  override def getHeight = canvas.getHeight

  override def setWidth(h: Any) = canvas.setWidth(h.asInstanceOf[Double])

  override def setHeight(h: Any) = canvas.setHeight(h.asInstanceOf[Double])

  override def change2Image = {
    val params = new SnapshotParameters
    params.setFill(Color.TRANSPARENT)
    canvas.snapshot(params, null)
  }
}
