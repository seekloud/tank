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

import org.seekloud.tank.shared.`object`.AirDropBox
import org.seekloud.tank.shared.game.GameContainerClientImpl
import org.seekloud.tank.shared.model.Constants.ObstacleType
import org.seekloud.tank.shared.model.Point
import org.seekloud.tank.shared.util.canvas.MiddleContext

import scala.collection.mutable

/**
  * Created by hongruying on 2018/8/29
  */
trait ObstacleDrawUtil{ this:GameContainerClientImpl =>

  private val obstacleCanvasCacheMap = mutable.HashMap[(Byte, Boolean), Any]()

  private val steelImg =drawFrame.createImage("/img/钢铁.png")
  private val riverImg =drawFrame.createImage("/img/river.png")
  protected val airBoxImg =drawFrame.createImage("/img/道具.png")

  def updateObstacleSize(canvasSize:Point)={
    obstacleCanvasCacheMap.clear()
  }

  //todo  此处需要调研图片complete
  protected def obstacleImgComplete: Boolean = steelImg.isComplete && riverImg.isComplete

  protected def generateObstacleCacheCanvas(width: Float, height: Float, color: String,unit:Int): Any = {
    val cacheCanvas = drawFrame.createCanvas((width * unit).toInt, (height * unit).toInt)
    val ctxCache = cacheCanvas.getCtx
    drawObstacle(Point(width / 2, height / 2), width, height, 1, color, ctxCache,unit)
    cacheCanvas.change2Image()
  }

  protected def drawObstacle(centerPosition:Point, width:Float, height:Float, bloodPercent:Float, color:String, context:MiddleContext ,unit: Int):Unit = {
    context.setFill(color)
    context.setStrokeStyle(color)
    context.setLineWidth(2)
    context.beginPath()
    context.fillRec((centerPosition.x - width / 2) * unit, (centerPosition.y + height / 2 - bloodPercent * height) * unit,
      width * unit, bloodPercent * height * unit)
    context.closePath()
    context.beginPath()
    context.rect((centerPosition.x - width / 2) * unit, (centerPosition.y - height / 2) * unit,
      width * unit, height * unit
    )
    context.stroke()
    context.closePath()
    context.setLineWidth(1)
  }


  protected def drawObstacles(offset:Point,view:Point,ctx:MiddleContext,unit:Int) = {
    obstacleMap.values.foreach{ obstacle =>
      if((obstacle.getPosition + offset).in(view,Point(obstacle.getWidth,obstacle.getHeight))) {
        val isAttacked: Boolean = obstacleAttackedAnimationMap.get(obstacle.oId).nonEmpty
        val color = (obstacle.obstacleType, obstacleAttackedAnimationMap.get(obstacle.oId).nonEmpty) match {
          case (ObstacleType.airDropBox, true) =>
            if (obstacleAttackedAnimationMap(obstacle.oId) <= 0) obstacleAttackedAnimationMap.remove(obstacle.oId)
            else obstacleAttackedAnimationMap.put(obstacle.oId, obstacleAttackedAnimationMap(obstacle.oId) - 1)
            "rgba(99, 255, 255, 0.5)"
          case (ObstacleType.airDropBox, false) => "rgba(0, 255, 255, 1)"
          case (ObstacleType.brick, true) =>
            if (obstacleAttackedAnimationMap(obstacle.oId) <= 0) obstacleAttackedAnimationMap.remove(obstacle.oId)
            else obstacleAttackedAnimationMap.put(obstacle.oId, obstacleAttackedAnimationMap(obstacle.oId) - 1)
            "rgba(139, 105, 105, 0.5)"
          case (ObstacleType.brick, false) => "rgba(139, 105, 105, 1)"
          case _ =>
            println(s"the obstacle=${obstacle} has not color")
            "rgba(139, 105, 105, 1)"
        }
        if(obstacle.obstacleType == ObstacleType.airDropBox){
          val p = obstacle.getPosition + offset - Point(obstacle.getWidth / 2, obstacle.getHeight / 2)
          if (isAttacked){
            ctx.setGlobalAlpha(0.5)
            ctx.drawImage(airBoxImg, p.x * unit, p.y * unit,
              Some(obstacle.getWidth * unit, obstacle.getHeight * unit))
            ctx.setGlobalAlpha(1)
          } else {
            ctx.drawImage(airBoxImg, p.x * unit, p.y * unit,
              Some(obstacle.getWidth * unit, obstacle.getHeight * unit))
          }
        }else{
          if (obstacle.bloodPercent() > 0.9999999) {
            drawObstacle(obstacle.getPosition + offset, obstacle.getWidth, obstacle.getHeight, 1, color, ctx,unit)
          } else {
            drawObstacle(obstacle.getPosition + offset, obstacle.getWidth, obstacle.getHeight, obstacle.bloodPercent(), color, ctx,unit)
          }
        }


      }
    }
  }


  def drawObstacleBloodSlider(offset:Point,ctx:MiddleContext,unit:Int) = {
    obstacleMap.values.filter(_.isInstanceOf[AirDropBox]).foreach{ obstacle =>
      if(obstacle.bloodPercent() < 0.99999999){
        val p = obstacle.getPosition + offset - Point(obstacle.getWidth / 2, obstacle.getHeight / 2)
        drawLine(ctx,p.x * unit, (p.y - 2) * unit, obstacle.getWidth * unit /2, obstacle.getWidth * unit, "#4D4D4D")
        drawLine(ctx,p.x * unit, (p.y - 2) * unit, obstacle.getWidth * unit / 4, obstacle.getWidth * unit * obstacle.bloodPercent(), "#98FB98")
      }
    }
  }

  //画血量条
  private def drawLine(ctx:MiddleContext,startX: Float, startY: Float, lineWidth:Float, lineLen:Float, color:String) = {
    ctx.save()
    ctx.setLineWidth(lineWidth)
    ctx.setLineCap("round")
    ctx.setStrokeStyle(color)
    ctx.beginPath()
    ctx.moveTo(startX, startY)
    ctx.lineTo(startX + lineLen, startY)
    ctx.stroke()
    ctx.closePath()
    ctx.restore()
  }



  protected def generateEnvironmentCacheCanvas(obstacleType:Byte, obstacleWidth:Float, obstacleHeight:Float,isAttacked:Boolean,unit:Int):Any = {
    val canvasCache = drawFrame.createCanvas(math.ceil(obstacleWidth * unit).toInt, math.ceil(obstacleHeight * unit).toInt)
    val ctxCache = canvasCache.getCtx
    val img = obstacleType match {
      case ObstacleType.steel => steelImg
      case ObstacleType.river => riverImg
    }
    if (!isAttacked){
      ctxCache.drawImage(img, 0, 0,
        Some(obstacleWidth * unit,obstacleHeight * unit))

    } else{
      ctxCache.setGlobalAlpha(0.5)
      ctxCache.drawImage(img, 0, 0,
        Some(obstacleWidth * unit,obstacleHeight * unit))
      ctxCache.setGlobalAlpha(1)
    }
    canvasCache.change2Image()
  }

  protected def drawEnvironment(offset:Point,view:Point,unit:Int,ctx:MiddleContext) = {
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
            generateEnvironmentCacheCanvas(obstacle.obstacleType, obstacle.getWidth, obstacle.getHeight, isAttacked,unit))
          ctx.drawImage(cacheCanvas, p.x * unit, p.y * unit, Some(obstacle.getWidth * unit, obstacle.getHeight * unit))
        } else {
          ctx.beginPath()
          ctx.drawImage(img, p.x * unit, p.y * unit,
            Some(obstacle.getWidth * unit, obstacle.getHeight * unit))
          ctx.fill()
          ctx.stroke()
          ctx.closePath()

        }
        if (obstacle.obstacleType == ObstacleType.steel && obstacleAttackedAnimationMap.contains(obstacle.oId)) {
          if (obstacleAttackedAnimationMap(obstacle.oId) <= 0) obstacleAttackedAnimationMap.remove(obstacle.oId)
          else obstacleAttackedAnimationMap.put(obstacle.oId, obstacleAttackedAnimationMap(obstacle.oId) - 1)
        }
      }
    }

  }
}
