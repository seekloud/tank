package com.neo.sk.tank.shared.game

import com.neo.sk.tank.shared.`object`.{ObstacleTank, Prop, Tank, TankState}
import com.neo.sk.tank.shared.config.TankGameConfig
import com.neo.sk.tank.shared.model
import com.neo.sk.tank.shared.model.Constants.TankColor
import com.neo.sk.tank.shared.model.{Point, Rectangle}
import com.neo.sk.tank.shared.util.QuadTree

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

  protected var isFakeMove = false
  protected var fakeStartFrame=0l

  protected var fakePosition=Point(0f,0f)

  override def startFillBullet(): Unit = {
    fillBulletCallBack(tankId)
  }

  final def setFakeTankDirection(actionSet:Set[Byte],frame:Long) = {
    fakePosition=position
    val targetDirectionOpt = getDirection(actionSet)
    if(targetDirectionOpt.nonEmpty) {
      isFakeMove = true
      fakeStartFrame=frame
      this.direction = targetDirectionOpt.get
    } else isFakeMove = false
  }

  def getFakeMoveState() = isFakeMove

  final def getInvincibleState = invincibleState

  def canMove(boundary:Point, quadTree:QuadTree)(implicit tankGameConfig: TankGameConfig):Option[Point] = {
    if(isFakeMove){
      val moveDistance = (tankGameConfig.getMoveDistanceByFrame(this.speedLevel)/3).rotate(direction)
      val horizontalDistance = moveDistance.copy(y = 0)
      val verticalDistance = moveDistance.copy(x = 0)
      List(horizontalDistance, verticalDistance).foreach { d =>
        if (d.x != 0 || d.y != 0) {
          var pos = this.fakePosition
          pos+= d
          val movedRec = Rectangle(pos - Point(radius, radius), pos + Point(radius, radius))
          val otherObjects = quadTree.retrieveFilter(this).filter(_.isInstanceOf[ObstacleTank])
          if (!otherObjects.exists(t => t.isIntersects(this)) && movedRec.topLeft > model.Point(0, 0) && movedRec.downRight < boundary) {
          } else {
            isFakeMove=false
          }
        }
      }
      Some(moveDistance)
    }else{
      if(isMove){
        var moveDistance = tankGameConfig.getMoveDistanceByFrame(this.speedLevel).rotate(direction)
        val horizontalDistance = moveDistance.copy(y = 0)
        val verticalDistance = moveDistance.copy(x = 0)
        val originPosition = this.position
        List(horizontalDistance,verticalDistance).foreach{ d =>
          if(d.x != 0 || d.y != 0){
            val pos = this.position
            this.position = this.position + d
            val movedRec = Rectangle(this.position-Point(radius,radius),this.position+Point(radius,radius))
            val otherObjects = quadTree.retrieveFilter(this).filter(_.isInstanceOf[ObstacleTank])
            if(!otherObjects.exists(t => t.isIntersects(this)) && movedRec.topLeft > model.Point(0,0) && movedRec.downRight < boundary){
              quadTree.updateObject(this)
            }else{
              this.position = pos
              moveDistance -= d
            }
          }
        }
        this.position = originPosition
        Some(moveDistance)
      }else{
        None
      }
    }

    /*if(com.neo.sk.tank.shared.model.Constants.fakeRender){
      if(isMove){
        if(!isFakeMove && (canvasFrame <= 0 || canvasFrame >= canvasFrameLeft)) {
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
    } else {
      if(isMove){
        var moveDistance = tankGameConfig.getMoveDistanceByFrame(this.speedLevel).rotate(direction)
        val horizontalDistance = moveDistance.copy(y = 0)
        val verticalDistance = moveDistance.copy(x = 0)

        val originPosition = this.position

        List(horizontalDistance,verticalDistance).foreach{ d =>
          if(d.x != 0 || d.y != 0){
            val pos = this.position
            this.position = this.position + d
            val movedRec = Rectangle(this.position-Point(radius,radius),this.position+Point(radius,radius))
            val otherObjects = quadTree.retrieveFilter(this).filter(_.isInstanceOf[ObstacleTank])
            if(!otherObjects.exists(t => t.isIntersects(this)) && movedRec.topLeft > model.Point(0,0) && movedRec.downRight < boundary){
              quadTree.updateObject(this)
            }else{
              this.position = pos
              moveDistance -= d
            }
          }
        }
        this.position = originPosition
        Some(moveDistance)
      }else{
        None
      }


    }*/


  }

  def getPosition4Animation(boundary: Point, quadTree: QuadTree, offSetTime: Long,frame:Long): Point = {
    if(isFakeMove&&fakeStartFrame+2<frame){
      isFakeMove=false
    }else{
    }
    val logicMoveDistanceOpt = this.canMove(boundary, quadTree)(config)
    if (logicMoveDistanceOpt.nonEmpty) {
      if(isFakeMove){
        this.fakePosition=this.position+logicMoveDistanceOpt.get*(frame-fakeStartFrame)
        this.fakePosition + logicMoveDistanceOpt.get / config.frameDuration * offSetTime
      }else{
        this.position + logicMoveDistanceOpt.get / config.frameDuration * offSetTime
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
