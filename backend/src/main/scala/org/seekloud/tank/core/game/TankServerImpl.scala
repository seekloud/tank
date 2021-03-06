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

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.TimerScheduler
import org.seekloud.tank.shared.`object`.{Prop, Tank, TankState}
import org.seekloud.tank.shared.config.TankGameConfig
import org.seekloud.tank.shared.model
import org.seekloud.tank.shared.model.Point
import org.seekloud.tank.core.RoomActor

import concurrent.duration._

/**
  * Created by hongruying on 2018/8/29
  */
case class TankServerImpl(
                           fillBulletCallBack: Int => Unit,
                           tankShotgunExpireCallBack:Int=> Unit,
                           config:TankGameConfig,
                           userId : String,
                           tankId : Int,
                           name : String,
                           protected var blood:Int,
                           tankColorType:Byte,
                           protected var position:model.Point,
                           protected var curBulletNum:Int,
                           var lives:Int,
                           var medicalNumOpt:Option[Int],
                           protected var bloodLevel:Byte = 1, //血量等级
                           protected var speedLevel:Byte = 1, //移动速度等级
                           protected var bulletLevel:Byte = 1, //子弹等级
                           protected var direction:Float = 0, //移动状态
                           protected var gunDirection:Float = 0,
                           protected var shotgunState:Boolean = false,
                           protected var invincibleState:Boolean = true,
                           var killTankNum:Int = 0,
                           var damageStatistics:Int = 0,
                           var speed: Point = Point(0,0),
                           protected var isMove: Boolean = false
                         ) extends Tank{

  def this(config: TankGameConfig,tankState: TankState,fillBulletCallBack: Int => Unit, tankShotgunExpireCallBack:Int=> Unit){
    this(fillBulletCallBack,tankShotgunExpireCallBack,config,tankState.userId,tankState.tankId,tankState.name,tankState.blood,tankState.tankColorType,tankState.position,tankState.curBulletNum,tankState.lives,tankState.medicalNumOpt,
      tankState.bloodLevel,tankState.speedLevel,tankState.bulletPowerLevel,tankState.direction,tankState.gunDirection,tankState.shotgunState,tankState.invincible,tankState.killTankNum,tankState.damageTank,tankState.speed,tankState.isMove)
  }



  val bulletMaxCapacity:Int = config.maxBulletCapacity
  override val radius: Float = config.tankRadius



  override def startFillBullet(): Unit = {
    fillBulletCallBack(tankId)
  }

  override def eatProp(p: Prop)(implicit config: TankGameConfig): Unit = {
    super.eatProp(p)
    if(p.propType == 5){
      tankShotgunExpireCallBack(tankId)
    }
  }
}

