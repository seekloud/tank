package com.neo.sk.tank.shared.ptcl

import com.neo.sk.tank.shared.ptcl.protocol.WsFrontProtocol.TankAction

/**
  * Created by hongruying on 2018/8/15
  */
package object protocol {
  trait Event{
    val frame:Long
  }

  final case class TankActionFrame(override val frame:Long,action:TankAction) extends Event




}
