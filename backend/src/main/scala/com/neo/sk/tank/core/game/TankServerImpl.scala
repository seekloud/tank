package com.neo.sk.tank.core.game

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.TimerScheduler
import com.neo.sk.tank.core.RoomActor
import com.neo.sk.tank.shared.`object`.{Prop, Tank, TankState}
import com.neo.sk.tank.shared.config.TankGameConfig
import com.neo.sk.tank.shared.model
import com.neo.sk.tank.shared.model.Point

import concurrent.duration._

/**
  * Created by hongruying on 2018/8/29
  */
case class TankServerImpl(
                           roomActor:ActorRef[RoomActor.Command],
                           timer:TimerScheduler[RoomActor.Command],
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

  def this(roomActor:ActorRef[RoomActor.Command], timer:TimerScheduler[RoomActor.Command],config: TankGameConfig,tankState: TankState){
    this(roomActor, timer, config,tankState.userId,tankState.tankId,tankState.name,tankState.blood,tankState.tankColorType,tankState.position,tankState.curBulletNum,tankState.lives,tankState.medicalNumOpt,
      tankState.bloodLevel,tankState.speedLevel,tankState.bulletPowerLevel,tankState.direction,tankState.gunDirection,tankState.shotgunState,tankState.invincible,tankState.killTankNum,tankState.damageTank,tankState.speed,tankState.isMove)
  }



  val bulletMaxCapacity:Int = config.maxBulletCapacity
  override val radius: Float = config.tankRadius



  override def startFillBullet(): Unit = {
    timer.startSingleTimer(s"TankFillABullet_${tankId}_${System.currentTimeMillis()}",
      RoomActor.TankFillABullet(tankId),
      config.fillBulletDuration.millis)
  }

  override def eatProp(p: Prop)(implicit config: TankGameConfig): Unit = {
    super.eatProp(p)
    if(p.propType == 5){
      timer.startSingleTimer(s"TankEatAShotgunProp_${tankId}",
        RoomActor.ShotgunExpire(tankId),
        config.shotgunDuration.second)
    }
  }
}

