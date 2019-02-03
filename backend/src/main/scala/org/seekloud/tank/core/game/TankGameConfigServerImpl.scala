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

package org.seekloud.tank.core.game

import akka.util.Helpers
import com.typesafe.config.Config
import org.seekloud.tank.shared.config._
import org.seekloud.tank.shared.model.Constants.PropAnimation
import org.seekloud.tank.shared.model.Point

/**
  * Created by hongruying on 2018/8/21
  */
final case class TankGameConfigServerImpl(
                                           config:Config
                                         ) extends TankGameConfig {

  import Helpers.Requiring

  import collection.JavaConverters._

  private[this] val gridBoundaryWidth = config.getInt("tankGame.gridBoundary.width")
    .requiring(_ > 100,"minimum supported grid boundary width is 100")
  private[this] val gridBoundaryHeight = config.getInt("tankGame.gridBoundary.height")
    .requiring(_ > 50,"minimum supported grid boundary height is 50")
  private[this] val gridBoundary = GridBoundary(gridBoundaryWidth,gridBoundaryHeight)

  private[this] val gameFameDuration = config.getLong("tankGame.frameDuration")
    .requiring(t => t >= 1l,"minimum game frame duration is 1 ms")
  private[this] val gamePlayRate = config.getInt("tankGame.playRate")
    .requiring(t => t >= 1,"minimum game playRate duration is 1")
  private[this] val gameReplayRate = config.getInt("tankGame.replayRate")
    .requiring(t => t >= 1,"minimum game playRate duration is 1")

  private[this] val bulletRadius = config.getDoubleList("tankGame.bullet.bulletRadius")
    .requiring(_.size() >= 3,"bullet radius size has 3 type").asScala.toList.map(_.toFloat)
  private[this] val bulletDamage = config.getIntList("tankGame.bullet.bulletDamage")
    .requiring(_.size() >= 3,"bullet damage size has 3 type").asScala.toList.map(_.toInt)
  private[this] val maxFlyFrameData = config.getInt("tankGame.bullet.maxFlyFrame")
    .requiring(_ > 0,"minimum bullet max fly frame is 1")
  private[this] val bulletSpeedData = config.getInt("tankGame.bullet.bulletSpeed")
    .requiring(_ > 0,"minimum bullet speed is 1")
  private val bulletParameters = BulletParameters(bulletRadius.zip(bulletDamage),maxFlyFrameData,bulletSpeedData)

  private[this] val obstacleWidthData = config.getDouble("tankGame.obstacle.width")
    .requiring(_ > 0,"minimum supported obstacle width is 1").toFloat
  private[this] val collisionWOffset = config.getDouble("tankGame.obstacle.collisionWidthOffset")
    .requiring(_ > 0,"minimum supported obstacle width is 1").toFloat


  private[this] val airDropBloodData = config.getInt("tankGame.obstacle.airDrop.blood")
    .requiring(_ > 0,"minimum supported air drop blood is 1")
  private[this] val airDropNumData = config.getInt("tankGame.obstacle.airDrop.num")
    .requiring(_ >= 0,"minimum supported air drop num is 0")

  private[this] val brickBloodData = config.getInt("tankGame.obstacle.brick.blood")
    .requiring(_ > 0,"minimum supported brick blood is 1")
  private[this] val brickNumData = config.getInt("tankGame.obstacle.brick.num")
    .requiring(_ >= 0,"minimum supported brick num is 0")
  private[this] val riverType = config.getStringList("tankGame.obstacle.river.type").asScala
  private[this] val riverTypePostion = riverType.foldLeft(List[List[(Int, Int)]]()){
    case (r,item) =>
      config.getIntList(s"tankGame.obstacle.river.${item}.x").asScala.map(_.toInt).toList
        .requiring(_.nonEmpty,s"minimum supported tankGame.obstacle.river.${item}.x relative position size is 1").zip(
        config.getIntList(s"tankGame.obstacle.river.${item}.y").asScala.map(_.toInt).toList
          .requiring(_.nonEmpty,s"minimum supported tankGame.obstacle.river.${item}.y relative position size is 1")
      ) :: r
  }

  private[this] val riverBarrierPosition = riverType.foldLeft(List[List[(Int,Int)]]()){
    case (r,item) =>
      config.getIntList(s"tankGame.obstacle.river.$item.barrier_x").asScala.map(_.toInt).toList
      .requiring(_.nonEmpty,s"minimum supported tankGame.obstacle.river.${item}.barrier_x relative position size is 1").zip(
        config.getIntList(s"tankGame.obstacle.river.$item.barrier_y").asScala.map(_.toInt).toList
          .requiring(_.nonEmpty,s"minimum supported tankGame.obstacle.river.${item}.barrier_x relative position size is 1")
      )::r
  }

  private[this] val steelType = config.getStringList("tankGame.obstacle.steel.type").asScala
  private[this] val steelTypePosition = steelType.foldLeft(List[List[(Int, Int)]]()){
    case (r,item) =>
      config.getIntList(s"tankGame.obstacle.steel.${item}.x").asScala.map(_.toInt).toList
        .requiring(_.nonEmpty,s"minimum supported tankGame.obstacle.steel.${item}.x relative position size is 1").zip(
        config.getIntList(s"tankGame.obstacle.steel.${item}.y").asScala.map(_.toInt).toList
          .requiring(_.nonEmpty,s"minimum supported tankGame.obstacle.steel.${item}.y relative position size is 1")
      ) :: r
  }
  private[this] val steelBarrierPosition = steelType.foldLeft(List[List[(Int,Int)]]()){
    case (r,item) =>
      config.getIntList(s"tankGame.obstacle.steel.${item}.barrier_x").asScala.map(_.toInt).toList
        .requiring(_.nonEmpty,s"minimum supported tankGame.obstacle.steel.${item}.barrier_x relative position size is 1").zip(
        config.getIntList(s"tankGame.obstacle.steel.$item.barrier_y").asScala.map(_.toInt).toList
          .requiring(_.nonEmpty,s"minimum supported tankGame.obstacle.steel.${item}.barrier_x relative position size is 1")
      )::r
  }



  private val obstacleParameters = ObstacleParameters(obstacleWidthData,collisionWOffset,
    airDropParameters = AirDropParameters(airDropBloodData,airDropNumData),
    brickParameters = BrickParameters(brickBloodData,brickNumData),
    riverParameters = RiverParameters(riverTypePostion,riverBarrierPosition),
    steelParameters = SteelParameters(steelTypePosition,steelBarrierPosition)
  )

  private[this] val propRadiusData = config.getDouble("tankGame.prop.radius")
    .requiring(_ > 0,"prop radius must more than 0").toFloat
  private[this] val propMedicalBloodData = config.getInt("tankGame.prop.medicalBlood")
    .requiring(_ > 0,"minimum supported prop medicalBlood is 1")
  private[this] val shotgunDurationData = config.getInt("tankGame.prop.shotgunDuration")
    .requiring(_ > 0,"minimum supported prop shotgun duration 1 ms")
  private[this] val disappearTime = config.getInt("tankGame.prop.disappearTime")
    .requiring(_ > PropAnimation.DisAniFrame1 * gameFameDuration,s"minimum supported prop disappearTime PropAnimation.DisAniFrame1 * frameDuration=${PropAnimation.DisAniFrame1 * frameDuration} ms")

  private val propParameters = PropParameters(propRadiusData,propMedicalBloodData, shotgunDurationData,disappearTime)

  private[this] val tankLivesLimit = config.getInt("tankGame.tank.livesLimit")
  private[this] val tankMedicalLimit = config.getInt("tankGame.tank.medicalLimit")
  private[this] val tankSpeedLevel = config.getIntList("tankGame.tank.tankSpeedLevel")
    .requiring(_.size() >= 3,"minimum supported tank speed size is 3").asScala.map(_.toInt).toList
  private[this] val accelerationTime = config.getIntList("tankGame.tank.accelerationTime")
    .requiring(_.size() >= 3,"minimum supported tank acceleration time size is 3").asScala.map(_.toInt).toList
  private[this] val decelerationTime = config.getIntList("tankGame.tank.decelerationTime")
    .requiring(_.size() >= 3,"minimum supported tank deceleration time size is 3").asScala.map(_.toInt).toList
  private[this] val tankBloodLevel = config.getIntList("tankGame.tank.tankBloodLevel")
    .requiring(_.size() >= 3,"minimum supported tank blood size is 3").asScala.map(_.toInt).toList
  private[this] val tankRadiusData = config.getDouble("tankGame.tank.radius")
    .requiring(_ > 0,"minimum supported tank radius is 1").toFloat
  private[this] val tankGunWidthData = config.getInt("tankGame.tank.gunWidth")
    .requiring(_ > 0,"minimum supported tank gun width is 1")
  private[this] val tankGunHeightData = config.getInt("tankGame.tank.gunHeight")
    .requiring(_ > 0,"minimum supported tank gun height is 1")
  private[this] val tankMaxBulletCapacity = config.getInt("tankGame.tank.maxBulletCapacity")
    .requiring(_ > 0,"minimum supported tank max bullet capacity is 1")
  private[this] val tankFillBulletDuration = config.getInt("tankGame.tank.fillBulletDuration")
    .requiring(_ > 0,"minimum supported tank fill bullet duration is 1ms")
  private[this] val tankInvincibleDuration = config.getInt("tankGame.tank.initInvincibleDuration")
    .requiring(_ > 0,"minimum supported tank invincible duration is 1ms")
  private[this] val tankReliveDuration = config.getInt("tankGame.tank.tankReliveDuration")
    .requiring(_ > 0,"minimum supported tank relive duration is 1ms")
  private val tankParameters = TankParameters(TankMoveSpeed(tankSpeedLevel,accelerationTime,decelerationTime),tankBloodLevel,tankLivesLimit,tankMedicalLimit,
    tankRadiusData,tankGunWidthData,tankGunHeightData,tankMaxBulletCapacity,tankFillBulletDuration,tankInvincibleDuration,tankReliveDuration)

  private val tankGameConfig = TankGameConfigImpl(gridBoundary,gameFameDuration,gamePlayRate,gameReplayRate,bulletParameters,obstacleParameters,propParameters,tankParameters)


  def getTankGameConfig:TankGameConfigImpl = tankGameConfig

  override def getTankLivesLimit: Int = tankGameConfig.tankParameters.tankLivesLimit
  def getTankMedicalLimit:Int = tankGameConfig.tankParameters.tankMedicalLimit





  def frameDuration:Long = tankGameConfig.frameDuration
  def playRate:Int = tankGameConfig.playRate
  def replayRate:Int = tankGameConfig.replayRate

  def getBulletRadius(l:Byte):Float = tankGameConfig.getBulletRadius(l)
  def getBulletDamage(l:Byte):Int = tankGameConfig.getBulletDamage(l)

  def maxFlyFrame:Int = tankGameConfig.maxFlyFrame

  def bulletSpeed:Point = tankGameConfig.bulletSpeed
  def getBulletRadiusByDamage(d:Int):Float = tankGameConfig.getBulletRadiusByDamage(d)



  def boundary:Point = tankGameConfig.boundary

  def obstacleWidth:Float = tankGameConfig.obstacleWidth

  def airDropBlood:Int = tankGameConfig.airDropBlood
  def airDropNum:Int = tankGameConfig.airDropNum

  def brickBlood:Int = tankGameConfig.brickBlood
  def brickNum:Int = tankGameConfig.brickNum

  def riverPosType:List[List[(Int, Int)]] = tankGameConfig.riverPosType
  def steelPosType:List[List[(Int, Int)]] = tankGameConfig.steelPosType
  def barrierPos4River:List[List[(Int,Int)]] = tankGameConfig.barrierPos4River
  def barrierPos4Steel:List[List[(Int,Int)]] = tankGameConfig.barrierPos4Steel

  def propRadius:Float = tankGameConfig.propRadius
  def propMedicalBlood:Int = tankGameConfig.propMedicalBlood
  def shotgunDuration:Int = tankGameConfig.shotgunDuration


  def tankRadius:Float = tankGameConfig.tankRadius
  def tankGunWidth:Float = tankGameConfig.tankGunWidth
  def tankGunHeight:Float = tankGameConfig.tankGunHeight
  def maxBulletCapacity:Int = tankGameConfig.maxBulletCapacity
  def fillBulletDuration:Int = tankGameConfig.fillBulletDuration
  def initInvincibleDuration:Int = tankGameConfig.initInvincibleDuration

  def getTankReliveDuration: Int = tankGameConfig.getTankReliveDuration
  def getTankSpeedByType(t:Byte):Point = tankGameConfig.getTankSpeedByType(t)

  def getTankBloodByLevel(l:Byte):Int = tankGameConfig.getTankBloodByLevel(l)

  def getTankGameConfigImpl(): TankGameConfigImpl = tankGameConfig

  def getBulletLevel(damage:Int):Byte = tankGameConfig.getBulletLevel(damage)

  def getTankSpeedMaxLevel():Byte = tankGameConfig.getTankSpeedMaxLevel()

  def getTankBloodMaxLevel():Byte = tankGameConfig.getTankBloodMaxLevel()

  def getBulletMaxLevel():Byte = tankGameConfig.getBulletMaxLevel()

  def getTankAccByLevel(l: Byte): Int = tankGameConfig.getTankAccByLevel(l)
  def getTankDecByLevel(l: Byte): Int = tankGameConfig.getTankDecByLevel(l)
  def obstacleWO: Float = tankGameConfig.obstacleWO

  def getPropDisappearFrame: Short = tankGameConfig.getPropDisappearFrame



}