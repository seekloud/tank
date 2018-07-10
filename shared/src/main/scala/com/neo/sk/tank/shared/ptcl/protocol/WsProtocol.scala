package com.neo.sk.tank.shared.ptcl.protocol

import com.neo.sk.tank.shared.ptcl.model.Point

/**
  * Created by hongruying on 2018/7/9
  */
object WsProtocol {

  sealed trait WsMsgFront

  trait TankAction

  final case class PressKeyDown(keyCode:Int) extends TankAction with WsMsgFront

  final case class PressKeyUp(keyCode:Int) extends TankAction with WsMsgFront

  final case class MoseMove(x:Double,y:Double) extends TankAction with WsMsgFront

  final case class MouseClick(t:Long) extends TankAction with WsMsgFront

  final case class RestartGame(name:String) extends WsMsgFront



  sealed trait WsMsgServer

  case object CompleteMsgServer extends WsMsgServer
  case class FailMsgServer(ex: Exception) extends WsMsgServer

//  case class DirectionAction

}
