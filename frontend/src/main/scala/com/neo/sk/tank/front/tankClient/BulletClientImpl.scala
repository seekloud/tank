package com.neo.sk.tank.front.tankClient

import com.neo.sk.tank.shared.ptcl.model
import com.neo.sk.tank.shared.ptcl.model.Point
import com.neo.sk.tank.shared.ptcl.tank.{Bullet, BulletState}
import com.neo.sk.tank.shared.ptcl.tank.Bullet
import org.scalajs.dom
import org.scalajs.dom.ext.Color

/**
  * Created by hongruying on 2018/7/10
  */
class BulletClientImpl(
                        override val bId: Int,
                        override val tankId: Int,
                        override protected val startPosition: model.Point,
                        override protected val createTime: Long,
                        override val damage: Int,
                        override protected val momentum: model.Point,
                        override var position: model.Point,
                        override val tankName: String,



                      ) extends Bullet{
  def this(b:BulletState) = {
    this(b.bId,b.tankId,b.startPosition,b.createTime,b.damage,b.momentum,b.position,b.name)
  }

  override val r: Float = model.TankParameters.TankBulletBulletPowerLevel.getBulletLevelByDamage(this.damage) match{
    case 1 => 1* model.BulletSize.r
    case 2 => 1.2f* model.BulletSize.r
    case 3 => 1.4f* model.BulletSize.r
  }



  //todo
  def getPositionCurFrame(curFrame:Int):Point = {
    //计算当前子弹动画渲染的位置
    this.position + momentum * curFrame / 1000 * model.Frame.millsAServerFrame / model.Frame.clientFrameAServerFrame
  }

  def getPositionByOffsetTime(offsetTime:Long):Point = {
    //计算当前子弹动画渲染的位置
    this.position + momentum / 1000 * offsetTime
  }







}

object BulletClientImpl{
  def drawBullet(ctx:dom.CanvasRenderingContext2D,canvasUnit:Int,bullet:BulletClientImpl,bulletDamage:Int,curFrame:Int,offset:Point) = {
    val position = bullet.getPositionCurFrame(curFrame)
    val color = model.TankParameters.TankBulletBulletPowerLevel.getBulletLevelByDamage(bulletDamage) match {
      case 1 => "#CD6600"
      case 2 => "#FF4500"
      case 3 => "#8B2323"
    }
//    val radiusMulti = model.TankParameters.TankBulletBulletPowerLevel.getBulletLevelByDamage(bulletDamage) match {
//      case 1 => 1
//      case 2 => 1.2
//      case 3 => 1.4
//    }
    ctx.fillStyle = color.toString()
    ctx.beginPath()
    ctx.arc((position.x  + offset.x) * canvasUnit,(position.y  + offset.y)*canvasUnit,bullet.r *canvasUnit,0, 360)
    ctx.fill()
    ctx.closePath()


//    ctx.canvas (bullet.getObjectRect(position - Point(model.BulletSize.r,model.BulletSize.r),position + Point(model.BulletSize.r,model.BulletSize.r)))

  }

  def drawBulletByOffsetTime(ctx:dom.CanvasRenderingContext2D,canvasUnit:Int,bullet:BulletClientImpl,bulletDamage:Int,offsetTime:Long,offset:Point) = {
    val position = bullet.getPositionByOffsetTime(offsetTime)
    val color = model.TankParameters.TankBulletBulletPowerLevel.getBulletLevelByDamage(bulletDamage) match {
      case 1 => "#CD6600"
      case 2 => "#FF4500"
      case 3 => "#8B2323"
    }
//    val radiusMulti = model.TankParameters.TankBulletBulletPowerLevel.getBulletLevelByDamage(bulletDamage) match {
//      case 1 => 1
//      case 2 => 1.2
//      case 3 => 1.4
//    }
    ctx.fillStyle = color.toString()
    ctx.beginPath()
    ctx.arc((position.x  + offset.x) * canvasUnit,(position.y  + offset.y)*canvasUnit,bullet.r * canvasUnit,0, 360)
    ctx.fill()
    ctx.closePath()


    //    ctx.canvas (bullet.getObjectRect(position - Point(model.BulletSize.r,model.BulletSize.r),position + Point(model.BulletSize.r,model.BulletSize.r)))

  }
}
