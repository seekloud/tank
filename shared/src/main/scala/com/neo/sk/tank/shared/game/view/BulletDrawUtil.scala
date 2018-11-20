package com.neo.sk.tank.shared.game.view

import com.neo.sk.tank.shared.`object`.Bullet
import com.neo.sk.tank.shared.game.GameContainerImpl
import com.neo.sk.tank.shared.model.Point

import scala.collection.mutable

/**
  * Created by hongruying on 2018/8/29
  */
trait BulletDrawUtil { this:GameContainerImpl =>

  private def generateCanvas(bullet:Bullet) = {
    val radius = bullet.getRadius
    val canvasCache = drawFrame.createCanvas(math.ceil(radius * canvasUnit * 2 + radius * canvasUnit / 5).toInt,math.ceil(radius * canvasUnit * 2 + radius * canvasUnit / 5).toInt)
    val ctxCache = canvasCache.getCtx

    val color = bullet.getBulletLevel() match {
      case 1 => "#CD6600"
      case 2 => "#CD5555"
      case 4 => "#CD3278"
      case 3 => "#FF4500"
      case 5 => "#8B2323"
    }
    ctxCache.setFill(color)
    ctxCache.beginPath()
    ctxCache.arc(radius * canvasUnit + radius * canvasUnit / 10,radius * canvasUnit + radius * canvasUnit / 10, radius * canvasUnit,0, 360)
    ctxCache.fill()
    ctxCache.setStrokeStyle("#474747")
    ctxCache.setLineWidth(radius * canvasUnit / 5)
    ctxCache.stroke()
    ctx.closePath()
    canvasCache.change2Image()
  }

  private val canvasCacheMap = mutable.HashMap[Byte,Any]()

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
