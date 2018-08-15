package com.neo.sk.tank.shared.ptcl.tank

import com.neo.sk.tank.shared.ptcl.model
import com.neo.sk.tank.shared.ptcl.model.Point

trait Steel extends Obstacle {
  override val oId: Int
  override val obstacleType: Byte
  override var position: Point

  override def getObstacleState(): ObstacleState = ObstacleState(oId, obstacleType, None, position)

  override protected val height: Float = model.ObstacleParameters.SteelParameters.border
  override protected val width: Float = model.ObstacleParameters.SteelParameters.border

  override def attackDamage(d: Int): Unit

  override def isLived(): Boolean = true

  override def getObjectRect(): model.Rectangle

  override def getObstacleType:Byte = obstacleType





}
