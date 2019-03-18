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

package org.seekloud.tank.shared.game.view

import org.seekloud.tank.shared.`object`.Bullet
import org.seekloud.tank.shared.game.GameContainerClientImpl
import org.seekloud.tank.shared.model.Constants.TankColor
import org.seekloud.tank.shared.model.Point

import scala.collection.mutable

/**
  * Created by hongruying on 2018/8/29
  */
trait BulletDrawUtil { this:GameContainerClientImpl =>

  protected def generateCanvas(bullet:Bullet,id:Option[Int]) = {
    val radius = bullet.getRadius
    val unit=if(id.isEmpty) canvasUnit else layerCanvasUnit
    val canvasCache = drawFrame.createCanvas(math.ceil(radius * unit * 2 + radius * unit / 5).toInt,math.ceil(radius * unit * 2 + radius * unit / 5).toInt)
    val ctxCache = canvasCache.getCtx
    val color = if(id.isEmpty) bullet.getBulletLevel() match {
      case 1 => "#CD6600"
      case 2 => "#FF4500"
      case 3 => "#8B2323"
    } else{
      if(bullet.tankId==myTankId) TankColor.green else s"rgba(${bullet.tankId%255}, ${bullet.tankId%255}, ${bullet.tankId%255}, 1)"
    }
    ctxCache.setFill(color)
    ctxCache.beginPath()
    ctxCache.arc(radius * unit + radius * unit / 10,radius * unit + radius * unit / 10, radius * unit,0, 360)
    ctxCache.fill()
    ctxCache.setStrokeStyle("#474747")
    ctxCache.setLineWidth(radius * unit / 5)
    ctxCache.stroke()
    canvasCache.change2Image()
  }

  protected val canvasCacheMap = mutable.HashMap[(Byte,Option[Int]),Any]()

  def updateBulletSize(canvasSize:Point)={
    canvasCacheMap.clear()
  }

  protected def drawBullet(offset:Point, offsetTime:Long, view:Point) = {
    bulletMap.values.foreach{ bullet =>
      val p = bullet.getPosition4Animation(offsetTime) + offset
      if(p.in(view,Point(bullet.getRadius * 4 ,bullet.getRadius *4))) {
        val cacheCanvas = canvasCacheMap.getOrElseUpdate((bullet.getBulletLevel(),None), generateCanvas(bullet,None))
        val radius = bullet.getRadius
        viewCtx.drawImage(cacheCanvas, (p.x - bullet.getRadius) * canvasUnit - radius * canvasUnit / 2.5, (p.y - bullet.getRadius) * canvasUnit - radius * canvasUnit / 2.5)
      }
    }
  }
}
