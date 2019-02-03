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
import org.seekloud.tank.shared.model.Point

/**
  * Created by hongruying on 2018/7/8
  * 子弹
  */

case class BulletState(bId:Int, tankId:Int, startFrame:Long, position:Point, damage:Byte, momentum:Point)


case class Bullet(
                 config:TankGameConfig,
                 override protected var position: Point,
                 startFrame:Long,
                 damage:Int, //威力
                 momentum:Point,
                 bId:Int,
                 tankId:Int
                 ) extends CircleObjectOfGame{

  def this(config:TankGameConfig, bulletState: BulletState){
    this(config,bulletState.position,bulletState.startFrame,bulletState.damage.toInt,bulletState.momentum,bulletState.bId,bulletState.tankId)
  }


//  val momentum: Point = momentum
  override val radius: Float = config.getBulletRadiusByDamage(damage)

  val maxFlyFrame:Int = config.maxFlyFrame

  // 获取子弹外形
  override def getObjectRect(): model.Rectangle = {
    model.Rectangle(this.position - Point(this.radius,this.radius),this.position + Point(this.radius, this.radius))
  }


  def getBulletState(): BulletState = {
    BulletState(bId,tankId,startFrame,position,damage.toByte,momentum)
  }


  //子弹碰撞检测
  def isIntersectsObject(o: ObjectOfGame):Boolean = {
    this.isIntersects(o)
  }

  // 生命周期是否截至或者打到地图边界
  def isFlyEnd(boundary: Point,frame:Long):Boolean = {
    if( frame-this.startFrame > maxFlyFrame || position.x <= 0 || position.y <= 0 || position.x >= boundary.x || position.y >= boundary.y)
      true
    else
      false
  }

  // 先检测是否生命周期结束，如果没结束继续移动
  def move(boundary: Point,frame:Long,flyEndCallBack:Bullet => Unit):Unit = {
    if(isFlyEnd(boundary,frame)){
      flyEndCallBack(this)
    } else{
      this.position = this.position + momentum
    }
  }

  // 检测是否子弹有攻击到，攻击到，执行回调函数
  def checkAttackObject[T <: ObjectOfGame](o:T,attackCallBack:T => Unit):Unit = {
    if(this.isIntersects(o)){
      attackCallBack(o)
    }
  }


  def getPosition4Animation(offsetTime:Long) = {
    this.position + momentum / config.frameDuration * offsetTime
  }

  def getBulletLevel() = {
    config.getBulletLevel(damage)
  }


}

