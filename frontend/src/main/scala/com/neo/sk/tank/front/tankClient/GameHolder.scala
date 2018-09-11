package com.neo.sk.tank.front.tankClient

import java.util.concurrent.atomic.AtomicInteger

import com.neo.sk.tank.front.common.Constants
import com.neo.sk.tank.front.components.StartGameModal
import com.neo.sk.tank.front.utils.{JsFunc, Shortcut}
import com.neo.sk.tank.shared.model.Point
import com.neo.sk.tank.shared.protocol.TankGameEvent
import mhtml.Var
import org.scalajs.dom
import org.scalajs.dom.ext.{Color, KeyCode}
import org.scalajs.dom.html.Canvas
import org.scalajs.dom.raw.{Event, HTMLElement, MouseEvent}
import org.scalajs.dom

import scala.collection.mutable
import scala.xml.Elem

/**
  * Created by hongruying on 2018/8/26
  */
case class GameHolder(canvasName:String) extends NetworkInfo {

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

  private[this] var gameContainerOpt : Option[GameContainerClientImpl] = None
  private[this] val webSocketClient = WebSocketClient(wsConnectSuccess,wsConnectError,wsMessageHandler,wsConnectClose)

  private[this] val actionSerialNumGenerator = new AtomicInteger(0)
  private[this] val preExecuteFrameOffset = 2


  private var timer:Int = 0
  private var nextFrame = 0
  private var logicFrameTime = System.currentTimeMillis()

  canvas.width = canvasWidth.toInt
  canvas.height = canvasHeight.toInt

  private var spaceKeyUpState = true
  private var lastMouseMoveTheta:Float = 0
  private val mouseMoveThreshold = math.Pi / 180
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
      val point = Point(e.clientX.toFloat, e.clientY.toFloat)
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
        val keyCode = changeKeys(e.keyCode)
        if (watchKeys.contains(keyCode) && !myKeySet.contains(keyCode)) {
          myKeySet.add(keyCode)
          println(s"key down: [${e.keyCode}]")
          val preExecuteAction = TankGameEvent.UserPressKeyDown(gameContainerOpt.get.myTankId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, keyCode, getActionSerialNum)
          gameContainerOpt.get.preExecuteUserEvent(preExecuteAction)
          sendMsg2Server(preExecuteAction)
          e.preventDefault()
        } else if (keyCode == KeyCode.Space && spaceKeyUpState) {
          spaceKeyUpState = false
          val preExecuteAction = TankGameEvent.UserMouseClick(gameContainerOpt.get.myTankId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, System.currentTimeMillis(), getActionSerialNum)
          gameContainerOpt.get.preExecuteUserEvent(preExecuteAction)
          sendMsg2Server(preExecuteAction) //发送鼠标位置
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
          e.preventDefault()
        } else if (e.keyCode == KeyCode.Space) {
          spaceKeyUpState = true
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
      webSocketClient.sendMsg(TankGameEvent.RestartGame(name))
      setGameState(Constants.GameState.loadingPlay)
      gameLoop()

    }else{
      JsFunc.alert("网络连接失败，请重新刷新")
    }
  }

  private def gameLoop():Unit = {
    gameState match {
      case Constants.GameState.loadingPlay =>
        println(s"等待同步数据")
        drawGameLoading()
      case Constants.GameState.play =>
        gameContainerOpt.foreach(_.update())
        logicFrameTime = System.currentTimeMillis()
        ping()

      case Constants.GameState.stop =>
        dom.window.cancelAnimationFrame(nextFrame)
        Shortcut.cancelSchedule(timer)
        drawGameStop()
        dom.document.getElementById("TankGameNameInput").asInstanceOf[HTMLElement].focus()
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
    println()
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
        gameContainerOpt = Some(GameContainerClientImpl(ctx,e.config,e.userId,e.tankId,e.name, canvasBoundary, canvasUnit,setGameState))

      case e:TankGameEvent.YouAreKilled =>
        println(s"you are killed")
        killerName = e.name
        setGameState(Constants.GameState.stop)

      case e:TankGameEvent.Ranks =>
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

}
