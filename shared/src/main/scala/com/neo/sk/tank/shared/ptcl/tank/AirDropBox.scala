package com.neo.sk.tank.shared.ptcl.tank
import com.neo.sk.tank.shared.ptcl.model

/**
  * Created by hongruying on 2018/7/9
  * 空头箱
  */
trait AirDropBox extends Obstacle{

  override val oId: Long

  protected var curBlood :Int //物体血量

  override var position: model.Point

  def getObstacleState():ObstacleState = ObstacleState(oId,obstacleType,Some(curBlood),position)



  override val obstacleType = model.ObstacleParameters.ObstacleType.airDropBox

  private val maxBlood = model.ObstacleParameters.AirDropBoxParameters.blood

  override protected val height: Double = model.ObstacleParameters.border
  override protected val width: Double = model.ObstacleParameters.border










  //todo 被子弹攻击，血量下降，当血量小于0，随机爆出道具
  def attacked(bullet: Bullet,destroyCallBack:Obstacle => Unit):Unit = {}

  override def attackDamage(d: Int): Unit = {
    curBlood -= d
  }

  override def isLived(): Boolean = {
    if(curBlood > 0) true
    else false
  }

}
