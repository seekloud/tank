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

package org.seekloud.tank.shared.config

import org.seekloud.tank.shared.model.Point

/**
  * Created by hongruying on 2018/8/28
  */
final case class GridBoundary(width:Int,height:Int){
  def getBoundary:Point = Point(width,height)
}

final case class GridLittleMap(width:Int,height:Int){
  def getBoundary:Point = Point(width,height)
}

final case class TankMoveSpeed(
                                speeds:List[Float],
                                accelerationTime:List[Int],
                                decelerationTime:List[Int]
                              ){

  def getTankSpeedByType(t:Byte) = Point(speeds(t-1),0)
}

final case class TankBloodLevel(
                                 first:Int,
                                 second:Int,
                                 third:Int
                               ){

  def getTankBloodByLevel(level:Int) :Int  = {
    level match {
      case 2 => second
      case 3 => third
      case _ => first
    }
  }

}

final case class TankParameters(
                                 tankSpeed:TankMoveSpeed,
                                 tankBloodLevel: List[Int],
                                 tankLivesLimit:Int,
                                 tankMedicalLimit:Int,
                                 tankRadius:Float,
                                 tankGunWidth:Float,
                                 tankGunHeight:Float,
                                 maxBulletCapacity:Int,
                                 fillBulletFrame:Int,
                                 initInvincibleFrame:Int,
                                 tankReliveFrame:Int
                               ){
  def getTankBloodByLevel(l:Byte):Int = tankBloodLevel(l-1)
}

final case class PropParameters(
                                 radius:Float,
                                 medicalBlood:Int,
                                 shotgunDuration:Int, //散弹持续时间
                                 disappearFrame:Int
                               )

final case class AirDropParameters(
                                    blood:Int,
                                    num:Int
                                  )

final case class BrickParameters(
                                  blood:Int,
                                  num:Int
                                )

final case class RiverParameters(
                                  typePos:List[List[(Int, Int)]], //河流的元素位置
                                  barrierPos:List[List[(Int,Int)]]
                                )

final case class SteelParameters(
                                  typePos:List[List[(Int, Int)]], //钢铁的元素位置
                                  barrierPos:List[List[(Int,Int)]]
                                )

final case class ObstacleParameters(
                                     width:Float,
                                     collisionWidthOffset: Float,
                                     airDropParameters: AirDropParameters,
                                     brickParameters: BrickParameters,
                                     riverParameters: RiverParameters,
                                     steelParameters: SteelParameters
                                   )


final case class BulletParameters(
                                   bulletLevelParameters:List[(Float,Int)], //size,damage length 3
                                   maxFlyFrame:Int,
                                   bulletSpeed:Int,
                                 ){
  require(bulletLevelParameters.size >= 3,println(s"bullet level parameter failed"))

  def getBulletRadius(l:Byte) = {
    bulletLevelParameters(l-1)._1
  }

  def getBulletDamage(l:Byte) = {
    bulletLevelParameters(l-1)._2
  }

  def getBulletRadiusByDamage(d:Int):Float = {
    bulletLevelParameters.find(_._2 == d).map(_._1).getOrElse(bulletLevelParameters.head._1)
  }

  def getBulletLevelByDamage(d:Int):Byte = {
    (bulletLevelParameters.zipWithIndex.find(_._1._2 == d).map(_._2).getOrElse(0) + 1).toByte
  }
}

trait TankGameConfig{
  def frameDuration:Long
  def playRate:Int
  def replayRate:Int

  def getBulletRadius(l:Byte):Float
  def getBulletDamage(l:Byte):Int
  def getBulletLevel(damage:Int):Byte
  def getBulletMaxLevel():Byte

  def maxFlyFrame:Int

  def bulletSpeed:Point



  def boundary:Point

  def obstacleWidth:Float
  def obstacleWO: Float

  def airDropBlood:Int
  def airDropNum:Int

  def brickBlood:Int
  def brickNum:Int

  def riverPosType:List[List[(Int, Int)]]
  def steelPosType:List[List[(Int, Int)]]

  def barrierPos4River:List[List[(Int,Int)]]
  def barrierPos4Steel:List[List[(Int,Int)]]

  def propRadius:Float
  def propMedicalBlood:Int
  def shotgunDuration:Int


  def tankRadius:Float
  def tankGunWidth:Float
  def tankGunHeight:Float
  def maxBulletCapacity:Int
  def fillBulletFrame:Int
  def initInvincibleFrame:Int
  def getTankReliveFrame:Int
  def getTankSpeedByType(t:Byte):Point
  def getTankAccByLevel(l: Byte): Int
  def getTankDecByLevel(l: Byte): Int
  def getTankSpeedMaxLevel():Byte
  def getTankBloodByLevel(l:Byte):Int
  def getTankBloodMaxLevel():Byte
  def getTankLivesLimit:Int
  def getTankMedicalLimit:Int

  def getBulletRadiusByDamage(d:Int):Float

  def getMoveDistanceByFrame(t:Byte) = getTankSpeedByType(t)

  def getTankGameConfigImpl(): TankGameConfigImpl

  def getPropDisappearFrame: Short
}

case class TankGameConfigImpl(
                               gridBoundary: GridBoundary,
                               frameDuration:Long,
                               playRate: Int,
                               replayRate: Int,
                               bulletParameters: BulletParameters,
                               obstacleParameters: ObstacleParameters,
                               propParameters: PropParameters,
                               tankParameters: TankParameters
                             ) extends TankGameConfig{

  def getTankGameConfigImpl(): TankGameConfigImpl = this


  def getBulletRadius(l:Byte) = {
    bulletParameters.getBulletRadius(l)
  }

  def getBulletDamage(l:Byte) = {
    bulletParameters.getBulletDamage(l)
  }

  def getBulletLevel(damage:Int):Byte = bulletParameters.getBulletLevelByDamage(damage)

  def getBulletMaxLevel():Byte = bulletParameters.bulletLevelParameters.size.toByte

  def maxFlyFrame = bulletParameters.maxFlyFrame
  def getBulletRadiusByDamage(d:Int):Float = bulletParameters.getBulletRadiusByDamage(d)

  def bulletSpeed = Point(bulletParameters.bulletSpeed,0)



  def boundary = gridBoundary.getBoundary

  def obstacleWidth = obstacleParameters.width

  def airDropBlood = obstacleParameters.airDropParameters.blood
  def airDropNum = obstacleParameters.airDropParameters.num

  def brickBlood = obstacleParameters.brickParameters.blood
  def brickNum = obstacleParameters.brickParameters.num

  def riverPosType = obstacleParameters.riverParameters.typePos
  def steelPosType = obstacleParameters.steelParameters.typePos

  def barrierPos4River: List[List[(Int,Int)]] = obstacleParameters.riverParameters.barrierPos
  def barrierPos4Steel: List[List[(Int,Int)]] = obstacleParameters.steelParameters.barrierPos

  def propRadius = propParameters.radius
  def propMedicalBlood = propParameters.medicalBlood
  def shotgunDuration = propParameters.shotgunDuration


  def tankRadius = tankParameters.tankRadius
  def tankGunWidth = tankParameters.tankGunWidth
  def tankGunHeight = tankParameters.tankGunHeight
  def maxBulletCapacity = tankParameters.maxBulletCapacity
  def fillBulletFrame = tankParameters.fillBulletFrame
  def initInvincibleFrame = tankParameters.initInvincibleFrame
  def getTankReliveFrame = tankParameters.tankReliveFrame
  def getTankSpeedByType(t:Byte) = tankParameters.tankSpeed.getTankSpeedByType(t)

  def getTankSpeedMaxLevel():Byte = tankParameters.tankSpeed.speeds.size.toByte

  def getTankBloodMaxLevel():Byte = tankParameters.tankBloodLevel.size.toByte

  def getTankBloodByLevel(l:Byte):Int = tankParameters.getTankBloodByLevel(l)

  def getTankLivesLimit: Int = tankParameters.tankLivesLimit
  def getTankMedicalLimit:Int = tankParameters.tankMedicalLimit


  def getTankAccByLevel(l: Byte): Int = tankParameters.tankSpeed.accelerationTime(l - 1)
  def getTankDecByLevel(l: Byte): Int = tankParameters.tankSpeed.decelerationTime(l - 1)
  def obstacleWO: Float = obstacleParameters.collisionWidthOffset

  def getPropDisappearFrame: Short = propParameters.disappearFrame.toShort




}
