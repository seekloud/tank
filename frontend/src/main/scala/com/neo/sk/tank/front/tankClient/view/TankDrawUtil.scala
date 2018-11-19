//package com.neo.sk.tank.front.tankClient.view
//
//import com.neo.sk.tank.front.common.Routes
//import com.neo.sk.tank.front.tankClient.game.GameContainerClientImpl
//import com.neo.sk.tank.shared.game.TankClientImpl
//import com.neo.sk.tank.shared.model.Constants.{InvincibleSize, SmallBullet}
//import com.neo.sk.tank.shared.model.Point
//import org.scalajs.dom
//import org.scalajs.dom.ext.Color
//import org.scalajs.dom.raw.HTMLElement
//import org.scalajs.dom.html
//
//import scala.collection.mutable
//
///**
//  * Created by hongruying on 2018/8/29
//  */
//trait TankDrawUtil{ this:GameContainerClientImpl =>
//
//  //fixme 将此处map暴露给子类
//  private val myTankInfoCacheMap = mutable.HashMap[(Byte,Byte,Byte),html.Canvas]()
//  private var canvasBoundary:Point=canvasSize
//
//  private val fillBulletImg = dom.document.createElement("img")
//  fillBulletImg.setAttribute("src", s"${Routes.base}/static/img/子弹初始重构.png")
//  private val emptyBulletImg = dom.document.createElement("img")
//  emptyBulletImg.setAttribute("src", s"${Routes.base}/static/img/子弹消失重构.png")
//
//  private val fillMedicalImg = dom.document.createElement("img")
//  fillMedicalImg.setAttribute("src", s"${Routes.base}/static/img/yiliao.png")
//  private val emptyMedicalImg = dom.document.createElement("img")
//  emptyMedicalImg.setAttribute("src", s"${Routes.base}/static/img/huiyiliao.png")
//
//
//  def updateTankSize(canvasSize:Point)={
//    myTankInfoCacheMap.clear()
//    canvasBoundary=canvasSize
//  }
//
//
//  protected def drawTank(offset:Point, offsetTime:Long, view:Point) = {
//    tankMap.values.foreach { t =>
//      val tank = t.asInstanceOf[TankClientImpl]
//      val p = tank.getPosition4Animation(boundary, quadTree, offsetTime) + offset
//      if (p.in(view, Point(t.getRadius * 4, t.getRadius * 4))) {
//        if (tankAttackedAnimationMap.contains(tank.tankId)) {
//          if (tankAttackedAnimationMap(tank.tankId) <= 0) tankAttackedAnimationMap.remove(tank.tankId)
//          else tankAttackedAnimationMap.put(tank.tankId, tankAttackedAnimationMap(tank.tankId) - 1)
//          ctx.globalAlpha = 0.5f
//        }
//
//        //------------------------绘制炮筒--------------------------#
//        val gunPositionList = tank.getGunPositions4Animation().map(t => (t + p) * canvasUnit)
//        ctx.beginPath()
//        ctx.moveTo(gunPositionList.last.x, gunPositionList.last.y)
//        gunPositionList.foreach(t => ctx.lineTo(t.x, t.y))
//        ctx.fillStyle = "#7A7A7A"
//        ctx.strokeStyle = "#636363"
//        ctx.fill()
//        ctx.lineWidth = 0.4 * canvasUnit
//        ctx.stroke()
//        ctx.closePath()
//        //----------------------------绘制坦克---------------------#
//        if (tank.getInvincibleState) {
//          ctx.beginPath()
//          ctx.fillStyle = "rgba(128, 100, 162, 0.2)"
//          ctx.arc(p.x * canvasUnit, p.y * canvasUnit, InvincibleSize.r * canvasUnit, 0, 2 * math.Pi)
//          ctx.fill()
//          ctx.closePath()
//        }
//        ctx.beginPath()
//        ctx.lineWidth = 0.4 * canvasUnit
//        ctx.strokeStyle = "#636363"
//        ctx.arc(p.x * canvasUnit, p.y * canvasUnit, tank.getRadius * canvasUnit, 0, 360)
//        val tankColor = tank.getTankColor()
//        ctx.fillStyle = tankColor
//        //      ctx.fillStyle = if(!justAttackedMap.keySet.contains(tank.tankId)) tank.getColor() else{
//        //        if(justAttackedMap(tank.tankId) < 9) justAttackedMap(tank.tankId) += 1 else justAttackedMap.remove(tank.tankId)
//        //        tank.getColorAttacked()
//        //      }
//        ctx.fill()
//        ctx.stroke()
//        ctx.closePath()
//        ctx.globalAlpha = 1
//
//
//        drawBloodSlider(p, tank)
//
//        ctx.beginPath()
//        val namePosition = (p + Point(0, 5)) * canvasUnit
//        ctx.fillStyle = "#006699"
//        ctx.textAlign = "center"
//        ctx.font = s"normal normal ${2 * canvasUnit}px 楷体"
//        ctx.lineWidth = 2
//        ctx.fillText(s"${tank.name}", namePosition.x, namePosition.y, 20 * canvasUnit)
//        ctx.closePath()
//
//        drawTankBullet(p, tank)
//      }
//    }
//  }
//
//  def drawBloodSlider(tankPosition:Point, tank:TankClientImpl) = {
//    val num = tank.getMaxBlood / 20
//    val sliderLength = 2f * tank.getRadius
//    val greyLength = 0.3f * sliderLength
//    val width = (sliderLength - greyLength) / num
//    val sliderPositions = tank.getSliderPositionByBloodLevel(num,sliderLength,width,greyLength).map(t => (t + tankPosition) * canvasUnit)
//    ctx.beginPath()
//    ctx.lineCap = "butt"
//    ctx.lineJoin = "miter"
//    ctx.lineWidth = 0.5 * canvasUnit
//    ctx.strokeStyle = "#BEBEBE"
//    ctx.moveTo(sliderPositions.last.x,sliderPositions.last.y)
//    ctx.lineTo(sliderPositions.head.x,sliderPositions.head.y)
//    ctx.stroke()
//    ctx.closePath()
//    for(i <- Range(1 ,sliderPositions.length,2)){
//      ctx.beginPath()
//      ctx.lineWidth = 0.5 * canvasUnit
//      if((i+1) / 2 <= 1f * tank.getCurBlood / 20){
//        ctx.strokeStyle = "red"
//        ctx.moveTo(sliderPositions(i-1).x,sliderPositions(i-1).y)
//        ctx.lineTo(sliderPositions(i).x,sliderPositions(i).y)
//        ctx.stroke()
//      }
////      else{
////        ctx.strokeStyle = "#BEBEBE"
////      }
//      if(tank.getCurBlood / 20 < 1f * tank.getCurBlood / 20 && (i+1) / 2 == tank.getCurBlood / 20 + 1){
//        ctx.strokeStyle = "red"
//        ctx.moveTo(sliderPositions(i-1).x,sliderPositions(i-1).y)
//        ctx.lineTo(sliderPositions(i-1).x + 1f * (tank.getCurBlood - tank.getCurBlood / 20 * 20) / 20 * width * canvasUnit,sliderPositions(i-1).y)
//        ctx.stroke()
//      }
//
//      ctx.closePath()
//    }
//  }
//
//  def drawTankBullet(tankPosition:Point, tank:TankClientImpl) = {
//    var left = tank.bulletMaxCapacity * SmallBullet.width / 2 * -1
//
//    (1 to tank.getCurBulletNum).foreach{ indedx =>
//      val smallBulletPosition = tankPosition + Point(left, -9)
//      val img = fillBulletImg
//      ctx.drawImage(img.asInstanceOf[HTMLElement], (smallBulletPosition.x - SmallBullet.width / 2) * canvasUnit,
//        (smallBulletPosition.y - SmallBullet.height / 2) * canvasUnit,
//        SmallBullet.width * canvasUnit, SmallBullet.height * canvasUnit)
//
//      left = left + SmallBullet.width
//    }
//    ctx.globalAlpha = 0.5
//
//    (tank.getCurBulletNum + 1 to tank.bulletMaxCapacity).foreach{ indedx =>
//      val smallBulletPosition = tankPosition + Point(left, -9)
//      val img = emptyBulletImg
//      ctx.drawImage(img.asInstanceOf[HTMLElement], (smallBulletPosition.x - SmallBullet.width / 2) * canvasUnit,
//        (smallBulletPosition.y - SmallBullet.height / 2) * canvasUnit,
//        SmallBullet.width * canvasUnit, SmallBullet.height * canvasUnit)
//      left = left + SmallBullet.width
//
//    }
//    ctx.globalAlpha = 1
//
//  }
//
//
//
//
//
//
//  private def generateMyTankInfoCanvas(tank:TankClientImpl):html.Canvas = {
//    myTankInfoCacheMap.clear()
//    val canvasCache = dom.document.createElement("canvas").asInstanceOf[html.Canvas]
//    val ctxCache = canvasCache.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
//
//    canvasCache.width = 30 * canvasUnit
//    canvasCache.height = 20 * canvasUnit
//    drawLevel(tank.getBloodLevel,config.getTankBloodMaxLevel(),"血量等级",Point(5,20 - 12) * canvasUnit,20 * canvasUnit,"#FF3030",ctxCache)
//    drawLevel(tank.getSpeedLevel,config.getTankSpeedMaxLevel(),"速度等级",Point(5,20 - 8) * canvasUnit,20 * canvasUnit,"#66CD00",ctxCache)
//    drawLevel(tank.getBulletLevel,config.getBulletMaxLevel(),"炮弹等级",Point(5,20 - 4) * canvasUnit,20 * canvasUnit,"#1C86EE",ctxCache)
//    drawLevel(tank.lives.toByte,config.getTankLivesLimit.toByte,s"生命值",Point(5,20-16) * canvasUnit,20 * canvasUnit,"#FFA500",ctxCache)
//    canvasCache
//
//  }
//
//  protected def drawMyTankInfo(tank:TankClientImpl) = {
//    val cache = myTankInfoCacheMap.getOrElseUpdate((tank.getBloodLevel,tank.getSpeedLevel,tank.getBulletLevel),generateMyTankInfoCanvas(tank))
//    ctx.drawImage(cache,0,(canvasBoundary.y - 20) * canvasUnit)
//  }
//
//  def drawLevel(level:Byte,maxLevel:Byte,name:String,start:Point,length:Float,color:String, context:dom.CanvasRenderingContext2D = ctx) = {
//    context.strokeStyle = "#4D4D4D"
//    context.lineCap = "round"
//    context.lineWidth = 3 * canvasUnit
//    context.beginPath()
//    context.moveTo(start.x,start.y)
//    context.lineTo(start.x+length,start.y)
//    context.stroke()
//    context.closePath()
//
//    context.lineWidth = 2.2 * canvasUnit
//    context.strokeStyle = color
//    if(level == maxLevel){
//      context.beginPath()
//      context.moveTo(start.x + length,start.y)
//      context.lineTo(start.x+length,start.y)
//      context.stroke()
//      context.closePath()
//    }
//
//    if(level >= 1){
//      context.beginPath()
//      context.moveTo(start.x,start.y)
//      context.lineTo(start.x,start.y)
//      context.stroke()
//      context.closePath()
//
//
//      context.lineCap = "butt"
//      (0 until level).foreach{ index =>
//        context.beginPath()
//        context.moveTo(start.x + index * (length / maxLevel) + 2,start.y)
//        context.lineTo(start.x + (index + 1) * (length / maxLevel) - 2,start.y)
//        context.stroke()
//        context.closePath()
//      }
//    }
//
//
//
//
//    context.font = s"bold ${1.8 * canvasUnit}px Arial"
//    context.textAlign = "center"
//    context.textBaseline = "middle"
//    context.fillStyle = "#FCFCFC"
//    context.fillText(name, start.x + length / 2, start.y)
//  }
//
//
//  def drawCurMedicalNum(tank:TankClientImpl) = {
//    ctx.beginPath()
//    ctx.fillStyle = Color.Black.toString()
//    ctx.textAlign = "left"
//    ctx.font=s"bold ${1.8 * canvasUnit}px 隶书"
//    ctx.lineWidth = 1
//    ctx.fillText(s"血包${("                       ").take(30)}(按E键使用)", 4.5*canvasUnit,(canvasBoundary.y - 22.5)  * canvasUnit , 30 * canvasUnit)
//    val medicalNum = tank.medicalNumOpt match{
//      case Some(num) =>num
//      case None =>0
//    }
//    (1 to medicalNum).foreach{ index =>
//      val smallMedicalPosition = (Point(8,(canvasBoundary.y - 21)) + Point(index * config.propRadius * 3 / 2,0))
//      val img = fillMedicalImg
//      ctx.drawImage(img.asInstanceOf[HTMLElement], (smallMedicalPosition.x - config.propRadius) * canvasUnit,
//        (smallMedicalPosition.y - config.propRadius) * canvasUnit,
//        1.5 * config.propRadius * canvasUnit, 1.5 * config.propRadius * canvasUnit)
//    }
//    ctx.globalAlpha = 0.5
//
//    (medicalNum + 1 to config.getTankMedicalLimit).foreach{ index =>
//      val smallMedicalPosition = (Point(8,(canvasBoundary.y - 21)) + Point(index * config.propRadius * 3 / 2,0))
//      val img = emptyMedicalImg
//      ctx.drawImage(img.asInstanceOf[HTMLElement], (smallMedicalPosition.x - config.propRadius) * canvasUnit,
//        (smallMedicalPosition.y - config.propRadius) * canvasUnit,
//        1.5 * config.propRadius * canvasUnit, 1.5 * config.propRadius * canvasUnit)
//    }
//    ctx.globalAlpha = 1
//
//  }
//
//
//
//}
