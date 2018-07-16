package com.neo.sk.tank.core.tank

import com.neo.sk.tank.shared.ptcl.model
import com.neo.sk.tank.shared.ptcl.tank.{AirDropBox, Bullet, ObjectOfGame, Obstacle}

/**
  * Created by hongruying on 2018/7/10
  */
class AirDropBoxImpl(
                      override val oId: Long,
                      override protected var curBlood:Int,
                      override protected var position: model.Point
                    ) extends AirDropBox{

  def this(oId:Long,position:model.Point)= {
    this(
      oId,
      model.ObstacleParameters.AirDropBoxParameters.blood,
      position
    )
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
