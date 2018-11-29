package com.neo.sk.tank.front.tankClient.control

import java.util.concurrent.atomic.AtomicInteger

import com.neo.sk.tank.front.common.Routes
import com.neo.sk.tank.front.components.StartGameModal
import com.neo.sk.tank.front.model.PlayerInfo
import com.neo.sk.tank.shared.game.GameContainerClientImpl
import com.neo.sk.tank.front.utils.{JsFunc, Shortcut}
import com.neo.sk.tank.shared.`object`.Tank
import com.neo.sk.tank.shared.game.TankClientImpl
import com.neo.sk.tank.shared.model.Constants.{GameState, ObstacleType}
import com.neo.sk.tank.shared.model.Point
import com.neo.sk.tank.shared.protocol.TankGameEvent
import org.scalajs.dom
import org.scalajs.dom.ext.KeyCode
import org.scalajs.dom.raw.HTMLElement
import sun.text.resources.sr.FormatData_sr_Latn

import scala.collection.mutable
import scala.xml.Elem

class GameTestHolderImpl(name:String, playerInfoOpt: Option[PlayerInfo] = None) extends GameHolder(name){
  private[this] val actionSerialNumGenerator = new AtomicInteger(0)
  private var spaceKeyUpState = true
  private var enterKeyUpstate = true
  private var lastMouseMoveTheta:Float = 0
  private var currentMouseMOveTheta:Float = 0
  private val mouseMoveThreshold = math.Pi / 180
  private val poKeyBoardMoveTheta = 2* math.Pi / 72 //炮筒顺时针转
  private val neKeyBoardMoveTheta = -2* math.Pi / 72 //炮筒逆时针转
  private var poKeyBoardFrame = 0L
  private var eKeyBoardState4AddBlood = true
  private val preExecuteFrameOffset = com.neo.sk.tank.shared.model.Constants.PreExecuteFrameOffset
  private val startGameModal = new StartGameModal(gameStateVar,start, playerInfoOpt)
  private var timerForClick = 0
  private var thisTankId = 0

  private var thisTank = gameContainerOpt.getOrElse(None)

  private val watchKeys = Set(
    KeyCode.Left,
    KeyCode.Up,
    KeyCode.Right,
    KeyCode.Down
  )

  private val watchBakKeys = Set(
    KeyCode.W,
    KeyCode.S,
    KeyCode.A,
    KeyCode.D
  )

  private val gunAngleAdjust = Set(
    KeyCode.K,
    KeyCode.L
  )

  private val myKeySet = mutable.HashSet[Int]()

  private def changeKeys(k:Int):Int = k match {
    case KeyCode.W => KeyCode.Up
    case KeyCode.S => KeyCode.Down
    case KeyCode.A => KeyCode.Left
    case KeyCode.D => KeyCode.Right
    case origin => origin
  }

  def getActionSerialNum:Int = actionSerialNumGenerator.getAndIncrement()

  def getStartGameModal():Elem = {
    startGameModal.render
  }

  private def start(name:String,roomIdOpt:Option[Long]):Unit = {
    canvas.getCanvas.focus()
    Shortcut.scheduleOnce(() => userAction,1000)
    if(firstCome){
      firstCome = false
      setGameState(GameState.loadingPlay)
      webSocketClient.setup(Routes.getJoinGameWebSocketUri(name, playerInfoOpt,roomIdOpt))
      gameLoop()
    }else if(webSocketClient.getWsState){
      gameContainerOpt match {
        case Some(gameContainer) =>
          webSocketClient.sendMsg(TankGameEvent.RestartGame(Some(gameContainer.myTankId),name))
        case None =>
          webSocketClient.sendMsg(TankGameEvent.RestartGame(None,name))
      }
      setGameState(GameState.loadingPlay)
      gameLoop()
    }else{
      JsFunc.alert("网络连接失败，请重新刷新")
    }
  }

//  private def fakeUserKeyDown(key:List[Int]) = {
//    if (gameContainerOpt.nonEmpty && gameState == GameState.play) {
//      val keyCode = changeKeys(key(curKeyDown))
//      if (watchKeys.contains(keyCode) && !myKeySet.contains(keyCode)) {
//        myKeySet.add(keyCode)
//        val preExecuteAction = TankGameEvent.UserPressKeyDown(gameContainerOpt.get.myTankId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, keyCode, getActionSerialNum)
//        gameContainerOpt.get.preExecuteUserEvent(preExecuteAction)
//        sendMsg2Server(preExecuteAction)
//        if (com.neo.sk.tank.shared.model.Constants.fakeRender) {
//          gameContainerOpt.get.addMyAction(preExecuteAction)
//        }
//      }
//      if (gunAngleAdjust.contains(keyCode) && poKeyBoardFrame != gameContainerOpt.get.systemFrame) {
//        myKeySet.remove(keyCode)
//        poKeyBoardFrame = gameContainerOpt.get.systemFrame
//        val Theta =
//          if (keyCode == KeyCode.K) poKeyBoardMoveTheta
//          else neKeyBoardMoveTheta
//        val preExecuteAction = TankGameEvent.UserKeyboardMove(gameContainerOpt.get.myTankId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, Theta.toFloat, getActionSerialNum)
//        gameContainerOpt.get.preExecuteUserEvent(preExecuteAction)
//        sendMsg2Server(preExecuteAction)
//
//      }
//      else if (keyCode == KeyCode.Space && spaceKeyUpState) {
//        spaceKeyUpState = false
//        val preExecuteAction = TankGameEvent.UserMouseClick(gameContainerOpt.get.myTankId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, System.currentTimeMillis(), getActionSerialNum)
//        gameContainerOpt.get.preExecuteUserEvent(preExecuteAction)
//        sendMsg2Server(preExecuteAction) //发送鼠标位置
//      }
//      Shortcut.scheduleOnce(() => fakeUserKeyUp(keyCode),1000)
//      curKeyDown = (curKeyDown + 1) % fakeKeyDownList.length
//    }
//  }

  private def userAction:Unit = {
    Shortcut.cancelSchedule(timerForClick)
    if(gameContainerOpt.nonEmpty && gameState == GameState.play){

      if(findTarget){
        timerForClick = Shortcut.schedule(() => userClick(currentMouseMOveTheta), 500)
        Shortcut.scheduleOnce(() => userAction, 1000)
      }
      else{
        userMove
        Shortcut.scheduleOnce(() => userAction, 1000)
      }
    }
    else if(gameState == GameState.stop){
      Shortcut.scheduleOnce(() => dom.document.getElementById("start_button").asInstanceOf[HTMLElement].click(), 5000)
    }
  }

  private def findTarget = {

    val gameContainer = gameContainerOpt.get

    val tankListOpt = gameContainer.findAllTank(thisTankId)
    val tankList = tankListOpt.getOrElse(List())

    if(tankList.nonEmpty){
      val thisTank = tankList.filter(_.tankId == thisTankId).head

      val obstacleList = gameContainer.findOtherObstacle(thisTank)
      val airDropList = obstacleList.filter(r => r.obstacleType == ObstacleType.airDropBox)
      val brickList = obstacleList.filter(r => r.obstacleType == ObstacleType.brick)

      val offset = canvasBoundary / 2 - thisTank.getTankState().position

      if(tankList.exists(r => r.tankId != thisTankId && jugeTheDistance(r.getTankState().position + offset))){
        val attackTank = tankList.filter(_.tankId != thisTankId).find(r => jugeTheDistance(r.getTankState().position + offset)).get
        val pos = (attackTank.getTankState().position + offset) * canvasUnit
        currentMouseMOveTheta = pos.getTheta(canvasBoundary * canvasUnit / 2).toFloat
        true
      }
      else if(airDropList.exists(r => jugeTheDistance(r.getObstacleState().p + offset))){
        val attackAir = airDropList.find(r => jugeTheDistance(r.getObstacleState().p + offset)).get
        val pos = (attackAir.getObstacleState().p + offset) * canvasUnit
        currentMouseMOveTheta = pos.getTheta(canvasBoundary * canvasUnit / 2).toFloat
        true
      }
      else if(brickList.exists(r => jugeTheDistance(r.getObstacleState().p + offset))){
        val attackBrick = brickList.find(r => jugeTheDistance(r.getObstacleState().p + offset)).get
        val pos = (attackBrick.getObstacleState().p + offset) * canvasUnit
        currentMouseMOveTheta = pos.getTheta(canvasBoundary * canvasUnit / 2).toFloat
        true
      }
      else false
    }
    else false
  }

  private def jugeTheDistance(p:Point) = {
    if((p * canvasUnit).distance(canvasBoundary * canvasUnit / 2) <= 70 * canvasUnit)
      true
    else
      false
  }

  //模拟坦克移动
  private def userMove:Unit = {
    val randomKeyCode = (new util.Random).nextInt(4) + 37
    val keyCode = changeKeys(randomKeyCode)
    if (watchKeys.contains(keyCode) && !myKeySet.contains(keyCode)) {
      myKeySet.add(keyCode)
      val preExecuteAction = TankGameEvent.UserPressKeyDown(gameContainerOpt.get.myTankId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, keyCode, getActionSerialNum)
      gameContainerOpt.get.preExecuteUserEvent(preExecuteAction)
      sendMsg2Server(preExecuteAction)
      if (com.neo.sk.tank.shared.model.Constants.fakeRender) {
        gameContainerOpt.get.addMyAction(preExecuteAction)
      }
    }
    Shortcut.scheduleOnce(() => fakeUserKeyUp(keyCode),1000)
  }

  private def userClick(theta:Float):Unit = {
    if(math.abs(theta - lastMouseMoveTheta) >= mouseMoveThreshold) {
      lastMouseMoveTheta = theta
      fakeUserMouseMove(theta)
    }
    fakeUserMouseClick
  }

  //模拟鼠标点击
  private def fakeUserMouseClick = {
    val preExecuteAction = TankGameEvent.UserMouseClick(gameContainerOpt.get.myTankId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, System.currentTimeMillis(), getActionSerialNum)
    gameContainerOpt.get.preExecuteUserEvent(preExecuteAction)
    sendMsg2Server(preExecuteAction)
  }

  private def fakeUserKeyUp(keyCode:Int) = {
    if (watchKeys.contains(keyCode)){
      myKeySet.remove(keyCode)
      val preExecuteAction = TankGameEvent.UserPressKeyUp(gameContainerOpt.get.myTankId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, keyCode, getActionSerialNum)
      gameContainerOpt.get.preExecuteUserEvent(preExecuteAction)
      sendMsg2Server(preExecuteAction)
      if (com.neo.sk.tank.shared.model.Constants.fakeRender) {
        gameContainerOpt.get.addMyAction(preExecuteAction)
      }
    }
  }

  private def fakeUserMouseMove(theta:Float) = {
    val preExecuteAction = TankGameEvent.UserMouseMove(gameContainerOpt.get.myTankId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, theta, getActionSerialNum)
    gameContainerOpt.get.preExecuteUserEvent(preExecuteAction)
    sendMsg2Server(preExecuteAction)
  }



  override protected def wsMessageHandler(data:TankGameEvent.WsMsgServer):Unit = {
    data match {
      case e:TankGameEvent.YourInfo =>
        timer = Shortcut.schedule(gameLoop, e.config.frameDuration)
        /**
          * 更新游戏数据
          * */
        gameContainerOpt = Some(GameContainerClientImpl(drawFrame,ctx,e.config,e.userId,e.tankId,e.name, canvasBoundary, canvasUnit,setGameState))
        gameContainerOpt.get.getTankId(e.tankId)
        thisTankId = e.tankId

      case e:TankGameEvent.YouAreKilled =>
        /**
          * 死亡重玩
          * */
        println(s"you are killed")
        gameContainerOpt.foreach(_.updateDamageInfo(e.killTankNum,e.name,e.damageStatistics))
        dom.window.cancelAnimationFrame(nextFrame)
        gameContainerOpt.foreach(_.drawGameStop())
        if(! e.hasLife){
          setGameState(GameState.stop)
        }

      case e:TankGameEvent.Ranks =>
        /**
          * 游戏排行榜
          * */
        gameContainerOpt.foreach{ t =>
          t.currentRank = e.currentRank
          t.historyRank = e.historyRank
          t.rankUpdated = true
        }

      case e:TankGameEvent.SyncGameState =>
        gameContainerOpt.foreach(_.receiveGameContainerState(e.state))

      case e:TankGameEvent.SyncGameAllState =>
        gameContainerOpt.foreach(_.receiveGameContainerAllState(e.gState))
        dom.window.cancelAnimationFrame(nextFrame)
        nextFrame = dom.window.requestAnimationFrame(gameRender())
        setGameState(GameState.play)

      case e:TankGameEvent.UserActionEvent =>
        //        Shortcut.scheduleOnce(() => gameContainerOpt.foreach(_.receiveUserEvent(e)),100)
        gameContainerOpt.foreach(_.receiveUserEvent(e))


      case e:TankGameEvent.GameEvent =>
        e match {
          case e:TankGameEvent.UserRelive =>
            gameContainerOpt.foreach(_.receiveGameEvent(e))
            if(e.userId == gameContainerOpt.get.myId){
              dom.window.cancelAnimationFrame(nextFrame)
              nextFrame = dom.window.requestAnimationFrame(gameRender())
            }

          case ee:TankGameEvent.GenerateBullet =>
            gameContainerOpt.foreach(_.receiveGameEvent(e))
          case _ => gameContainerOpt.foreach(_.receiveGameEvent(e))
        }

      case e:TankGameEvent.PingPackage =>
        receivePingPackage(e)

      case TankGameEvent.RebuildWebSocket=>
        gameContainerOpt.foreach(_.drawReplayMsg("存在异地登录。。"))
        closeHolder

      case _ => println(s"unknow msg={sss}")
    }
  }

}
