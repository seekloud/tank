package com.neo.sk.tank.shared.ptcl.protocol

import com.neo.sk.tank.shared.ptcl.model.{Point, Score}
import com.neo.sk.tank.shared.ptcl.tank._

/**
  * Created by hongruying on 2018/7/9
  */
object WsProtocol {





  sealed trait WsMsgServer extends WsServerSourceProtocol.WsMsgSource


  case class YourInfo(uId:Long,tankId:Int) extends WsMsgServer

  case class UserEnterRoom(userId:Long,name:String,tank:TankState) extends WsMsgServer

  case class UserLeftRoom(tankId:Int,name:String) extends WsMsgServer

  case class YouAreKilled(tankId:Int,userId:Long) extends WsMsgServer

  case class TankActionFrameMouse(tankId:Int,frame:Long,actM:WsFrontProtocol.MouseMove) extends WsMsgServer

  case class TankActionFrameKeyDown(tankId:Int,frame:Long,actKd:WsFrontProtocol.PressKeyDown) extends WsMsgServer

  case class TankActionFrameKeyUp(tankId:Int,frame:Long,actKu:WsFrontProtocol.PressKeyUp) extends WsMsgServer



  case class Ranks(currentRank: List[Score], historyRank: List[Score]) extends WsMsgServer

  case class GridSyncState(d:GridStateWithoutBullet) extends WsMsgServer

  case class GridSyncAllState(gState:GridState) extends WsMsgServer

  case class TankAttacked(frame:Long,bId:Int,tId:Int,d:Int) extends WsMsgServer

  case class ObstacleAttacked(frame:Long,bId:Int,oId:Int,d:Int) extends WsMsgServer

  case class TankEatProp(frame:Long,pId:Int,tId:Int,pType:Byte) extends WsMsgServer

  case class AddProp(frame:Long,pId:Int,propState: PropState) extends WsMsgServer

  case class AddObstacle(frame:Long, oId :Int,obstacleState: ObstacleState) extends WsMsgServer

  case class TankLaunchBullet(f:Long,bullet:BulletState) extends WsMsgServer

  case class TankInvincible(f:Long,tId:Int) extends WsMsgServer


//  case class DirectionAction

}
