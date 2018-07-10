package com.neo.sk.tank.front.tankClient

import com.neo.sk.tank.front.common.Constants
import com.neo.sk.tank.front.components.StartGameModal
import com.neo.sk.tank.front.utils.{JsFunc, Shortcut}
import com.neo.sk.tank.shared.ptcl.model.{Boundary, Point}
import mhtml.Var
import org.scalajs.dom
import org.scalajs.dom.ext.KeyCode
import org.scalajs.dom.html.Canvas
import org.scalajs.dom.raw.{Event, MessageEvent, MouseEvent}

/**
  * Created by hongruying on 2018/7/9
  */
class GameHolder(canvasName:String) {

  private[this] val canvas = dom.document.getElementById(canvasName).asInstanceOf[Canvas]
  private[this] val ctx = canvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]

  private[this] val bounds = Point(canvas.width, canvas.height)
  private[this] val canvasUnit = 10
  private[this] val canvasBoundary = bounds * canvasUnit

  private[this] var myId = -1L
  private[this] var myTankId = -1L

  private[this] val grid = new GridClient(bounds)

  private[this] var firstCome = true
  private[this] var justSyncedFromServer = false
  private[this] val websocketClient = new WebSocketClient("",wsConnectSuccess,wsConnectError,wsMessageHandler,wsConnectClose)


  canvas.width = canvasBoundary.x.toInt
  canvas.height = canvasBoundary.y.toInt

  private val gameStateVar:Var[Int] = Var(Constants.GameState.firstCome)
  private var gameState:Int = Constants.GameState.firstCome


  private val startGameModal = new StartGameModal(gameStateVar,start)

  private var timer:Int = 0

  private var justSynced:Boolean = false

  private val maxClientFrameDrawForSystemFrame:Int = 4 //比系统桢多渲染3桢
  private var clientFrame:Int = 1

  val watchKeys = Set(
    KeyCode.Left,
    KeyCode.Up,
    KeyCode.Right,
    KeyCode.Down,
  )




  //todo
  private def wsConnectSuccess(e:Event) = {

  }

  //todo
  private def wsConnectError(e:Event) = {

  }

  //todo
  private def wsConnectClose(e:Event) = {

  }

  //todo
  private def wsMessageHandler(e:MessageEvent) = {

  }


  private def setGameState(s:Int):Unit = {
    gameStateVar := s
    gameState = s
  }

  def sendMsg2Server(msg:Any):Unit ={
    if(gameState == Constants.GameState.play)
      websocketClient.sendMsg("")

  }

  def addActionListenEvent():Unit = {
    canvas.focus()
    canvas.onmousemove = { (e:dom.MouseEvent) =>
      sendMsg2Server("")//发送鼠标位置
    }
    canvas.onclick = {(e:MouseEvent) =>
      sendMsg2Server("")//发送炮弹数据
    }

    canvas.onkeydown = {
      (e: dom.KeyboardEvent) => {
        if (watchKeys.contains(e.keyCode)) {
          println(s"key down: [${e.keyCode}]")
          sendMsg2Server("")//发送操作指令
          e.preventDefault()
        }
      }
    }

    canvas.onkeyup = {
      (e: dom.KeyboardEvent) => {
        if (watchKeys.contains(e.keyCode)) {
          println(s"key up: [${e.keyCode}]")
          sendMsg2Server("")//发送操作指令
          e.preventDefault()
        }
      }
    }
  }

  //游戏启动
  def start(name:String):Unit = {
    if(firstCome){
      firstCome = false
      websocketClient.setup()
      websocketClient.sendMsg("StartGame")
      setGameState(Constants.GameState.loadingPlay)
      gameLoop()
      timer = Shortcut.schedule(gameLoop,100)
    }
    if(websocketClient.getWsState){
      websocketClient.sendMsg("ReStartGame")
      setGameState(Constants.GameState.loadingPlay)
      timer = Shortcut.schedule(gameLoop,100)
    }else{
      JsFunc.alert("网络连接失败，请重新刷新")
    }
  }

  def gameLoop():Unit = {
    gameState match {
      case Constants.GameState.loadingPlay =>
        println(s"等待同步数据")
        drawGameLoading()
      case Constants.GameState.play =>
        justSynced match {
          case true =>


            clientFrame = maxClientFrameDrawForSystemFrame
          case false =>
            if(clientFrame == maxClientFrameDrawForSystemFrame)
              grid.update()
        }
        drawGame(clientFrame,maxClientFrameDrawForSystemFrame)
        clientFrame = clientFrame % maxClientFrameDrawForSystemFrame + 1



      case Constants.GameState.stop =>
        Shortcut.cancelSchedule(timer)

    }
  }

  def drawGameLoading():Unit = {

  }

  def drawGame(curFrame:Int,maxClientFrame:Int): Unit ={

  }









}
