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
  private val airBoxImg =drawFrame.createImage("/img/道具.png")

  def updateObstacleSize(canvasSize:Point)={
    obstacleCanvasCacheMap.clear()
  }

  //todo  此处需要调研图片complete
  protected def obstacleImgComplete: Boolean = steelImg.isComplete && riverImg.isComplete

  private def generateObstacleCacheCanvas(width: Float, height: Float, color: String): Any = {
    val cacheCanvas = drawFrame.createCanvas((width * canvasUnit).toInt, (height * canvasUnit).toInt)
    val ctxCache = cacheCanvas.getCtx
    drawObstacle(Point(width / 2, height / 2), width, height, 1, color, ctxCache)
    cacheCanvas.change2Image()
  }

  private def drawObstacle(centerPosition:Point, width:Float, height:Float, bloodPercent:Float, color:String, context:MiddleContext = viewCtx):Unit = {
    context.setFill(color)
    context.setStrokeStyle(color)
    context.setLineWidth(2)
    context.beginPath()
    context.fillRec((centerPosition.x - width / 2) * canvasUnit, (centerPosition.y + height / 2 - bloodPercent * height) * canvasUnit,
      width * canvasUnit, bloodPercent * height * canvasUnit)
    context.closePath()
    context.beginPath()
    context.rect((centerPosition.x - width / 2) * canvasUnit, (centerPosition.y - height / 2) * canvasUnit,
      width * canvasUnit, height * canvasUnit
    )
    context.stroke()
    context.closePath()
    context.setLineWidth(1)
  }


  protected def drawObstacles(offset:Point,view:Point) = {
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
            viewCtx.setGlobalAlpha(0.5)
            viewCtx.drawImage(airBoxImg, p.x * canvasUnit, p.y * canvasUnit,
              Some(obstacle.getWidth * canvasUnit, obstacle.getHeight * canvasUnit))
            viewCtx.setGlobalAlpha(1)
            if(isBot){
              mutableCtx.setGlobalAlpha(0.5)
              mutableCtx.drawImage(airBoxImg, p.x * canvasUnit /layerCanvasUnit, p.y * canvasUnit /layerCanvasUnit,
                Some(obstacle.getWidth * canvasUnit /layerCanvasUnit, obstacle.getHeight * canvasUnit /layerCanvasUnit))
              mutableCtx.setGlobalAlpha(1)
            }
          } else {
            viewCtx.drawImage(airBoxImg, p.x * canvasUnit, p.y * canvasUnit,
              Some(obstacle.getWidth * canvasUnit, obstacle.getHeight * canvasUnit))
            if(isBot){
              mutableCtx.drawImage(airBoxImg, p.x * canvasUnit /layerCanvasUnit, p.y * canvasUnit /layerCanvasUnit,
                Some(obstacle.getWidth * canvasUnit /layerCanvasUnit, obstacle.getHeight * canvasUnit /layerCanvasUnit))
            }
          }
        }else{
          if (obstacle.bloodPercent() > 0.9999999) {
            val p = obstacle.getPosition + offset - Point(obstacle.getWidth / 2, obstacle.getHeight / 2)
            val cache = obstacleCanvasCacheMap.getOrElseUpdate((obstacle.obstacleType, false), generateObstacleCacheCanvas(obstacle.getWidth, obstacle.getHeight, color))
            viewCtx.drawImage(cache, p.x * canvasUnit, p.y * canvasUnit)
            if(isBot){
              mutableCtx.drawImage(cache, p.x * canvasUnit /layerCanvasUnit, p.y * canvasUnit /layerCanvasUnit)
            }
          } else {
            drawObstacle(obstacle.getPosition + offset, obstacle.getWidth, obstacle.getHeight, obstacle.bloodPercent(), color)
            if(isBot){
              drawObstacle(obstacle.getPosition /layerCanvasUnit + offset /layerCanvasUnit, obstacle.getWidth /layerCanvasUnit, obstacle.getHeight /layerCanvasUnit, obstacle.bloodPercent(), color,mutableCtx)
            }
          }
        }


      }
    }
  }


  def drawObstacleBloodSlider(offset:Point) = {
    obstacleMap.values.filter(_.isInstanceOf[AirDropBox]).foreach{ obstacle =>
      if(obstacle.bloodPercent() < 0.99999999){
        val p = obstacle.getPosition + offset - Point(obstacle.getWidth / 2, obstacle.getHeight / 2)
        drawLine(p.x * canvasUnit, (p.y - 2) * canvasUnit, 10, obstacle.getWidth * canvasUnit, "#4D4D4D")
        drawLine(p.x * canvasUnit, (p.y - 2) * canvasUnit, 5, obstacle.getWidth * canvasUnit * obstacle.bloodPercent(), "#98FB98")
      }
    }
  }

  //画血量条
  private def drawLine(startX: Float, startY: Float, lineWidth:Float, lineLen:Float, color:String) = {
    viewCtx.save()
    viewCtx.setLineWidth(lineWidth)
    viewCtx.setLineCap("round")
    viewCtx.setStrokeStyle(color)
    viewCtx.beginPath()
    viewCtx.moveTo(startX, startY)
    viewCtx.lineTo(startX + lineLen, startY)
    viewCtx.stroke()
    viewCtx.closePath()
    viewCtx.restore()
    if(isBot){
      mutableCtx.save()
      mutableCtx.setLineWidth(lineWidth)
      mutableCtx.setLineCap("round")
      mutableCtx.setStrokeStyle(color)
      mutableCtx.beginPath()
      mutableCtx.moveTo(startX /layerCanvasUnit, startY /layerCanvasUnit)
      mutableCtx.lineTo(startX /layerCanvasUnit + lineLen /layerCanvasUnit, startY /layerCanvasUnit)
      mutableCtx.stroke()
      mutableCtx.closePath()
      mutableCtx.restore()
    }
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
            generateEnvironmentCacheCanvas(obstacle.obstacleType, obstacle.getWidth, obstacle.getHeight, isAttacked,canvasUnit))
          ctx.drawImage(cacheCanvas, p.x * unit, p.y * unit)
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
