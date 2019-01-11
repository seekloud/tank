package com.neo.sk.tank.shared.protocol

import com.neo.sk.tank.shared.`object`._
import com.neo.sk.tank.shared.config.{TankGameConfig, TankGameConfigImpl}
import com.neo.sk.tank.shared.model.Score
import com.neo.sk.tank.shared.protocol.TankGameEvent.UserEvent

/**
  * Created by hongruying on 2018/8/28
  */
object TankGameEvent {
  final case class GameContainerAllState(
                                          f:Long,
                                          tanks:List[TankState],
                                          bullet:List[BulletState],
                                          props:List[PropState],
                                          obstacle:List[ObstacleState],
                                          environment:List[ObstacleState],
                                          tankMoveAction:List[(Int,List[Int])]
                                        )

  case class GameContainerState(
                                 f:Long
                               )

  /**前端建立WebSocket*/
  sealed trait WsMsgFrontSource
  case object CompleteMsgFrontServer extends WsMsgFrontSource
  case class FailMsgFrontServer(ex: Exception) extends WsMsgFrontSource

  sealed trait WsMsgFront extends WsMsgFrontSource
  /**
    * 携带原来tankId
    * */
  final case class RestartGame(tankIdOpt:Option[Int],name:String) extends WsMsgFront
  final case object GetSyncGameState extends WsMsgFront

  /**后台建立WebSocket*/
  trait WsMsgSource
  case object CompleteMsgServer extends WsMsgSource
  case class FailMsgServer(ex: Exception) extends WsMsgSource

  sealed trait WsMsgServer extends WsMsgSource

  final case class WsMsgErrorRsp(errCode:Int, msg:String) extends WsMsgServer
  //  final case class GameConfig(config:TankGameConfigImpl) extends WsMsgServer
  final case class WsSuccess(roomId:Option[Long]) extends WsMsgServer
  final case class StartGame(roomId:Option[Long],password:Option[String]) extends WsMsgFront
  final case class CreateRoom(roomId:Option[Long],password:Option[String]) extends WsMsgFront
  final case class YourInfo(userId:String,tankId:Int,name:String,config:TankGameConfigImpl) extends WsMsgServer
  final case class YouAreKilled(tankId:Int, name:String, hasLife:Boolean,killTankNum:Int,lives:Int,damageStatistics:Int) extends WsMsgServer //可能会丢弃
  final case class Ranks(currentRank: List[Score], historyRank: List[Score] = Nil) extends WsMsgServer
  final case class SyncGameState(state:GameContainerState) extends WsMsgServer
  final case class SyncGameAllState(gState:GameContainerAllState) extends WsMsgServer
  final case class FirstSyncGameAllState(gState:GameContainerAllState,tankId:Int,name:String,config:TankGameConfigImpl) extends WsMsgServer
  final case class Wrap(ws:Array[Byte],isKillMsg:Boolean = false) extends WsMsgSource
//  final case class Wrap(ws:Array[Byte],isKillMsg:Boolean = false) extends WsMsgSource
  final case class PingPackage(sendTime:Long) extends WsMsgServer with WsMsgFront

  sealed trait GameEvent {
    val frame:Long
  }

  trait UserEvent extends GameEvent
  trait EnvironmentEvent extends GameEvent  //游戏环境产生事件
  trait FollowEvent extends GameEvent  //游戏逻辑产生事件
  trait UserActionEvent extends UserEvent{   //游戏用户动作事件
    val tankId:Int
    val serialNum:Int
  }
  /**异地登录消息
    * WebSocket连接重新建立*/
  final case object RebuildWebSocket extends WsMsgServer

  /**
    * replay-frame-msg*/
  final case class ReplayFrameData(ws:Array[Byte]) extends WsMsgSource
  final case class InitReplayError(msg:String) extends WsMsgServer
  final case class ReplayFinish() extends WsMsgServer
  final case object StartReplay extends WsMsgServer
  /**
    * replay in front
    * */
  final case class ReplayInfo(userId:String,tankId:Int,name:String,f:Long,config:TankGameConfigImpl) extends WsMsgServer
  final case class EventData(list:List[WsMsgServer]) extends WsMsgServer
  final case class DecodeError() extends WsMsgServer

  final case class UserJoinRoom(userId:String, name:String, tankState:TankState, override val frame: Long) extends  UserEvent with WsMsgServer
  final case class UserRelive(userId:String,name:String,tankState: TankState,override val frame:Long) extends UserEvent with WsMsgServer
  final case class UserLeftRoomByKill(userId:String, name:String, tankId:Int, override val frame:Long) extends UserEvent with WsMsgServer
  final case class UserLeftRoom(userId:String, name:String, tankId:Int, override val frame:Long) extends UserEvent with WsMsgServer
  final case class PlayerLeftRoom(userId:String,name:String,tankId:Int,override val frame:Long) extends UserEvent with WsMsgServer
  final case class UserMouseMove(tankId:Int,override val frame:Long,d:Float,override val serialNum:Int) extends UserActionEvent with WsMsgFront with WsMsgServer
  final case class UserKeyboardMove(tankId:Int,override val frame:Long,angle:Float,override val serialNum:Int) extends UserActionEvent with WsMsgFront with WsMsgServer

  final case class UserMouseClick(tankId:Int,override val frame:Long,time:Long,override val serialNum:Int) extends UserActionEvent with WsMsgFront with WsMsgServer
  final case class UserPressKeyDown(tankId:Int,override val frame:Long,keyCodeDown:Int,override val serialNum:Int) extends UserActionEvent with WsMsgFront with WsMsgServer
  final case class UserPressKeyUp(tankId:Int,override val frame:Long,keyCodeUp:Int,override val serialNum:Int) extends UserActionEvent with WsMsgFront with WsMsgServer
  /**使用医疗包*/
  final case class UserPressKeyMedical(tankId:Int,override val frame:Long, override val serialNum: Int) extends UserActionEvent with WsMsgFront with WsMsgServer
  /**tank吃道具*/
  final case class TankEatProp(tankId:Int,propId:Int,propType:Byte,frame:Long) extends GameEvent with WsMsgServer
  /**生成道具*/
  final case class GenerateProp(override val frame:Long,propState: PropState,generateType:Byte = 0) extends EnvironmentEvent with WsMsgServer
  final case class GenerateBullet(override val frame:Long,bullet:BulletState) extends EnvironmentEvent with WsMsgServer
  /**生成河流，钢铁*/
  final case class GenerateObstacle(override val frame:Long,obstacleState: ObstacleState) extends EnvironmentEvent with WsMsgServer
  /**砖块消失事件*/
  final case class ObstacleRemove(obstacleId:Int, override val frame:Long) extends EnvironmentEvent with WsMsgServer

  /**
    * tank初次进入游戏时用于同步游戏逻辑产生延时事件
    * */
  final case class TankFollowEventSnap(override val frame:Long,
                                       tankFillList:List[TankFillBullet],
                                       invincibleList:List[TankInvincible],
                                       shotExpireList:List[TankShotgunExpire]) extends GameEvent with WsMsgServer

  /**
    * 游戏逻辑产生事件
    * */
  final case class TankFillBullet(tankId:Int,override val frame:Long) extends FollowEvent
  /**tank无敌时间消除*/
  final case class TankInvincible(tankId:Int,override val frame:Long) extends FollowEvent
  /**散弹枪失效*/
  final case class TankShotgunExpire(tankId:Int,override val frame:Long) extends FollowEvent
  /**伤害计算*/
  final case class TankAttacked(tankId:Int,bulletId:Int, bulletTankId:Int, bulletTankName:String, damage:Int,override val frame:Long) extends FollowEvent

  final case class ObstacleAttacked(obstacleId:Int, bulletId:Int, damage:Int, override val frame:Long) extends FollowEvent

  sealed trait GameSnapshot

  final case class TankGameSnapshot(
                                     state:GameContainerAllState
                                   ) extends GameSnapshot


  final case class GameInformation(
                                    gameStartTime:Long,
                                    tankConfig: TankGameConfigImpl
                                  )

}
