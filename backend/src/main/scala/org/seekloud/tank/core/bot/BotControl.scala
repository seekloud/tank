/*
 * Copyright 2018 seekloud (https://github.com/seekloud)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.seekloud.tank.core.bot

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.typed.ActorRef
import org.seekloud.tank.Boot.roomManager
import org.seekloud.tank.core.RoomActor
import org.seekloud.tank.core.game.GameContainerServerImpl
import org.seekloud.tank.shared.model.Constants.{GameState, ObstacleType}

import concurrent.duration._
import org.seekloud.tank.Boot.executor
import org.seekloud.tank.shared.`object`.{Obstacle, Prop, Tank}
import org.seekloud.tank.shared.model.Point
import org.seekloud.tank.shared.protocol.TankGameEvent

import scala.collection.mutable
/**
  * @author wmy
  * @edit sky
  *       直接对接RoomActor
  **/
case class BotControl(bid: String, tankId: Int, name: String, roomId:Long,roomActor: ActorRef[RoomActor.Command], gameContainer: GameContainerServerImpl) {
  private var gameState: Int = GameState.play

  private var lastMouseMoveTheta: Float = 0
  private var currentMouseMoveTheta: Float = 0
  private var currentMouseMoveAngle: Byte = 0
  private val mouseMoveThreshold = math.Pi / 180
  private val clickRatio = 2
  private val eatRatio = 2
  private var isEatProp = false
  private var turnMsg:Byte = 0

  private val preExecuteFrameOffset = org.seekloud.tank.shared.model.Constants.PreExecuteFrameOffset
  private val actionSerialNumGenerator = new AtomicInteger(0)

  protected val myKeySet = mutable.HashSet[Int]()

  def getActionSerialNum: Byte = actionSerialNumGenerator.getAndIncrement().toByte

  def setGameState(s: Int) = gameState = s

  def sendMsg2Actor: Unit = {
    val click = (new util.Random).nextInt(10)
    val eat = (new util.Random).nextInt(10)
    if (gameState == GameState.play) {
      val isHaveTarget = findTarget
      if (isHaveTarget && !isEatProp && click > clickRatio) {
        if (math.abs(currentMouseMoveTheta - lastMouseMoveTheta) >= mouseMoveThreshold) {
          lastMouseMoveTheta = currentMouseMoveTheta
          roomActor ! RoomActor.WebSocketMsg(bid, tankId, userMouseMove(currentMouseMoveAngle))
        }
        roomActor ! RoomActor.WebSocketMsg(bid, tankId, userMouseClick(currentMouseMoveTheta))
      }
      else if (isHaveTarget && isEatProp && eat > eatRatio) {
        if (turnMsg > 0) {
          //          log.debug(s"${userActor.path} begin to do to eat the prop ${turnMsg}")
          roomActor ! RoomActor.WebSocketMsg(bid, tankId, userMoveState(turnMsg))
          roomActor ! RoomActor.WebSocketMsg(bid, tankId, userMoveStateDelay(8))
        }
      }
      else {
        val randomKeyCode = ((new util.Random).nextInt(4) * 2).toByte
        roomActor ! RoomActor.WebSocketMsg(bid, tankId, userMoveState(randomKeyCode))
        roomActor ! RoomActor.WebSocketMsg(bid, tankId, userMoveStateDelay(8))
      }
    }
  }

  private def chooseTheDirection(p: Point, q: Point) :Byte = {
    val x_dis = p.x - q.x
    val y_dis = p.y - q.y
    if (x_dis < -2.5) 0
    else if (x_dis > 2.5) 4
    else if (y_dis < -2.5) 2
    else if (y_dis > 2.5) 6
    else 8
  }

  def userMoveState(moveState:Byte) = {
    TankGameEvent.UserMoveState(tankId, gameContainer.systemFrame + preExecuteFrameOffset, moveState, getActionSerialNum)
  }
  def userMoveStateDelay(moveState:Byte) = {
    TankGameEvent.UserMoveState(tankId, gameContainer.systemFrame + 10 + preExecuteFrameOffset, moveState, getActionSerialNum)
  }

  private def userMouseClick(d:Float) = {
    TankGameEvent.UC(tankId, gameContainer.systemFrame + preExecuteFrameOffset, d, getActionSerialNum)
  }

  private def userMouseMove(theta: Byte) = {
    TankGameEvent.UM(tankId, gameContainer.systemFrame + preExecuteFrameOffset, theta, getActionSerialNum)
  }

  def findAllTank = {
    if (gameContainer.tankMap.contains(tankId))
      Some(gameContainer.quadTree.retrieve(gameContainer.tankMap(tankId)).filter(_.isInstanceOf[Tank]).map(_.asInstanceOf[Tank]))
    else None
  }

  def findOtherProp(thisTank: Tank) = {
    gameContainer.quadTree.retrieveFilter(thisTank).filter(_.isInstanceOf[Prop]).map(_.asInstanceOf[Prop])
  }

  def findOtherObstacle(thisTank: Tank) = {
    gameContainer.quadTree.retrieveFilter(thisTank).filter(_.isInstanceOf[Obstacle]).map(_.asInstanceOf[Obstacle])
  }

  def findTarget = {
    isEatProp = false

    val tankListOpt = findAllTank
    val tankList = tankListOpt.getOrElse(List())

    if (tankList.nonEmpty && tankList.exists(_.tankId == tankId)) {
      val thisTank = tankList.filter(_.tankId == tankId).head
      val thisTankPos = thisTank.getTankState().position
      val obstacleList = findOtherObstacle(thisTank)
      val airDropList = obstacleList.filter(r => r.obstacleType == ObstacleType.airDropBox)
      val brickList = obstacleList.filter(r => r.obstacleType == ObstacleType.brick)
      val propList = findOtherProp(thisTank)

      if (tankList.exists(r => r.tankId != tankId && judgeTheDistance(r.getTankState().position, thisTank.getTankState().position, 70))) {
        val attackTankList = tankList.filter(_.tankId != tankId).filter(r => judgeTheDistance(r.getTankState().position, thisTankPos, 70))
        val attackTank = attackTankList.minBy(tank => tank.getTankState().position.distance(thisTankPos))
        val pos = attackTank.getTankState().position
        currentMouseMoveTheta = pos.getTheta(thisTankPos).toFloat
        currentMouseMoveAngle = pos.getAngle(thisTankPos)
        true
      }
      else if (propList.exists(r => judgeTheDistance(r.position, thisTankPos, 70))) {
        val eatPropList = propList.filter(r => judgeTheDistance(r.position, thisTankPos, 70))
        val eatProp = eatPropList.minBy(p => p.position.distance(thisTankPos))
        turnMsg = chooseTheDirection(thisTankPos, eatProp.position)
        isEatProp = true
        true
      }
      else if (airDropList.exists(r => judgeTheDistance(r.getObstacleState().p, thisTankPos, 70))) {
        val attackAirList = airDropList.filter(r => judgeTheDistance(r.getObstacleState().p, thisTankPos, 70))
        val attackAir = attackAirList.minBy(air => air.getObstacleState().p.distance(thisTankPos))
        val pos = attackAir.getObstacleState().p
        currentMouseMoveTheta = pos.getTheta(thisTankPos).toFloat
        currentMouseMoveAngle = pos.getAngle(thisTankPos)
        true
      }
      else if (brickList.exists(r => judgeTheDistance(r.getObstacleState().p, thisTankPos, 70))) {
        val attackBrickList = brickList.filter(r => judgeTheDistance(r.getObstacleState().p, thisTankPos, 70))
        val attackBrick = attackBrickList.minBy(brick => brick.getObstacleState().p.distance(thisTankPos))
        val pos = attackBrick.getObstacleState().p
        currentMouseMoveTheta = pos.getTheta(thisTankPos).toFloat
        currentMouseMoveAngle = pos.getAngle(thisTankPos)
        true
      }
      else false
    }
    else false
  }

  private def judgeTheDistance(p: Point, q: Point, dis: Int) = {
    if (p.distance(q) <= dis)
      true
    else
      false
  }

  def leftRoom={
    roomManager ! RoomActor.BotLeftRoom(bid,tankId,name,roomId)
  }

}
