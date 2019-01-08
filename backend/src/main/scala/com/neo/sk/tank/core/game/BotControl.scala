package com.neo.sk.tank.core.game

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.typed.ActorRef
import com.neo.sk.tank.core.BotActor._
import com.neo.sk.tank.core.UserActor
import com.neo.sk.tank.core.UserActor.WebSocketMsg
import com.neo.sk.tank.shared.`object`.{Prop, Tank}
import com.neo.sk.tank.shared.model.Constants.{GameState, ObstacleType}
import com.neo.sk.tank.shared.model.Point
import com.neo.sk.tank.shared.protocol.TankGameEvent
import com.neo.sk.tank.shared.protocol.TankGameEvent.WsMsgSource
import org.slf4j.LoggerFactory
import scala.language.postfixOps

class BotControl(name:String, actor:ActorRef[WsMsgSource]) {

  private val log = LoggerFactory.getLogger(this.getClass)
  var gameState:Int = GameState.loadingPlay

  private var lastMouseMoveTheta:Float = 0
  private var currentMouseMOveTheta:Float = 0
  private val mouseMoveThreshold = math.Pi / 180
  private val clickRatio = 2
  private val eatRatio = 2
  private var isEatProp = false
  private var turnMsg = 0


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
    val click = (new util.Random).nextInt(10)
    val eat = (new util.Random).nextInt(10)
    if(gameContainerOpt.nonEmpty && gameState == GameState.play){
      val isHaveTarget = findTarget
      if(isHaveTarget && !isEatProp && click > clickRatio){
        if(math.abs(currentMouseMOveTheta - lastMouseMoveTheta) >= mouseMoveThreshold) {
          lastMouseMoveTheta = currentMouseMOveTheta
          userActor ! userMouseMove(currentMouseMOveTheta)
        }
        userActor ! userMouseClick
      }
      else if(isHaveTarget && isEatProp && eat > eatRatio){
        if(turnMsg > 0){
          log.debug(s"${userActor.path} begin to do to eat the prop ${turnMsg}")
          userActor ! userKeyDown(turnMsg)
          actor ! StartUserKeyUp(turnMsg)
        }
      }
      else{
        val randomKeyCode = (new util.Random).nextInt(4) + 37
        userActor ! userKeyDown(randomKeyCode)
        actor ! StartUserKeyUp(randomKeyCode)
      }
    }
  }

  private def chooseTheDirection(p:Point, q:Point) = {
    val x_dis = p.x - q.x
    val y_dis = p.y - q.y
    if(x_dis < -2.5) 39
    else if(x_dis > 2.5)  37
    else if(y_dis < -2.5) 40
    else if(y_dis > 2.5)  38
    else 0
  }

  private def userKeyDown(keyCode:Int) = {
    val preExecuteAction = TankGameEvent.UserPressKeyDown(gameContainerOpt.get.myTankId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, keyCode, getActionSerialNum)
    gameContainerOpt.get.preExecuteUserEvent(preExecuteAction)
    if(com.neo.sk.tank.shared.model.Constants.fakeRender){
      gameContainerOpt.get.addMyAction(preExecuteAction)
    }
    WebSocketMsg(Some(preExecuteAction))
  }

  def userKeyUp(keyCode:Int, userActor:ActorRef[UserActor.Command]) = {
    val preExecuteAction = TankGameEvent.UserPressKeyUp(gameContainerOpt.get.myTankId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, keyCode, getActionSerialNum)
    gameContainerOpt.get.preExecuteUserEvent(preExecuteAction)
    if(com.neo.sk.tank.shared.model.Constants.fakeRender){
      gameContainerOpt.get.addMyAction(preExecuteAction)
    }
    userActor ! WebSocketMsg(Some(preExecuteAction))
  }

  private def userMouseClick = {
    val preExecuteAction = TankGameEvent.UserMouseClick(gameContainerOpt.get.myTankId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, System.currentTimeMillis(), getActionSerialNum)
    gameContainerOpt.get.preExecuteUserEvent(preExecuteAction)
    WebSocketMsg(Some(preExecuteAction))
  }

  private def userMouseMove(theta:Float) = {
    val preExecuteAction= TankGameEvent.UserMouseMove(gameContainerOpt.get.myTankId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, theta, getActionSerialNum)
    gameContainerOpt.get.preExecuteUserEvent(preExecuteAction)
    WebSocketMsg(Some(preExecuteAction))
  }

  def findTarget = {
    isEatProp = false

    val gameContainer = gameContainerOpt.get

    val tankListOpt = gameContainer.findAllTank(gameContainer.myTankId)
    val tankList = tankListOpt.getOrElse(List())

    if(tankList.nonEmpty){
      val thisTank = tankList.filter(_.tankId == gameContainer.myTankId).head
      val thisTankPos = thisTank.getTankState().position
      val obstacleList = gameContainer.findOtherObstacle(thisTank)
      val airDropList = obstacleList.filter(r => r.obstacleType == ObstacleType.airDropBox)
      val brickList = obstacleList.filter(r => r.obstacleType == ObstacleType.brick)
      val propList = gameContainer.findOtherProp(thisTank)

      if(tankList.exists(r => r.tankId != gameContainer.myTankId && judgeTheDistance(r.getTankState().position, thisTank.getTankState().position, 70))){
        val attackTankList = tankList.filter(_.tankId != gameContainer.myTankId).filter(r => judgeTheDistance(r.getTankState().position, thisTankPos, 70))
        val attakTank = attackTankList.minBy(tank => tank.getTankState().position.distance(thisTankPos))
        val pos = attakTank.getTankState().position
        currentMouseMOveTheta = pos.getTheta(thisTankPos).toFloat
        true
      }
      else if(propList.exists(r => judgeTheDistance(r.position, thisTankPos, 70))){
        val eatPropList = propList.filter(r => judgeTheDistance(r.position, thisTankPos, 70))
        val eatProp = eatPropList.minBy(p => p.position.distance(thisTankPos))
        turnMsg = chooseTheDirection(thisTankPos, eatProp.position)
        isEatProp = true
        true
      }
      else if(airDropList.exists(r => judgeTheDistance(r.getObstacleState().p, thisTankPos, 70))){
        val attackAirList = airDropList.filter(r => judgeTheDistance(r.getObstacleState().p, thisTankPos, 70))
        val attackAir = attackAirList.minBy(air => air.getObstacleState().p.distance(thisTankPos))
        val pos = attackAir.getObstacleState().p
        currentMouseMOveTheta = pos.getTheta(thisTankPos).toFloat
        true
      }
      else if(brickList.exists(r => judgeTheDistance(r.getObstacleState().p, thisTankPos, 70))){
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

  private def judgeTheDistance(p:Point, q:Point, dis:Int) = {
    if(p.distance(q) <= dis)
      true
    else
      false
  }

  def reStart(userActor:ActorRef[UserActor.Command]) = {
    userActor ! WebSocketMsg(Some(TankGameEvent.RestartGame(Some(gameContainerOpt.get.myTankId), name)))
  }

}
