package com.neo.sk.tank.shared.ptcl.tank
import com.neo.sk.tank.shared.ptcl.model

/**
  * Created by hongruying on 2018/7/9
  * 空头箱
  */
trait AirDropBox extends Obstacle{

  override protected val oId: Long

  protected final var curBlood = 10 //物体血量

  override protected var position: model.Point



  override protected val obstacleType = model.ObstacleParameters.ObstacleType.airDropBox

  private val maxBlood = model.ObstacleParameters.AirDropBoxParameters.blood





  //todo
  override def getObjectRect(): model.Rectangle = null



  //todo 被子弹攻击，血量下降，当血量小于0，随机爆出道具
  def attacked(bullet: Bullet,destroyCallBack:Obstacle => Unit):Unit = {}

}
