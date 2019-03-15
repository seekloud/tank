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

import org.seekloud.tank.shared.game.{GameContainerClientImpl, TankClientImpl}
import org.seekloud.tank.shared.model.Constants.{InvincibleSize, SmallBullet, TankStar}
import org.seekloud.tank.shared.model.Point
import org.seekloud.tank.shared.util.canvas.MiddleContext

import scala.collection.mutable

/**
  * Created by hongruying on 2018/8/29
  */
trait TankDrawUtil{ this:GameContainerClientImpl =>

  private val myTankInfoCacheMap = mutable.HashMap[(Byte,Byte,Byte), Any]()
  private var canvasBoundary:Point=canvasSize / canvasUnit

  private val fillBulletImg = drawFrame.createImage("/img/子弹初始重构.png")
  private val emptyBulletImg = drawFrame.createImage("/img/子弹消失重构.png")
  private val fillMedicalImg = drawFrame.createImage("/img/yiliao.png")
  private val emptyMedicalImg = drawFrame.createImage("/img/huiyiliao.png")
//  private val sunImg = drawFrame.createImage("/img/sun.png")
  private val tankStarImg = drawFrame.createImage("/img/star.png")


  private val Pi = 3.14f


  def updateTankSize(canvasSize:Point)={
    myTankInfoCacheMap.clear()
    canvasBoundary=canvasSize
  }


  protected def drawTank(offset:Point, offsetTime:Long, view:Point) = {
    tankMap.values.foreach { t =>
      val tank = t.asInstanceOf[TankClientImpl]
      val p = tank.getPosition4Animation(boundary, quadTree, offsetTime,systemFrame) + offset
      if (p.in(view, Point(t.getRadius * 4, t.getRadius * 4))) {
        if (tankAttackedAnimationMap.contains(tank.tankId)) {
          if (tankAttackedAnimationMap(tank.tankId) <= 0) tankAttackedAnimationMap.remove(tank.tankId)
          else tankAttackedAnimationMap.put(tank.tankId, tankAttackedAnimationMap(tank.tankId) - 1)
          viewCtx.setGlobalAlpha(0.5)
        }

        //------------------------绘制炮筒--------------------------#
        val gunPositionList = tank.getGunPositions4Animation().map(t => (t + p) * canvasUnit)
        viewCtx.beginPath()
        viewCtx.moveTo(gunPositionList.last.x, gunPositionList.last.y)
        gunPositionList.foreach(t => viewCtx.lineTo(t.x, t.y))
        viewCtx.setFill("#7A7A7A")
        viewCtx.setStrokeStyle("#636363")
        viewCtx.fill()
        viewCtx.setLineWidth(0.4 * canvasUnit)
        viewCtx.stroke()
        viewCtx.closePath()
        if(isBot){
          bodiesCtx.beginPath()
          bodiesCtx.moveTo(gunPositionList.last.x /layerCanvasUnit , gunPositionList.last.y /layerCanvasUnit)
          gunPositionList.foreach(t => bodiesCtx.lineTo(t.x /layerCanvasUnit, t.y /layerCanvasUnit))
          bodiesCtx.setFill("#7A7A7A")
          bodiesCtx.setStrokeStyle("#636363")
          bodiesCtx.fill()
          bodiesCtx.setLineWidth(0.4 * canvasUnit  /layerCanvasUnit )
          bodiesCtx.stroke()
          bodiesCtx.closePath()

        }
        //----------------------------绘制坦克---------------------#
        if (tank.getInvincibleState) {
          viewCtx.beginPath()
          viewCtx.setFill("rgba(128, 100, 162, 0.2)")
          val centerX = p.x * canvasUnit
          val centerY = p.y * canvasUnit
          val radius = InvincibleSize.r * canvasUnit
          val startAngle = 0
          val lengthAngle = 360
          viewCtx.arc(centerX.toFloat, centerY.toFloat, radius, startAngle.toFloat, lengthAngle.toFloat)
          viewCtx.fill()
          viewCtx.closePath()
          if(isBot){
            bodiesCtx.beginPath()
            bodiesCtx.setFill("rgba(128, 100, 162, 0.2)")
            bodiesCtx.arc(centerX.toFloat /layerCanvasUnit, centerY.toFloat /layerCanvasUnit, radius /layerCanvasUnit , startAngle.toFloat , lengthAngle.toFloat )
            bodiesCtx.fill()
            bodiesCtx.closePath()

          }
        }
        viewCtx.beginPath()
        viewCtx.setLineWidth( 0.4 * canvasUnit)
        viewCtx.setStrokeStyle("#636363")
        val centerX = p.x * canvasUnit
        val centerY = p.y * canvasUnit
        val radius =  tank.getRadius * canvasUnit
        val startAngle = 0
        val lengthAngle = 360
        viewCtx.arc(centerX.toFloat, centerY.toFloat, radius, startAngle.toFloat, lengthAngle.toFloat)
        val tankColor = tank.getTankColor()
        viewCtx.setFill(tankColor)
        viewCtx.fill()
        viewCtx.stroke()
        viewCtx.closePath()
        viewCtx.setGlobalAlpha(1)
        if(isBot){
          bodiesCtx.beginPath()
          bodiesCtx.setLineWidth(0.4*canvasUnit  /layerCanvasUnit)
          bodiesCtx.setStrokeStyle("#636363")
          bodiesCtx.arc(centerX.toFloat /layerCanvasUnit, centerY.toFloat /layerCanvasUnit, radius /layerCanvasUnit , startAngle.toFloat , lengthAngle.toFloat )
          bodiesCtx.setFill(tankColor)
          bodiesCtx.fill()
          bodiesCtx.stroke()
          bodiesCtx.closePath()
          bodiesCtx.setGlobalAlpha(1)

        }


        drawBloodSlider(p, tank)

        viewCtx.beginPath()
        val namePosition = (p + Point(0, 5)) * canvasUnit
        viewCtx.setFill("#006699")
        viewCtx.setTextAlign("center")
        viewCtx.setFont("楷体", "normal", 2 * canvasUnit)
        viewCtx.setLineWidth(2)
        viewCtx.fillText(s"${tank.name}", namePosition.x, namePosition.y, 20 * canvasUnit)
        viewCtx.closePath()
        if(isBot){
          bodiesCtx.beginPath()
          bodiesCtx.setFill("#006699")
          bodiesCtx.setTextAlign("center")
          bodiesCtx.setFont("楷体", "normal", 2 * canvasUnit )
          bodiesCtx.setLineWidth(2)
          bodiesCtx.fillText(s"${tank.name}", namePosition.x /layerCanvasUnit, namePosition.y /layerCanvasUnit, 20 * canvasUnit /layerCanvasUnit)
          bodiesCtx.closePath()

        }

        drawTankBullet(p, tank)
        drawTankStar(p, tank)
      }
    }
  }

  def drawBloodSlider(tankPosition:Point, tank:TankClientImpl) = {
    val num = tank.getMaxBlood / 20
    val sliderLength = 2f * tank.getRadius
    val greyLength = 0.3f * sliderLength
    val width = (sliderLength - greyLength) / num
    val sliderPositions = tank.getSliderPositionByBloodLevel(num,sliderLength,width,greyLength).map(t => (t + tankPosition) * canvasUnit)
    viewCtx.beginPath()
    viewCtx.setLineCap("butt")
    viewCtx.setLineJoin("miter")
    viewCtx.setLineWidth(0.5 * canvasUnit)
    viewCtx.setStrokeStyle("#BEBEBE")
    viewCtx.moveTo(sliderPositions.last.x,sliderPositions.last.y)
    viewCtx.lineTo(sliderPositions.head.x,sliderPositions.head.y)
    viewCtx.stroke()
    viewCtx.closePath()
    for(i <- Range(1 ,sliderPositions.length,2)){
      viewCtx.beginPath()
      viewCtx.setLineWidth(0.5 * canvasUnit)
      if((i+1) / 2 <= 1f * tank.getCurBlood / 20){
        viewCtx.setStrokeStyle("rgb(255,0,0)")
        viewCtx.moveTo(sliderPositions(i-1).x,sliderPositions(i-1).y)
        viewCtx.lineTo(sliderPositions(i).x,sliderPositions(i).y)
        viewCtx.stroke()
      }
      if(tank.getCurBlood / 20 < 1f * tank.getCurBlood / 20 && (i+1) / 2 == tank.getCurBlood / 20 + 1){
        viewCtx.setStrokeStyle("rgb(255,0,0)")
        viewCtx.moveTo(sliderPositions(i-1).x,sliderPositions(i-1).y)
        viewCtx.lineTo(sliderPositions(i-1).x + 1f * (tank.getCurBlood - tank.getCurBlood / 20 * 20) / 20 * width * canvasUnit,sliderPositions(i-1).y)
        viewCtx.stroke()
      }

      viewCtx.closePath()
    }

    if(isBot){
      bodiesCtx.beginPath()
      bodiesCtx.setLineCap("butt")
      bodiesCtx.setLineJoin("miter")
      bodiesCtx.setLineWidth(0.5 * canvasUnit /layerCanvasUnit)
      bodiesCtx.setStrokeStyle("#BEBEBE")
      bodiesCtx.moveTo(sliderPositions.last.x /layerCanvasUnit,sliderPositions.last.y /layerCanvasUnit)
      bodiesCtx.lineTo(sliderPositions.head.x /layerCanvasUnit,sliderPositions.head.y /layerCanvasUnit)
      bodiesCtx.stroke()
      bodiesCtx.closePath()

      for(i <- Range(1 ,sliderPositions.length,2)){
        bodiesCtx.beginPath()
        bodiesCtx.setLineWidth(0.5 * canvasUnit  /layerCanvasUnit)


        if((i+1) / 2 <= 1f * tank.getCurBlood / 20){
          bodiesCtx.setStrokeStyle("rgb(255,0,0)")
          bodiesCtx.moveTo(sliderPositions(i-1).x /layerCanvasUnit ,sliderPositions(i-1).y  /layerCanvasUnit)
          bodiesCtx.lineTo(sliderPositions(i).x  /layerCanvasUnit,sliderPositions(i).y /layerCanvasUnit )
          bodiesCtx.stroke()
        }
        if(tank.getCurBlood / 20 < 1f * tank.getCurBlood / 20 && (i+1) / 2 == tank.getCurBlood / 20 + 1){
          bodiesCtx.setStrokeStyle("rgb(255,0,0)")
          bodiesCtx.moveTo(sliderPositions(i-1).x  /layerCanvasUnit,sliderPositions(i-1).y  /layerCanvasUnit)
          bodiesCtx.lineTo(sliderPositions(i-1).x  /layerCanvasUnit+ 1f * (tank.getCurBlood - tank.getCurBlood / 20 * 20) / 20 * width * canvasUnit /layerCanvasUnit ,sliderPositions(i-1).y /layerCanvasUnit )
          bodiesCtx.stroke()
        }
        bodiesCtx.closePath()

      }
    }

  }

  def drawTankBullet(tankPosition:Point, tank:TankClientImpl) = {
    var left = tank.bulletMaxCapacity * SmallBullet.width / 2 * -1

    (1 to tank.getCurBulletNum).foreach{ indedx =>
      val smallBulletPosition = tankPosition + Point(left, -9)
      val img = fillBulletImg
      viewCtx.drawImage(img, (smallBulletPosition.x - SmallBullet.width / 2) * canvasUnit,
        (smallBulletPosition.y - SmallBullet.height / 2) * canvasUnit,
        Some(SmallBullet.width * canvasUnit, SmallBullet.height * canvasUnit))
      if(isBot){
        bodiesCtx.drawImage(img, (smallBulletPosition.x - SmallBullet.width / 2) * canvasUnit /layerCanvasUnit,
          (smallBulletPosition.y - SmallBullet.height / 2) * canvasUnit /layerCanvasUnit,
          Some(SmallBullet.width * canvasUnit /layerCanvasUnit , SmallBullet.height * canvasUnit  /layerCanvasUnit))

      }
      left = left + SmallBullet.width
    }
    viewCtx.setGlobalAlpha(0.5)

    (tank.getCurBulletNum + 1 to tank.bulletMaxCapacity).foreach{ indedx =>
      val smallBulletPosition = tankPosition + Point(left, -9)
      val img = emptyBulletImg
      viewCtx.drawImage(img, (smallBulletPosition.x - SmallBullet.width / 2) * canvasUnit,
        (smallBulletPosition.y - SmallBullet.height / 2) * canvasUnit,
        Some(SmallBullet.width * canvasUnit, SmallBullet.height * canvasUnit))
      if(isBot){
        bodiesCtx.drawImage(img, (smallBulletPosition.x - SmallBullet.width / 2) * canvasUnit /layerCanvasUnit,
          (smallBulletPosition.y - SmallBullet.height / 2) * canvasUnit /layerCanvasUnit,
          Some(SmallBullet.width * canvasUnit  /layerCanvasUnit, SmallBullet.height * canvasUnit  /layerCanvasUnit))
      }
      left = left + SmallBullet.width

    }
    viewCtx.setGlobalAlpha(1)

  }

  def drawTankStar(tankPosition:Point, tank:TankClientImpl) = {
    val firstStarPos = Point(tank.getRadius+TankStar.interval, -(tank.getRadius+TankStar.interval))
    val endStarNum = math.min(TankStar.maxNum ,tank.killTankNum)
//    val endStarDigits = endStarNum / 10
//    val endStarUnits = endStarNum - (endStarDigits * 10)

//    (0 until endStarDigits).foreach{idx =>
//      val starPos = Point(tankPosition.x-1.1f,tankPosition.y-1.1f) + firstStarPos.rotate(idx * Pi/10)
//      val img = tankSunImg
//      ctx.drawImage(img, starPos.x * canvasUnit,
//        starPos.y * canvasUnit,
//        Some(TankStar.width * canvasUnit, TankStar.height * canvasUnit))
//    }
    (0 until endStarNum).foreach{idx =>
      val starPos = Point(tankPosition.x-1.1f,tankPosition.y-1.1f) + firstStarPos.rotate(idx * Pi/10)
      val img = tankStarImg
      viewCtx.drawImage(img, starPos.x * canvasUnit,
        starPos.y * canvasUnit,
        Some(TankStar.width * canvasUnit, TankStar.height * canvasUnit))
      if(isBot){
        bodiesCtx.drawImage(img, starPos.x * canvasUnit /layerCanvasUnit,
          starPos.y * canvasUnit /layerCanvasUnit,
          Some(TankStar.width * canvasUnit  /layerCanvasUnit, TankStar.height * canvasUnit /layerCanvasUnit))
      }
    }

  }

  private def generateMyTankInfoCanvas(tank:TankClientImpl,supportLiveLimit:Boolean):Any = {
    myTankInfoCacheMap.clear()
    val canvasCache = drawFrame.createCanvas(30 * canvasUnit, 20 * canvasUnit)
    val ctxCache = canvasCache.getCtx
    drawLevel(tank.getBloodLevel,config.getTankBloodMaxLevel(),"血量等级",Point(5,20 - 12) * canvasUnit,20 * canvasUnit,"#FF3030",ctxCache)
    drawLevel(tank.getSpeedLevel,config.getTankSpeedMaxLevel(),"速度等级",Point(5,20 - 8) * canvasUnit,20 * canvasUnit,"#66CD00",ctxCache)
    drawLevel(tank.getBulletLevel,config.getBulletMaxLevel(),"炮弹等级",Point(5,20 - 4) * canvasUnit,20 * canvasUnit,"#1C86EE",ctxCache)
    if(supportLiveLimit){
      drawLevel(tank.lives.toByte,config.getTankLivesLimit.toByte,s"生命值",Point(5,20-16) * canvasUnit,20 * canvasUnit,"#FFA500",ctxCache)
    }
    canvasCache.change2Image()
  }

  protected def drawMyTankInfo(tank:TankClientImpl,supportLiveLimit:Boolean) = {
    val cache = myTankInfoCacheMap.getOrElseUpdate((tank.getBloodLevel,tank.getSpeedLevel,tank.getBulletLevel),generateMyTankInfoCanvas(tank,supportLiveLimit))
    viewCtx.drawImage(cache,0,2 * canvasUnit)
    if(isBot){
      statusCtx.drawImage(cache,0,2 * canvasUnit)

    }
  }

  def drawLevel(level:Byte,maxLevel:Byte,name:String,start:Point,length:Float,color:String, context:MiddleContext) = {
    println(s"${level}")
    context.setStrokeStyle("#4D4D4D")
    context.setLineCap("round")
    context.setLineWidth(3 * canvasUnit)
    context.beginPath()
    context.moveTo(start.x,start.y)
    context.lineTo(start.x+length,start.y)
    context.stroke()
    context.closePath()

    context.setLineWidth(2.2 * canvasUnit)
    context.setStrokeStyle(color)
    if(level == maxLevel){
      context.beginPath()
      context.moveTo(start.x + length,start.y)
      context.lineTo(start.x + length,start.y)
      context.stroke()
      context.closePath()
    }

    if(level >= 1){
      context.beginPath()
      context.moveTo(start.x,start.y)
      context.lineTo(start.x,start.y)
      context.stroke()
      context.closePath()

      context.setLineCap("butt")
      (0 until level).foreach{ index =>
        context.beginPath()
        context.moveTo(start.x + index * (length / maxLevel) + 2,start.y)
        context.lineTo(start.x + (index + 1) * (length / maxLevel) - 2,start.y)
        context.stroke()
        context.closePath()
      }
    }
    context.setFont("Arial", "bold", 1.8 * canvasUnit)
    context.setTextAlign("center")
    context.setTextBaseline("middle")
    context.setFill("#FCFCFC")
    context.fillText(name, start.x + length / 2, start.y)
  }


  def drawCurMedicalNum(tank:TankClientImpl) = {
    viewCtx.beginPath()
    viewCtx.setStrokeStyle("rgb(0,0,0)")
    viewCtx.setTextAlign("left")
    viewCtx.setFont("隶书", "bold", 1.8 * canvasUnit)
    viewCtx.setLineWidth(1)
    viewCtx.fillText(s"血包${("                       ").take(30)}(按E键使用)", 4.5*canvasUnit,5.5  * canvasUnit , 30 * canvasUnit)

    if(isBot){
      statusCtx.setStrokeStyle("rgb(0,0,0)")
      statusCtx.setTextAlign("left")
      statusCtx.setFont("隶书", "bold", 1.8 * canvasUnit)
      statusCtx.setLineWidth(1)
      statusCtx.fillText(s"血包${("                       ").take(30)}(按E键使用)", 4.5*canvasUnit,5.5  * canvasUnit , 30 * canvasUnit)

    }

    val medicalNum = tank.medicalNumOpt match{
      case Some(num) =>num
      case None =>0
    }
    (1 to medicalNum).foreach{ index =>
      val smallMedicalPosition = (Point(8,6) + Point(index * config.propRadius * 3 / 2,0))
      val img = fillMedicalImg
      viewCtx.drawImage(img, (smallMedicalPosition.x - config.propRadius) * canvasUnit - 5,
        (smallMedicalPosition.y - config.propRadius) * canvasUnit - 7,
        Some(1.5 * config.propRadius * canvasUnit, 1.5 * config.propRadius * canvasUnit))
      if(isBot){
        statusCtx.drawImage(img, (smallMedicalPosition.x - config.propRadius) * canvasUnit - 5,
          (smallMedicalPosition.y - config.propRadius) * canvasUnit - 7,
          Some(1.5 * config.propRadius * canvasUnit, 1.5 * config.propRadius * canvasUnit))

      }
    }
    viewCtx.setGlobalAlpha(0.5)

    (medicalNum + 1 to config.getTankMedicalLimit).foreach{ index =>
      val smallMedicalPosition = (Point(8,6) + Point(index * config.propRadius * 3 / 2,0))
      val img = emptyMedicalImg
      viewCtx.drawImage(img, (smallMedicalPosition.x - config.propRadius) * canvasUnit - 5,
        (smallMedicalPosition.y - config.propRadius) * canvasUnit - 7,
        Some(1.5 * config.propRadius * canvasUnit, 1.5 * config.propRadius * canvasUnit))
      if(isBot){
        statusCtx.drawImage(img, (smallMedicalPosition.x - config.propRadius) * canvasUnit - 5,
          (smallMedicalPosition.y - config.propRadius) * canvasUnit - 7,
          Some(1.5 * config.propRadius * canvasUnit, 1.5 * config.propRadius * canvasUnit))

      }
    }
    viewCtx.setGlobalAlpha(1)

  }




}
