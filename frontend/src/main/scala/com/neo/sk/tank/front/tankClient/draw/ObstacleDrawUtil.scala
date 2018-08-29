package com.neo.sk.tank.front.tankClient.draw

import com.neo.sk.tank.front.common.Routes
import com.neo.sk.tank.front.tankClient.GameContainerClientImpl
import com.neo.sk.tank.shared.model.Constants.ObstacleType
import com.neo.sk.tank.shared.model.Point
import org.scalajs.dom
import org.scalajs.dom.raw.HTMLElement

/**
  * Created by hongruying on 2018/8/29
  */
trait ObstacleDrawUtil{ this:GameContainerClientImpl =>



  private def drawObstacle(centerPosition:Point, width:Float, height:Float, bloodPercent:Float, color:String):Unit = {
    ctx.fillStyle = color
    ctx.strokeStyle = color
    ctx.lineWidth = 2
    ctx.beginPath()
    ctx.fillRect((centerPosition.x - width / 2) * canvasUnit, (centerPosition.y + height / 2 - bloodPercent * height) * canvasUnit,
      width * canvasUnit, bloodPercent * height * canvasUnit)
    ctx.closePath()
    ctx.beginPath()
    ctx.rect((centerPosition.x - width / 2) * canvasUnit, (centerPosition.y - height / 2) * canvasUnit,
      width * canvasUnit, height * canvasUnit
    )
    ctx.stroke()
    ctx.closePath()
    ctx.lineWidth = 1
  }


  protected def drawObstacles(offset:Point) = {
    obstacleMap.values.foreach{ obstacle =>
      val color = (obstacle.obstacleType,obstacleAttackedAnimationMap.get(obstacle.oId).nonEmpty) match {
        case (ObstacleType.airDropBox, true) =>
          if(obstacleAttackedAnimationMap(obstacle.oId) <= 0) obstacleAttackedAnimationMap.remove(obstacle.oId)
          else obstacleAttackedAnimationMap.put(obstacle.oId, obstacleAttackedAnimationMap(obstacle.oId) - 1)
          s"rgba(99, 255, 255, 0.5)"
        case (ObstacleType.airDropBox, false) => s"rgba(0, 255, 255, 1)"
        case (ObstacleType.brick, true) =>
          if(obstacleAttackedAnimationMap(obstacle.oId) <= 0) obstacleAttackedAnimationMap.remove(obstacle.oId)
          else obstacleAttackedAnimationMap.put(obstacle.oId, obstacleAttackedAnimationMap(obstacle.oId) - 1)
          s"rgba(139 ,105, 105, 0.5)"
        case (ObstacleType.brick, false) => s"rgba(139 ,105, 105,1)"
        case _ =>
          println(s"the obstacle=${obstacle} has not color")
          s"rgba(139 ,105, 105,1)"
      }
      drawObstacle(obstacle.getPosition + offset, obstacle.getWidth, obstacle.getHeight, obstacle.bloodPercent(),color)
    }
  }

  protected def drawEnvironment(offset:Point) = {
    environmentMap.values.foreach{ obstacle =>
      val img = dom.document.createElement("img")
      obstacle.obstacleType match {
        case ObstacleType.steel => img.setAttribute("src",s"${Routes.base}/static/img/钢铁.png")
        case ObstacleType.river => img.setAttribute("src",s"${Routes.base}/static/img/river.png")
      }
      val p = obstacle.getPosition - Point(obstacle.getWidth, obstacle.getHeight) / 2 + offset
      ctx.beginPath()
      ctx.drawImage(img.asInstanceOf[HTMLElement], p.x * canvasUnit, p.y * canvasUnit,
        obstacle.getWidth * canvasUnit,obstacle.getHeight * canvasUnit)
      ctx.fill()
      ctx.stroke()
      ctx.closePath()
      if(obstacle.obstacleType == ObstacleType.steel && obstacleAttackedAnimationMap.contains(obstacle.oId)){
        val imgData = ctx.getImageData(p.x * canvasUnit, p.y * canvasUnit,
          obstacle.getWidth * canvasUnit, obstacle.getHeight * canvasUnit)
        var i = 0
        val len = imgData.data.length
        while ( {
          i < len
        }) { // 改变每个像素的透明度
          imgData.data(i + 3) =  math.ceil(imgData.data(i + 3) * 0.5).toInt
          i += 4
        }
        // 将获取的图片数据放回去。
        ctx.putImageData(imgData, p.x * canvasUnit, p.y * canvasUnit)
        if(obstacleAttackedAnimationMap(obstacle.oId) <= 0) obstacleAttackedAnimationMap.remove(obstacle.oId)
        else obstacleAttackedAnimationMap.put(obstacle.oId, obstacleAttackedAnimationMap(obstacle.oId) - 1)
      }
    }
  }
}
