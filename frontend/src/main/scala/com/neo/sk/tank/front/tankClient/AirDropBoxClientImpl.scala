package com.neo.sk.tank.front.tankClient

import com.neo.sk.tank.shared.ptcl
import com.neo.sk.tank.shared.ptcl.tank
import com.neo.sk.tank.shared.ptcl.model
import com.neo.sk.tank.shared.ptcl.model.Point
import com.neo.sk.tank.shared.ptcl.tank._
import org.scalajs.dom
import org.scalajs.dom.ext.Color

/**
  * Created by hongruying on 2018/7/10
  */
class AirDropBoxClientImpl(
                            override val oId: Int,
                            override protected var curBlood:Int,
                            override var position: model.Point
                          ) extends AirDropBox{
  def this(o:ObstacleState) = {
    this(o.oId,o.b.getOrElse(ptcl.model.ObstacleParameters.AirDropBoxParameters.blood),o.p)
  }

  override def getObjectRect(): model.Rectangle = {
    model.Rectangle(position- model.Point(model.ObstacleParameters.halfBorder,model.ObstacleParameters.halfBorder),position + model.Point(model.ObstacleParameters.halfBorder,model.ObstacleParameters.halfBorder))
  }

  override def attacked(bullet: Bullet, destroyCallBack: Obstacle => Unit): Unit = {
    attackDamage(bullet.damage)
    if(!isLived()){
      destroyCallBack
    }
  }

  def isIntersectsObject(o:Seq[ObjectOfGame]):Boolean = {
    val rec = this.getObjectRect()
    o.exists(t => t.getObjectRect().intersects(rec))
  }


}
object AirDropBoxClientImpl{
  def drawAirDrop(ctx:dom.CanvasRenderingContext2D,offset:Point,canvasUnit:Int,drop:tank.Obstacle)={
    val position = drop.getObstacleState().p
    val curBlood = drop.getObstacleState().b.getOrElse(ptcl.model.ObstacleParameters.AirDropBoxParameters.blood)
    //    ctx.fillStyle = Color.Black.toString()
    //    ctx.fillText(curBlood.toString,(position.x + offset.x - model.ObstacleParameters.halfBorder) * canvasUnit,(position.y + offset.y - model.ObstacleParameters.halfBorder) * canvasUnit,14)
    ctx.beginPath()
    ctx.fillStyle = Color.Cyan.toString()
    ctx.fillRect((position.x - model.ObstacleParameters.halfBorder + offset.x) * canvasUnit
      ,(position.y + model.ObstacleParameters.halfBorder + offset.y - (model.ObstacleParameters.border * curBlood).toFloat/ model.ObstacleParameters.BrickDropBoxParameters.blood) * canvasUnit,
      model.ObstacleParameters.border * canvasUnit,((model.ObstacleParameters.border * curBlood).toFloat/ model.ObstacleParameters.BrickDropBoxParameters.blood) * canvasUnit)
    ctx.strokeStyle = Color.Cyan.toString()
    ctx.lineWidth = 2
    ctx.closePath()
    ctx.beginPath()
    ctx.rect((position.x - model.ObstacleParameters.halfBorder + offset.x) * canvasUnit ,
      (position.y - model.ObstacleParameters.halfBorder + offset.y) * canvasUnit,
      model.ObstacleParameters.border * canvasUnit , model.ObstacleParameters.border * canvasUnit)
    ctx.stroke()
    ctx.lineWidth =1
    ctx.closePath()
//    println((model.ObstacleParameters.border * curBlood).toFloat/ model.ObstacleParameters.AirDropBoxParameters.blood)
  }

}
