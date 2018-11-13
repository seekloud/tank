package com.neo.sk.tank.front.tankClient.draw

import com.neo.sk.tank.front.tankClient.game.GameContainerClientImpl
import com.neo.sk.tank.shared.`object`.Bullet
import com.neo.sk.tank.shared.model.Point
import org.scalajs.dom
import org.scalajs.dom.html

import scala.collection.mutable

/**
  * Created by hongruying on 2018/8/29
  */
trait BulletDrawUtil { this:GameContainerClientImpl =>

  private def generateCanvas(bullet:Bullet):html.Canvas = {
    val canvasCache = dom.document.createElement("canvas").asInstanceOf[html.Canvas]
    val ctxCache = canvasCache.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
    val radius = bullet.getRadius
    val color = bullet.getBulletLevel() match {
      case 1 => "#CD6600"
      case 2 => "#CD5555"
      case 4 => "#CD3278"
      case 3 => "#FF4500"
      case 5 => "#8B2323"
    }

    canvasCache.width = math.ceil(radius * canvasUnit * 2 + radius * canvasUnit / 5).toInt
    canvasCache.height = math.ceil(radius * canvasUnit * 2 + radius * canvasUnit / 5).toInt
    ctxCache.fillStyle = color.toString()
    ctxCache.beginPath()
    ctxCache.arc(radius * canvasUnit + radius * canvasUnit / 10,radius * canvasUnit + radius * canvasUnit / 10, radius * canvasUnit,0, 360)
    ctxCache.fill()
    ctxCache.strokeStyle = "#474747"
    ctxCache.lineWidth = radius * canvasUnit / 5
    ctxCache.stroke()
    ctx.closePath()
    canvasCache
  }

  private val canvasCacheMap = mutable.HashMap[Byte,html.Canvas]()

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
        //      val color = bullet.getBulletLevel() match {
        //        case 1 => "#CD6600"
        //        case 2 => "#FF4500"
        //        case 3 => "#8B2323"
        //      }
        //      ctx.fillStyle = color.toString()
        //      ctx.beginPath()
        //      ctx.arc(p.x * canvasUnit,p.y * canvasUnit, bullet.getRadius * canvasUnit,0, 360)
        //      ctx.fill()
        //      ctx.strokeStyle = "#474747"
        //      ctx.lineWidth = bullet.getRadius * canvasUnit / 5
        //      ctx.stroke()
        //      ctx.closePath()
      }
    }
  }
}
