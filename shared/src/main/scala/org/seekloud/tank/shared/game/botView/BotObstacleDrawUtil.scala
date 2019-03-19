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

package org.seekloud.tank.shared.game.botView

import org.seekloud.tank.shared.game.GameContainerClientImpl
import org.seekloud.tank.shared.game.view.ObstacleDrawUtil
import org.seekloud.tank.shared.model.Constants.ObstacleType
import org.seekloud.tank.shared.model.Point
import org.seekloud.tank.shared.util.canvas.MiddleContext

import scala.collection.mutable

/**
  * Created by sky
  * Date on 2019/3/18
  * Time at 上午11:40
  */
trait BotObstacleDrawUtil extends ObstacleDrawUtil{this:GameContainerClientImpl=>

  private val obstacleCanvasCacheMap = mutable.HashMap[(Byte, Boolean), Any]()
  private val steelImg =drawFrame.createImage("/img/钢铁.png")
  private val riverImg =drawFrame.createImage("/img/river.png")

  protected def drawObstacles4Bot(offset:Point,view:Point) = {
    drawObstacles(offset,view,mutableCtx,layerCanvasUnit)
  }

  def drawEnvironment4Bot(offset:Point,view:Point) :Unit={

    environmentMap.values.foreach { obstacle =>
      val img = obstacle.obstacleType match {
        case ObstacleType.steel => steelImg
        case ObstacleType.river => riverImg
      }
      val p = obstacle.getPosition - Point(obstacle.getWidth, obstacle.getHeight) / 2 + offset
      if(p.in(view,Point(obstacle.getWidth,obstacle.getHeight))) {
        if (obstacleImgComplete) {
          val isAttacked = obstacle.obstacleType == ObstacleType.steel && obstacleAttackedAnimationMap.contains(obstacle.oId)
          val cacheCanvas = obstacleCanvasCacheMap.getOrElseUpdate((obstacle.obstacleType, isAttacked),
            generateEnvironmentCacheCanvas(obstacle.obstacleType, obstacle.getWidth, obstacle.getHeight, isAttacked,layerCanvasUnit))
          immutableCtx.drawImage(cacheCanvas, p.x * layerCanvasUnit, p.y * layerCanvasUnit)
        } else {
          immutableCtx.beginPath()
          immutableCtx.drawImage(img, p.x * layerCanvasUnit, p.y * layerCanvasUnit,
            Some(obstacle.getWidth * layerCanvasUnit, obstacle.getHeight * layerCanvasUnit))
          immutableCtx.fill()
          immutableCtx.stroke()
          immutableCtx.closePath()

        }
        if (obstacle.obstacleType == ObstacleType.steel && obstacleAttackedAnimationMap.contains(obstacle.oId)) {
          if (obstacleAttackedAnimationMap(obstacle.oId) <= 0) obstacleAttackedAnimationMap.remove(obstacle.oId)
          else obstacleAttackedAnimationMap.put(obstacle.oId, obstacleAttackedAnimationMap(obstacle.oId) - 1)
        }
      }
    }
  }

}
