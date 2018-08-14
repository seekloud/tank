package com.neo.sk.tank.core.tank

import com.neo.sk.tank.shared.ptcl.model
import com.neo.sk.tank.shared.ptcl.tank.{Bullet, ObjectOfGame, Obstacle, Steel}

class SteelServerImpl (
                      override val oId:Int,
                      override val obstacleType: Byte,
                      override var position:model.Point,
                      override protected val width: Float,
                      override protected val height: Float
                      )extends Steel{
  def this(oId:Int,position:model.Point,obstacleType:Byte,width:Float,height:Float) = {
    this(
      oId,
      obstacleType,
      position,
      width,
      height
    )
  }

  override def attacked(bullet: Bullet, destroyCallBack: Obstacle => Unit): Unit = {}

  override def attackDamage(d: Int): Unit = {}

  override def getObjectRect(): model.Rectangle = {
    this.obstacleType match {
      case model.ObstacleParameters.ObstacleType.steel => model.Rectangle(position- model.Point(model.ObstacleParameters.SteelParameters.border / 2,model.ObstacleParameters.SteelParameters.border / 2),position + model.Point(model.ObstacleParameters.SteelParameters.border /2,model.ObstacleParameters.SteelParameters.border / 2))
      case model.ObstacleParameters.ObstacleType.river => model.Rectangle(position- model.Point(model.ObstacleParameters.RiverParameters.width / 2,model.ObstacleParameters.RiverParameters.height / 2),position + model.Point(model.ObstacleParameters.RiverParameters.width / 2,model.ObstacleParameters.RiverParameters.height / 2))
    }
  }

  def isIntersectsObject(o:Seq[ObjectOfGame]):Boolean = {
    val rec = this.getObjectRect()
    o.exists(t => t.getObjectRect().intersects(rec))
//    o.exists(t => t.isIntersects(this))
  }

}
