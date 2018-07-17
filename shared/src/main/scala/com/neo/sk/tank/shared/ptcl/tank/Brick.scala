package com.neo.sk.tank.shared.ptcl.tank

import com.neo.sk.tank.shared.ptcl.model

/**
  * Created by hongruying on 2018/7/9
  */
trait Brick extends Obstacle{

  override val oId: Long

  override val obstacleType:Int = model.ObstacleParameters.ObstacleType.brick



  protected var curBlood:Int //物体血量

  override protected var position: model.Point

  def getObstacleState():ObstacleState = ObstacleState(oId,obstacleType,Some(curBlood),position)


  private val maxBlood:Int = model.ObstacleParameters.BrickDropBoxParameters.blood


  override protected val height: Double = model.ObstacleParameters.BrickDropBoxParameters.



  //todo 被子弹攻击，血量下降，当血量小于0，消失
  def attacked(bullet: Bullet,destroyCallBack:Obstacle => Unit):Unit = {


  }

  override def attackDamage(d: Int): Unit = {
    curBlood -= d
  }

  override def isLived(): Boolean = {
    if(curBlood > 0) true
    else false
  }

}
