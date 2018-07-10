package com.neo.sk.tank.shared.ptcl.tank

import com.neo.sk.tank.shared.ptcl.model

/**
  * Created by hongruying on 2018/7/9
  */
trait Brick extends Obstacle{

  override protected val oId: Long

  override protected val obstacleType:Int = model.ObstacleParameters.ObstacleType.brick



  protected var curBlood:Int //物体血量

  override protected var position: model.Point


  private val maxBlood:Int = model.ObstacleParameters.BrickDropBoxParameters.blood





  //todo
  override def getObjectRect(): model.Rectangle = null



  //todo 被子弹攻击，血量下降，当血量小于0，消失
  def attacked(bullet: Bullet,destroyCallBack:Obstacle => Unit):Unit = {}

}
