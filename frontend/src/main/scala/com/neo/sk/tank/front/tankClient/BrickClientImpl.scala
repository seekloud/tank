package com.neo.sk.tank.front.tankClient

import com.neo.sk.tank.shared.ptcl
import com.neo.sk.tank.shared.ptcl.{model, tank}
import com.neo.sk.tank.shared.ptcl.model.Point
import com.neo.sk.tank.shared.ptcl.tank._
import org.scalajs.dom
import org.scalajs.dom.ext.Color

/**
  * Created by hongruying on 2018/7/10
  */
class BrickClientImpl(
                       override val oId: Long,
                       override protected var curBlood: Int,
                       override protected var position: model.Point
                     ) extends Brick{

  def this(o:ObstacleState) = {
    this(o.oId,o.b.getOrElse(ptcl.model.ObstacleParameters.BrickDropBoxParameters.blood),o.p)
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
object BrickClientImpl{

  def drawBrick(ctx:dom.CanvasRenderingContext2D,offset:Point,canvasUnit:Int,brick:tank.Obstacle)={
    val position = brick.getObstacleState().p
    val curBlood = brick.getObstacleState().b.getOrElse(ptcl.model.ObstacleParameters.AirDropBoxParameters.blood)
    ctx.fillStyle = "#ADADAD"
    ctx.fillRect((position.x - model.ObstacleParameters.halfBorder + offset.x) * canvasUnit ,
      (position.y - model.ObstacleParameters.halfBorder + offset.y) * canvasUnit,
      model.ObstacleParameters.border * canvasUnit , model.ObstacleParameters.border * canvasUnit)
//    ctx.fillStyle = Color.Black.toString()
//    ctx.fillText(curBlood.toString,(position.x + offset.x - model.ObstacleParameters.halfBorder) * canvasUnit,(position.y + offset.y - model.ObstacleParameters.halfBorder) * canvasUnit,14)
  }

}

