package com.neo.sk.tank.shared.ptcl.tank
import com.neo.sk.tank.shared.ptcl.model
import com.neo.sk.tank.shared.ptcl.model.{Point, Rectangle}

/**
  * Created by hongruying on 2018/7/8
  * 游戏中的坦克
  */
case class TankState(userId:Long,tankId:Long,direction:Double,gunDirection:Double,blood:Int,bloodLevel:Int,speedLevel:Int,curBulletNum:Int,position:Point,bulletPowerLevel:Int)

trait Tank extends ObjectOfGame {

  override protected var position: model.Point

  protected val userId:Long

  val tankId:Long

  protected var blood:Int //当前血量

  protected var bloodLevel:Int //血量等级

  protected var speedLevel:Int //移动速度等级



  protected var curBulletNum:Int //当前子弹数量

  protected var direction:Double //移动方向

  protected var gunDirection:Double //

  protected var bulletPowerLevel:Int //子弹等级

  private var isFillingBullet:Boolean = false

  protected val bulletMaxCapacity:Int = model.TankParameters.tankBulletMaxCapacity //子弹最大容量

  def isLived() : Boolean = blood > 0

  def setTankGunDirection(d:Double) = {
    gunDirection = d
  }



  def launchBullet():Option[(Double,Point,Int)] = {
    if(curBulletNum > 0){
      curBulletNum = curBulletNum - 1
      if(!isFillingBullet){
        isFillingBullet = true
        startFillBullet()
      }
      Some(gunDirection,getLaunchBulletPosition(),getTankBulletDamage())
    }else None
  }

  private def getTankBulletDamage():Int = {
    model.TankParameters.TankBulletBulletPowerLevel.getBulletDamage(bulletPowerLevel)
  }

  def fillABullet():Unit = {
    if(curBulletNum < bulletMaxCapacity) {
      curBulletNum += 1
      if(curBulletNum >= bulletMaxCapacity) isFillingBullet = false
      else startFillBullet()
    }
  }

  // 获取坦克状态
  def getTankState():TankState = {
    TankState(userId,tankId,direction,gunDirection,blood,bloodLevel,speedLevel,curBulletNum,position,bulletPowerLevel)
  }

  //  开始填充炮弹
  protected def startFillBullet() :Unit



  // TODO: 获取发射子弹位置
  private def getLaunchBulletPosition():Point = {
    Point(20,30)
  }


  // TODO: 根据坦克的位置获取坦克的外形，目前考虑以矩形来代表坦克 待实现
  override def getObjectRect(): Rectangle = {
    null
  }

  //todo
  def attackedBullet(bullet: Bullet,destroy:(Bullet,Tank) => Unit):Unit = {

  }


  //检测是否获得道具
  def checkEatProp(p:Prop,obtainPropCallback:Prop => Unit):Unit = {

  }

  // TODO: 根据方向和地图边界以及地图所有的障碍物和坦克（不包括子弹）进行碰撞检测和移动
  def move(direction:Double,boundary: Point, otherObject:Seq[ObjectOfGame]):Unit = {

  }

  def isIntersectsObject(o:Seq[ObjectOfGame]):Boolean = {
    false
  }


  def attackedDamage(d:Int):Unit = {
    blood -= d
  }

  def eatProp(p:Prop):Unit = {

  }
}
