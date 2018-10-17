package com.neo.sk.tank.front.tankClient
//import java.awt.Event

import com.neo.sk.tank.front.common.{Constants, Routes}
import com.neo.sk.tank.front.utils.Shortcut
import mhtml.Var
import org.scalajs.dom
import org.scalajs.dom.ext.Color
import org.scalajs.dom.raw.Event
import com.neo.sk.tank.front.utils.JsFunc
import com.neo.sk.tank.shared.model.Point
import com.neo.sk.tank.shared.protocol.TankGameEvent
import org.scalajs.dom.html.Canvas

class GameHolderObserver(canvasName:String,roomId:Int,playerId:Long){
  //如果gameHolderObserve继承gameHold,在gameHolder初始化的时候可以初始化observe
  //逻辑，TankDemo对象初始化GameHolder，进入到StartGameModal页面，点击房间ID观战，所以在StartGameModal要发接口获取当前房间的id
  //点击某个房间id进入该房间的观战页面
  private[this] val canvas = dom.document.getElementById(canvasName).asInstanceOf[Canvas]
  private[this] val ctx = canvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]

  private[this] val canvasWidth = dom.window.innerWidth.toFloat
  private[this] val canvasHeight = dom.window.innerHeight.toFloat
  private[this] val canvasUnit = 10
  private[this] val canvasBoundary = Point(canvasWidth, canvasHeight) / canvasUnit
  canvas.width = canvasWidth.toInt
  canvas.height = canvasHeight.toInt

  private[this] var gameContainerOpt:Option[GameContainerClientImpl] = None
  private[this] val webSocketClient = WebSocketClient(wsConnectSuccess,wsConnectError,wsMessageHandler,wsConnectClose)


  private var nextFrame = 0
  private var logicFrameTime = System.currentTimeMillis()
  private var timer:Int = 0
  private val gameStateVar:Var[Int] = Var(Constants.GameState.firstCome)
  private var gameState:Int = Constants.GameState.firstCome
  private def setGameState(s:Int) = {
    gameStateVar := s
    gameState = s
  }

  def gameRender():Double => Unit = {d =>
    val curTime = System.currentTimeMillis()
    val offsetTime = curTime - logicFrameTime
    drawGame(offsetTime)
    nextFrame = dom.window.requestAnimationFrame(gameRender())
  }

  def drawGame(offsetTime:Long) = {
    gameContainerOpt.foreach(_.drawGame(offsetTime,60))
  }

  private def wsConnectSuccess(e:Event) = {
    println("连接服务器成功")
    e
  }

  private def wsConnectError(e:Event) = {
    JsFunc.alert("网络连接失败，请重新刷新")
    e
  }

  private def wsConnectClose(e:Event):Event = {
    JsFunc.alert("网络连接失败，请重新刷新")
    e
  }

  private def drawGameStop():Unit = {
    ctx.fillStyle = Color.Black.toString()
    ctx.fillRect(0, 0, canvasBoundary.x * canvasUnit, canvasBoundary.y * canvasUnit)
    ctx.fillStyle = "rgb(250, 250, 250)"
    ctx.textAlign = "left"
    ctx.textBaseline = "top"
    ctx.font = "36px Helvetica"
    ctx.fillText(s"玩家已经死亡", 150, 180)
    println()
  }

  def gameLoop() ={
    gameContainerOpt.foreach(_.update())
    logicFrameTime = System.currentTimeMillis()

  }
  private def wsMessageHandler(data:TankGameEvent.WsMsgServer):Unit = {
    data match {
      case e: TankGameEvent.FirstSyncGameAllState=>
        e.tankIdOpt match {
          case Some(tankId) =>
            timer = Shortcut.schedule(gameLoop,e.configOpt.get.frameDuration)
            gameContainerOpt = Some(GameContainerClientImpl(ctx,e.configOpt.get,playerId,tankId,e.nameOpt.get,canvasBoundary,canvasUnit,setGameState))
            gameContainerOpt.foreach(_.receiveGameContainerAllState(e.gStateOpt.get))
            gameContainerOpt.get.getTankId(tankId)
            nextFrame = dom.window.requestAnimationFrame(gameRender())
        }

      case e:TankGameEvent.SyncGameAllState =>
        gameContainerOpt.foreach(_.receiveGameContainerAllState(e.gState))
        nextFrame = dom.window.requestAnimationFrame(gameRender())

      case e:TankGameEvent.SyncGameState =>
        gameContainerOpt.foreach(_.receiveGameContainerState(e.state))

      case e:TankGameEvent.Ranks =>
        /**
          * 游戏排行榜
          * */
        gameContainerOpt.foreach{ t =>
          t.currentRank = e.currentRank
          t.historyRank = e.historyRank
          t.rankUpdated = true
        }

      case e:TankGameEvent.UserActionEvent =>
        gameContainerOpt.foreach(_.receiveUserEvent(e))


      case e:TankGameEvent.GameEvent =>
        e match {
          case ee:TankGameEvent.GenerateBullet =>
            gameContainerOpt.foreach(_.receiveGameEvent(e))
          case _ => gameContainerOpt.foreach(_.receiveGameEvent(e))
        }

      case _ =>
    }

  }

  def watchGame() = {
    canvas.focus()
    webSocketClient.setup(Routes.wsWatchGameUrl(roomId,playerId))
    gameLoop()
  }



}
