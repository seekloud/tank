package com.neo.sk.tank.shared.`object`

import com.neo.sk.tank.shared.config.TankGameConfig
import com.neo.sk.tank.shared.model
import com.neo.sk.tank.shared.model.Point

/**
  * Created by hongruying on 2018/7/8
  * 子弹
  */
case class BulletState(bId:Int,tankId:Int,position:Point,damage:Int,startPosition:Point,createTime:Long, name:String, direction:Float)

case class Bullet(
                 config:TankGameConfig,
                 override protected var position: Point,
                 damage:Int, //威力
                 startPosition: model.Point, //起始位置
                 createTime:Long,
                 bId:Int,
                 tankId:Int,
                 tankName:String,
                 direction:Float
                 ) extends CircleObjectOfGame{

  def this(config:TankGameConfig, bulletState: BulletState){
    this(config,bulletState.position,bulletState.damage,bulletState.startPosition,bulletState.createTime,bulletState.bId,bulletState.tankId,bulletState.name,bulletState.direction)
  }


  val momentum: Point = config.bulletSpeed.rotate(direction) * config.frameDuration / 1000 //速度 每帧子弹运动的距离
  override val radius: Float = config.getBulletRadiusByDamage(damage)

  val maxFlyDistance:Int = config.maxFlyDistance





  // 获取子弹外形
  override def getObjectRect(): model.Rectangle = {
    model.Rectangle(this.position - Point(this.radius,this.radius),this.position + Point(this.radius, this.radius))
  }


  def getBulletState(): BulletState = {
    BulletState(bId,tankId,position,damage,startPosition,createTime,tankName,direction)
  }


  //子弹碰撞检测
  def isIntersectsObject(o: ObjectOfGame):Boolean = {
    this.isIntersects(o)
  }

  // 生命周期是否截至或者打到地图边界
  def isFlyEnd(boundary: Point):Boolean = {
    if( this.position.distance(startPosition) > maxFlyDistance || position.x <= 0 || position.y <= 0 || position.x >= boundary.x || position.y >= boundary.y)
      true
    else
      false
  }

  // 先检测是否生命周期结束，如果没结束继续移动
  def move(boundary: Point,flyEndCallBack:Bullet => Unit):Unit = {
    if(isFlyEnd(boundary)){
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

