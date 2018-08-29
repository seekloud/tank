package com.neo.sk.tank.shared.`object`

import java.awt.event.KeyEvent

import com.neo.sk.tank.shared.config.TankGameConfig
import com.neo.sk.tank.shared.model
import com.neo.sk.tank.shared.model.Constants.{DirectionType, TankColor}
import com.neo.sk.tank.shared.model.{Point, Rectangle}
import com.neo.sk.tank.shared.util.QuadTree


/**
  * Created by hongruying on 2018/8/22
  */

case class TankState(userId:Long,tankId:Int,direction:Option[Float],gunDirection:Float,blood:Int,bloodLevel:Byte,speedLevel:Byte,curBulletNum:Int,position:Point,bulletPowerLevel:Byte,tankColorType:Byte,
                     name:String,killTankNum:Int,damageTank:Int,invincible:Boolean,shotgunState:Boolean)
trait Tank extends CircleObjectOfGame with ObstacleTank{
  val userId : Long
  val tankId : Int
  val name : String
  var killTankNum:Int
  var damageStatistics:Int
  val tankColorType:Byte
  val bulletMaxCapacity:Int
  protected var blood:Int


  protected var bloodLevel:Byte //血量等级
  protected var speedLevel:Byte //移动速度等级
  protected var bulletLevel:Byte //子弹等级

  protected var curBulletNum:Int
  protected var direction:Option[Float] //移动状态
  protected var gunDirection:Float

  protected var shotgunState:Boolean
  protected var invincibleState:Boolean

  private var isFillBulletState:Boolean = false

  def getShotGunState():Boolean = shotgunState

  def isLived() : Boolean = blood > 0

  def setTankGunDirection(d:Float) = {
    gunDirection = d
  }

  def setTankGunDirectionByOffset(o:Float) = {
    gunDirection += o
  }

  def launchBullet()(implicit config: TankGameConfig):Option[(Float,Point,Int)] = {
    if(curBulletNum > 0){
      curBulletNum = curBulletNum - 1
      if(!isFillBulletState){
        isFillBulletState = true
        startFillBullet()
      }
      Some(gunDirection,getLaunchBulletPosition(),getTankBulletDamage())
    }else None
  }

  private def getTankBulletDamage()(implicit config:TankGameConfig):Int = {
    config.getBulletDamage(bulletLevel)
  }

  def fillABullet():Unit = {
    if(curBulletNum < bulletMaxCapacity) {
      curBulletNum += 1
      if(curBulletNum >= bulletMaxCapacity) isFillBulletState = false
      else startFillBullet()
    }
  }

  def clearInvincibleState():Unit ={
    invincibleState = false
  }

  // 获取坦克状态
  def getTankState():TankState = {
    TankState(userId,tankId,direction,gunDirection,blood,bloodLevel,speedLevel,curBulletNum,position,bulletLevel,tankColorType,name,killTankNum,damageStatistics,invincibleState,shotgunState)
  }

  //  开始填充炮弹
  protected def startFillBullet() :Unit



  // 获取发射子弹位置
  private def getLaunchBulletPosition()(implicit config: TankGameConfig):Point = {
    position + Point(config.tankGunWidth,0).rotate(gunDirection)
  }

  private def getOtherGunDirection(angle:Float) = {
    gunDirection + angle
  }

  def getOtherLaunchBulletPosition(angle:Float)
                                  (implicit config: TankGameConfig) = {
    position + Point(config.tankGunWidth,0).rotate(getOtherGunDirection(angle))
  }


  //被子弹攻击到
  def attackedBullet(bullet: Bullet,destroy:(Bullet,Tank) => Unit):Unit = {
    if(!invincibleState) {
      this.blood -= bullet.damage
      if (!isLived()) destroy(bullet, this)
    }
  }


  //检测是否获得道具
  def checkEatProp(p:Prop,obtainPropCallback:Prop => Unit):Unit = {
    if(this.isIntersects(p)){
      obtainPropCallback(p)
    }
  }

  // 根据方向和地图边界以及地图所有的障碍物和坦克（不包括子弹）进行碰撞检测和移动
  def move(boundary: Point,quadTree: QuadTree)(implicit tankGameConfig: TankGameConfig):Unit = {
    if(this.direction.nonEmpty){
      val originPosition = this.position
      this.position = this.position + tankGameConfig.getMoveDistanceByFrame(speedLevel).rotate(this.direction.get)
      val movedRec = Rectangle(this.position-Point(radius,radius),this.position+Point(radius,radius))
      val otherObjects = quadTree.retrieveFilter(this).filter(_.isInstanceOf[ObstacleTank])

      if(!otherObjects.exists(t => t.isIntersects(this)) && movedRec.topLeft > model.Point(0,0) && movedRec.downRight < boundary){
        quadTree.updateObject(this)
      }else{
        this.position = originPosition
      }
    }
  }

  def canMove(boundary:Point,quadTree:QuadTree)(implicit tankGameConfig: TankGameConfig):Boolean = {
    if(direction.nonEmpty){
      val originPosition = this.position
      this.position = this.position + tankGameConfig.getMoveDistanceByFrame(speedLevel).rotate(this.direction.get)
      val movedRec = Rectangle(this.position - Point(radius, radius), this.position + Point(radius, radius))
      val otherObjects = quadTree.retrieveFilter(this).filter(_.isInstanceOf[ObstacleTank])
      val result = if(!otherObjects.exists(t => t.isIntersects(this)) && movedRec.topLeft > model.Point(0,0) && movedRec.downRight < boundary){
        true
      }else{
        false
      }
      this.position = originPosition
      result
    }else{
      false
    }
  }

  def isIntersectsObject(o:Seq[ObjectOfGame]):Boolean = {
    o.exists(t => t.isIntersects(this))
  }


  def attackedDamage(d:Int):Unit = {
    if(!invincibleState) blood -= d
  }


  def clearShotgunState() = {
    shotgunState = false
  }

  /**
    * 地图接收到吃道具的事件
    * */
  def eatProp(p:Prop)(implicit config: TankGameConfig):Unit = {
    p.getPropState.t match {
      case 1 =>
        if(bloodLevel < 3){
          val diff = config.getTankBloodByLevel(bloodLevel) - blood
          bloodLevel = (bloodLevel + 1).toByte
          blood = config.getTankBloodByLevel(bloodLevel) - diff
        }
      case 2 => if(speedLevel < 3) speedLevel = (speedLevel + 1).toByte
      case 3 => if(bulletLevel < 3) bulletLevel = (bulletLevel + 1).toByte
      case 4 => blood = math.min(blood + config.propMedicalBlood, config.getTankBloodByLevel(bloodLevel))
      case 5 => shotgunState = true
    }
  }


  /**
    * 根据坦克的按键修改坦克的方向状态
    * */
  final def setTankDirection(actionSet:Set[Int]) = {
    direction = getDirection(actionSet)
  }

  private final def getDirection(actionSet:Set[Int]):Option[Float] = {
    if(actionSet.contains(KeyEvent.VK_LEFT) && actionSet.contains(KeyEvent.VK_UP)){
      Some(DirectionType.upLeft.toFloat)
    }else if(actionSet.contains(KeyEvent.VK_RIGHT) && actionSet.contains(KeyEvent.VK_UP)){
      Some(DirectionType.upRight.toFloat)
    }else if(actionSet.contains(KeyEvent.VK_LEFT) && actionSet.contains(KeyEvent.VK_DOWN)){
      Some(DirectionType.downLeft.toFloat)
    }else if(actionSet.contains(KeyEvent.VK_RIGHT) && actionSet.contains(KeyEvent.VK_DOWN)){
      Some(DirectionType.downRight.toFloat)
    }else if(actionSet.contains(KeyEvent.VK_RIGHT)){
      Some(DirectionType.right.toFloat)
    }else if(actionSet.contains(KeyEvent.VK_LEFT)){
      Some(DirectionType.left.toFloat)
    }else if(actionSet.contains(KeyEvent.VK_UP) ){
      Some(DirectionType.up.toFloat)
    }else if(actionSet.contains(KeyEvent.VK_DOWN)){
      Some(DirectionType.down.toFloat)
    }else None
  }

}

case class TankImpl(
                   config: TankGameConfig,
                   userId : Long,
                   tankId : Int,
                   name : String,
                   protected var blood:Int,
                   tankColorType:Byte,
                   protected var position:model.Point,
                   protected var curBulletNum:Int,
                   protected var bloodLevel:Byte = 1, //血量等级
                   protected var speedLevel:Byte = 1, //移动速度等级
                   protected var bulletLevel:Byte = 1, //子弹等级
                   protected var direction:Option[Float] = None, //移动状态
                   protected var gunDirection:Float = 0,
                   protected var shotgunState:Boolean = false,
                   protected var invincibleState:Boolean = true,
                   var killTankNum:Int = 0,
                   var damageStatistics:Int = 0
                   ) extends Tank{

  def this(config: TankGameConfig,tankState: TankState){
    this(config,tankState.userId,tankState.tankId,tankState.name,tankState.blood,tankState.tankColorType,tankState.position,tankState.curBulletNum,
      tankState.bloodLevel,tankState.speedLevel,tankState.bulletPowerLevel,tankState.direction,tankState.gunDirection,tankState.shotgunState,tankState.invincible,tankState.killTankNum,tankState.damageTank)
  }


  val bulletMaxCapacity:Int = config.maxBulletCapacity
  override val radius: Float = config.tankRadius

  override def startFillBullet(): Unit = {}

  final def getInvincibleState = invincibleState


  def getPosition4Animation(boundary:Point, quadTree: QuadTree ,offSetTime:Long):Point = {
    if(this.canMove(boundary,quadTree)(config)){
      this.position + config.getMoveDistanceByFrame(speedLevel).rotate(this.direction.get) / config.frameDuration * offSetTime //移动的距离
    }else position
  }

  def getGunPositions4Animation():List[Point] = {
    val gunWidth = config.tankGunWidth
    if(this.shotgunState){
      val gunHeight = config.tankGunHeight
      List(
        Point(0,- gunHeight / 2).rotate(this.gunDirection),
        Point(0, gunHeight / 2).rotate(this.gunDirection),
        Point(gunWidth, gunHeight).rotate(this.gunDirection),
        Point(gunWidth, -gunHeight).rotate(this.gunDirection)
      )
    }else{
      val gunHeight = config.tankGunHeight * (1 + (this.bulletLevel - 1) * 0.2f)
      List(
        Point(0,- gunHeight / 2).rotate(this.gunDirection),
        Point(0, gunHeight / 2).rotate(this.gunDirection),
        Point(gunWidth, gunHeight / 2).rotate(this.gunDirection),
        Point(gunWidth, - gunHeight / 2).rotate(this.gunDirection)
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


  def getSliderPositionByBloodLevel(num:Int,sliderLength:Float,width:Float,greyLength:Float) = {
    val startPoint = Point(sliderLength / 2,-(3 + getRadius))
    var positionList:List[Point] = startPoint :: Nil
    for(i <- 2 to 2 * num){
      if(i % 2 == 0){
        val position = startPoint - Point(i / 2 * width + (i/2 - 1)*greyLength / (num - 1),0)
        positionList = position :: positionList
      }
      else{
        val position = startPoint - Point(i / 2 * (width + greyLength / (num - 1)),0)
        positionList = position :: positionList
      }
    }
    positionList
  }
}
