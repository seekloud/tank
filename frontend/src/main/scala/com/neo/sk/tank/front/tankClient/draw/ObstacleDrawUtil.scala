package com.neo.sk.tank.front.tankClient.draw

import com.neo.sk.tank.front.common.Routes
import com.neo.sk.tank.front.tankClient.GameContainerClientImpl
import com.neo.sk.tank.shared.`object`.{AirDropBox, Obstacle}
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

  //fixme 将此处map暴露给子类
  private val obstacleCanvasCacheMap = mutable.HashMap[(Byte, Boolean),html.Canvas]()

  private val steelImg = dom.document.createElement("img").asInstanceOf[html.Image]
  steelImg.setAttribute("src",s"${Routes.base}/static/img/钢铁.png")

  private val riverImg = dom.document.createElement("img").asInstanceOf[html.Image]
  riverImg.setAttribute("src",s"${Routes.base}/static/img/river.png")

  private val airBoxImg = dom.document.createElement("img").asInstanceOf[html.Image]
  airBoxImg.setAttribute("src",s"${Routes.base}/static/img/道具.png")


  def updateObstacleSize(canvasSize:Point)={
    obstacleCanvasCacheMap.clear()
  }

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
        val isAttacked: Boolean = obstacleAttackedAnimationMap.get(obstacle.oId).nonEmpty
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
        if(obstacle.obstacleType == ObstacleType.airDropBox){
          val p = obstacle.getPosition + offset - Point(obstacle.getWidth / 2, obstacle.getHeight / 2)
          if (isAttacked){
            ctx.globalAlpha = 0.5
            ctx.drawImage(airBoxImg.asInstanceOf[HTMLElement], p.x * canvasUnit, p.y * canvasUnit,
              obstacle.getWidth * canvasUnit, obstacle.getHeight * canvasUnit)
            ctx.globalAlpha = 1
          } else {
            ctx.drawImage(airBoxImg.asInstanceOf[HTMLElement], p.x * canvasUnit, p.y * canvasUnit,
              obstacle.getWidth * canvasUnit, obstacle.getHeight * canvasUnit)
          }

//          if(obstacle.bloodPercent() < 0.99999999){
//            drawLine(p.x * canvasUnit, (p.y - 2) * canvasUnit, 10, obstacle.getWidth * canvasUnit, "#4D4D4D")
//            drawLine(p.x * canvasUnit, (p.y - 2) * canvasUnit, 5, obstacle.getWidth * canvasUnit * obstacle.bloodPercent(), "#98FB98")
//          }


//          if (obstacle.bloodPercent() > 0.99999999){
//            ctx.drawImage(airBoxImg.asInstanceOf[HTMLElement], p.x * canvasUnit, p.y * canvasUnit,
//              obstacle.getWidth * canvasUnit, obstacle.getHeight * canvasUnit)
//          }
//          else{
//            val p1 = obstacle.getPosition + offset - Point(obstacle.getWidth / 2, obstacle.getHeight / 2)
//            val p2 = obstacle.getPosition + offset - Point(obstacle.getWidth / 2, obstacle.getHeight / 2)+Point(0,(1-obstacle.bloodPercent())*obstacle.getHeight)
//            ctx.globalAlpha = 0.3
//
//            val cacheCanvas = dom.document.createElement("canvas").asInstanceOf[html.Canvas]
//            val ctxCache = cacheCanvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
//            cacheCanvas.width = (obstacle.getWidth * canvasUnit).toInt
//            cacheCanvas.height = (obstacle.getHeight * canvasUnit).toInt
//            ctxCache.drawImage(airBoxImg,0,0,obstacle.getWidth * canvasUnit,obstacle.getHeight * canvasUnit)
//
//
//
//            ctx.drawImage(cacheCanvas, 0, 0, obstacle.getWidth * canvasUnit, obstacle.getHeight *(1-obstacle.bloodPercent())* canvasUnit,
//              p1.x * canvasUnit, p1.y * canvasUnit,
//              obstacle.getWidth * canvasUnit, obstacle.getHeight *(1-obstacle.bloodPercent())* canvasUnit)
//            ctx.globalAlpha = 1
//            ctx.drawImage(cacheCanvas, 0,(1-obstacle.bloodPercent())*obstacle.getHeight * canvasUnit ,
//              obstacle.getWidth * canvasUnit, obstacle.getHeight*obstacle.bloodPercent() * canvasUnit,
//              p2.x * canvasUnit, p2.y * canvasUnit,
//              obstacle.getWidth * canvasUnit,obstacle.getHeight *obstacle.bloodPercent()* canvasUnit)
//
//          }

        }else{
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
  }


  def drawObstacleBloodSlider(offset:Point) = {
    obstacleMap.values.filter(_.isInstanceOf[AirDropBox]).foreach{ obstacle =>
      if(obstacle.bloodPercent() < 0.99999999){
        val p = obstacle.getPosition + offset - Point(obstacle.getWidth / 2, obstacle.getHeight / 2)
        drawLine(p.x * canvasUnit, (p.y - 2) * canvasUnit, 1 * canvasUnit, obstacle.getWidth * canvasUnit, "#4D4D4D")
        drawLine(p.x * canvasUnit, (p.y - 2) * canvasUnit, (0.5 * canvasUnit).toInt, obstacle.getWidth * canvasUnit * obstacle.bloodPercent(), "#98FB98")
      }
    }
  }

  //画血量条
  private def drawLine(startX: Float, startY: Float, lineWidth:Float, lineLen:Float, color:String) = {
    ctx.save()
    ctx.lineWidth = lineWidth
    ctx.lineCap = "round"
    ctx.strokeStyle = color
    ctx.beginPath()
    ctx.moveTo(startX, startY)
    ctx.lineTo(startX + lineLen, startY)
    ctx.stroke()
    ctx.closePath()
    ctx.restore()
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
