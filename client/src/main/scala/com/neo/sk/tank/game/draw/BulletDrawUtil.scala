package com.neo.sk.tank.game.draw

import com.neo.sk.tank.game.GameContainerClientImpl
import com.neo.sk.tank.shared.`object`.Bullet
import com.neo.sk.tank.shared.model.Point
import javafx.scene.canvas.Canvas
import javafx.scene.paint.Color
import javafx.scene.SnapshotParameters
import javafx.scene.image.Image

import scala.collection.mutable

/**
  * Created by hongruying on 2018/8/29
  */
trait BulletDrawUtil { this:GameContainerClientImpl =>
  private def generateCanvas(bullet:Bullet):Image = {
    val radius = bullet.getRadius
    val canvasCache = new Canvas(math.ceil(radius * canvasUnit * 2 + radius * canvasUnit / 5).toInt, math.ceil(radius * canvasUnit * 2 + radius * canvasUnit / 5).toInt)
    val ctxCache = canvasCache.getGraphicsContext2D
    val color = bullet.getBulletLevel() match {
      case 1 => "#CD6600"
      case 2 => "#CD5555"
      case 4 => "#CD3278"
      case 3 => "#FF4500"
      case 5 => "#8B2323"
    }
    ctxCache.setFill(Color.web(color))
    ctxCache.beginPath()
    val centerX = radius * canvasUnit + radius * canvasUnit / 10
    val centerY = radius * canvasUnit + radius * canvasUnit / 10
    val radiusX =  radius * canvasUnit
    val radiusY =  radius * canvasUnit
    val startAngle = 0
    val lengthAngle = 360
    ctxCache.arc(centerX.toFloat, centerY.toFloat, radiusX.toFloat, radiusY.toFloat, startAngle.toFloat, lengthAngle.toFloat)
    ctxCache.fill()
    ctxCache.setStroke(Color.web("#474747"))
    ctxCache.setLineWidth(radius * canvasUnit / 5)
    ctxCache.stroke()
    ctx.closePath()
    val params = new SnapshotParameters
    params.setFill(Color.TRANSPARENT)
    canvasCache.snapshot(params, null)
  }

  private val canvasCacheMap = mutable.HashMap[Byte, Image]()

  def updateBulletSize(canvasSize:Point)={
    canvasCacheMap.clear()
  }

  protected def drawBullet(offset:Point, offsetTime:Long, view:Point) = {
    bulletMap.values.foreach{ bullet =>
      val p = bullet.getPosition4Animation(offsetTime) + offset
      if(p.in(view,Point(bullet.getRadius * 4 ,bullet.getRadius *4))) {
        val cacheCanvas = canvasCacheMap.getOrElseUpdate(bullet.getBulletLevel(), generateCanvas(bullet))
        val radius = bullet.getRadius
        ctx.drawImage(cacheCanvas, (p.x - bullet.getRadius) * canvasUnit - radius * canvasUnit / 2.5, (p.y - bullet.getRadius) * canvasUnit - radius * canvasUnit / 2.5)
      }
    }
  }
}
