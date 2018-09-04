package com.neo.sk.tank.front.tankClient.draw

import com.neo.sk.tank.front.common.Routes
import com.neo.sk.tank.front.tankClient.GameContainerClientImpl
import com.neo.sk.tank.shared.`object`.Obstacle
import com.neo.sk.tank.shared.model.Constants.ObstacleType
import com.neo.sk.tank.shared.model.Point
import org.scalajs.dom
import org.scalajs.dom.ext.Color
import org.scalajs.dom.raw.HTMLElement
import org.scalajs.dom.html

import scala.collection.mutable

/**
  * Created by hongruying on 2018/8/29
  */
trait ObstacleDrawUtil{ this:GameContainerClientImpl =>

  private val obstacleCanvasCacheMap = mutable.HashMap[(Byte, Boolean),html.Canvas]()

  private val steelImg = dom.document.createElement("img").asInstanceOf[html.Image]
  steelImg.setAttribute("src",s"${Routes.base}/static/img/钢铁.png")

  private val riverImg = dom.document.createElement("img").asInstanceOf[html.Image]
  riverImg.setAttribute("src",s"${Routes.base}/static/img/river.png")


  protected def obstacleImgComplete: Boolean = steelImg.complete && riverImg.complete


  private def generateObstacleCacheCanvas(width: Float, height: Float, color: String): html.Canvas = {
    val cacheCanvas = dom.document.createElement("canvas").asInstanceOf[html.Canvas]
    val ctxCache = cacheCanvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
    cacheCanvas.width = (width * canvasUnit).toInt
    cacheCanvas.height = (height * canvasUnit).toInt
    drawObstacle(Point(width / 2, height / 2), width, height, 1, color, ctxCache)
    cacheCanvas
  }


  private def drawObstacle(centerPosition:Point, width:Float, height:Float, bloodPercent:Float, color:String, context: dom.CanvasRenderingContext2D = ctx):Unit = {
    context.fillStyle = color
    context.strokeStyle = color
    context.lineWidth = 2
    context.beginPath()
    context.fillRect((centerPosition.x - width / 2) * canvasUnit, (centerPosition.y + height / 2 - bloodPercent * height) * canvasUnit,
      width * canvasUnit, bloodPercent * height * canvasUnit)
    context.closePath()
    context.beginPath()
    context.rect((centerPosition.x - width / 2) * canvasUnit, (centerPosition.y - height / 2) * canvasUnit,
      width * canvasUnit, height * canvasUnit
    )
    context.stroke()
    context.closePath()
    context.lineWidth = 1
  }


  protected def drawObstacles(offset:Point,view:Point) = {
    obstacleMap.values.foreach{ obstacle =>
      if((obstacle.getPosition + offset).in(view,Point(obstacle.getWidth,obstacle.getHeight))) {
        val color = (obstacle.obstacleType, obstacleAttackedAnimationMap.get(obstacle.oId).nonEmpty) match {
          case (ObstacleType.airDropBox, true) =>
            if (obstacleAttackedAnimationMap(obstacle.oId) <= 0) obstacleAttackedAnimationMap.remove(obstacle.oId)
            else obstacleAttackedAnimationMap.put(obstacle.oId, obstacleAttackedAnimationMap(obstacle.oId) - 1)
            s"rgba(99, 255, 255, 0.5)"
          case (ObstacleType.airDropBox, false) => s"rgba(0, 255, 255, 1)"
          case (ObstacleType.brick, true) =>
            if (obstacleAttackedAnimationMap(obstacle.oId) <= 0) obstacleAttackedAnimationMap.remove(obstacle.oId)
            else obstacleAttackedAnimationMap.put(obstacle.oId, obstacleAttackedAnimationMap(obstacle.oId) - 1)
            s"rgba(139 ,105, 105, 0.5)"
          case (ObstacleType.brick, false) => s"rgba(139 ,105, 105,1)"
          case _ =>
            println(s"the obstacle=${obstacle} has not color")
            s"rgba(139 ,105, 105,1)"
        }
        if (obstacle.bloodPercent() > 0.9999999) {
          val p = obstacle.getPosition + offset - Point(obstacle.getWidth / 2, obstacle.getHeight / 2)
          val cache = obstacleCanvasCacheMap.getOrElseUpdate((obstacle.obstacleType, false), generateObstacleCacheCanvas(obstacle.getWidth, obstacle.getHeight, color))
          ctx.drawImage(cache, p.x * canvasUnit, p.y * canvasUnit)
        } else {
          drawObstacle(obstacle.getPosition + offset, obstacle.getWidth, obstacle.getHeight, obstacle.bloodPercent(), color)
        }
      }
    }
  }



  private def generateEnvironmentCacheCanvas(obstacleType:Byte, obstacleWidth:Float, obstacleHeight:Float,isAttacked:Boolean):html.Canvas = {
    val canvasCache = dom.document.createElement("canvas").asInstanceOf[html.Canvas]
    val ctxCache = canvasCache.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
    canvasCache.width = math.ceil(obstacleWidth * canvasUnit).toInt
    canvasCache.height = math.ceil(obstacleHeight * canvasUnit).toInt
    val img = obstacleType match {
      case ObstacleType.steel => steelImg
      case ObstacleType.river => riverImg
    }
    if (!isAttacked){
      ctxCache.drawImage(img.asInstanceOf[HTMLElement], 0, 0,
        obstacleWidth * canvasUnit,obstacleHeight * canvasUnit)
    } else{
      ctxCache.globalAlpha = 0.5
      ctxCache.drawImage(img.asInstanceOf[HTMLElement], 0, 0,
        obstacleWidth * canvasUnit,obstacleHeight * canvasUnit)
      ctxCache.globalAlpha = 1
    }


    canvasCache
  }

  protected def drawEnvironment(offset:Point,view:Point) = {
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
            generateEnvironmentCacheCanvas(obstacle.obstacleType, obstacle.getWidth, obstacle.getHeight, isAttacked))
          ctx.drawImage(cacheCanvas, p.x * canvasUnit, p.y * canvasUnit)
        } else {
          ctx.beginPath()
          ctx.drawImage(img.asInstanceOf[HTMLElement], p.x * canvasUnit, p.y * canvasUnit,
            obstacle.getWidth * canvasUnit, obstacle.getHeight * canvasUnit)
          ctx.fill()
          ctx.stroke()
          ctx.closePath()
        }
        if (obstacle.obstacleType == ObstacleType.steel && obstacleAttackedAnimationMap.contains(obstacle.oId)) {
          //        val imgData = ctx.getImageData(p.x * canvasUnit, p.y * canvasUnit,
          //          obstacle.getWidth * canvasUnit, obstacle.getHeight * canvasUnit)
          //        var i = 0
          //        val len = imgData.data.length
          //        while ( {
          //          i < len
          //        }) { // 改变每个像素的透明度
          //          imgData.data(i + 3) =  math.ceil(imgData.data(i + 3) * 0.5).toInt
          //          i += 4
          //        }
          //        // 将获取的图片数据放回去。
          //        ctx.putImageData(imgData, p.x * canvasUnit, p.y * canvasUnit)
          if (obstacleAttackedAnimationMap(obstacle.oId) <= 0) obstacleAttackedAnimationMap.remove(obstacle.oId)
          else obstacleAttackedAnimationMap.put(obstacle.oId, obstacleAttackedAnimationMap(obstacle.oId) - 1)
        }
      }
    }

  }
}
