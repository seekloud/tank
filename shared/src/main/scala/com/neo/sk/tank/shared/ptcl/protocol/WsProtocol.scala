package com.neo.sk.tank.shared.ptcl.protocol

import com.neo.sk.tank.shared.ptcl.model.{Point, Score}
import com.neo.sk.tank.shared.ptcl.tank._

/**
  * Created by hongruying on 2018/7/9
  */
object WsProtocol {

  sealed trait WsMsgFront

  trait TankAction

  final case class PressKeyDown(keyCode:Int) extends TankAction with WsMsgFront

  final case class PressKeyUp(keyCode:Int) extends TankAction with WsMsgFront

  final case class MouseMove(d:Double) extends TankAction with WsMsgFront

  final case class MouseClick(t:Long) extends TankAction with WsMsgFront

  final case class RestartGame(name:String) extends WsMsgFront



  sealed trait WsMsgServer

  case object CompleteMsgServer extends WsMsgServer
  case class FailMsgServer(ex: Exception) extends WsMsgServer

  case class UserEnterRoom(userId:Long,name:String,tank:TankState) extends WsMsgServer

  case class UserLeftRoom(tankId:Long,name:String) extends WsMsgServer

  case class TankActionFrame(tankId:Long,frame:Long,a:TankAction) extends WsMsgServer

  case class Ranks(currentRank: List[Score], historyRank: List[Score]) extends WsMsgServer

  case class GridSyncState(d:GridStateWithoutBullet) extends WsMsgServer

  case class GridSyncAllState(d:GridState) extends WsMsgServer

  case class TankAttacked(bId:Long,tId:Long,d:Int) extends WsMsgServer

  case class ObstacleAttacked(bId:Long,oId:Long,d:Int) extends WsMsgServer

  case class TankEatProp(pId:Long,tId:Long,pType:Int) extends WsMsgServer

  case class TankLaunchBullet(f:Long,bullet:BulletState) extends WsMsgServer

//  case class DirectionAction

}
