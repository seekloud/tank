package com.neo.sk.tank.shared.`object`

import com.neo.sk.tank.shared.config.TankGameConfig
import com.neo.sk.tank.shared.model
import com.neo.sk.tank.shared.model.Constants.ObstacleType

/**
  * Created by hongruying on 2018/8/22
  * 道具箱
  */
case class AirDropBox(
                     config:TankGameConfig,
                     override val oId: Int,
                     override protected var position: model.Point,
                     protected var curBlood :Int //物体血量
                     ) extends Obstacle with ObstacleBullet with ObstacleTank{

  def this(config: TankGameConfig,obstacleState: ObstacleState){
    this(config,obstacleState.oId,obstacleState.p,obstacleState.b.getOrElse(config.airDropBlood))
  }

  override val obstacleType = ObstacleType.airDropBox
  override protected val height: Float = config.obstacleWidth
  override protected val width: Float = config.obstacleWidth
  override protected val collisionOffset: Float = config.obstacleWO



  val maxBlood : Int = config.airDropBlood

  def getObstacleState():ObstacleState = ObstacleState(oId,obstacleType,Some(curBlood),position)


  override def attackDamage(d: Int): Unit = {
    curBlood -= d
  }

  override def isLived(): Boolean = {
    if(curBlood > 0) true
    else false
  }

  override def bloodPercent():Float = curBlood.toFloat / maxBlood

}
