package com.neo.sk.tank.shared.ptcl.protocol

import com.neo.sk.tank.shared.ptcl.model.{Point, Score}
import com.neo.sk.tank.shared.ptcl.tank._

/**
  * Created by hongruying on 2018/7/9
  */
object WsProtocol {





  sealed trait WsMsgServer extends WsServerSourceProtocol.WsMsgSource


  case class YourInfo(uId:Long,tankId:Long) extends WsMsgServer

  case class UserEnterRoom(userId:Long,name:String,tank:TankState) extends WsMsgServer

  case class UserLeftRoom(tankId:Long,name:String) extends WsMsgServer

  case class YouAreKilled(tankId:Long,userId:Long) extends WsMsgServer

  case class TankActionFrameMouse(tankId:Long,frame:Long,actM:WsFrontProtocol.MouseMove) extends WsMsgServer

  case class TankActionFrameKeyDown(tankId:Long,frame:Long,actKd:WsFrontProtocol.PressKeyDown) extends WsMsgServer

  case class TankActionFrameKeyUp(tankId:Long,frame:Long,actKu:WsFrontProtocol.PressKeyUp) extends WsMsgServer



  case class Ranks(currentRank: List[Score], historyRank: List[Score]) extends WsMsgServer

  case class GridSyncState(d:GridStateWithoutBullet) extends WsMsgServer

  case class GridSyncAllState(gState:GridState) extends WsMsgServer

  case class TankAttacked(frame:Long,bId:Long,tId:Long,d:Int) extends WsMsgServer

  case class ObstacleAttacked(frame:Long,bId:Long,oId:Long,d:Int) extends WsMsgServer

  case class TankEatProp(frame:Long,pId:Long,tId:Long,pType:Int) extends WsMsgServer

  case class AddProp(pId:Long,propState: PropState) extends WsMsgServer

  case class TankLaunchBullet(f:Long,bullet:BulletState) extends WsMsgServer


//  case class DirectionAction

}
