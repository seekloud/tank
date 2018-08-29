package com.neo.sk.tank.front.tankClient.draw

import com.neo.sk.tank.front.tankClient.GameContainerClientImpl
import com.neo.sk.tank.shared.model.Point

/**
  * Created by hongruying on 2018/8/29
  */
trait BulletDrawUtil { this:GameContainerClientImpl =>

  protected def drawBullet(offset:Point, offsetTime:Long) = {
    bulletMap.values.foreach{ bullet =>
      val p = bullet.getPosition4Animation(offsetTime) + offset
      val color = bullet.getBulletLevel() match {
        case 1 => "#CD6600"
        case 2 => "#FF4500"
        case 3 => "#8B2323"
      }
      ctx.fillStyle = color.toString()
      ctx.beginPath()
      ctx.arc(p.x * canvasUnit,p.y * canvasUnit, bullet.getRadius * canvasUnit,0, 360)
      ctx.fill()
      ctx.strokeStyle = "#474747"
      ctx.lineWidth = bullet.getRadius * canvasUnit / 5
      ctx.stroke()
      ctx.closePath()

    }
  }
}
