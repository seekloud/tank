package com.neo.sk.tank.core.bot

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.typed.ActorRef
import com.neo.sk.tank.Boot.roomManager
import com.neo.sk.tank.core.{RoomActor, UserActor}
import com.neo.sk.tank.core.UserActor.WebSocketMsg
import com.neo.sk.tank.core.game.GameContainerServerImpl
import com.neo.sk.tank.shared.`object`.{Obstacle, Prop, Tank}
import com.neo.sk.tank.shared.model.Constants.{GameState, ObstacleType}
import com.neo.sk.tank.shared.model.Point
import com.neo.sk.tank.shared.protocol.TankGameEvent

/**
  * @author wmy
  * @edit sky
  *       直接对接RoomActor
  **/
case class BotControl(bid: String, tankId: Int, name: String, roomId:Long,roomActor: ActorRef[RoomActor.Command], gameContainer: GameContainerServerImpl) {
  private var gameState: Int = GameState.play

  private var lastMouseMoveTheta: Float = 0
  private var currentMouseMOveTheta: Float = 0
  private val mouseMoveThreshold = math.Pi / 180
  private val clickRatio = 2
  private val eatRatio = 2
  private var isEatProp = false
  private var turnMsg = 0

  private val preExecuteFrameOffset = com.neo.sk.tank.shared.model.Constants.PreExecuteFrameOffset
  private val actionSerialNumGenerator = new AtomicInteger(0)

  def getActionSerialNum: Int = actionSerialNumGenerator.getAndIncrement()

  def setGameState(s: Int) = gameState = s

  def sendMsg2Actor: Unit = {
    val click = (new util.Random).nextInt(10)
    val eat = (new util.Random).nextInt(10)
    if (gameState == GameState.play) {
      val isHaveTarget = findTarget
      if (isHaveTarget && !isEatProp && click > clickRatio) {
        if (math.abs(currentMouseMOveTheta - lastMouseMoveTheta) >= mouseMoveThreshold) {
          lastMouseMoveTheta = currentMouseMOveTheta
          roomActor ! RoomActor.WebSocketMsg(bid, tankId, userMouseMove(currentMouseMOveTheta))
        }
        roomActor ! RoomActor.WebSocketMsg(bid, tankId, userMouseClick)
      }
      else if (isHaveTarget && isEatProp && eat > eatRatio) {
        if (turnMsg > 0) {
          //          log.debug(s"${userActor.path} begin to do to eat the prop ${turnMsg}")
          roomActor ! RoomActor.WebSocketMsg(bid, tankId, userKeyDown(turnMsg))
          roomActor ! RoomActor.WebSocketMsg(bid, tankId, userKeyDown(turnMsg))
        }
      }
      else {
        val randomKeyCode = (new util.Random).nextInt(4) + 37
        roomActor ! RoomActor.WebSocketMsg(bid, tankId, userKeyDown(randomKeyCode))
        roomActor ! RoomActor.WebSocketMsg(bid, tankId, userKeyUp(randomKeyCode))
      }
    }
  }

  private def chooseTheDirection(p: Point, q: Point) = {
    val x_dis = p.x - q.x
    val y_dis = p.y - q.y
    if (x_dis < -2.5) 39
    else if (x_dis > 2.5) 37
    else if (y_dis < -2.5) 40
    else if (y_dis > 2.5) 38
    else 0
  }

  private def userKeyDown(keyCode: Int) = {
    TankGameEvent.UserPressKeyDown(tankId, gameContainer.systemFrame + preExecuteFrameOffset, keyCode, getActionSerialNum)
  }

  def userKeyUp(keyCode: Int) = {
    TankGameEvent.UserPressKeyUp(tankId, gameContainer.systemFrame + 10 + preExecuteFrameOffset, keyCode, getActionSerialNum)
  }

  private def userMouseClick = {
    TankGameEvent.UC(tankId, gameContainer.systemFrame + preExecuteFrameOffset, System.currentTimeMillis(), getActionSerialNum)
  }

  private def userMouseMove(theta: Float) = {
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

    if (tankList.nonEmpty) {
      val thisTank = tankList.filter(_.tankId == tankId).head
      val thisTankPos = thisTank.getTankState().position
      val obstacleList = findOtherObstacle(thisTank)
      val airDropList = obstacleList.filter(r => r.obstacleType == ObstacleType.airDropBox)
      val brickList = obstacleList.filter(r => r.obstacleType == ObstacleType.brick)
      val propList = findOtherProp(thisTank)

      if (tankList.exists(r => r.tankId != tankId && judgeTheDistance(r.getTankState().position, thisTank.getTankState().position, 70))) {
        val attackTankList = tankList.filter(_.tankId != tankId).filter(r => judgeTheDistance(r.getTankState().position, thisTankPos, 70))
        val attakTank = attackTankList.minBy(tank => tank.getTankState().position.distance(thisTankPos))
        val pos = attakTank.getTankState().position
        currentMouseMOveTheta = pos.getTheta(thisTankPos).toFloat
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
        currentMouseMOveTheta = pos.getTheta(thisTankPos).toFloat
        true
      }
      else if (brickList.exists(r => judgeTheDistance(r.getObstacleState().p, thisTankPos, 70))) {
        val attackBrickList = brickList.filter(r => judgeTheDistance(r.getObstacleState().p, thisTankPos, 70))
        val attackBrick = attackBrickList.minBy(brick => brick.getObstacleState().p.distance(thisTankPos))
        val pos = attackBrick.getObstacleState().p
        currentMouseMOveTheta = pos.getTheta(thisTankPos).toFloat
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
