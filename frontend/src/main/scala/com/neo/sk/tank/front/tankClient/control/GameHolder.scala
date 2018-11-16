package com.neo.sk.tank.front.tankClient.control

import com.neo.sk.tank.front.tankClient.game.GameContainerClientImpl
import com.neo.sk.tank.front.tankClient.{NetworkInfo, WebSocketClient}
import com.neo.sk.tank.front.utils.{JsFunc, Shortcut}
import com.neo.sk.tank.shared.model.Constants.GameState
import com.neo.sk.tank.shared.model.{Constants, Point}
import com.neo.sk.tank.shared.protocol.TankGameEvent
import mhtml.Var
import org.scalajs.dom
import org.scalajs.dom.ext.Color
import org.scalajs.dom.html.{Canvas, Div}
import org.scalajs.dom.raw.{Event, HTMLElement}

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


  protected var canvasUnit = getCanvasUnit(canvasWidth)
  protected var canvasBoundary = Point(canvasWidth, canvasHeight) / canvasUnit
  canvas.width = canvasWidth.toInt
  canvas.height = canvasHeight.toInt


  println(s"test111111111111=${canvasUnit},=${canvasWidth}")

  protected var killNum:Int = 0
  protected var damageNum:Int = 0
  var killerList = List.empty[String] //（击杀者）

  protected var firstCome = true

  protected val gameStateVar:Var[Int] = Var(GameState.firstCome)
  protected var gameState:Int = GameState.firstCome

  protected var killerName:String = ""


  protected var gameContainerOpt : Option[GameContainerClientImpl] = None // 这里存储tank信息，包括tankId

  protected val webSocketClient: WebSocketClient = WebSocketClient(wsConnectSuccess,wsConnectError, wsMessageHandler, wsConnectClose)


  protected var timer:Int = 0
//  protected var reStartTimer:Int = 0
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
    webSocketClient.closeWs
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

  protected def checkScreenSize={
    val newWidth=dom.window.innerWidth.toFloat
    val newHeight=dom.window.innerHeight.toFloat
    if(newWidth!=canvasWidth||newHeight!=canvasHeight){
      println("the screen size is change")
      canvasWidth=newWidth
      canvasHeight=newHeight
      canvasUnit = getCanvasUnit(canvasWidth)
      canvasBoundary=Point(canvasWidth, canvasHeight) / canvasUnit
      println(s"update screen=${canvasUnit},=${(canvasWidth,canvasHeight)}")
      canvas.width = canvasWidth.toInt
      canvas.height = canvasHeight.toInt
      gameContainerOpt.foreach{r=>
        r.updateClientSize(canvasBoundary, canvasUnit)
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
//        Shortcut.cancelSchedule(reStartTimer)
//        drawGameStop()
        Shortcut.scheduleOnce(() => drawCombatGains(), 3000)
//        dom.document.getElementById("start_button").asInstanceOf[HTMLElement].focus()
//        drawCombatGains()
//        dom.document.getElementById("start_button").asInstanceOf[HTMLElement].focus()

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
  }

  protected def drawGameStop():Unit = {
    ctx.fillStyle = Color.Black.toString()
    ctx.fillRect(0, 0, canvasBoundary.x * canvasUnit, canvasBoundary.y * canvasUnit)
    ctx.fillStyle = "rgb(250, 250, 250)"
    ctx.textAlign = "left"
    ctx.textBaseline = "top"
    ctx.font = s"${3.6 * canvasUnit}px Helvetica"
    ctx.fillText(s"您已经死亡,被玩家=${killerName}所杀,等待倒计时进入游戏", 150, 180)
    println()
  }

  protected def drawReplayMsg(m:String):Unit = {
    ctx.fillStyle = Color.Black.toString()
    ctx.fillRect(0, 0, canvasBoundary.x * canvasUnit, canvasBoundary.y * canvasUnit)
    ctx.fillStyle = "rgb(250, 250, 250)"
    ctx.textAlign = "left"
    ctx.textBaseline = "top"
    ctx.font = s"${3.6 * canvasUnit}px Helvetica"
    ctx.fillText(m, 150, 180)
    println()
  }

  protected def drawCombatGains(): Unit = {
    ctx.fillStyle = Color.Black.toString()
    ctx.fillRect(0, 0, canvasBoundary.x * canvasUnit, canvasBoundary.y * canvasUnit)
    val combatGians = dom.document.getElementById("combat_gains").asInstanceOf[Div]
    val temp = killerList.map(r => s"<span>${r.take(3)}</span>")
    combatGians.innerHTML = s"<p>击杀数:<span>${killNum}</span></p>" +
      s"<p>伤害量:<span>${damageNum}</span></p>" +
      s"<p>击杀者ID:" + temp.mkString("、")+ "</p>"
    killerList = List.empty[String]
  }



//  protected def drawGameRestart()

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


  protected def getCanvasUnit(canvasWidth:Float):Int = (canvasWidth / Constants.WindowView.x).toInt
}
