package com.neo.sk.tank.shared.ptcl.tank
import com.neo.sk.tank.shared.ptcl.model
import com.neo.sk.tank.shared.ptcl.model.{Point, Rectangle}

/**
  * Created by hongruying on 2018/7/8
  * 游戏中的坦克
  */
case class TankState(userId:Long,tankId:Int,direction:Float,gunDirection:Float,blood:Int,bloodLevel:Byte,speedLevel:Byte,curBulletNum:Int,position:Point,bulletPowerLevel:Byte,tankColorType:Byte,
                     name:String,killTankNum:Int,damageTank:Int,invincible:Boolean)

trait Tank extends CircleObjectOfGame {

  override var position: model.Point

  protected val userId:Long

  val tankId:Int
  val name:String
  var killTankNum:Int
  var damageTank:Int

  protected var blood:Int //当前血量

  protected var bloodLevel:Byte //血量等级

  protected var speedLevel:Byte //移动速度等级






  protected var curBulletNum:Int //当前子弹数量

  protected var direction:Float //移动方向

  protected var gunDirection:Float //

  protected var bulletPowerLevel:Byte //子弹等级

  protected val tankColorType:Byte

  protected var invincible:Boolean

  private var isFillingBullet:Boolean = false

  protected val bulletMaxCapacity:Int = model.TankParameters.tankBulletMaxCapacity //子弹最大容量

  override val r: Float = model.TankParameters.TankSize.r

  def isLived() : Boolean = blood > 0

  def setTankGunDirection(d:Float) = {
    gunDirection = d
  }

  def setTankGunDirectionByOffset(o:Float) = {
    gunDirection += o
  }



  def launchBullet():Option[(Float,Point,Int)] = {
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

  def isInvincibleTime():Unit ={
    invincible = false
  }

  // 获取坦克状态
  def getTankState():TankState = {
    TankState(userId,tankId,direction,gunDirection,blood,bloodLevel,speedLevel,curBulletNum,position,bulletPowerLevel,tankColorType,name,killTankNum,damageTank,invincible)
  }

  //  开始填充炮弹
  protected def startFillBullet() :Unit



  // 获取发射子弹位置
  private def getLaunchBulletPosition():Point = {
    position + Point(model.TankParameters.TankSize.gunLen,0).rotate(gunDirection)
  }


//  //
//  override def getObjectRect(): Rectangle = {
//    Rectangle(position- Point(model.TankParameters.TankSize.r,model.TankParameters.TankSize.r),position + Point(model.TankParameters.TankSize.r,model.TankParameters.TankSize.r))
//  }

  //todo
  def attackedBullet(bullet: Bullet,destroy:(Bullet,Tank) => Unit):Unit = {
    if(invincible == false) {
      this.blood -= bullet.damage
      if (!isLived()) destroy(bullet, this)
    }
  }


  //检测是否获得道具
  def checkEatProp(p:Prop,obtainPropCallback:Prop => Unit):Unit = {
    if(this.isIntersects(p)){
      eatProp(p)
      obtainPropCallback(p)
    }
  }

  // 根据方向和地图边界以及地图所有的障碍物和坦克（不包括子弹）进行碰撞检测和移动
  def move(direction:Float,boundary: Point,quadTree: QuadTree):Unit = {
    this.direction = direction
    val originPosition = this.position
    this.position = this.position + model.TankParameters.SpeedType.getMoveByFrame(speedLevel).rotate(direction)
    val movedRec = Rectangle(this.position-Point(model.TankParameters.TankSize.r,model.TankParameters.TankSize.r),this.position+Point(model.TankParameters.TankSize.r,model.TankParameters.TankSize.r))
    val otherObjects = quadTree.retrieveFilter(this).filter(t => t.isInstanceOf[Tank] || t.isInstanceOf[Obstacle])

    if(!otherObjects.exists(t => t.isIntersects(this)) && movedRec.topLeft > model.Point(0,0) && movedRec.downRight < boundary){
      //更新坦克的位置
      quadTree.updateObject(this)
    }else{
      this.position = originPosition
    }
  }

  def canMove(direction:Float,boundary:Point,quadTree:QuadTree):Boolean = {
    val originPosition = this.position
    this.position = this.position + model.TankParameters.SpeedType.getMoveByFrame(speedLevel).rotate(direction)
    val movedRec = Rectangle(this.position-Point(model.TankParameters.TankSize.r,model.TankParameters.TankSize.r),this.position+Point(model.TankParameters.TankSize.r,model.TankParameters.TankSize.r))
    val otherObjects = quadTree.retrieveFilter(this).filter(t => t.isInstanceOf[Tank] || t.isInstanceOf[Obstacle])
    val result = if(!otherObjects.exists(t => t.isIntersects(this)) && movedRec.topLeft > model.Point(0,0) && movedRec.downRight < boundary){
      true
    }else{
      false
    }
    this.position = originPosition
    result
  }

  def isIntersectsObject(o:Seq[ObjectOfGame]):Boolean = {
    o.exists(t => t.isIntersects(this))
  }


  def attackedDamage(d:Int):Unit = {
    if(invincible == false)
    blood -= d
  }

  def eatProp(p:Prop):Unit = {
    p.getPropState.t match {
      case 1 =>
        if(bloodLevel < 3){
          val diff = model.TankParameters.TankBloodLevel.getTankBlood(bloodLevel) - blood
          bloodLevel = (bloodLevel + 1).toByte
          blood = model.TankParameters.TankBloodLevel.getTankBlood(bloodLevel) - diff
        }
      case 2 => if(speedLevel < 3) speedLevel = (speedLevel + 1).toByte
      case 3 => if(bulletPowerLevel < 3) bulletPowerLevel = (bulletPowerLevel + 1).toByte
      case 4 =>
        blood += model.TankParameters.addBlood
        if(blood > model.TankParameters.TankBloodLevel.getTankBlood(bloodLevel)){
          blood = model.TankParameters.TankBloodLevel.getTankBlood(bloodLevel)
        }
    }

  }
}
