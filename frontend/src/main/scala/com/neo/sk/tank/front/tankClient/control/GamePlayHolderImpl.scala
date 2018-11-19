package com.neo.sk.tank.front.tankClient.control

import java.util.concurrent.atomic.AtomicInteger

import com.neo.sk.tank.front.common.Routes
import com.neo.sk.tank.front.components.StartGameModal
import com.neo.sk.tank.front.model.PlayerInfo
import com.neo.sk.tank.front.tankClient.game.GameContainerClientImpl
import com.neo.sk.tank.front.utils.{JsFunc, Shortcut}
import com.neo.sk.tank.shared.model.Constants.GameState
import com.neo.sk.tank.shared.model.Point
import com.neo.sk.tank.shared.protocol.TankGameEvent
import org.scalajs.dom
import org.scalajs.dom.ext.{Color, KeyCode}
import org.scalajs.dom.raw.MouseEvent

import scala.collection.mutable
import scala.xml.Elem

/**
  * User: sky
  * Date: 2018/10/29
  * Time: 13:00
  */
class GamePlayHolderImpl(name:String, playerInfoOpt: Option[PlayerInfo] = None) extends GameHolder(name) {
  private[this] val actionSerialNumGenerator = new AtomicInteger(0)
  private var spaceKeyUpState = true
  private var lastMouseMoveTheta:Float = 0
  private val mouseMoveThreshold = math.Pi / 180
  private val poKeyBoardMoveTheta = 2* math.Pi / 72 //炮筒顺时针转
  private val neKeyBoardMoveTheta = -2* math.Pi / 72 //炮筒逆时针转
  private var poKeyBoardFrame = 0L
  private var eKeyBoardState4AddBlood = true
  private val preExecuteFrameOffset = com.neo.sk.tank.shared.model.Constants.PreExecuteFrameOffset
  private val startGameModal = new StartGameModal(gameStateVar,start, playerInfoOpt)

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
    canvas.focus()
    if(firstCome){
      firstCome = false
      addUserActionListenEvent()
      setGameState(GameState.loadingPlay)
      //      webSocketClient.setup(Routes.wsJoinGameUrl(name))
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

  private def addUserActionListenEvent():Unit = {
    canvas.focus()
    canvas.onmousemove = { e: dom.MouseEvent =>
      val point = Point(e.clientX.toFloat, e.clientY.toFloat) + Point(16,16)
      val theta = point.getTheta(canvasBoundary * canvasUnit / 2).toFloat
      if (gameContainerOpt.nonEmpty && gameState == GameState.play) {
        if(math.abs(theta - lastMouseMoveTheta) >= mouseMoveThreshold){
          lastMouseMoveTheta = theta
          val preExecuteAction = TankGameEvent.UserMouseMove(gameContainerOpt.get.myTankId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, theta, getActionSerialNum)
          gameContainerOpt.get.preExecuteUserEvent(preExecuteAction)
          sendMsg2Server(preExecuteAction) //发送鼠标位置
          e.preventDefault()
        }
      }
    }
    canvas.onclick = { e: MouseEvent =>
      if (gameContainerOpt.nonEmpty && gameState == GameState.play) {
        val preExecuteAction = TankGameEvent.UserMouseClick(gameContainerOpt.get.myTankId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, System.currentTimeMillis(), getActionSerialNum)
        gameContainerOpt.get.preExecuteUserEvent(preExecuteAction)
        sendMsg2Server(preExecuteAction) //发送鼠标位置
        e.preventDefault()
      }
    }

    canvas.onkeydown = { e: dom.KeyboardEvent =>
      if (gameContainerOpt.nonEmpty && gameState == GameState.play) {
        /**
          * 增加按键操作
          * */
        val keyCode = changeKeys(e.keyCode)
        if (watchKeys.contains(keyCode) && !myKeySet.contains(keyCode)) {
          myKeySet.add(keyCode)
          println(s"key down: [${e.keyCode}]")
          val preExecuteAction = TankGameEvent.UserPressKeyDown(gameContainerOpt.get.myTankId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, keyCode, getActionSerialNum)
          gameContainerOpt.get.preExecuteUserEvent(preExecuteAction)
          sendMsg2Server(preExecuteAction)
          if(com.neo.sk.tank.shared.model.Constants.fakeRender){
            gameContainerOpt.get.addMyAction(preExecuteAction)
          }
          e.preventDefault()
        }
        if (gunAngleAdjust.contains(keyCode) && poKeyBoardFrame != gameContainerOpt.get.systemFrame) {
          myKeySet.remove(keyCode)
          println(s"key down: [${e.keyCode}]")
          poKeyBoardFrame = gameContainerOpt.get.systemFrame
          val Theta =
            if(keyCode == KeyCode.K) poKeyBoardMoveTheta
            else neKeyBoardMoveTheta
          val preExecuteAction = TankGameEvent.UserKeyboardMove(gameContainerOpt.get.myTankId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset,Theta.toFloat , getActionSerialNum)
          gameContainerOpt.get.preExecuteUserEvent(preExecuteAction)
          sendMsg2Server(preExecuteAction)
          e.preventDefault()

        }
        else if (keyCode == KeyCode.Space && spaceKeyUpState) {
          spaceKeyUpState = false
          val preExecuteAction = TankGameEvent.UserMouseClick(gameContainerOpt.get.myTankId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, System.currentTimeMillis(), getActionSerialNum)
          gameContainerOpt.get.preExecuteUserEvent(preExecuteAction)
          sendMsg2Server(preExecuteAction) //发送鼠标位置
          e.preventDefault()
        }
        else if(keyCode == KeyCode.E){
          /**
            * 吃道具
            * */
          eKeyBoardState4AddBlood = false
          val preExecuteAction = TankGameEvent.UserPressKeyMedical(gameContainerOpt.get.myTankId,gameContainerOpt.get.systemFrame + preExecuteFrameOffset,serialNum = getActionSerialNum)
          gameContainerOpt.get.preExecuteUserEvent(preExecuteAction)
          sendMsg2Server(preExecuteAction)
          e.preventDefault()
        }
      }
    }

    canvas.onkeyup = { e: dom.KeyboardEvent =>
      if (gameContainerOpt.nonEmpty && gameState == GameState.play) {
        val keyCode = changeKeys(e.keyCode)
        if (watchKeys.contains(keyCode)) {
          myKeySet.remove(keyCode)
          println(s"key up: [${e.keyCode}]")
          val preExecuteAction = TankGameEvent.UserPressKeyUp(gameContainerOpt.get.myTankId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, keyCode, getActionSerialNum)
          gameContainerOpt.get.preExecuteUserEvent(preExecuteAction)
          sendMsg2Server(preExecuteAction)
          if(com.neo.sk.tank.shared.model.Constants.fakeRender) {
            gameContainerOpt.get.addMyAction(preExecuteAction)
          }
          e.preventDefault()

        }
        //        if (gunAngleAdjust.contains(keyCode)) {
        //          myKeySet.remove(keyCode)
        //          println(s"key up: [${e.keyCode}]")
        //
        //          val Theta = if(keyCode == KeyCode.K){
        //             poKeyBoardMoveTheta
        //          }
        //          else {
        //            neKeyBoardMoveTheta
        //          }
        //          val preExecuteAction = TankGameEvent.UserKeyboardMove(gameContainerOpt.get.myTankId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset,Theta.toFloat , getActionSerialNum)
        //          gameContainerOpt.get.preExecuteUserEvent(preExecuteAction)
        //          sendMsg2Server(preExecuteAction)
        //          e.preventDefault()

        //        }
        else if (e.keyCode == KeyCode.Space) {
          spaceKeyUpState = true
          e.preventDefault()
        }
        else if(e.keyCode == KeyCode.E){
          eKeyBoardState4AddBlood = true
          e.preventDefault()
        }
      }
    }
  }

  override protected def wsMessageHandler(data:TankGameEvent.WsMsgServer):Unit = {
//    println(data.getClass)
    data match {
      case e:TankGameEvent.YourInfo =>
        timer = Shortcut.schedule(gameLoop, e.config.frameDuration)
        /**
          * 更新游戏数据
          * */
        gameContainerOpt = Some(GameContainerClientImpl(ctx,e.config,e.userId,e.tankId,e.name, canvasBoundary, canvasUnit,setGameState))
        gameContainerOpt.get.getTankId(e.tankId)

      case e:TankGameEvent.YouAreKilled =>
        /**
          * 死亡重玩
          * */
        println(s"you are killed")
        killNum = e.killTankNum
        killerList = killerList :+ e.name
        damageNum = e.damageStatistics
        killerName = e.name
        dom.window.cancelAnimationFrame(nextFrame)
        if(! e.hasLife){
          setGameState(GameState.stop)
        }else drawGameStop()

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

      case e:TankGameEvent.TankReliveInfo =>
        dom.window.cancelAnimationFrame(nextFrame)
        nextFrame = dom.window.requestAnimationFrame(gameRender())

      case e:TankGameEvent.UserActionEvent =>
        //        Shortcut.scheduleOnce(() => gameContainerOpt.foreach(_.receiveUserEvent(e)),100)
        gameContainerOpt.foreach(_.receiveUserEvent(e))


      case e:TankGameEvent.GameEvent =>
        e match {
          case ee:TankGameEvent.GenerateBullet =>
            gameContainerOpt.foreach(_.receiveGameEvent(e))
          //            if(gameContainerOpt.get.systemFrame > ee.frame)
          //              println(s"recv GenerateBullet, curFrame=${gameContainerOpt.get.systemFrame}, eventFrame=${ee.frame}. event=${ee}")
          //            Shortcut.scheduleOnce(() => gameContainerOpt.foreach(_.receiveGameEvent(e)),100)
          case _ => gameContainerOpt.foreach(_.receiveGameEvent(e))
        }

      case e:TankGameEvent.PingPackage =>
        receivePingPackage(e)

      case TankGameEvent.RebuildWebSocket=>
        drawReplayMsg("存在异地登录。。")
        closeHolder

      case _ => println(s"unknow msg={sss}")
    }
  }
}
