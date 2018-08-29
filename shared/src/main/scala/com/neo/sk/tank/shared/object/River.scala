package com.neo.sk.tank.shared.`object`

import com.neo.sk.tank.shared.config.TankGameConfig
import com.neo.sk.tank.shared.model
import com.neo.sk.tank.shared.model.Constants.ObstacleType

/**
  * Created by hongruying on 2018/8/22
  */
case class River(
                  config:TankGameConfig,
                  override val oId: Int,
                  override protected var position: model.Point
                ) extends Obstacle with ObstacleTank{

  def this(config: TankGameConfig,obstacleState: ObstacleState){
    this(config,obstacleState.oId,obstacleState.p)
  }



  override val obstacleType = ObstacleType.river
  override protected val height: Float = config.obstacleWidth
  override protected val width: Float = config.obstacleWidth

  override def getObstacleState():ObstacleState = ObstacleState(oId,obstacleType,None,position)

  override def attackDamage(d: Int): Unit = {}

  override def isLived(): Boolean = true

  override def bloodPercent():Float = 1

}
