package com.neo.sk.tank.shared.game.view

import com.neo.sk.tank.shared.game.{GameContainerClientImpl, TankClientImpl}
import com.neo.sk.tank.shared.model.Constants.{InvincibleSize, SmallBullet, TankStar}
import com.neo.sk.tank.shared.model.Point
import com.neo.sk.tank.shared.util.canvas.MiddleContext

import scala.collection.mutable

/**
  * Created by hongruying on 2018/8/29
  */
trait TankDrawUtil{ this:GameContainerClientImpl =>

  private val myTankInfoCacheMap = mutable.HashMap[(Byte,Byte,Byte), Any]()
  private var canvasBoundary:Point=canvasSize

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
      val p = tank.getPosition4Animation(boundary, quadTree, offsetTime) + offset
      if (p.in(view, Point(t.getRadius * 4, t.getRadius * 4))) {
        if (tankAttackedAnimationMap.contains(tank.tankId)) {
          if (tankAttackedAnimationMap(tank.tankId) <= 0) tankAttackedAnimationMap.remove(tank.tankId)
          else tankAttackedAnimationMap.put(tank.tankId, tankAttackedAnimationMap(tank.tankId) - 1)
          ctx.setGlobalAlpha(0.5)
        }

        //------------------------绘制炮筒--------------------------#
        val gunPositionList = tank.getGunPositions4Animation().map(t => (t + p) * canvasUnit)
        ctx.beginPath()
        ctx.moveTo(gunPositionList.last.x, gunPositionList.last.y)
        gunPositionList.foreach(t => ctx.lineTo(t.x, t.y))
        ctx.setFill("#7A7A7A")
        ctx.setStrokeStyle("#636363")
        ctx.fill()
        ctx.setLineWidth(0.4 * canvasUnit)
        ctx.stroke()
        ctx.closePath()
        //----------------------------绘制坦克---------------------#
        if (tank.getInvincibleState) {
          ctx.beginPath()
          ctx.setFill("rgba(128, 100, 162, 0.2)")
          val centerX = p.x * canvasUnit
          val centerY = p.y * canvasUnit
          val radius = InvincibleSize.r * canvasUnit
          val startAngle = 0
          val lengthAngle = 360
          ctx.arc(centerX.toFloat, centerY.toFloat, radius, startAngle.toFloat, lengthAngle.toFloat)
          ctx.fill()
          ctx.closePath()
        }
        ctx.beginPath()
        ctx.setLineWidth( 0.4 * canvasUnit)
        ctx.setStrokeStyle("#636363")
        val centerX = p.x * canvasUnit
        val centerY = p.y * canvasUnit
        val radius =  tank.getRadius * canvasUnit
        val startAngle = 0
        val lengthAngle = 360
        ctx.arc(centerX.toFloat, centerY.toFloat, radius, startAngle.toFloat, lengthAngle.toFloat)
        val tankColor = tank.getTankColor()
        ctx.setFill(tankColor)
        ctx.fill()
        ctx.stroke()
        ctx.closePath()
        ctx.setGlobalAlpha(1)


        drawBloodSlider(p, tank)

        ctx.beginPath()
        val namePosition = (p + Point(0, 5)) * canvasUnit
        ctx.setFill("#006699")
        ctx.setTextAlign("center")
        ctx.setFont("楷体", "normal", 2 * canvasUnit)
        ctx.setLineWidth(2)
        ctx.fillText(s"${tank.name}", namePosition.x, namePosition.y, 20 * canvasUnit)
        ctx.closePath()

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
    ctx.beginPath()
    ctx.setLineCap("butt")
    ctx.setLineJoin("miter")
    ctx.setLineWidth(0.5 * canvasUnit)
    ctx.setStrokeStyle("#BEBEBE")
    ctx.moveTo(sliderPositions.last.x,sliderPositions.last.y)
    ctx.lineTo(sliderPositions.head.x,sliderPositions.head.y)
    ctx.stroke()
    ctx.closePath()
    for(i <- Range(1 ,sliderPositions.length,2)){
      ctx.beginPath()
      ctx.setLineWidth(0.5 * canvasUnit)
      if((i+1) / 2 <= 1f * tank.getCurBlood / 20){
        ctx.setStrokeStyle("rgb(255,0,0)")
        ctx.moveTo(sliderPositions(i-1).x,sliderPositions(i-1).y)
        ctx.lineTo(sliderPositions(i).x,sliderPositions(i).y)
        ctx.stroke()
      }
      if(tank.getCurBlood / 20 < 1f * tank.getCurBlood / 20 && (i+1) / 2 == tank.getCurBlood / 20 + 1){
        ctx.setStrokeStyle("rgb(255,0,0)")
        ctx.moveTo(sliderPositions(i-1).x,sliderPositions(i-1).y)
        ctx.lineTo(sliderPositions(i-1).x + 1f * (tank.getCurBlood - tank.getCurBlood / 20 * 20) / 20 * width * canvasUnit,sliderPositions(i-1).y)
        ctx.stroke()
      }

      ctx.closePath()
    }
  }

  def drawTankBullet(tankPosition:Point, tank:TankClientImpl) = {
    var left = tank.bulletMaxCapacity * SmallBullet.width / 2 * -1

    (1 to tank.getCurBulletNum).foreach{ indedx =>
      val smallBulletPosition = tankPosition + Point(left, -9)
      val img = fillBulletImg
      ctx.drawImage(img, (smallBulletPosition.x - SmallBullet.width / 2) * canvasUnit,
        (smallBulletPosition.y - SmallBullet.height / 2) * canvasUnit,
        Some(SmallBullet.width * canvasUnit, SmallBullet.height * canvasUnit))

      left = left + SmallBullet.width
    }
    ctx.setGlobalAlpha(0.5)

    (tank.getCurBulletNum + 1 to tank.bulletMaxCapacity).foreach{ indedx =>
      val smallBulletPosition = tankPosition + Point(left, -9)
      val img = emptyBulletImg
      ctx.drawImage(img, (smallBulletPosition.x - SmallBullet.width / 2) * canvasUnit,
        (smallBulletPosition.y - SmallBullet.height / 2) * canvasUnit,
        Some(SmallBullet.width * canvasUnit, SmallBullet.height * canvasUnit))
      left = left + SmallBullet.width

    }
    ctx.setGlobalAlpha(1)

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
      ctx.drawImage(img, starPos.x * canvasUnit,
        starPos.y * canvasUnit,
        Some(TankStar.width * canvasUnit, TankStar.height * canvasUnit))
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
    ctx.drawImage(cache,0,2 * canvasUnit)
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
    ctx.beginPath()
    ctx.setStrokeStyle("rgb(0,0,0)")
    ctx.setTextAlign("left")
    ctx.setFont("隶书", "bold", 1.8 * canvasUnit)
    ctx.setLineWidth(1)
    ctx.fillText(s"血包${("                       ").take(30)}(按E键使用)", 4.5*canvasUnit,5.5  * canvasUnit , 30 * canvasUnit)
    val medicalNum = tank.medicalNumOpt match{
      case Some(num) =>num
      case None =>0
    }
    (1 to medicalNum).foreach{ index =>
      val smallMedicalPosition = (Point(8,6) + Point(index * config.propRadius * 3 / 2,0))
      val img = fillMedicalImg
      ctx.drawImage(img, (smallMedicalPosition.x - config.propRadius) * canvasUnit - 5,
        (smallMedicalPosition.y - config.propRadius) * canvasUnit - 7,
        Some(1.5 * config.propRadius * canvasUnit, 1.5 * config.propRadius * canvasUnit))
    }
    ctx.setGlobalAlpha(0.5)

    (medicalNum + 1 to config.getTankMedicalLimit).foreach{ index =>
      val smallMedicalPosition = (Point(8,6) + Point(index * config.propRadius * 3 / 2,0))
      val img = emptyMedicalImg
      ctx.drawImage(img, (smallMedicalPosition.x - config.propRadius) * canvasUnit - 5,
        (smallMedicalPosition.y - config.propRadius) * canvasUnit - 7,
        Some(1.5 * config.propRadius * canvasUnit, 1.5 * config.propRadius * canvasUnit))
    }
    ctx.setGlobalAlpha(1)

  }




}
