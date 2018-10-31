package com.neo.sk.tank.game.draw

import com.neo.sk.tank.game.GameContainerClientImpl
import com.neo.sk.tank.shared.model.Constants.{GameAnimation, PropAnimation}
import com.neo.sk.tank.shared.model.Point
import javafx.scene.image.Image

/**
  * Created by hongruying on 2018/8/29
  */
trait PropDrawUtil { this: GameContainerClientImpl =>
  private val bloodPropImg = new Image(s"file:client/src/main/resources/img/xueliang.png")
  private val speedPropImg = new Image(s"file:client/src/main/resources/img/sudu.png")
  private val bulletPowerPropImg = new Image(s"file:client/src/main/resources/img/qiang.png")
  private val medicalPropImg = new Image(s"file:client/src/main/resources/img/yiliao.png")
  private val shotgunPropImg = new Image(s"file:client/src/main/resources/img/sandan.png")
  private val boomImg = new Image(s"file:client/src/main/resources/img/boom.png")

  protected def drawProps(offset: Point, view: Point) = {
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
            ctx.drawImage(img, (p.x - prop.getRadius) * canvasUnit, (p.y - prop.getRadius) * canvasUnit,
              prop.getRadius * 2 * canvasUnit, prop.getRadius * 2 * canvasUnit)
          }
        } else if(prop.getDisappearTime < PropAnimation.DisAniFrame1){
          val mod = prop.getDisappearTime % (PropAnimation.DisappearF1 + PropAnimation.DisplayF1) + 1
          if(mod <= PropAnimation.DisplayF1){
            ctx.drawImage(img, (p.x - prop.getRadius) * canvasUnit, (p.y - prop.getRadius) * canvasUnit,
              prop.getRadius * 2 * canvasUnit, prop.getRadius * 2 * canvasUnit)
          }
        }else{
          ctx.drawImage(img, (p.x - prop.getRadius) * canvasUnit, (p.y - prop.getRadius) * canvasUnit,
            prop.getRadius * 2 * canvasUnit, prop.getRadius * 2 * canvasUnit)
        }

        if (tankDestroyAnimationMap.contains(prop.pId)) {
          if (tankDestroyAnimationMap(prop.pId) > GameAnimation.tankDestroyAnimationFrame * 2 / 3) {
            ctx.drawImage(boomImg, (p.x - prop.getRadius) * canvasUnit, (p.y - prop.getRadius) * canvasUnit, prop.getRadius * 2 * canvasUnit, prop.getRadius * 2 * canvasUnit)
          } else if (tankDestroyAnimationMap(prop.pId) > GameAnimation.tankDestroyAnimationFrame / 3) {
            ctx.drawImage(boomImg, (p.x - prop.getRadius * 2.5f) * canvasUnit, (p.y - prop.getRadius * 2.5f) * canvasUnit, prop.getRadius * 2 * 2.5 * canvasUnit, prop.getRadius * 2 * 2.5 * canvasUnit)
          } else if (tankDestroyAnimationMap(prop.pId) > 0) {
            ctx.setGlobalAlpha(0.5)
            ctx.drawImage(boomImg, (p.x - prop.getRadius * 2.5f) * canvasUnit, (p.y - prop.getRadius * 2.5f) * canvasUnit, prop.getRadius * 2 * 2.5 * canvasUnit, prop.getRadius * 2 * 2.5 * canvasUnit)
            ctx.setGlobalAlpha(1)

          }

          if (tankDestroyAnimationMap(prop.pId) <= 0) tankDestroyAnimationMap.remove(prop.pId)
          else tankDestroyAnimationMap.put(prop.pId, tankDestroyAnimationMap(prop.pId) - 1)

        }

      }
    }
  }
}
