//package com.neo.sk.tank.front.tankClient.view
//
//import com.neo.sk.tank.front.common.Routes
//import com.neo.sk.tank.front.tankClient.game.GameContainerClientImpl
//import com.neo.sk.tank.shared.model.Constants.{GameAnimation, PropAnimation}
//import com.neo.sk.tank.shared.model.Point
//import org.scalajs.dom
//import org.scalajs.dom.raw.HTMLElement
//import org.scalajs.dom.html
//
///**
//  * Created by hongruying on 2018/8/29
//  */
//trait PropDrawUtil {
//  this: GameContainerClientImpl =>
//
//  private val bloodPropImg = dom.document.createElement("img").asInstanceOf[html.Image]
//  bloodPropImg.setAttribute("src", s"${Routes.base}/static/img/xueliang.png")
//  private val speedPropImg = dom.document.createElement("img").asInstanceOf[html.Image]
//  speedPropImg.setAttribute("src", s"${Routes.base}/static/img/sudu.png")
//  private val bulletPowerPropImg = dom.document.createElement("img").asInstanceOf[html.Image]
//  bulletPowerPropImg.setAttribute("src", s"${Routes.base}/static/img/qiang.png")
//  private val medicalPropImg = dom.document.createElement("img").asInstanceOf[html.Image]
//  medicalPropImg.setAttribute("src", s"${Routes.base}/static/img/yiliao.png")
//  private val shotgunPropImg = dom.document.createElement("img").asInstanceOf[html.Image]
//  shotgunPropImg.setAttribute("src", s"${Routes.base}/static/img/sandan.png")
//  private val boomImg = dom.document.createElement("img").asInstanceOf[html.Image]
//  boomImg.setAttribute("src", s"${Routes.base}/static/img/boom.png")
//
//
//  protected def drawProps(offset: Point, view: Point) = {
//    propMap.values.foreach { prop =>
//      val p = prop.getPosition + offset
//      if (p.in(view, Point(prop.getRadius * 3, prop.getRadius * 3))) {
//        val img = prop.propType match {
//          case 1 => bloodPropImg
//          case 2 => speedPropImg
//          case 3 => bulletPowerPropImg
//          case 4 => medicalPropImg
//          case 5 => shotgunPropImg
//        }
//
//        if(prop.getDisappearTime < PropAnimation.DisAniFrame2){
//          val mod = prop.getDisappearTime % (PropAnimation.DisappearF2 + PropAnimation.DisplayF2) + 1
//          if(mod <= PropAnimation.DisplayF2){
//            ctx.drawImage(img.asInstanceOf[HTMLElement], (p.x - prop.getRadius) * canvasUnit, (p.y - prop.getRadius) * canvasUnit,
//              prop.getRadius * 2 * canvasUnit, prop.getRadius * 2 * canvasUnit)
//          }
//        } else if(prop.getDisappearTime < PropAnimation.DisAniFrame1){
//          val mod = prop.getDisappearTime % (PropAnimation.DisappearF1 + PropAnimation.DisplayF1) + 1
//          if(mod <= PropAnimation.DisplayF1){
//            ctx.drawImage(img.asInstanceOf[HTMLElement], (p.x - prop.getRadius) * canvasUnit, (p.y - prop.getRadius) * canvasUnit,
//              prop.getRadius * 2 * canvasUnit, prop.getRadius * 2 * canvasUnit)
//          }
//        }else{
//          ctx.drawImage(img.asInstanceOf[HTMLElement], (p.x - prop.getRadius) * canvasUnit, (p.y - prop.getRadius) * canvasUnit,
//            prop.getRadius * 2 * canvasUnit, prop.getRadius * 2 * canvasUnit)
//        }
//
//        if (tankDestroyAnimationMap.contains(prop.pId)) {
//          if (tankDestroyAnimationMap(prop.pId) > GameAnimation.tankDestroyAnimationFrame * 2 / 3) {
//            ctx.drawImage(boomImg.asInstanceOf[HTMLElement], (p.x - prop.getRadius) * canvasUnit, (p.y - prop.getRadius) * canvasUnit, prop.getRadius * 2 * canvasUnit, prop.getRadius * 2 * canvasUnit)
//          } else if (tankDestroyAnimationMap(prop.pId) > GameAnimation.tankDestroyAnimationFrame / 3) {
//            ctx.drawImage(boomImg.asInstanceOf[HTMLElement], (p.x - prop.getRadius * 2.5f) * canvasUnit, (p.y - prop.getRadius * 2.5f) * canvasUnit, prop.getRadius * 2 * 2.5 * canvasUnit, prop.getRadius * 2 * 2.5 * canvasUnit)
//          } else if (tankDestroyAnimationMap(prop.pId) > 0) {
//            ctx.globalAlpha = 0.5
//            ctx.drawImage(boomImg.asInstanceOf[HTMLElement], (p.x - prop.getRadius * 2.5f) * canvasUnit, (p.y - prop.getRadius * 2.5f) * canvasUnit, prop.getRadius * 2 * 2.5 * canvasUnit, prop.getRadius * 2 * 2.5 * canvasUnit)
//            ctx.globalAlpha = 1
//
//          }
//
//          if (tankDestroyAnimationMap(prop.pId) <= 0) tankDestroyAnimationMap.remove(prop.pId)
//          else tankDestroyAnimationMap.put(prop.pId, tankDestroyAnimationMap(prop.pId) - 1)
//
//        }
//
//      }
//    }
//  }
//}
