package com.neo.sk.tank.game.draw

import com.neo.sk.tank.App
import com.neo.sk.tank.game.GameContainerClientImpl
import com.neo.sk.tank.shared.`object`.TankImpl
import com.neo.sk.tank.shared.model.Constants.{InvincibleSize, SmallBullet}
import com.neo.sk.tank.shared.model.Point
import javafx.geometry.VPos
import javafx.scene.SnapshotParameters
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import javafx.scene.paint.Color
import javafx.scene.shape.{StrokeLineCap, StrokeLineJoin}
import javafx.scene.text.{Font, FontPosture, FontWeight, TextAlignment}

import scala.collection.mutable

/**
  * Created by hongruying on 2018/8/29
  */
trait TankDrawUtil{ this:GameContainerClientImpl =>


  //fixme 将此处map暴露给子类
  private val myTankInfoCacheMap = mutable.HashMap[(Byte,Byte,Byte), Canvas]()
  private var canvasBoundary:Point=canvasSize

  private val fillBulletImg = new Image(App.getClass.getResourceAsStream("/img/子弹初始重构.png"))
  private val emptyBulletImg = new Image(App.getClass.getResourceAsStream("/img/子弹消失重构.png"))
  private val fillMedicalImg = new Image(App.getClass.getResourceAsStream("/img/yiliao.png"))
  private val emptyMedicalImg = new Image(App.getClass.getResourceAsStream("/img/huiyiliao.png"))


  def updateTankSize(canvasSize:Point)={
    myTankInfoCacheMap.clear()
    canvasBoundary=canvasSize
  }


  protected def drawTank(offset:Point, offsetTime:Long, view:Point) = {
    tankMap.values.foreach { t =>
      val tank = t.asInstanceOf[TankImpl]
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
        ctx.setFill(Color.web("#7A7A7A"))
        ctx.setStroke(Color.web("#636363"))
        ctx.fill()
        ctx.setLineWidth(0.4 * canvasUnit)
        ctx.stroke()
        ctx.closePath()
        //----------------------------绘制坦克---------------------#
        if (tank.getInvincibleState) {
          ctx.beginPath()
          ctx.setFill(Color.rgb(128, 100, 162, 0.2))
          val centerX = p.x * canvasUnit
          val centerY = p.y * canvasUnit
          val radiusX =  InvincibleSize.r * canvasUnit
          val radiusY =  InvincibleSize.r * canvasUnit
          val startAngle = 0
          val lengthAngle = 360
          ctx.arc(centerX.toFloat, centerY.toFloat, radiusX.toFloat, radiusY.toFloat, startAngle.toFloat, lengthAngle.toFloat)
          ctx.fill()
          ctx.closePath()
        }
        ctx.beginPath()
        ctx.setLineWidth( 0.4 * canvasUnit)
        ctx.setStroke(Color.web("#636363"))
        val centerX = p.x * canvasUnit
        val centerY = p.y * canvasUnit
        val radiusX =  tank.getRadius * canvasUnit
        val radiusY =  tank.getRadius * canvasUnit
        val startAngle = 0
        val lengthAngle = 360
        ctx.arc(centerX.toFloat, centerY.toFloat, radiusX.toFloat, radiusY.toFloat, startAngle.toFloat, lengthAngle.toFloat)
        val tankColor = tank.getTankColor()
        ctx.setFill(Color.web(tankColor))
        ctx.fill()
        ctx.stroke()
        ctx.closePath()
        ctx.setGlobalAlpha(1)


        drawBloodSlider(p, tank)

        ctx.beginPath()
        val namePosition = (p + Point(0, 5)) * canvasUnit
        ctx.setFill(Color.web("#006699"))
        ctx.setTextAlign(TextAlignment.CENTER)
        ctx.setFont(Font.font("楷体", FontWeight.NORMAL, 2 * canvasUnit))
        ctx.setLineWidth(2)
        ctx.fillText(s"${tank.name}", namePosition.x, namePosition.y, 20 * canvasUnit)
        ctx.closePath()

        drawTankBullet(p, tank)
      }
    }
  }

  def drawBloodSlider(tankPosition:Point, tank:TankImpl) = {
    val num = tank.getMaxBlood / 20
    val sliderLength = 2f * tank.getRadius
    val greyLength = 0.3f * sliderLength
    val width = (sliderLength - greyLength) / num
    val sliderPositions = tank.getSliderPositionByBloodLevel(num,sliderLength,width,greyLength).map(t => (t + tankPosition) * canvasUnit)
    ctx.beginPath()
    ctx.setLineCap(StrokeLineCap.BUTT)
    ctx.setLineJoin(StrokeLineJoin.MITER)
    ctx.setLineWidth(0.5 * canvasUnit)
    ctx.setStroke(Color.web("#BEBEBE"))
    ctx.moveTo(sliderPositions.last.x,sliderPositions.last.y)
    ctx.lineTo(sliderPositions.head.x,sliderPositions.head.y)
    ctx.stroke()
    ctx.closePath()
    for(i <- Range(1 ,sliderPositions.length,2)){
      ctx.beginPath()
      ctx.setLineWidth(0.5 * canvasUnit)
      if((i+1) / 2 <= 1f * tank.getCurBlood / 20){
        ctx.setStroke(Color.RED)
        ctx.moveTo(sliderPositions(i-1).x,sliderPositions(i-1).y)
        ctx.lineTo(sliderPositions(i).x,sliderPositions(i).y)
        ctx.stroke()
      }
      if(tank.getCurBlood / 20 < 1f * tank.getCurBlood / 20 && (i+1) / 2 == tank.getCurBlood / 20 + 1){
        ctx.setStroke(Color.RED)
        ctx.moveTo(sliderPositions(i-1).x,sliderPositions(i-1).y)
        ctx.lineTo(sliderPositions(i-1).x + 1f * (tank.getCurBlood - tank.getCurBlood / 20 * 20) / 20 * width * canvasUnit,sliderPositions(i-1).y)
        ctx.stroke()
      }

      ctx.closePath()
    }
  }

  def drawTankBullet(tankPosition:Point, tank:TankImpl) = {
    var left = tank.bulletMaxCapacity * SmallBullet.width / 2 * -1

    (1 to tank.getCurBulletNum).foreach{ indedx =>
      val smallBulletPosition = tankPosition + Point(left, -9)
      val img = fillBulletImg
      ctx.drawImage(img, (smallBulletPosition.x - SmallBullet.width / 2) * canvasUnit,
        (smallBulletPosition.y - SmallBullet.height / 2) * canvasUnit,
        SmallBullet.width * canvasUnit, SmallBullet.height * canvasUnit)

      left = left + SmallBullet.width
    }
    ctx.setGlobalAlpha(0.5)

    (tank.getCurBulletNum + 1 to tank.bulletMaxCapacity).foreach{ indedx =>
      val smallBulletPosition = tankPosition + Point(left, -9)
      val img = emptyBulletImg
      ctx.drawImage(img, (smallBulletPosition.x - SmallBullet.width / 2) * canvasUnit,
        (smallBulletPosition.y - SmallBullet.height / 2) * canvasUnit,
        SmallBullet.width * canvasUnit, SmallBullet.height * canvasUnit)
      left = left + SmallBullet.width

    }
    ctx.setGlobalAlpha(1)

  }

  private def generateMyTankInfoCanvas(tank:TankImpl):Canvas = {
    myTankInfoCacheMap.clear()
    val canvasCache = new Canvas(30 * canvasUnit, 20 * canvasUnit)
    val ctxCache = canvasCache.getGraphicsContext2D
    drawLevel(tank.getBloodLevel,config.getTankBloodMaxLevel(),"血量等级",Point(5,20 - 12) * canvasUnit,20 * canvasUnit,"#FF3030",ctxCache)
    drawLevel(tank.getSpeedLevel,config.getTankSpeedMaxLevel(),"速度等级",Point(5,20 - 8) * canvasUnit,20 * canvasUnit,"#66CD00",ctxCache)
    drawLevel(tank.getBulletLevel,config.getBulletMaxLevel(),"炮弹等级",Point(5,20 - 4) * canvasUnit,20 * canvasUnit,"#1C86EE",ctxCache)
    drawLevel(tank.lives.toByte,config.getTankLivesLimit.toByte,s"生命值",Point(5,20-16) * canvasUnit,20 * canvasUnit,"#FFA500",ctxCache)
    canvasCache
  }

  protected def drawMyTankInfo(tank:TankImpl) = {
    val cache = myTankInfoCacheMap.getOrElseUpdate((tank.getBloodLevel,tank.getSpeedLevel,tank.getBulletLevel),generateMyTankInfoCanvas(tank))
    ctx.drawImage(cache.snapshot(new SnapshotParameters(), null),0,(canvasBoundary.y - 20) * canvasUnit)
  }

  def drawLevel(level:Byte,maxLevel:Byte,name:String,start:Point,length:Float,color:String, context:GraphicsContext = ctx) = {
    ctx.setStroke(Color.web("#4D4D4D"))
    ctx.setLineCap(StrokeLineCap.ROUND)
    context.setLineWidth(3 * canvasUnit)
    context.beginPath()
    context.moveTo(start.x,start.y)
    context.lineTo(start.x+length,start.y)
    context.stroke()
    context.closePath()

    context.setLineWidth(2.2 * canvasUnit)
    ctx.setStroke(Color.web(color))
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

      ctx.setLineCap(StrokeLineCap.BUTT)
      (0 until level).foreach{ index =>
        context.beginPath()
        context.moveTo(start.x + index * (length / maxLevel) + 2,start.y)
        context.lineTo(start.x + (index + 1) * (length / maxLevel) - 2,start.y)
        context.stroke()
        context.closePath()
      }
    }
    ctx.setFont(Font.font("Arial", FontWeight.BOLD, 1.8 * canvasUnit))
    context.setTextAlign(TextAlignment.CENTER)
    context.setTextBaseline(VPos.CENTER)
    ctx.setStroke(Color.web("#FCFCFC"))
    context.fillText(name, start.x + length / 2, start.y)
  }


  def drawCurMedicalNum(tank:TankImpl) = {
    ctx.beginPath()
    ctx.setStroke(Color.BLACK)
    ctx.setTextAlign(TextAlignment.LEFT)
    ctx.setFont(Font.font("隶书", FontWeight.BOLD, 1.8 * canvasUnit))
    ctx.setLineWidth(1)
    ctx.fillText(s"血包${("                       ").take(30)}(按E键使用)", 4.5*canvasUnit,(canvasBoundary.y - 22.5)  * canvasUnit , 30 * canvasUnit)
    val medicalNum = tank.medicalNumOpt match{
      case Some(num) =>num
      case None =>0
    }
    (1 to medicalNum).foreach{ index =>
      val smallMedicalPosition = (Point(8,(canvasBoundary.y - 21)) + Point(index * config.propRadius * 3 / 2,0))
      val img = fillMedicalImg
      ctx.drawImage(img, (smallMedicalPosition.x - config.propRadius) * canvasUnit,
        (smallMedicalPosition.y - config.propRadius) * canvasUnit,
        1.5 * config.propRadius * canvasUnit, 1.5 * config.propRadius * canvasUnit)
    }
    ctx.setGlobalAlpha(0.5)

    (medicalNum + 1 to config.getTankMedicalLimit).foreach{ index =>
      val smallMedicalPosition = (Point(8,(canvasBoundary.y - 21)) + Point(index * config.propRadius * 3 / 2,0))
      val img = emptyMedicalImg
      ctx.drawImage(img, (smallMedicalPosition.x - config.propRadius) * canvasUnit,
        (smallMedicalPosition.y - config.propRadius) * canvasUnit,
        1.5 * config.propRadius * canvasUnit, 1.5 * config.propRadius * canvasUnit)
    }
    ctx.setGlobalAlpha(1)

  }




}
