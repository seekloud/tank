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

import org.seekloud.tank.shared.`object`.Tank
import org.seekloud.tank.shared.game.{GameContainerClientImpl, TankClientImpl}
import org.seekloud.tank.shared.model.Constants.{InvincibleSize, SmallBullet, TankStar}
import org.seekloud.tank.shared.model.Point
import org.seekloud.tank.shared.util.canvas.MiddleContext

import scala.collection.mutable

/**
  * Created by hongruying on 2018/8/29
  */
trait TankDrawUtil {
  this: GameContainerClientImpl =>

  private val myTankInfoCacheMap = mutable.HashMap[(Byte, Byte, Byte), Any]()

  private val fillBulletImg = drawFrame.createImage("/img/子弹初始重构.png")
  private val emptyBulletImg = drawFrame.createImage("/img/子弹消失重构.png")
  private val fillMedicalImg = drawFrame.createImage("/img/yiliao.png")
  private val emptyMedicalImg = drawFrame.createImage("/img/huiyiliao.png")
  private val tankStarImg = drawFrame.createImage("/img/star.png")


  private val Pi = 3.14f


  def updateTankSize(canvasSize: Point) = {
    myTankInfoCacheMap.clear()
  }


  protected def drawTankList(offset: Point, offsetTime: Long, view: Point) = {
    tankMap.values.foreach { t =>
      val tank = t.asInstanceOf[TankClientImpl]
      val p = tank.getPosition4Animation(boundary, quadTree, offsetTime, systemFrame) + offset
      if (p.in(view, Point(t.getRadius * 4, t.getRadius * 4))) {
        drawTankGun(p,tank,canvasUnit,viewCtx)
        drawTank(p,tank,tank.getTankColor(),canvasUnit,viewCtx)
        drawBloodSlider(p, tank, canvasUnit, viewCtx)
        drawTankName(p, tank.name, canvasUnit, viewCtx)
        drawTankBullet(p, tank, canvasUnit, viewCtx)
        drawTankStar(p, tank, canvasUnit, viewCtx)
      }
    }
  }

  protected def drawTankGun(p:Point,tank:TankClientImpl,unit:Int,ctx:MiddleContext)={
    if (tankAttackedAnimationMap.contains(tank.tankId)) {
      if (tankAttackedAnimationMap(tank.tankId) <= 0) tankAttackedAnimationMap.remove(tank.tankId)
      else tankAttackedAnimationMap.put(tank.tankId, tankAttackedAnimationMap(tank.tankId) - 1)
      ctx.setGlobalAlpha(0.5)
    }
    //------------------------绘制炮筒--------------------------#
    val gunPositionList = tank.getGunPositions4Animation().map(t => (t + p) * unit)
    ctx.beginPath()
    ctx.moveTo(gunPositionList.last.x, gunPositionList.last.y)
    gunPositionList.foreach(t => ctx.lineTo(t.x, t.y))
    ctx.setFill("#7A7A7A")
    ctx.setStrokeStyle("#636363")
    ctx.fill()
    ctx.setLineWidth(0.4 * unit)
    ctx.stroke()
    ctx.closePath()
    //----------------------------绘制坦克---------------------#
    if (tank.getInvincibleState) {
      ctx.beginPath()
      ctx.setFill("rgba(128, 100, 162, 0.2)")
      val centerX = p.x * unit
      val centerY = p.y * unit
      val radius = InvincibleSize.r * unit
      val startAngle = 0
      val lengthAngle = 360
      ctx.arc(centerX.toFloat, centerY.toFloat, radius, startAngle.toFloat, lengthAngle.toFloat)
      ctx.fill()
      ctx.closePath()
    }
  }

  protected def drawTank(p:Point,tank:TankClientImpl,tankColor:String,unit:Int,ctx:MiddleContext)={
    ctx.beginPath()
    ctx.setLineWidth(0.4 * unit)
    ctx.setStrokeStyle("#636363")
    val centerX = p.x * unit
    val centerY = p.y * unit
    val radius = tank.getRadius * unit
    val startAngle = 0
    val lengthAngle = 360
    ctx.arc(centerX.toFloat, centerY.toFloat, radius, startAngle.toFloat, lengthAngle.toFloat)
    ctx.setFill(tankColor)
    ctx.fill()
    ctx.stroke()
    ctx.closePath()
    ctx.setGlobalAlpha(1)
  }

  protected def drawTankName(p: Point, tankName: String, unit: Int, ctx: MiddleContext) = {
    ctx.beginPath()
    val namePosition = (p + Point(0, 5)) * unit
    ctx.setFill("#006699")
    ctx.setTextAlign("center")
    ctx.setFont("楷体", "normal", 2 * unit)
    ctx.setLineWidth(2)
    ctx.fillText(s"${tankName}", namePosition.x, namePosition.y, 20 * unit)
    ctx.closePath()
  }

  protected def drawBloodSlider(tankPosition: Point, tank: TankClientImpl, unit: Int, ctx: MiddleContext) = {
    val num = tank.getMaxBlood / 20
    val sliderLength = 2f * tank.getRadius
    val greyLength = 0.3f * sliderLength
    val width = (sliderLength - greyLength) / num
    val sliderPositions = tank.getSliderPositionByBloodLevel(num, sliderLength, width, greyLength).map(t => (t + tankPosition) * unit)
    ctx.beginPath()
    ctx.setLineCap("butt")
    ctx.setLineJoin("miter")
    ctx.setLineWidth(0.5 * unit)
    ctx.setStrokeStyle("#BEBEBE")
    ctx.moveTo(sliderPositions.last.x, sliderPositions.last.y)
    ctx.lineTo(sliderPositions.head.x, sliderPositions.head.y)
    ctx.stroke()
    ctx.closePath()
    for (i <- Range(1, sliderPositions.length, 2)) {
      ctx.beginPath()
      ctx.setLineWidth(0.5 * unit)
      if ((i + 1) / 2 <= 1f * tank.getCurBlood / 20) {
        ctx.setStrokeStyle("rgb(255,0,0)")
        ctx.moveTo(sliderPositions(i - 1).x, sliderPositions(i - 1).y)
        ctx.lineTo(sliderPositions(i).x, sliderPositions(i).y)
        ctx.stroke()
      }
      if (tank.getCurBlood / 20 < 1f * tank.getCurBlood / 20 && (i + 1) / 2 == tank.getCurBlood / 20 + 1) {
        ctx.setStrokeStyle("rgb(255,0,0)")
        ctx.moveTo(sliderPositions(i - 1).x, sliderPositions(i - 1).y)
        ctx.lineTo(sliderPositions(i - 1).x + 1f * (tank.getCurBlood - tank.getCurBlood / 20 * 20) / 20 * width * unit, sliderPositions(i - 1).y)
        ctx.stroke()
      }
      ctx.closePath()
    }

  }

  protected def drawTankBullet(tankPosition: Point, tank: TankClientImpl, unit: Int, ctx: MiddleContext) = {
    var left = tank.bulletMaxCapacity * SmallBullet.width / 2 * -1

    (1 to tank.getCurBulletNum).foreach { i =>
      val smallBulletPosition = tankPosition + Point(left, -9)
      val img = fillBulletImg
      ctx.drawImage(img, (smallBulletPosition.x - SmallBullet.width / 2) * unit,
        (smallBulletPosition.y - SmallBullet.height / 2) * unit,
        Some(SmallBullet.width * unit, SmallBullet.height * unit))
      left = left + SmallBullet.width
    }
    ctx.setGlobalAlpha(0.5)

    (tank.getCurBulletNum + 1 to tank.bulletMaxCapacity).foreach { indedx =>
      val smallBulletPosition = tankPosition + Point(left, -9)
      val img = emptyBulletImg
      ctx.drawImage(img, (smallBulletPosition.x - SmallBullet.width / 2) * unit,
        (smallBulletPosition.y - SmallBullet.height / 2) * unit,
        Some(SmallBullet.width * unit, SmallBullet.height * unit))
      left = left + SmallBullet.width

    }
    ctx.setGlobalAlpha(1)

  }

  protected def drawTankStar(tankPosition: Point, tank: TankClientImpl, unit: Int, ctx: MiddleContext) = {
    val firstStarPos = Point(tank.getRadius + TankStar.interval, -(tank.getRadius + TankStar.interval))
    val endStarNum = math.min(TankStar.maxNum, tank.killTankNum)
    (0 until endStarNum).foreach { idx =>
      val starPos = Point(tankPosition.x - 1.1f, tankPosition.y - 1.1f) + firstStarPos.rotate(idx * Pi / 10)
      val img = tankStarImg
      ctx.drawImage(img, starPos.x * unit,
        starPos.y * unit,
        Some(TankStar.width * unit, TankStar.height * unit))
    }

  }

  private def generateMyTankInfoCanvas(tank: TankClientImpl, supportLiveLimit: Boolean): Any = {
    myTankInfoCacheMap.clear()
    val canvasCache = drawFrame.createCanvas(30 * canvasUnit, 20 * canvasUnit)
    val ctxCache = canvasCache.getCtx
    drawLevel(tank.getBloodLevel, config.getTankBloodMaxLevel(), "血量等级", Point(5, 20 - 12) * canvasUnit, 20 * canvasUnit, "#FF3030",canvasUnit, ctxCache)
    drawLevel(tank.getSpeedLevel, config.getTankSpeedMaxLevel(), "速度等级", Point(5, 20 - 8) * canvasUnit, 20 * canvasUnit, "#66CD00", canvasUnit,ctxCache)
    drawLevel(tank.getBulletLevel, config.getBulletMaxLevel(), "炮弹等级", Point(5, 20 - 4) * canvasUnit, 20 * canvasUnit, "#1C86EE",canvasUnit, ctxCache)
    if (supportLiveLimit) {
      drawLevel(tank.lives.toByte, config.getTankLivesLimit.toByte, s"生命值", Point(5, 20 - 16) * canvasUnit, 20 * canvasUnit, "#FFA500",canvasUnit, ctxCache)
    }
    canvasCache.change2Image()
  }

  protected def drawMyTankInfo(tank: TankClientImpl, supportLiveLimit: Boolean) = {
    val cache = myTankInfoCacheMap.getOrElseUpdate((tank.getBloodLevel, tank.getSpeedLevel, tank.getBulletLevel), generateMyTankInfoCanvas(tank, supportLiveLimit))
    viewCtx.drawImage(cache, 0, 2 * canvasUnit)
  }

  protected def drawLevel(level: Byte, maxLevel: Byte, name: String, start: Point, length: Float, color: String,unit:Int, context: MiddleContext) = {
    context.setStrokeStyle("#4D4D4D")
    context.setLineCap("round")
    context.setLineWidth(3 * unit)
    context.beginPath()
    context.moveTo(start.x, start.y)
    context.lineTo(start.x + length, start.y)
    context.stroke()
    context.closePath()

    context.setLineWidth(2.2 * unit)
    context.setStrokeStyle(color)
    if (level == maxLevel) {
      context.beginPath()
      context.moveTo(start.x + length, start.y)
      context.lineTo(start.x + length, start.y)
      context.stroke()
      context.closePath()
    }

    if (level >= 1) {
      context.beginPath()
      context.moveTo(start.x, start.y)
      context.lineTo(start.x, start.y)
      context.stroke()
      context.closePath()

      context.setLineCap("butt")
      (0 until level).foreach { index =>
        context.beginPath()
        context.moveTo(start.x + index * (length / maxLevel) + 2, start.y)
        context.lineTo(start.x + (index + 1) * (length / maxLevel) - 2, start.y)
        context.stroke()
        context.closePath()
      }
    }
    context.setFont("Arial", "bold", 1.8 * unit)
    context.setTextAlign("center")
    context.setTextBaseline("middle")
    context.setFill("#FCFCFC")
    context.fillText(name, start.x + length / 2, start.y)
  }

  protected def drawCurMedicalNum(tank: TankClientImpl) = {
    viewCtx.beginPath()
    viewCtx.setStrokeStyle("rgb(0,0,0)")
    viewCtx.setTextAlign("left")
    viewCtx.setFont("隶书", "bold", 1.8 * canvasUnit)
    viewCtx.setLineWidth(1)
    viewCtx.fillText(s"血包${("                       ").take(30)}(按E键使用)", 4.5 * canvasUnit, 5.5 * canvasUnit, 30 * canvasUnit)

    val medicalNum = tank.medicalNumOpt match {
      case Some(num) => num
      case None => 0
    }
    (1 to medicalNum).foreach { index =>
      val smallMedicalPosition = (Point(8, 6) + Point(index * config.propRadius * 3 / 2, 0))
      val img = fillMedicalImg
      viewCtx.drawImage(img, (smallMedicalPosition.x - config.propRadius) * canvasUnit - 5,
        (smallMedicalPosition.y - config.propRadius) * canvasUnit - 7,
        Some(1.5 * config.propRadius * canvasUnit, 1.5 * config.propRadius * canvasUnit))
    }
    viewCtx.setGlobalAlpha(0.5)

    (medicalNum + 1 to config.getTankMedicalLimit).foreach { index =>
      val smallMedicalPosition = (Point(8, 6) + Point(index * config.propRadius * 3 / 2, 0))
      val img = emptyMedicalImg
      viewCtx.drawImage(img, (smallMedicalPosition.x - config.propRadius) * canvasUnit - 5,
        (smallMedicalPosition.y - config.propRadius) * canvasUnit - 7,
        Some(1.5 * config.propRadius * canvasUnit, 1.5 * config.propRadius * canvasUnit))
    }
    viewCtx.setGlobalAlpha(1)

  }


}
