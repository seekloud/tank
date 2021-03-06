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

import org.seekloud.tank.shared.game.GameContainerClientImpl
import org.seekloud.tank.shared.model.Constants.{GameAnimation, PropAnimation}
import org.seekloud.tank.shared.model.Point
import org.seekloud.tank.shared.util.canvas.MiddleContext

/**
  * Created by hongruying on 2018/8/29
  */
trait PropDrawUtil { this: GameContainerClientImpl =>
  private val bloodPropImg =drawFrame.createImage("/img/xueliang.png")
  private val speedPropImg = drawFrame.createImage("/img/加速3.png")
  private val bulletPowerPropImg = drawFrame.createImage("/img/qiang.png")
  private val medicalPropImg =drawFrame.createImage("/img/yiliao.png")
  private val shotgunPropImg = drawFrame.createImage("/img/sandan.png")
  private val boomImg = drawFrame.createImage("/img/boom.png")

  protected def drawProps(offset: Point, view: Point,unit: Int, ctx: MiddleContext) = {
    propMap.values.foreach { prop =>
      val p = prop.getPosition + offset
      if (p.in(view, Point(prop.getRadius * 3, prop.getRadius * 3))) {
        val img = prop.propType match {
          case 1 => bloodPropImg
          case 2 => speedPropImg
          case 3 => bulletPowerPropImg
          case 4 => medicalPropImg
          case 5 => shotgunPropImg
        }

        if(prop.getDisappearTime < PropAnimation.DisAniFrame2){
          val mod = prop.getDisappearTime % (PropAnimation.DisappearF2 + PropAnimation.DisplayF2) + 1
          if(mod <= PropAnimation.DisplayF2){
            ctx.drawImage(img, (p.x - prop.getRadius) * unit, (p.y - prop.getRadius) * unit,
              Some(prop.getRadius * 2 * unit, prop.getRadius * 2 * unit))
          }
        } else if(prop.getDisappearTime < PropAnimation.DisAniFrame1){
          val mod = prop.getDisappearTime % (PropAnimation.DisappearF1 + PropAnimation.DisplayF1) + 1
          if(mod <= PropAnimation.DisplayF1){
            ctx.drawImage(img, (p.x - prop.getRadius) * unit, (p.y - prop.getRadius) * unit,
              Some(prop.getRadius * 2 * unit, prop.getRadius * 2 * unit))
          }
        }else{
          ctx.drawImage(img, (p.x - prop.getRadius) * unit, (p.y - prop.getRadius) * unit,
            Some(prop.getRadius * 2 * unit, prop.getRadius * 2 * unit))
        }

        if (tankDestroyAnimationMap.contains(prop.pId)) {
          if (tankDestroyAnimationMap(prop.pId) > GameAnimation.tankDestroyAnimationFrame * 2 / 3) {
            ctx.drawImage(boomImg, (p.x - prop.getRadius) * unit, (p.y - prop.getRadius) * unit, Some(prop.getRadius * 2 * unit, prop.getRadius * 2 * unit))

          } else if (tankDestroyAnimationMap(prop.pId) > GameAnimation.tankDestroyAnimationFrame / 3) {
            ctx.drawImage(boomImg, (p.x - prop.getRadius * 2.5f) * unit, (p.y - prop.getRadius * 2.5f) * unit, Some(prop.getRadius * 2 * 2.5 * unit, prop.getRadius * 2 * 2.5 * unit))

          } else if (tankDestroyAnimationMap(prop.pId) > 0) {
            ctx.setGlobalAlpha(0.5)
            ctx.drawImage(boomImg, (p.x - prop.getRadius * 2.5f) * unit, (p.y - prop.getRadius * 2.5f) * unit, Some(prop.getRadius * 2 * 2.5 * unit, prop.getRadius * 2 * 2.5 * unit))
            ctx.setGlobalAlpha(1)

          }

          if (tankDestroyAnimationMap(prop.pId) <= 0) tankDestroyAnimationMap.remove(prop.pId)
          else tankDestroyAnimationMap.put(prop.pId, tankDestroyAnimationMap(prop.pId) - 1)

        }

      }
    }
  }
}
