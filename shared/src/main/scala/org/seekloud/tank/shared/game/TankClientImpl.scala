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

package org.seekloud.tank.shared.game

import org.seekloud.tank.shared.`object`.{Prop, Tank, TankState}
import org.seekloud.tank.shared.config.TankGameConfig
import org.seekloud.tank.shared.model
import org.seekloud.tank.shared.model.Constants.TankColor
import org.seekloud.tank.shared.model.Point
import org.seekloud.tank.shared.util.QuadTree

/**
  * Created by sky
  * Date on 2018/11/13
  * Time at 上午10:43
  */
case class TankClientImpl(
                           fillBulletCallBack: Int => Unit,
                           tankShotgunExpireCallBack:Int=> Unit,
                           config: TankGameConfig,
                           userId: String,
                           tankId: Int,
                           name: String,
                           protected var blood: Int,
                           tankColorType: Byte,
                           protected var position: model.Point,
                           protected var curBulletNum: Int,
                           var lives: Int,
                           var medicalNumOpt: Option[Int],
                           protected var bloodLevel: Byte = 1, //血量等级
                           protected var speedLevel: Byte = 1, //移动速度等级
                           protected var bulletLevel: Byte = 1, //子弹等级
                           protected var direction: Float = 0, //移动状态
                           protected var gunDirection: Float = 0,
                           protected var shotgunState: Boolean = false,
                           protected var invincibleState: Boolean = true,
                           var killTankNum: Int = 0,
                           var damageStatistics: Int = 0,
                           var speed: Point,
                           protected var isMove: Boolean
                         ) extends Tank {

  def this(config: TankGameConfig, tankState: TankState, fillBulletCallBack: Int => Unit, tankShotgunExpireCallBack:Int=> Unit) {
    this(fillBulletCallBack, tankShotgunExpireCallBack, config, tankState.userId, tankState.tankId, tankState.name, tankState.blood, tankState.tankColorType, tankState.position, tankState.curBulletNum,
      tankState.lives, tankState.medicalNumOpt, tankState.bloodLevel, tankState.speedLevel, tankState.bulletPowerLevel, tankState.direction, tankState.gunDirection, tankState.shotgunState, tankState.invincible, tankState.killTankNum, tankState.damageTank,
      tankState.speed, tankState.isMove)
  }


  val bulletMaxCapacity: Int = config.maxBulletCapacity
  override val radius: Float = config.tankRadius

  override def startFillBullet(): Unit = {
    fillBulletCallBack(tankId)
  }

  final def getInvincibleState = invincibleState


  def getPosition4Animation(boundary: Point, quadTree: QuadTree, offSetTime: Long): Point = {
    val canvasFrameLeft = if (fakeFrame < 3) 4 else 5
    val logicMoveDistanceOpt = this.canMove(boundary, quadTree, canvasFrameLeft)(config)
    if (logicMoveDistanceOpt.nonEmpty) {
      if (!isFakeMove && (canvasFrame <= 0 || canvasFrame >= canvasFrameLeft)) {
        this.position + logicMoveDistanceOpt.get / config.frameDuration * offSetTime
      } else {
        this.fakePosition + logicMoveDistanceOpt.get / config.frameDuration * offSetTime
      }
    } else position
  }

  def getGunPositions4Animation(): List[Point] = {
    val gunWidth = config.tankGunWidth
    if (this.shotgunState) {
      val gunHeight = config.tankGunHeight
      List(
        Point(0, -gunHeight / 2).rotate(this.gunDirection),
        Point(0, gunHeight / 2).rotate(this.gunDirection),
        Point(gunWidth, gunHeight).rotate(this.gunDirection),
        Point(gunWidth, -gunHeight).rotate(this.gunDirection)
      )
    } else {
      val gunHeight = config.tankGunHeight * (1 + (this.bulletLevel - 1) * 0.1f)
      List(
        Point(0, -gunHeight / 2).rotate(this.gunDirection),
        Point(0, gunHeight / 2).rotate(this.gunDirection),
        Point(gunWidth, gunHeight / 2).rotate(this.gunDirection),
        Point(gunWidth, -gunHeight / 2).rotate(this.gunDirection)
      )
    }
  }


  def getTankColor() = {
    TankColor.tankColorList(this.tankColorType)
  }

  def getMaxBlood = config.getTankBloodByLevel(bloodLevel)

  def getCurBlood = blood

  def getCurBulletNum = curBulletNum

  def getBloodLevel = bloodLevel

  def getBulletLevel = bulletLevel

  def getSpeedLevel = speedLevel

  def getCurMedicalNum = medicalNumOpt match {
    case Some(num) => num
    case None => 0
  }

  override def eatProp(p: Prop)(implicit config: TankGameConfig): Unit = {
    super.eatProp(p)
    if(p.propType == 5){
      tankShotgunExpireCallBack(tankId)
    }
  }


  def getSliderPositionByBloodLevel(num: Int, sliderLength: Float, width: Float, greyLength: Float) = {
    val startPoint = Point(sliderLength / 2, -(3 + getRadius))
    var positionList: List[Point] = startPoint :: Nil
    for (i <- 2 to 2 * num) {
      if (i % 2 == 0) {
        val position = startPoint - Point(i / 2 * width + (i / 2 - 1) * greyLength / (num - 1), 0)
        positionList = position :: positionList
      }
      else {
        val position = startPoint - Point(i / 2 * (width + greyLength / (num - 1)), 0)
        positionList = position :: positionList
      }
    }
    positionList
  }
}
