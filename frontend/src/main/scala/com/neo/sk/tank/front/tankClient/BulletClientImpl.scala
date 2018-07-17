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
                        override val bId: Long,
                        override val tankId: Long,
                        override protected val startPosition: model.Point,
                        override protected val createTime: Long,
                        override val damage: Int,
                        override protected val momentum: model.Point,
                        override var position: model.Point
                      ) extends Bullet{
  def this(b:BulletState) = {
    this(b.bId,b.tankId,b.startPosition,b.createTime,b.damage,b.momentum,b.position)
  }



  //todo
  def getPositionCurFrame(curFrame:Int):Point = {
    //计算当前子弹动画渲染的位置
    this.position + momentum * curFrame / 1000 * model.Frame.millsAServerFrame / model.Frame.clientFrameAServerFrame
  }







}

object BulletClientImpl{
  def drawBullet(ctx:dom.CanvasRenderingContext2D,canvasUnit:Int,bullet:BulletClientImpl,curFrame:Int,offset:Point) = {
    val position = bullet.getPositionCurFrame(curFrame)
    ctx.fillStyle = Color.Red.toString()
    ctx.beginPath()
    ctx.arc((position.x  + offset.x) * canvasUnit,(position.y  + offset.y)*canvasUnit,model.BulletSize.r*canvasUnit,0, 360)
    ctx.fill()
    ctx.closePath()


//    ctx.canvas (bullet.getObjectRect(position - Point(model.BulletSize.r,model.BulletSize.r),position + Point(model.BulletSize.r,model.BulletSize.r)))

  }
}
