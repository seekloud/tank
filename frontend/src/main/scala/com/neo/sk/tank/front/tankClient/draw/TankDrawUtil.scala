package com.neo.sk.tank.front.tankClient.draw

import com.neo.sk.tank.front.common.Routes
import com.neo.sk.tank.front.tankClient.GameContainerClientImpl
import com.neo.sk.tank.shared.`object`.TankImpl
import com.neo.sk.tank.shared.model.Constants.{InvincibleSize, SmallBullet}
import com.neo.sk.tank.shared.model.Point
import org.scalajs.dom
import org.scalajs.dom.ext.Color
import org.scalajs.dom.raw.HTMLElement

/**
  * Created by hongruying on 2018/8/29
  */
trait TankDrawUtil{ this:GameContainerClientImpl =>
  protected def drawTank(offset:Point, offsetTime:Long) = {
    tankMap.values.foreach{ t =>
      val tank = t.asInstanceOf[TankImpl]
      val p = tank.getPosition4Animation(boundary,quadTree,offsetTime) + offset

      if(tankAttackedAnimationMap.contains(tank.tankId)){
        if(tankAttackedAnimationMap(tank.tankId) <= 0) tankAttackedAnimationMap.remove(tank.tankId)
        else tankAttackedAnimationMap.put(tank.tankId, tankAttackedAnimationMap(tank.tankId) - 1)
        ctx.globalAlpha = 0.5f
      }

      //------------------------绘制炮筒--------------------------#
      val gunPositionList = tank.getGunPositions4Animation().map(t => (t + p) * canvasUnit)
      ctx.beginPath()
      ctx.moveTo(gunPositionList.last.x,gunPositionList.last.y)
      gunPositionList.foreach(t => ctx.lineTo(t.x,t.y))
      ctx.fillStyle = "#7A7A7A"
      ctx.strokeStyle = "#636363"
      ctx.fill()
      ctx.lineWidth = 4
      ctx.stroke()
      ctx.closePath()
      //----------------------------绘制坦克---------------------#
      if(tank.getInvincibleState) {
        ctx.beginPath()
        ctx.fillStyle = "rgba(128, 100, 162, 0.2)"
        ctx.arc(p.x * canvasUnit, p.y * canvasUnit, InvincibleSize.r * canvasUnit, 0, 2 * math.Pi)
        ctx.fill()
        ctx.closePath()
      }
      ctx.beginPath()
      ctx.lineWidth = 4
      ctx.strokeStyle = "#636363"
      ctx.arc(p.x * canvasUnit, p.y * canvasUnit, tank.getRadius * canvasUnit, 0, 360)
      val tankColor = tank.getTankColor()
      ctx.fillStyle = tankColor
      //      ctx.fillStyle = if(!justAttackedMap.keySet.contains(tank.tankId)) tank.getColor() else{
      //        if(justAttackedMap(tank.tankId) < 9) justAttackedMap(tank.tankId) += 1 else justAttackedMap.remove(tank.tankId)
      //        tank.getColorAttacked()
      //      }
      ctx.fill()
      ctx.stroke()
      ctx.closePath()
      ctx.globalAlpha = 1


      drawBloodSlider(p,tank)

      ctx.beginPath()
      val namePosition = (p + Point(0,5)) * canvasUnit
      ctx.fillStyle = "#006699"
      ctx.textAlign = "center"
      ctx.font = "normal normal 20px 楷体"
      ctx.lineWidth = 2
      ctx.fillText(s"${tank.name}",namePosition.x,namePosition.y,20 * canvasUnit)
      ctx.closePath()

      drawTankBullet(p,tank)
    }
  }

  def drawBloodSlider(tankPosition:Point, tank:TankImpl) = {
    val num = tank.getMaxBlood / 20
    val sliderLength = 2f * tank.getRadius
    val greyLength = 0.3f * sliderLength
    val width = (sliderLength - greyLength) / num
    val sliderPositions = tank.getSliderPositionByBloodLevel(num,sliderLength,width,greyLength).map(t => (t + tankPosition) * canvasUnit)
    ctx.beginPath()
    ctx.lineCap = "butt"
    ctx.lineJoin = "miter"
    ctx.lineWidth = 5
    ctx.strokeStyle = "grey"
    ctx.moveTo(sliderPositions.last.x,sliderPositions.last.y)
    ctx.lineTo(sliderPositions.head.x,sliderPositions.head.y)
    ctx.stroke()
    ctx.closePath()
    for(i <- Range(1 ,sliderPositions.length,2)){
      ctx.beginPath()
      ctx.lineWidth = 5
      if((i+1) / 2 <= 1f * tank.getCurBlood / 20){
        ctx.strokeStyle = "red"
      }
      else{
        ctx.strokeStyle = "black"
      }
      ctx.moveTo(sliderPositions(i-1).x,sliderPositions(i-1).y)
      ctx.lineTo(sliderPositions(i).x,sliderPositions(i).y)
      ctx.stroke()
      ctx.closePath()
    }
  }

  def drawTankBullet(tankPosition:Point, tank:TankImpl) = {
    var left = tank.bulletMaxCapacity * SmallBullet.width / 2 * -1

    (1 to tank.getCurBulletNum).foreach{ indedx =>

      ctx.beginPath()
      val smallBulletPosition = tankPosition + Point(left, -9)
      val img = dom.document.createElement("img")
      img.setAttribute("src", s"${Routes.base}/static/img/子弹初始重构.png")
      ctx.drawImage(img.asInstanceOf[HTMLElement], (smallBulletPosition.x - SmallBullet.width / 2) * canvasUnit,
        (smallBulletPosition.y - SmallBullet.height / 2) * canvasUnit,
        SmallBullet.width * canvasUnit, SmallBullet.height * canvasUnit)
      ctx.fill()
      ctx.stroke()
      ctx.closePath()
      left = left + SmallBullet.width
    }
    (tank.getCurBulletNum + 1 to tank.bulletMaxCapacity).foreach{ indedx =>
      ctx.beginPath()
      val smallBulletPosition = tankPosition + Point(left, -9)
      val img = dom.document.createElement("img")
      val image =  img.setAttribute("src", s"${Routes.base}/static/img/子弹消失重构.png")

      ctx.drawImage(img.asInstanceOf[HTMLElement], (smallBulletPosition.x - SmallBullet.width / 2) * canvasUnit,
        (smallBulletPosition.y - SmallBullet.height / 2) * canvasUnit,
        SmallBullet.width * canvasUnit, SmallBullet.height * canvasUnit)
      ctx.fill()
      ctx.stroke()
      ctx.closePath()
      left = left + SmallBullet.width

    }

  }

  protected def drawMyTankInfo(tank:TankImpl) = {
    ctx.font = "bold 20px Arial"
    ctx.textAlign = "center"
    ctx.textBaseline = "middle"
    ctx.fillStyle = Color.Black.toString()
    ctx.fillText(myName, 15 * canvasUnit,(canvasBoundary.y - 16) * canvasUnit)
    drawLevel(tank.getBloodLevel,config.getTankBloodMaxLevel(),"血量等级",Point(5,canvasBoundary.y - 12) * canvasUnit,20 * canvasUnit,"#FF3030")
    drawLevel(tank.getSpeedLevel,config.getTankSpeedMaxLevel(),"速度等级",Point(5,canvasBoundary.y - 8) * canvasUnit,20 * canvasUnit,"#66CD00")
    drawLevel(tank.getBulletLevel,config.getBulletMaxLevel(),"炮弹等级",Point(5,canvasBoundary.y - 4) * canvasUnit,20 * canvasUnit,"#1C86EE")

    ctx.closePath()
  }

}
