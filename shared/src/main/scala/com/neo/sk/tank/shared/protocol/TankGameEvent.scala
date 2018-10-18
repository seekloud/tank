package com.neo.sk.tank.shared.protocol

import com.neo.sk.tank.shared.`object`.{BulletState, ObstacleState, PropState, TankState}
import com.neo.sk.tank.shared.config.{TankGameConfig, TankGameConfigImpl}
import com.neo.sk.tank.shared.game.{GameContainerAllState, GameContainerState}
import com.neo.sk.tank.shared.model.Score

/**
  * Created by hongruying on 2018/8/28
  */
object TankGameEvent {


  sealed trait WsMsgFront
  /**
    * 携带原来tankId
    * */
  final case class RestartGame(tankIdOpt:Option[Int],name:String) extends WsMsgFront

  sealed trait WsMsgSource
  case object CompleteMsgServer extends WsMsgSource
  case class FailMsgServer(ex: Exception) extends WsMsgSource

  sealed trait WsMsgServer extends WsMsgSource
  //  final case class GameConfig(config:TankGameConfigImpl) extends WsMsgServer
  final case class YourInfo(userId:Long,tankId:Int,name:String,config:TankGameConfigImpl) extends WsMsgServer
  final case class YouAreKilled(tankId:Int,name:String) extends WsMsgServer //可能会丢弃
  final case class Ranks(currentRank: List[Score], historyRank: List[Score]) extends WsMsgServer
  final case class SyncGameState(state:GameContainerState) extends WsMsgServer
  final case class SyncGameAllState(gState:GameContainerAllState) extends WsMsgServer
  final case class Wrap(ws:Array[Byte],isKillMsg:Boolean = false) extends WsMsgSource
  final case class PingPackage(sendTime:Long) extends WsMsgServer with WsMsgFront

  sealed trait GameEvent {
    val frame:Long
  }

  trait UserEvent extends GameEvent
  trait EnvironmentEvent extends GameEvent
  trait UserActionEvent extends UserEvent{
    val tankId:Int
    val serialNum:Int
  }

  /**
    * replay-frame-msg*/
  final case class ReplayFrameData(ws:Array[Byte]) extends WsMsgSource
  /**
    * replay in front
    * */
  final case class ReplayInfo(userId:Long,tankId:Int,name:String,f:Long,config:TankGameConfigImpl) extends WsMsgServer
  final case class EventData(list:List[WsMsgServer]) extends WsMsgServer
  final case class DecodeError() extends WsMsgServer

  final case class UserJoinRoom(userId:Long, name:String, tankState:TankState, override val frame: Long) extends  UserEvent with WsMsgServer
  final case class UserLeftRoom(userId:Long, name:String, tankId:Int, override val frame:Long) extends UserEvent with WsMsgServer

  final case class UserMouseMove(tankId:Int,override val frame:Long,d:Float,override val serialNum:Int) extends UserActionEvent with WsMsgFront with WsMsgServer
  final case class UserKeyboardMove(tankId:Int,override val frame:Long,angle:Float,override val serialNum:Int) extends UserActionEvent with WsMsgFront with WsMsgServer

  final case class UserMouseClick(tankId:Int,override val frame:Long,time:Long,override val serialNum:Int) extends UserActionEvent with WsMsgFront with WsMsgServer
  final case class UserPressKeyDown(tankId:Int,override val frame:Long,keyCodeDown:Int,override val serialNum:Int) extends UserActionEvent with WsMsgFront with WsMsgServer
  final case class UserPressKeyUp(tankId:Int,override val frame:Long,keyCodeUp:Int,override val serialNum:Int) extends UserActionEvent with WsMsgFront with WsMsgServer
  /**
    * 使用医疗包,
    * */
  final case class UserPressKeyMedical(tankId:Int,override val frame:Long, override val serialNum: Int) extends UserActionEvent with WsMsgFront with WsMsgServer

  final case class TankAttacked(tankId:Int,bulletId:Int, bulletTankId:Int, bulletTankName:String, damage:Int,override val frame:Long) extends GameEvent with WsMsgServer
  final case class ObstacleAttacked(obstacleId:Int, bulletId:Int, damage:Int, override val frame:Long) extends GameEvent with WsMsgServer

  final case class TankEatProp(tankId:Int,propId:Int,propType:Byte,frame:Long) extends GameEvent with WsMsgServer


  final case class TankFillBullet(tankId:Int,override val frame:Long) extends EnvironmentEvent with WsMsgServer
  final case class TankInvincible(tankId:Int,override val frame:Long) extends EnvironmentEvent with WsMsgServer
  final case class TankShotgunExpire(tankId:Int,override val frame:Long) extends EnvironmentEvent with WsMsgServer

  final case class GenerateProp(override val frame:Long,propState: PropState,generateType:Byte = 0) extends EnvironmentEvent with WsMsgServer
  final case class GenerateBullet(override val frame:Long,bullet:BulletState) extends EnvironmentEvent with WsMsgServer
  final case class GenerateObstacle(override val frame:Long,obstacleState: ObstacleState) extends EnvironmentEvent with WsMsgServer

  sealed trait GameSnapshot

  final case class TankGameSnapshot(
                                     state:GameContainerAllState
                                   ) extends GameSnapshot


  final case class GameInformation(
                                    gameStartTime:Long,
                                    tankConfig: TankGameConfigImpl
                                  )

}
