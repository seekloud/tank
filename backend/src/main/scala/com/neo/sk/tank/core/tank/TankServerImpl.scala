package com.neo.sk.tank.core.tank

import akka.actor.typed.ActorRef
import com.neo.sk.tank.core.RoomActor
import com.neo.sk.tank.shared.ptcl.model
import com.neo.sk.tank.shared.ptcl.tank.{Prop, Tank}
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
                       override val tankId: Int,
                       override protected var blood: Int,
                       override protected var bloodLevel: Byte,
                       override protected var bulletPowerLevel: Byte,
                       override protected var curBulletNum: Int,
                       override protected var direction: Float,
                       override protected var gunDirection: Float,
                       override var position: model.Point,
                       override protected var speedLevel: Byte,
                       override protected val tankColorType: Byte,
                       override val name: String,
                       override var killTankNum: Int,
                       override var damageTank: Int,
                       override protected var invincible:Boolean,
                       override protected var bulletStrengthen: Int
                     ) extends Tank{



  def this(
            roomActor:ActorRef[RoomActor.Command],
            userId:Long,
            tankId: Int,
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
      true,0
    )
  }

  override protected def startFillBullet(): Unit = {
    scheduler.scheduleOnce(ptcl.model.TankParameters.tankFillBulletSpeed.second){
      roomActor ! RoomActor.TankFillABullet(tankId)
    }
  }

  override def eatProp(p:Prop):Unit = {
    p.getPropState.t match {
      case 1 =>
        if (bloodLevel < 3) {
          val diff = model.TankParameters.TankBloodLevel.getTankBlood(bloodLevel) - blood
          bloodLevel = (bloodLevel + 1).toByte
          blood = model.TankParameters.TankBloodLevel.getTankBlood(bloodLevel) - diff
        }
      case 2 => if (speedLevel < 3) speedLevel = (speedLevel + 1).toByte
      case 3 => if (bulletPowerLevel < 3) bulletPowerLevel = (bulletPowerLevel + 1).toByte
      case 4 =>
        blood += model.TankParameters.addBlood
        if (blood > model.TankParameters.TankBloodLevel.getTankBlood(bloodLevel)) {
          blood = model.TankParameters.TankBloodLevel.getTankBlood(bloodLevel)
        }

      case 5 =>
        bulletStrengthen = model.TankParameters.bulletStrengthenTime
        scheduler.scheduleOnce(model.TankParameters.bulletStrengthenTime.seconds){
          roomActor ! RoomActor.BulletStrengthenOver(tankId)
        }

    }
  }

}
