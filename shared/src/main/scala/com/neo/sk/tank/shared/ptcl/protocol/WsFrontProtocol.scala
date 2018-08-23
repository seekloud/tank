package com.neo.sk.tank.shared.ptcl.protocol

/**
  * Created by hongruying on 2018/7/11
  */
object WsFrontProtocol {

  sealed trait WsMsgFront

  trait TankAction{
    val serialNum:Int
    val frame:Long
  }

  final case class PressKeyDown(keyCodeDown:Int,override val serialNum:Int,override val frame:Long) extends TankAction with WsMsgFront

  final case class GunDirectionOffset(d:Float,override val serialNum:Int,override val frame:Long) extends TankAction with WsMsgFront

  final case class PressKeyUp(keyCodeUp:Int,override val serialNum:Int,override val frame:Long) extends TankAction with WsMsgFront

  final case class MouseMove(d:Float,override val serialNum:Int,override val frame:Long) extends TankAction with WsMsgFront

  final case class MouseClick(time:Long,override val serialNum:Int,override val frame:Long) extends TankAction with WsMsgFront

  final case class RestartGame(name:String) extends WsMsgFront

}
