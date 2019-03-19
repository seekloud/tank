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

package org.seekloud.tank.shared.game.botView

import org.seekloud.tank.shared.`object`.Tank
import org.seekloud.tank.shared.game.{GameContainerClientImpl, TankClientImpl}
import org.seekloud.tank.shared.game.view.TankDrawUtil
import org.seekloud.tank.shared.model.Constants.TankColor
import org.seekloud.tank.shared.model.Point
import org.seekloud.tank.shared.util.canvas.MiddleContext

/**
  * Created by sky
  * Date on 2019/3/18
  * Time at 上午11:40
  */
trait BotTankDrawUtil extends TankDrawUtil {
  this: GameContainerClientImpl =>
  protected def drawKernel4Bot(offset: Point, tank: Tank): Unit = {
    val p = tank.getPosition + offset
    selfCtx.beginPath()
    selfCtx.setStrokeStyle("white")
    val x = p.x * layerCanvasUnit
    val y = p.y * layerCanvasUnit
    selfCtx.setFill("white")
    selfCtx.arc(x, y, tank.getRadius * layerCanvasUnit, 0, 360)
    selfCtx.stroke()
    selfCtx.closePath()
  }

  protected def drawTankList4Bot(offset: Point, view: Point) = {
    tankMap.values.foreach { t =>
      val tank = t.asInstanceOf[TankClientImpl]
      val p = tank.getPosition + offset
      if (p.in(view, Point(t.getRadius * 4, t.getRadius * 4))) {
        drawTankGun(p,tank,layerCanvasUnit,bodiesCtx)
        drawTank(p, tank,if(tank.tankId==myTankId) TankColor.green else s"rgba(${tank.tankId%255}, ${tank.tankId%100}, ${tank.tankId%50}, 1)",layerCanvasUnit, bodiesCtx)
        drawTank(p, tank,if(tank.tankId==myTankId) TankColor.green else s"rgba(${tank.tankId%255}, ${tank.tankId%100}, ${tank.tankId%50}, 1)",layerCanvasUnit, ownerShipCtx)
        if(tank.tankId==myTankId){
          drawTank(p, tank,if(tank.tankId==myTankId) TankColor.green else s"rgba(${tank.tankId%255}, ${tank.tankId%100}, ${tank.tankId%50}, 1)",layerCanvasUnit, selfCtx)
        }
        drawBloodSlider(p, tank, layerCanvasUnit, bodiesCtx)
        drawTankName(p, tank.name, layerCanvasUnit, bodiesCtx)
        drawTankBullet(p, tank, layerCanvasUnit, bodiesCtx)
        drawTankStar(p, tank, layerCanvasUnit, bodiesCtx)
      }
    }
  }

  protected def drawState4Bot(tank:TankClientImpl): Unit = {
    drawLevel(tank.getBloodLevel, config.getTankBloodMaxLevel(), "血量等级", Point(5, 10) * layerCanvasUnit, 100 , "#FF3030",layerCanvasUnit*3, statusCtx)
    drawLevel(tank.getSpeedLevel, config.getTankSpeedMaxLevel(), "速度等级", Point(5, 20) * layerCanvasUnit, 100 , "#66CD00", layerCanvasUnit*3,statusCtx)
    drawLevel(tank.getBulletLevel, config.getBulletMaxLevel(), "伤害等级", Point(5, 30) * layerCanvasUnit, 100, "#FF3030",layerCanvasUnit*3, statusCtx)
    drawLevel(tank.getCurMedicalNum.toByte, config.getTankMedicalLimit.toByte, "医疗", Point(5, 40) * layerCanvasUnit, 100, "#66CD00", layerCanvasUnit*3,statusCtx)
    drawLevel(tank.getCurBulletNum.toByte, config.maxBulletCapacity.toByte, "子弹剩余", Point(5, 50) * layerCanvasUnit, 100 , "#FF3030",layerCanvasUnit*3, statusCtx)
    drawLevel(((tank.getCurBlood.toFloat/tank.getMaxBlood)*10).toByte, 10, "血量剩余", Point(5, 60) * layerCanvasUnit, 100 , "#66CD00", layerCanvasUnit*3,statusCtx)
    if(tank.getInvincibleState){
      drawLevel(1, 1, "无敌状态", Point(5, 70) * layerCanvasUnit, 100, "#FF3030",layerCanvasUnit*3, statusCtx)
    }else{
      drawLevel(0, 1, "无敌状态", Point(5, 70) * layerCanvasUnit, 100, "#FF3030",layerCanvasUnit*3, statusCtx)
    }
    if(tank.getShotGunState()){
      drawLevel(1, 1, "散弹状态", Point(5, 80) * layerCanvasUnit, 100, "#66CD00",layerCanvasUnit*3, statusCtx)
    }else{
      drawLevel(0, 10, "散弹状态", Point(5, 80) * layerCanvasUnit, 100, "#66CD00",layerCanvasUnit*3, statusCtx)
    }
  }
}
