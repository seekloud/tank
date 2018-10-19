package com.neo.sk.tank.front.tankClient
//import java.awt.Event

import com.neo.sk.tank.front.common.{Constants, Routes}
import com.neo.sk.tank.front.utils.Shortcut
import mhtml.Var
import org.scalajs.dom
import org.scalajs.dom.ext.Color
import org.scalajs.dom.raw.{Event, HTMLElement}
import com.neo.sk.tank.front.utils.JsFunc
import com.neo.sk.tank.shared.`object`.Tank
import com.neo.sk.tank.shared.model.Point
import com.neo.sk.tank.shared.protocol.TankGameEvent
import org.scalajs.dom.html.Canvas

class GameHolderObserver(canvasObserver:String,roomId:Long, accessCode:String, playerId:Option[Long]){

  private[this] val canvas = dom.document.getElementById(canvasObserver).asInstanceOf[Canvas]
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
  private var killerName:String = ""

  def setGameState(s:Int):Unit = {
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

  def gameLoop() ={
    gameContainerOpt.foreach(_.update())
    logicFrameTime = System.currentTimeMillis()
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
    println(data.getClass)
    data match {
      case e:TankGameEvent.YourInfo =>
        setGameState(Constants.GameState.loadingPlay)
        gameContainerOpt = Some(GameContainerClientImpl(ctx,e.config,e.userId,e.tankId,e.name, canvasBoundary, canvasUnit,setGameState, true))
        gameContainerOpt.get.getTankId(e.tankId)
        timer = Shortcut.schedule(gameLoop, e.config.frameDuration)

      case e:TankGameEvent.PlayerLeftRoom =>
        Shortcut.cancelSchedule(timer)
        JsFunc.alert(s"玩家${e.name}已经离开房间")

//      case e: TankGameEvent.FirstSyncGameAllState=>
//        setGameState(Constants.GameState.play)
//        gameContainerOpt.foreach(_.receiveGameContainerAllState(e.gState))
//        nextFrame = dom.window.requestAnimationFrame(gameRender())

      case e:TankGameEvent.SyncGameAllState =>
        println("sssssssssssssssssssssssssssssssssssssssssssss=")
        setGameState(Constants.GameState.play)
        gameContainerOpt.foreach(_.receiveGameContainerAllState(e.gState))
        dom.window.cancelAnimationFrame(nextFrame)
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



      case e:TankGameEvent.YouAreKilled =>
        /**
          * 死亡重玩
          * */
        println(s"you are killed")
        killerName = e.name
        setGameState(Constants.GameState.stop)
        dom.window.cancelAnimationFrame(nextFrame)
        Shortcut.cancelSchedule(timer)
        gameContainerOpt.foreach(_.drawDeadImg())



      case _ =>
    }

  }

  def watchGame() = {
    canvas.focus()
    webSocketClient.setup(Routes.getWsSocketUri(roomId, accessCode, playerId))
  }



}
