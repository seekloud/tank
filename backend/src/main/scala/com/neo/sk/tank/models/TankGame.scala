package com.neo.sk.tank.models

import com.neo.sk.tank.shared.ptcl.protocol.{WsFrontProtocol, WsProtocol}
import com.neo.sk.tank.shared.ptcl.tank.{GridState, ObstacleState, PropState, TankState}

/**
  * Created by hongruying on 2018/8/16
  */
object TankGame {

  trait GameEvent{
    val frame:Long
  }

  trait UserEvent
  trait EnvironmentEvent


  final case class UserJoinRoom(userId:Long, name:String, tankState:TankState, override val frame: Long) extends GameEvent with UserEvent
  final case class UserLeftRoom(userId:Long, name:String, tankId:Long, override val frame:Long) extends GameEvent with UserEvent
  final case class UserMouseMove(tankId:Int,frame:Long,action:WsFrontProtocol.MouseMove) extends GameEvent with UserEvent
  final case class UserMouseClick(tankId:Int,frame:Long,action:WsFrontProtocol.MouseClick) extends GameEvent with UserEvent
  final case class UserPressKeyDown(tankId:Int,frame:Long,action:WsFrontProtocol.PressKeyDown) extends GameEvent with UserEvent
  final case class UserPressKeyUp(tankId:Int,frame:Long,action:WsFrontProtocol.PressKeyUp) extends GameEvent with UserEvent
  final case class GunDirectionOffset(tankId:Int,frame:Long,action:WsFrontProtocol.GunDirectionOffset) extends GameEvent with UserEvent

  final case class TankFillBullet(tankId:Int,frame:Long) extends GameEvent with EnvironmentEvent
  final case class TankInvincible(tankId:Int,frame:Long) extends GameEvent with EnvironmentEvent
  final case class GenerateProp(frame:Long,pId:Int,propState: PropState) extends GameEvent with EnvironmentEvent
  final case class GenerateObstacle(frame:Long, oId :Int,obstacleState: ObstacleState) extends GameEvent with EnvironmentEvent


  trait GameSnapshot

  final case class TankGameSnapshot(
                                   state:GridState
                                   ) extends GameSnapshot







}
