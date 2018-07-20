package com.neo.sk.tank.core.tank

import akka.actor.typed.ActorRef
import com.neo.sk.tank.core.RoomActor
import com.neo.sk.tank.shared.ptcl.model
import com.neo.sk.tank.shared.ptcl.tank.Tank
import com.neo.sk.tank.Boot.{executor, scheduler}
import com.neo.sk.tank.shared.ptcl
import com.neo.sk.tank.shared.ptcl.model.Point

import concurrent.duration._

/**
  * Created by hongruying on 2018/7/10
  */
class TankServerImpl (
                       val roomActor:ActorRef[RoomActor.Command],
                       override protected val userId:Long,
                       override val tankId: Long,
                       override protected var blood: Int,
                       override protected var bloodLevel: Int,
                       override protected var bulletPowerLevel: Int,
                       override protected var curBulletNum: Int,
                       override protected var direction: Double,
                       override protected var gunDirection: Double,
                       override var position: model.Point,
                       override protected var speedLevel: Int,
                       override protected val tankColorType: Int,
                       override val name: String,
                       override var killTankNum: Int,
                       override var damageTank: Int,
                       override protected var invincible:Boolean
                     ) extends Tank{



  def this(
            roomActor:ActorRef[RoomActor.Command],
            userId:Long,
            tankId: Long,
            name:String,
            position:Point
          ) = {
    this(
      roomActor,
      userId,
      tankId,
      ptcl.model.TankParameters.TankBloodLevel.getTankBlood(ptcl.model.TankParameters.TankBloodLevel.first),
      ptcl.model.TankParameters.TankBloodLevel.first,
      ptcl.model.TankParameters.TankBulletBulletPowerLevel.first,
      ptcl.model.TankParameters.tankBulletMaxCapacity,
      0,
      0,
      position,
      ptcl.model.TankParameters.SpeedType.low,
      model.TankParameters.TankColor.getRandomColorType(),
      name,0,0,
      true
    )
  }

  override protected def startFillBullet(): Unit = {
    scheduler.scheduleOnce(ptcl.model.TankParameters.tankFillBulletSpeed.second){
      roomActor ! RoomActor.TankFillABullet(tankId)
    }
  }

}
