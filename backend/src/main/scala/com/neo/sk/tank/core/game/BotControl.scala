package com.neo.sk.tank.core.game

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.Cancellable
import akka.actor.typed.ActorRef
import com.neo.sk.tank.Boot.{executor, scheduler}
import com.neo.sk.tank.core.BotActor._
import com.neo.sk.tank.core.UserActor
import com.neo.sk.tank.core.UserActor.WebSocketMsg
import com.neo.sk.tank.shared.model.Constants.{GameState, ObstacleType}
import com.neo.sk.tank.shared.model.Point
import com.neo.sk.tank.shared.protocol.TankGameEvent
import com.neo.sk.tank.shared.protocol.TankGameEvent.WsMsgSource
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.language.postfixOps

class BotControl(name:String, actor:ActorRef[WsMsgSource]) {

  private val log = LoggerFactory.getLogger(this.getClass)
  var gameState:Int = GameState.loadingPlay

  private var lastMouseMoveTheta:Float = 0
  private var currentMouseMOveTheta:Float = 0
  private val mouseMoveThreshold = math.Pi / 180
  private var isMove = true

  private val preExecuteFrameOffset = com.neo.sk.tank.shared.model.Constants.PreExecuteFrameOffset
  private val actionSerialNumGenerator = new AtomicInteger(0)
  var gameContainerOpt : Option[GameContainerTestImpl] = None

  private def setGameState(s:Int):Unit = {
    gameState = s
  }

  def getActionSerialNum:Int = actionSerialNumGenerator.getAndIncrement()

  def gameLoop():Unit = {
    gameState match {
      case GameState.loadingPlay =>
        println(s"等待同步数据")

      case GameState.play =>
        gameContainerOpt.foreach(_.update())

      case GameState.stop =>
        actor ! StopGameLoop
        actor ! RestartAGame

      case _ =>
        println(s"state=${gameState} failed")


    }
  }

  def wsMsgHandler(tankGameEvent: TankGameEvent.WsMsgServer) = {
    tankGameEvent match{
      case e: TankGameEvent.YourInfo =>
        actor ! StartGameLoop
        actor ! StartUserAction
        gameContainerOpt = Some(GameContainerTestImpl(e.config,e.userId,e.tankId,e.name,setGameState))
        gameContainerOpt.get.getTankId(e.tankId)

      case e: TankGameEvent.YouAreKilled =>
        /**
          * 死亡重玩
          **/
        println(s"you are killed")
        if(!e.hasLife){
          setGameState(GameState.stop)
        }

      case e:TankGameEvent.Ranks =>
        gameContainerOpt.foreach{ t =>
          t.currentRank = e.currentRank
          t.historyRank = e.historyRank
        }

      case e: TankGameEvent.SyncGameState =>
        gameContainerOpt.foreach(_.receiveGameContainerState(e.state))

      case e: TankGameEvent.SyncGameAllState =>
        gameContainerOpt.foreach(_.receiveGameContainerAllState(e.gState))
        setGameState(GameState.play)

      case e: TankGameEvent.UserActionEvent =>
        gameContainerOpt.foreach(_.receiveUserEvent(e))

      case e: TankGameEvent.GameEvent =>
        gameContainerOpt.foreach(_.receiveGameEvent(e))

      case _:TankGameEvent.DecodeError=>
        log.info("hahahha")

      case r =>
        log.info(s"unknow msg=${r}")
    }

  }

  def sendMsg2Actor(userActor:ActorRef[UserActor.Command]):Unit = {

    if(gameContainerOpt.nonEmpty && gameState == GameState.play){
      if(findTarget){
        if(math.abs(currentMouseMOveTheta - lastMouseMoveTheta) >= mouseMoveThreshold) {
          lastMouseMoveTheta = currentMouseMOveTheta
          userMouseMove(currentMouseMOveTheta, userActor)
        }
        userMouseClick(userActor)
      }
      else{
        val randomKeyCode = (new util.Random).nextInt(4) + 37
        userKeyDown(randomKeyCode, userActor)
        actor ! StartUserKeyUp(randomKeyCode)
      }
    }
  }

  private def userKeyDown(keyCode:Int, userActor:ActorRef[UserActor.Command]) = {
    val preExecuteAction = TankGameEvent.UserPressKeyDown(gameContainerOpt.get.myTankId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, keyCode, getActionSerialNum)
    gameContainerOpt.get.preExecuteUserEvent(preExecuteAction)
    if(com.neo.sk.tank.shared.model.Constants.fakeRender){
      gameContainerOpt.get.addMyAction(preExecuteAction)
    }
    userActor ! WebSocketMsg(Some(preExecuteAction))
  }

  def userKeyUp(keyCode:Int, userActor:ActorRef[UserActor.Command]) = {
    val preExecuteAction = TankGameEvent.UserPressKeyUp(gameContainerOpt.get.myTankId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, keyCode, getActionSerialNum)
    gameContainerOpt.get.preExecuteUserEvent(preExecuteAction)
    if(com.neo.sk.tank.shared.model.Constants.fakeRender){
      gameContainerOpt.get.addMyAction(preExecuteAction)
    }
    userActor ! WebSocketMsg(Some(preExecuteAction))
  }

  private def userMouseClick(userActor:ActorRef[UserActor.Command]) = {
    val preExecuteAction = TankGameEvent.UserMouseClick(gameContainerOpt.get.myTankId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, System.currentTimeMillis(), getActionSerialNum)
    gameContainerOpt.get.preExecuteUserEvent(preExecuteAction)
    userActor ! WebSocketMsg(Some(preExecuteAction))
  }

  private def userMouseMove(theta:Float, userActor:ActorRef[UserActor.Command]) = {
    val preExecuteAction= TankGameEvent.UserMouseMove(gameContainerOpt.get.myTankId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, theta, getActionSerialNum)
    gameContainerOpt.get.preExecuteUserEvent(preExecuteAction)
    userActor ! WebSocketMsg(Some(preExecuteAction))
  }

  def findTarget = {

    val gameContainer = gameContainerOpt.get

    val tankListOpt = gameContainer.findAllTank(gameContainer.myTankId)
    val tankList = tankListOpt.getOrElse(List())

    if(tankList.nonEmpty){
      val thisTank = tankList.filter(_.tankId == gameContainer.myTankId).head
      val obstacleList = gameContainer.findOtherObstacle(thisTank)
      val airDropList = obstacleList.filter(r => r.obstacleType == ObstacleType.airDropBox)
      val brickList = obstacleList.filter(r => r.obstacleType == ObstacleType.brick)

      if(tankList.exists(r => r.tankId != gameContainer.myTankId && jugeTheDistance(r.getTankState().position, thisTank.getTankState().position))){
        val attackTank = tankList.filter(_.tankId != gameContainer.myTankId).find(r => jugeTheDistance(r.getTankState().position, thisTank.getTankState().position)).get
        val pos = attackTank.getTankState().position
        currentMouseMOveTheta = pos.getTheta(thisTank.getTankState().position).toFloat
        true
      }
      else if(airDropList.exists(r => jugeTheDistance(r.getObstacleState().p, thisTank.getTankState().position))){
        val attackAir = airDropList.find(r => jugeTheDistance(r.getObstacleState().p, thisTank.getTankState().position)).get
        val pos = attackAir.getObstacleState().p
        currentMouseMOveTheta = pos.getTheta(thisTank.getTankState().position).toFloat
        true
      }
      else if(brickList.exists(r => jugeTheDistance(r.getObstacleState().p, thisTank.getTankState().position))){
        val attackBrick = brickList.find(r => jugeTheDistance(r.getObstacleState().p, thisTank.getTankState().position)).get
        val pos = attackBrick.getObstacleState().p
        currentMouseMOveTheta = pos.getTheta(thisTank.getTankState().position).toFloat
        true
      }
      else false
    }
    else false
  }

  private def jugeTheDistance(p:Point, q:Point) = {
    if(p.distance(q) <= 70)
      true
    else
      false
  }

  def reStart(userActor:ActorRef[UserActor.Command]) = {
    userActor ! WebSocketMsg(Some(TankGameEvent.RestartGame(Some(gameContainerOpt.get.myTankId), name)))
  }

}
