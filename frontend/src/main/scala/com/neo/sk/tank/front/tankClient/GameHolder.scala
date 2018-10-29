package com.neo.sk.tank.front.tankClient

import java.util.concurrent.atomic.AtomicInteger

import com.neo.sk.tank.front.components.StartGameModal
import com.neo.sk.tank.front.model.ReplayInfo
import com.neo.sk.tank.front.utils.{JsFunc, Shortcut}
import com.neo.sk.tank.shared.model.Constants.GameState
import com.neo.sk.tank.shared.model.Point
import com.neo.sk.tank.shared.protocol.TankGameEvent
import mhtml.Var
import org.scalajs.dom
import org.scalajs.dom.ext.{Color, KeyCode}
import org.scalajs.dom.html.Canvas
import org.scalajs.dom.raw.{Event, HTMLElement}

import scala.collection.mutable
import scala.xml.Elem

/**
  * User: sky
  * Date: 2018/10/29
  * Time: 12:47
  * 需要构造参数，所以重构为抽象类
  */
abstract class GameHolder(name:String) extends NetworkInfo{
  protected val canvas = dom.document.getElementById(name).asInstanceOf[Canvas]
  protected val ctx = canvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]

  protected var canvasWidth = dom.window.innerWidth.toFloat
  protected var canvasHeight = dom.window.innerHeight.toFloat
  protected val canvasUnit = 10
  protected var canvasBoundary = Point(canvasWidth, canvasHeight) / canvasUnit
  canvas.width = canvasWidth.toInt
  canvas.height = canvasHeight.toInt

  protected var firstCome = true

  protected val gameStateVar:Var[Int] = Var(GameState.firstCome)
  protected var gameState:Int = GameState.firstCome

  protected var killerName:String = ""

  protected var gameContainerOpt : Option[GameContainerClientImpl] = None // 这里存储tank信息，包括tankId

  protected val webSocketClient: WebSocketClient = WebSocketClient(wsConnectSuccess,wsConnectError, wsMessageHandler, wsConnectClose)


  protected var timer:Int = 0
  protected var reStartTimer:Int = 0
  /**
    * 倒计时，config
    * */
  protected val reStartInterval = 1000
  protected val countDown = 3
  protected var countDownTimes = countDown
  protected var nextFrame = 0
  protected var logicFrameTime = System.currentTimeMillis()


  def closeHolder={
    dom.window.cancelAnimationFrame(nextFrame)
    Shortcut.cancelSchedule(timer)
    Shortcut.cancelSchedule(reStartTimer)
    //    webSocketClient.closeWs
  }

  protected def gameRender():Double => Unit = {d =>
    val curTime = System.currentTimeMillis()
    val offsetTime = curTime - logicFrameTime
    drawGame(offsetTime)
    nextFrame = dom.window.requestAnimationFrame(gameRender())
  }


  protected def setGameState(s:Int):Unit = {
    gameStateVar := s
    gameState = s
  }

  protected def sendMsg2Server(msg:TankGameEvent.WsMsgFront):Unit ={
    if(gameState == GameState.play)
      webSocketClient.sendMsg(msg)
  }

  private def checkScreenSize={
    val newWidth=dom.window.innerWidth.toFloat
    val newHeight=dom.window.innerHeight.toFloat
    if(newWidth!=canvasWidth||newHeight!=canvasHeight){
      println("the screen size is change")
      canvasWidth=newWidth
      canvasHeight=newHeight
      canvasBoundary=Point(canvasWidth, canvasHeight) / canvasUnit
      canvas.width = canvasWidth.toInt
      canvas.height = canvasHeight.toInt
      gameContainerOpt.foreach{r=>
        r.updateClientSize(canvasBoundary)
      }
    }
  }

  protected def gameLoop():Unit = {
    checkScreenSize
    gameState match {
      case GameState.loadingPlay =>
        println(s"等待同步数据")
        drawGameLoading()
      case GameState.play =>
        /***/
        gameContainerOpt.foreach(_.update())
        logicFrameTime = System.currentTimeMillis()
        ping()

      case GameState.stop =>
        dom.window.cancelAnimationFrame(nextFrame)
        Shortcut.cancelSchedule(timer)
        Shortcut.cancelSchedule(reStartTimer)
        drawGameStop()
        dom.document.getElementById("start_button").asInstanceOf[HTMLElement].focus()

      case GameState.relive =>
        /**
          * 在生命值之内死亡重玩，倒计时进入
          * */
        dom.window.cancelAnimationFrame(nextFrame)
        Shortcut.cancelSchedule(timer)
        drawGameRestart()

      case _ => println(s"state=${gameState} failed")
    }
  }

  private def drawGame(offsetTime:Long) = {
    gameContainerOpt.foreach(_.drawGame(offsetTime,getNetworkLatency))
  }

  protected def drawGameLoading():Unit = {
    ctx.fillStyle = Color.Black.toString()
    ctx.fillRect(0, 0, canvasBoundary.x * canvasUnit, canvasBoundary.y * canvasUnit)
    ctx.fillStyle = "rgb(250, 250, 250)"
    ctx.textAlign = "left"
    ctx.textBaseline = "top"
    ctx.font = "36px Helvetica"
    ctx.fillText("请稍等，正在连接服务器", 150, 180)
    //    println()
  }

  protected def drawGameStop():Unit = {
    ctx.fillStyle = Color.Black.toString()
    ctx.fillRect(0, 0, canvasBoundary.x * canvasUnit, canvasBoundary.y * canvasUnit)
    ctx.fillStyle = "rgb(250, 250, 250)"
    ctx.textAlign = "left"
    ctx.textBaseline = "top"
    ctx.font = "36px Helvetica"
    ctx.fillText(s"您已经死亡,被玩家=${killerName}所杀", 150, 180)
    println()
  }

  protected def drawReplayMsg(m:String):Unit = {
    ctx.fillStyle = Color.Black.toString()
    ctx.fillRect(0, 0, canvasBoundary.x * canvasUnit, canvasBoundary.y * canvasUnit)
    ctx.fillStyle = "rgb(250, 250, 250)"
    ctx.textAlign = "left"
    ctx.textBaseline = "top"
    ctx.font = "36px Helvetica"
    ctx.fillText(m, 150, 180)
    println()
  }

  protected def drawGameRestart()

  protected def wsConnectSuccess(e:Event) = {
    println(s"连接服务器成功")
    e
  }

  protected def wsConnectError(e:Event) = {
    JsFunc.alert("网络连接失败，请重新刷新")
    e
  }

  protected def wsConnectClose(e:Event) = {
    JsFunc.alert("网络连接失败，请重新刷新")
    e
  }

  protected def wsMessageHandler(data:TankGameEvent.WsMsgServer)
}
