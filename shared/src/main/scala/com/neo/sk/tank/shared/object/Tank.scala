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

case class TankState(userId:Long,tankId:Int,direction:Float,gunDirection:Float,blood:Int,bloodLevel:Byte,speedLevel:Byte,curBulletNum:Int,position:Point,bulletPowerLevel:Byte,tankColorType:Byte,
                     name:String,lives:Int,medicalNumOpt:Option[Int],killTankNum:Int,damageTank:Int,invincible:Boolean,shotgunState:Boolean, speed: Point, isMove: Boolean)
trait Tank extends CircleObjectOfGame with ObstacleTank{
  val userId : Long
  val tankId : Int
  val name : String
  var lives:Int  // 记录tank当前的生命值
  var medicalNumOpt:Option[Int]
  var killTankNum:Int
  var damageStatistics:Int
  val tankColorType:Byte
  val bulletMaxCapacity:Int
  protected var blood:Int

  var isFakeMove = false
  var fakePosition = Point(0,0)
  protected var bloodLevel:Byte //血量等级
  protected var speedLevel:Byte //移动速度等级
  protected var bulletLevel:Byte //子弹等级

  protected var curBulletNum:Int
  protected var direction:Float //移动方向
  protected var gunDirection:Float
  var cavasFrame = 0
  protected var shotgunState:Boolean
  protected var invincibleState:Boolean
  protected var isMove: Boolean

  private var isFillBulletState:Boolean = false

  var speed: Point
  var fakeFrame = 0l
  private def accelerationTime(implicit config: TankGameConfig) = config.getTankAccByLevel(speedLevel)
  private def decelerationTime(implicit config: TankGameConfig) = config.getTankDecByLevel(speedLevel)

  def getShotGunState():Boolean = shotgunState

  def isLived() : Boolean = blood > 0

  def setTankGunDirection(d:Float) = {
    gunDirection = d
  }

  def getIsMove() = isMove

  def setTankKeyBoardDirection(angle:Float) ={
    gunDirection += angle

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
    if(getShotGunState() && bulletLevel > 3) 3 else
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
    TankState(userId,tankId,direction,gunDirection,blood,bloodLevel,speedLevel,curBulletNum,position,bulletLevel,tankColorType,name,lives,medicalNumOpt,killTankNum,damageStatistics,invincibleState,shotgunState,speed,isMove)
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

//  def getAcceleration(implicit tankGameConfig: TankGameConfig):Float = {
//    if (isMove){
//      tankGameConfig.getMoveDistanceByFrame(speedLevel).distance(Point(0,0)).toFloat / (accelerationTime / tankGameConfig.frameDuration)
//    } else{
//      - tankGameConfig.getMoveDistanceByFrame(speedLevel).distance(Point(0,0)).toFloat / (decelerationTime / tankGameConfig.frameDuration)
//    }
//  }

  def getAcceleration(implicit tankGameConfig: TankGameConfig):Point =
    tankGameConfig.getMoveDistanceByFrame(speedLevel) / (accelerationTime / tankGameConfig.frameDuration)


  def getDeceleration(v:Float)(implicit tankGameConfig: TankGameConfig):Point ={
    val d = if(v > 0) -1 else 1
    tankGameConfig.getMoveDistanceByFrame(speedLevel) / ( d * decelerationTime / tankGameConfig.frameDuration)
  }




  // 根据方向和地图边界以及地图所有的障碍物和坦克（不包括子弹）进行碰撞检测和移动
  def move(boundary: Point,quadTree: QuadTree)(implicit tankGameConfig: TankGameConfig):Unit = {
    def modifySpeed(): Unit = {
      val targetSpeed = if(isMove){
        tankGameConfig.getMoveDistanceByFrame(speedLevel).rotate(this.direction)
      }else Point(0,0)
//      println(s"target:${targetSpeed}")

      targetSpeed.x * speed.x match {
        case x:Float if math.abs(x) <= 1e-5 =>
          if(math.abs(targetSpeed.x) <= 1e-5){
            val vx = getDeceleration(speed.x).x + speed.x
            if(speed.x * vx > 0) speed = speed.copy(x = vx)
            else speed = speed.copy(x = 0)
          }else{
            val vx = getAcceleration.rotate(this.direction).x + speed.x
            if(math.abs(vx) < math.abs(targetSpeed.x)) speed = speed.copy(x = vx)
            else speed = speed.copy(x = targetSpeed.x)
          }
        case x:Float if x > 0 =>
          if(math.abs(targetSpeed.x) >= math.abs(speed.x)){
            val vx = getAcceleration.rotate(this.direction).x + speed.x
            if(math.abs(vx) < math.abs(targetSpeed.x)) speed = speed.copy(x = vx)
            else speed = speed.copy(x = targetSpeed.x)
          }else{
            val vx = getDeceleration(speed.x).x + speed.x
            if(math.abs(vx) > math.abs(targetSpeed.x)) speed = speed.copy(x = vx)
            else speed = speed.copy(x = targetSpeed.x)
          }
        case x:Float if x < 0 =>
          val vx = getAcceleration.rotate(this.direction).x + getDeceleration(speed.x).x + speed.x
          speed = speed.copy(x = vx)
      }


      targetSpeed.y * speed.y match {
        case x:Float if math.abs(x) <= 1e-5 =>
          if(math.abs(targetSpeed.y) <= 1e-5){
            val vy = getDeceleration(speed.y).x + speed.y
//            println(s"sssssssss${vy}")
            if(speed.y * vy > 0) speed = speed.copy(y = vy)
            else speed = speed.copy(y = 0)
          }else{
            val vy = getAcceleration.rotate(this.direction).y + speed.y
            if(math.abs(vy) < math.abs(targetSpeed.y)) speed = speed.copy(y = vy)
            else speed = speed.copy(y = targetSpeed.y)
          }
        case x:Float if x > 0 =>
          if(math.abs(targetSpeed.y) >= math.abs(speed.y)){
            val vy = getAcceleration.rotate(this.direction).y + speed.y
//            println(s"targert${math.abs(targetSpeed.y)} , spped by=${math.abs(vy)}")
            if(math.abs(vy) < math.abs(targetSpeed.y)) speed = speed.copy(y = vy)
            else speed = speed.copy(y = targetSpeed.y)
          }else{
            val vy = getDeceleration(speed.y).x + speed.y
            if(math.abs(vy) > math.abs(targetSpeed.y)) speed = speed.copy(y = vy)
            else speed = speed.copy(y = targetSpeed.y)
          }
        case x:Float if x < 0 =>
          val vy = getAcceleration.rotate(this.direction).y + getDeceleration(speed.y).x + speed.y
          speed = speed.copy(y = vy)
      }
      //    speed += acceleration
      //    if(speed > maxSpeed) speed = maxSpeed
      //    if(speed < 0) speed = 0
    }

    if(isMove) {
      if (!isFakeMove) {
        val moveDistance = tankGameConfig.getMoveDistanceByFrame(this.speedLevel).rotate(direction)

        val horizontalDistance = moveDistance.copy(y = 0)
        val verticalDistance = moveDistance.copy(x = 0)
        List(horizontalDistance, verticalDistance).foreach { d =>
          if (d.x != 0 || d.y != 0) {
            val originPosition = this.position
            this.position = this.position + d
            val movedRec = Rectangle(this.position - Point(radius, radius), this.position + Point(radius, radius))
            val otherObjects = quadTree.retrieveFilter(this).filter(_.isInstanceOf[ObstacleTank])
            if (!otherObjects.exists(t => t.isIntersects(this)) && movedRec.topLeft > model.Point(0, 0) && movedRec.downRight < boundary) {
              quadTree.updateObject(this)
            } else {
              this.position = originPosition
            }
          }
        }
      }else{
        val moveDistance = (tankGameConfig.getMoveDistanceByFrame(this.speedLevel) * 0.19f).rotate(direction)
        val horizontalDistance = moveDistance.copy(y = 0)
        val verticalDistance = moveDistance.copy(x = 0)
        List(horizontalDistance, verticalDistance).foreach { d =>
          if (d.x != 0 || d.y != 0) {
            val originPosition = this.fakePosition
            this.fakePosition = this.fakePosition + d
            val movedRec = Rectangle(this.fakePosition - Point(radius, radius), this.fakePosition + Point(radius, radius))
            val otherObjects = quadTree.retrieveFilter(this).filter(_.isInstanceOf[ObstacleTank])
            if (!otherObjects.exists(t => t.isIntersects(this)) && movedRec.topLeft > model.Point(0, 0) && movedRec.downRight < boundary) {
            } else {
              this.fakePosition = originPosition
            }
          }
        }
      }

    }
//    val maxSpeed = tankGameConfig.getMoveDistanceByFrame(speedLevel).distance(Point(0,0)).toFloat
//    val acceleration = getAcceleration
//    speed += acceleration
//    if(speed > maxSpeed) speed = maxSpeed
//    if(speed < 0) speed = 0
//    modifySpeed()
  }

  def canMove(boundary:Point,quadTree:QuadTree, cavasFrameLeft:Int)(implicit tankGameConfig: TankGameConfig):Option[Point] = {
    if(isMove){
      if(!isFakeMove && (cavasFrame <= 0 || cavasFrame >= cavasFrameLeft)) {
        var moveDistance = tankGameConfig.getMoveDistanceByFrame(this.speedLevel).rotate(direction)
        val horizontalDistance = moveDistance.copy(y = 0)
        val verticalDistance = moveDistance.copy(x = 0)

        val originPosition = this.position

        List(horizontalDistance, verticalDistance).foreach { d =>
          if (d.x != 0 || d.y != 0) {
            val pos = this.position
            this.position = this.position + d
            val movedRec = Rectangle(this.position - Point(radius, radius), this.position + Point(radius, radius))
            val otherObjects = quadTree.retrieveFilter(this).filter(_.isInstanceOf[ObstacleTank])
            if (!otherObjects.exists(t => t.isIntersects(this)) && movedRec.topLeft > model.Point(0, 0) && movedRec.downRight < boundary) {
              quadTree.updateObject(this)
            } else {
              this.position = pos
              moveDistance -= d
            }
          }
        }
        this.position = originPosition
        Some(moveDistance)
      }else{
        var moveDistance =( tankGameConfig.getMoveDistanceByFrame(this.speedLevel) * 0.19f).rotate(direction)
        val horizontalDistance = moveDistance.copy(y = 0)
        val verticalDistance = moveDistance.copy(x = 0)
        val originPosition = this.fakePosition
        List(horizontalDistance, verticalDistance).foreach { d =>
          if (d.x != 0 || d.y != 0) {
            val pos = this.fakePosition
            this.fakePosition = this.fakePosition + d
            val movedRec = Rectangle(this.fakePosition - Point(radius, radius), this.fakePosition + Point(radius, radius))
            val otherObjects = quadTree.retrieveFilter(this).filter(_.isInstanceOf[ObstacleTank])
            if (!otherObjects.exists(t => t.isIntersects(this)) && movedRec.topLeft > model.Point(0, 0) && movedRec.downRight < boundary) {
            } else {
              this.fakePosition = pos
              moveDistance -= d
            }
          }
        }
        this.fakePosition = originPosition
        Some(moveDistance)
      }


//      this.position = this.position + tankGameConfig.getMoveDistanceByFrame(speedLevel).rotate(this.direction.get)
//      val movedRec = Rectangle(this.position - Point(radius, radius), this.position + Point(radius, radius))
//      val otherObjects = quadTree.retrieveFilter(this).filter(_.isInstanceOf[ObstacleTank])
//      val result = if(!otherObjects.exists(t => t.isIntersects(this)) && movedRec.topLeft > model.Point(0,0) && movedRec.downRight < boundary){
//        true
//      }else{
//        false
//      }
//
//      result
    }else{
      None
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
        if(bloodLevel < config.getTankBloodMaxLevel()){
          val diff = config.getTankBloodByLevel(bloodLevel) - blood
          bloodLevel = (bloodLevel + 1).toByte
          blood = config.getTankBloodByLevel(bloodLevel) - diff
        }
      case 2 => if(speedLevel < config.getTankSpeedMaxLevel()) speedLevel = (speedLevel + 1).toByte
      case 3 => if(bulletLevel < config.getBulletMaxLevel()) bulletLevel = (bulletLevel + 1).toByte
      case 4 => blood = math.min(blood + config.propMedicalBlood, config.getTankBloodByLevel(bloodLevel))
      case 5 => shotgunState = true
    }
  }
  def addBlood()(implicit config: TankGameConfig):Unit = {
    medicalNumOpt match {
      case Some(num) if(num > 0)=>
        if(num - 1 == 0)medicalNumOpt = None
        else  medicalNumOpt = Some(num - 1)
        blood = math.min(blood + config.propMedicalBlood, config.getTankBloodByLevel(bloodLevel))
      case None =>
    }
  }



  /**
    * 根据坦克的按键修改坦克的方向状态
    * */
  final def setTankDirection(actionSet:Set[Int]) = {
    val targetDirectionOpt = getDirection(actionSet)
    if(targetDirectionOpt.nonEmpty) {
      isMove = true
      this.direction = targetDirectionOpt.get
    }
    else isMove = false
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
                   var speed: Point,
                   protected var isMove: Boolean
                   ) extends Tank{

  def this(config: TankGameConfig,tankState: TankState){
    this(config,tankState.userId,tankState.tankId,tankState.name,tankState.blood,tankState.tankColorType,tankState.position,tankState.curBulletNum,
      tankState.lives,tankState.medicalNumOpt,tankState.bloodLevel,tankState.speedLevel,tankState.bulletPowerLevel,tankState.direction,tankState.gunDirection,tankState.shotgunState,tankState.invincible,tankState.killTankNum,tankState.damageTank,
      tankState.speed,tankState.isMove)
  }


  val bulletMaxCapacity:Int = config.maxBulletCapacity
  override val radius: Float = config.tankRadius

  override def startFillBullet(): Unit = {}

  final def getInvincibleState = invincibleState



  def getPosition4Animation(boundary:Point, quadTree: QuadTree ,offSetTime:Long):Point = {
    val cavasFrameLeft = if(fakeFrame < 3) 3 else 4
    val logicMoveDistanceOpt = this.canMove(boundary,quadTree,cavasFrameLeft)(config)
    if(logicMoveDistanceOpt.nonEmpty){
      if(!isFakeMove && (cavasFrame <= 0 || cavasFrame >= cavasFrameLeft)) {
        this.position + logicMoveDistanceOpt.get / config.frameDuration * offSetTime
      }else{
        this.fakePosition + logicMoveDistanceOpt.get / config.frameDuration * offSetTime
      }
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
      val gunHeight = config.tankGunHeight * (1 + (this.bulletLevel - 1) * 0.1f)
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
