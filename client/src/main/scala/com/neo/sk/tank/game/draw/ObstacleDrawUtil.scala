package com.neo.sk.tank.game.draw

import com.neo.sk.tank.game.GameContainerClientImpl
import com.neo.sk.tank.shared.`object`.AirDropBox
import com.neo.sk.tank.shared.model.Constants.ObstacleType
import com.neo.sk.tank.shared.model.Point
import javafx.scene.SnapshotParameters
import javafx.scene.canvas.{Canvas, GraphicsContext}
import javafx.scene.image.Image
import javafx.scene.paint.Color
import javafx.scene.shape.StrokeLineCap

import scala.collection.mutable

/**
  * Created by hongruying on 2018/8/29
  */
trait ObstacleDrawUtil{ this:GameContainerClientImpl =>

  //fixme 将此处map暴露给子类
  private val obstacleCanvasCacheMap = mutable.HashMap[(Byte, Boolean), Canvas]()

  private val steelImg = new Image(s"/tank/static/img/钢铁.png")
  private val riverImg = new Image(s"/tank/static/img/river.png")
  private val airBoxImg = new Image(s"/tank/static/img/道具.png")

  def updateObstacleSize(canvasSize:Point)={
    obstacleCanvasCacheMap.clear()
  }

  //todo  此处需要调研图片complete
  protected def obstacleImgComplete: Boolean = steelImg.isBackgroundLoading && riverImg.isBackgroundLoading

  private def generateObstacleCacheCanvas(width: Float, height: Float, color: String): Canvas = {
    val cacheCanvas = new Canvas((width * canvasUnit).toInt, (height * canvasUnit).toInt)
    val ctxCache = cacheCanvas.getGraphicsContext2D
    drawObstacle(Point(width / 2, height / 2), width, height, 1, color, ctxCache)
    cacheCanvas
  }

  private def drawObstacle(centerPosition:Point, width:Float, height:Float, bloodPercent:Float, color:String, context:GraphicsContext = ctx):Unit = {
    context.setFill(Color.web(color))
    context.setStroke(Color.web(color))
    context.setLineWidth(2)
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
            ctx.setGlobalAlpha(0.5)
            ctx.drawImage(airBoxImg, p.x * canvasUnit, p.y * canvasUnit,
              obstacle.getWidth * canvasUnit, obstacle.getHeight * canvasUnit)
            ctx.setGlobalAlpha(1)
          } else {
            ctx.drawImage(airBoxImg, p.x * canvasUnit, p.y * canvasUnit,
              obstacle.getWidth * canvasUnit, obstacle.getHeight * canvasUnit)
          }
        }else{
          if (obstacle.bloodPercent() > 0.9999999) {
            val p = obstacle.getPosition + offset - Point(obstacle.getWidth / 2, obstacle.getHeight / 2)
            val cache = obstacleCanvasCacheMap.getOrElseUpdate((obstacle.obstacleType, false), generateObstacleCacheCanvas(obstacle.getWidth, obstacle.getHeight, color))
            ctx.drawImage(cache.snapshot(new SnapshotParameters(), null), p.x * canvasUnit, p.y * canvasUnit)
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
        drawLine(p.x * canvasUnit, (p.y - 2) * canvasUnit, 10, obstacle.getWidth * canvasUnit, "#4D4D4D")
        drawLine(p.x * canvasUnit, (p.y - 2) * canvasUnit, 5, obstacle.getWidth * canvasUnit * obstacle.bloodPercent(), "#98FB98")
      }
    }
  }

  //画血量条
  private def drawLine(startX: Float, startY: Float, lineWidth:Float, lineLen:Float, color:String) = {
    ctx.save()
    ctx.setLineWidth(lineWidth)
    ctx.setLineCap(StrokeLineCap.ROUND)
    ctx.setStroke(Color.web(color))
    ctx.beginPath()
    ctx.moveTo(startX, startY)
    ctx.lineTo(startX + lineLen, startY)
    ctx.stroke()
    ctx.closePath()
    ctx.restore()
  }



  private def generateEnvironmentCacheCanvas(obstacleType:Byte, obstacleWidth:Float, obstacleHeight:Float,isAttacked:Boolean):Canvas = {
    val canvasCache = new Canvas(math.ceil(obstacleWidth * canvasUnit).toInt, math.ceil(obstacleHeight * canvasUnit).toInt)
    val ctxCache = canvasCache.getGraphicsContext2D
    val img = obstacleType match {
      case ObstacleType.steel => steelImg
      case ObstacleType.river => riverImg
    }
    if (!isAttacked){
      ctxCache.drawImage(img, 0, 0,
        obstacleWidth * canvasUnit,obstacleHeight * canvasUnit)
    } else{
      ctxCache.setGlobalAlpha(0.5)
      ctxCache.drawImage(img, 0, 0,
        obstacleWidth * canvasUnit,obstacleHeight * canvasUnit)
      ctxCache.setGlobalAlpha(1)
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
          ctx.drawImage(cacheCanvas.snapshot(new SnapshotParameters(), null), p.x * canvasUnit, p.y * canvasUnit)
        } else {
          ctx.beginPath()
          ctx.drawImage(img, p.x * canvasUnit, p.y * canvasUnit,
            obstacle.getWidth * canvasUnit, obstacle.getHeight * canvasUnit)
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
