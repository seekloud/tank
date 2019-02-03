/*
 * Copyright 2018 seekloud (https://github.com/seekloud)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.seekloud.tank.shared.`object`

import org.seekloud.tank.shared.config.TankGameConfig
import org.seekloud.tank.shared.model
import org.seekloud.tank.shared.model.Constants.ObstacleType

/**
  * Created by hongruying on 2018/8/22
  */
case class Steel(
                  config:TankGameConfig,
                  override val oId: Int,
                  override protected var position: model.Point,
                ) extends Obstacle with ObstacleTank with ObstacleBullet{

  def this(config: TankGameConfig,obstacleState: ObstacleState){
    this(config,obstacleState.oId,obstacleState.p)
  }

  override val obstacleType = ObstacleType.steel
  override protected val height: Float = config.obstacleWidth
  override protected val width: Float = config.obstacleWidth
  override protected val collisionOffset: Float = config.obstacleWO

  override def getObstacleState():ObstacleState = ObstacleState(oId,obstacleType,None,position)

  override def attackDamage(d: Int): Unit = {}

  override def isLived(): Boolean = true

  override def bloodPercent():Float = 1

}
