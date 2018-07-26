package com.neo.sk.tank.shared.ptcl.protocol

/**
  * Created by hongruying on 2018/7/11
  */
object WsFrontProtocol {

  sealed trait WsMsgFront

  trait TankAction

  final case class PressKeyDown(keyCodeDown:Int) extends TankAction with WsMsgFront

  final case class GunDirectionOffset(d:Float) extends TankAction with WsMsgFront

  final case class PressKeyUp(keyCodeUp:Int) extends TankAction with WsMsgFront

  final case class MouseMove(d:Float) extends TankAction with WsMsgFront

  final case class MouseClick(time:Long) extends TankAction with WsMsgFront

  final case class RestartGame(name:String) extends WsMsgFront

}
