package com.neo.sk.tank.front.tankClient

import java.util.concurrent.atomic.AtomicInteger

import com.neo.sk.tank.front.common.Constants
import com.neo.sk.tank.front.components.StartGameModal
import com.neo.sk.tank.front.utils.{JsFunc, Shortcut}
import com.neo.sk.tank.shared.game.GameContainerState
import com.neo.sk.tank.shared.model.Point
import com.neo.sk.tank.shared.protocol.TankGameEvent
import mhtml.Var
import org.scalajs.dom
import org.scalajs.dom.ext.{Color, KeyCode}
import org.scalajs.dom.html.Canvas
import org.scalajs.dom.raw.{Event, HTMLElement, MouseEvent}
import org.scalajs.dom

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.xml.Elem

/**
  * Created by hongruying on 2018/8/26
  */
case class GameHolder(canvasName:String,replay:Boolean=false) extends NetworkInfo {

  private[this] val canvas = dom.document.getElementById(canvasName).asInstanceOf[Canvas]
  private[this] val ctx = canvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]

  private[this] val canvasWidth = dom.window.innerWidth.toFloat
  private[this] val canvasHeight = dom.window.innerHeight.toFloat
  private[this] val canvasUnit = 10
  private[this] val canvasBoundary = Point(canvasWidth, canvasHeight) / canvasUnit

  private[this] var firstCome = true

  private val gameStateVar:Var[Int] = Var(Constants.GameState.firstCome)
  private var gameState:Int = Constants.GameState.firstCome
  private val startGameModal = new StartGameModal(gameStateVar,start)

  private var killerName:String = ""

  private[this] var gameContainerOpt : Option[GameContainerClientImpl] = None // 这里存储tank信息，包括tankId
  private[this] val webSocketClient = WebSocketClient(wsConnectSuccess,wsConnectError,if(replay) replayMessageHandler else wsMessageHandler,wsConnectClose,replay)

  private[this] val actionSerialNumGenerator = new AtomicInteger(0)
  private[this] val preExecuteFrameOffset = com.neo.sk.tank.shared.model.Constants.PreExecuteFrameOffset


  private var timer:Int = 0
  private var reStartTimer:Int = 0
  /**
    * 倒计时，config
    * */
  private val reStartInterval = 1000
  private val countDown = 3
  private var countDownTimes = countDown
  private var nextFrame = 0
  private var logicFrameTime = System.currentTimeMillis()

  canvas.width = canvasWidth.toInt
  canvas.height = canvasHeight.toInt

  private var spaceKeyUpState = true
  private var lastMouseMoveTheta:Float = 0
  private val mouseMoveThreshold = math.Pi / 180
  private val poKeyBoardMoveTheta = 2* math.Pi / 72 //炮筒顺时针转
  private val neKeyBoardMoveTheta = -2* math.Pi / 72 //炮筒逆时针转
  private var poKeyBoardFrame = 0L
  private var eKeyBoardState4AddBlood = true




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

  def getStartReplayModel(name:String,uid:Long,rid:Long)= {
    startReplay(name,uid,rid)
  }

  def gameRender():Double => Unit = {d =>
    val curTime = System.currentTimeMillis()
    val offsetTime = curTime - logicFrameTime
    drawGame(offsetTime)
    nextFrame = dom.window.requestAnimationFrame(gameRender())
  }


  private def setGameState(s:Int):Unit = {
    gameStateVar := s
    gameState = s
  }

  protected def sendMsg2Server(msg:TankGameEvent.WsMsgFront):Unit ={
    if(gameState == Constants.GameState.play)
      webSocketClient.sendMsg(msg)

  }

  private def addUserActionListenEvent():Unit = {
    canvas.focus()
    canvas.onmousemove = { e: dom.MouseEvent =>
      val point = Point(e.clientX.toFloat, e.clientY.toFloat) + Point(16,16)
      val theta = point.getTheta(canvasBoundary * canvasUnit / 2).toFloat
      if (gameContainerOpt.nonEmpty && gameState == Constants.GameState.play) {
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
      if (gameContainerOpt.nonEmpty && gameState == Constants.GameState.play) {
        val preExecuteAction = TankGameEvent.UserMouseClick(gameContainerOpt.get.myTankId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, System.currentTimeMillis(), getActionSerialNum)
        gameContainerOpt.get.preExecuteUserEvent(preExecuteAction)
        sendMsg2Server(preExecuteAction) //发送鼠标位置
        e.preventDefault()
      }
    }

    canvas.onkeydown = { e: dom.KeyboardEvent =>
      if (gameContainerOpt.nonEmpty && gameState == Constants.GameState.play) {
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
      if (gameContainerOpt.nonEmpty && gameState == Constants.GameState.play) {
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


  def start(name:String):Unit = {
    canvas.focus()
    if(firstCome){
      firstCome = false
      addUserActionListenEvent()
      setGameState(Constants.GameState.loadingPlay)
      webSocketClient.setup(name)
      gameLoop()

    }else if(webSocketClient.getWsState){
      gameContainerOpt match {
        case Some(gameContainer) =>
          webSocketClient.sendMsg(TankGameEvent.RestartGame(Some(gameContainer.myTankId),name))
        case None =>
          webSocketClient.sendMsg(TankGameEvent.RestartGame(None,name))
      }
      setGameState(Constants.GameState.loadingPlay)
      gameLoop()

    }else{
      JsFunc.alert("网络连接失败，请重新刷新")
    }
  }

  def startReplay(name:String,uid:Long,rid:Long)={
    canvas.focus()
    setGameState(Constants.GameState.loadingPlay)
    webSocketClient.setup(name,Some(uid),Some(rid))
    gameLoop()
  }

  private def gameLoop():Unit = {
    gameState match {
      case Constants.GameState.loadingPlay =>
        println(s"等待同步数据")
        drawGameLoading()
      case Constants.GameState.play =>
        /***/
        gameContainerOpt.foreach(_.update())
        logicFrameTime = System.currentTimeMillis()
        ping()

      case Constants.GameState.stop =>
        gameContainerOpt.foreach{t =>
          t.tankMap.get(t.myTankId) match {
            case Some(tank) =>
              if(tank.lives-1 > 0){
                /**
                  * 在生命值之内死亡重玩，倒计时进入
                  * */
                dom.window.cancelAnimationFrame(nextFrame)
                Shortcut.cancelSchedule(timer)
                drawGameRestart()
              }else{
                /**
                  * 重新生成id
                  * */
                dom.window.cancelAnimationFrame(nextFrame)
                Shortcut.cancelSchedule(timer)
                Shortcut.cancelSchedule(reStartTimer)
                drawGameStop()
                dom.document.getElementById("start_button").asInstanceOf[HTMLElement].focus()
              }
            case None =>
              dom.window.cancelAnimationFrame(nextFrame)
              Shortcut.cancelSchedule(timer)
              Shortcut.cancelSchedule(reStartTimer)
              drawGameStop()
              dom.document.getElementById("start_button").asInstanceOf[HTMLElement].focus()
          }
        }
    }
  }

  def drawGame(offsetTime:Long) = {
    gameContainerOpt.foreach(_.drawGame(offsetTime,getNetworkLatency))
  }

  private def drawGameLoading():Unit = {
    ctx.fillStyle = Color.Black.toString()
    ctx.fillRect(0, 0, canvasBoundary.x * canvasUnit, canvasBoundary.y * canvasUnit)
    ctx.fillStyle = "rgb(250, 250, 250)"
    ctx.textAlign = "left"
    ctx.textBaseline = "top"
    ctx.font = "36px Helvetica"
    ctx.fillText("请稍等，正在连接服务器", 150, 180)
//    println()
  }

  private def drawGameStop():Unit = {
    ctx.fillStyle = Color.Black.toString()
    ctx.fillRect(0, 0, canvasBoundary.x * canvasUnit, canvasBoundary.y * canvasUnit)
    ctx.fillStyle = "rgb(250, 250, 250)"
    ctx.textAlign = "left"
    ctx.textBaseline = "top"
    ctx.font = "36px Helvetica"
    ctx.fillText(s"您已经死亡,被玩家=${killerName}所杀", 150, 180)
    println()
  }

  private def drawGameRestart() = {
    ctx.fillStyle = Color.Black.toString()
    ctx.globalAlpha = 1
    ctx.fillRect(0, 0, canvasBoundary.x * canvasUnit, canvasBoundary.y * canvasUnit)
    if(countDownTimes > 0){
      ctx.fillStyle = Color.Black.toString()
      ctx.fillRect(0, 0, canvasBoundary.x * canvasUnit, canvasBoundary.y * canvasUnit)
      ctx.globalAlpha = 0.4
      ctx.fillStyle = "rgb(250, 250, 250)"
      ctx.textAlign = "left"
      ctx.textBaseline = "top"
      ctx.font = "36px Helvetica"
      ctx.fillText(s"重新进入房间，倒计时：${countDownTimes}",150,100)
      ctx.fillText(s"您已经死亡,被玩家=${killerName}所杀", 150, 180)
      countDownTimes = countDownTimes - 1
    }
    else{
      Shortcut.cancelSchedule(reStartTimer)
      gameContainerOpt.foreach(t => start(t.myName))
      countDownTimes = countDown
    }

  }

  private def wsConnectSuccess(e:Event) = {
    println(s"连接服务器成功")
    e
  }

  private def wsConnectError(e:Event) = {
    JsFunc.alert("网络连接失败，请重新刷新")
    e
  }


  private def wsConnectClose(e:Event) = {
    JsFunc.alert("网络连接失败，请重新刷新")
    e
  }

  private def wsMessageHandler(data:TankGameEvent.WsMsgServer):Unit = {
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
        killerName = e.name
        reStartTimer = Shortcut.schedule(drawGameRestart,reStartInterval)
        setGameState(Constants.GameState.stop)

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
        nextFrame = dom.window.requestAnimationFrame(gameRender())
        setGameState(Constants.GameState.play)


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

      case _ => println(s"unknow msg={sss}")
    }
  }

  var count=true
  private def replayMessageHandler(data:TankGameEvent.WsMsgServer):Unit = {
    data match {
      case e:TankGameEvent.YourInfo =>
        println("----Start!!!!!")
        timer = Shortcut.schedule(gameLoop, e.config.frameDuration)
        gameContainerOpt = Some(GameContainerClientImpl(ctx,e.config,e.userId,e.tankId,e.name, canvasBoundary, canvasUnit,setGameState))
        gameContainerOpt.get.getTankId(e.tankId)
//        setGameState(Constants.GameState.play)

      case e:TankGameEvent.SyncGameAllState =>
        if (count){
          count=false
          gameContainerOpt.foreach(_.receiveGameContainerAllState(e.gState))
          nextFrame = dom.window.requestAnimationFrame(gameRender())
          setGameState(Constants.GameState.play)
        }else{
          //fixme 此处存在重复操作
//          gameContainerOpt.foreach(_.receiveGameContainerAllState(e.gState))
          gameContainerOpt.foreach(_.receiveGameContainerState(GameContainerState(e.gState.f,e.gState.tanks,e.gState.props,e.gState.obstacle,e.gState.tankMoveAction)))
        }



      case e:TankGameEvent.UserActionEvent =>
        //        Shortcut.scheduleOnce(() => gameContainerOpt.foreach(_.receiveUserEvent(e)),100)
        gameContainerOpt.get.preExecuteUserEvent(e)
        gameContainerOpt.foreach(_.receiveUserEvent(e))


      case e:TankGameEvent.GameEvent =>
      /*  e match {
          case ee:TankGameEvent.GenerateBullet =>
            gameContainerOpt.foreach(_.receiveGameEvent(e))
          //            if(gameContainerOpt.get.systemFrame > ee.frame)
          //              println(s"recv GenerateBullet, curFrame=${gameContainerOpt.get.systemFrame}, eventFrame=${ee.frame}. event=${ee}")
          //            Shortcut.scheduleOnce(() => gameContainerOpt.foreach(_.receiveGameEvent(e)),100)
          case _ => gameContainerOpt.foreach(_.receiveGameEvent(e))
        }*/
        gameContainerOpt.foreach(_.receiveGameEvent(e))
      case e:TankGameEvent.EventData =>
        e.list.foreach(r=>replayMessageHandler(r))

      case e:TankGameEvent.PingPackage =>
        receivePingPackage(e)

      case e:TankGameEvent.DecodeError=>


      case _ => println(s"unknow msg={sss}")
    }
  }

}
