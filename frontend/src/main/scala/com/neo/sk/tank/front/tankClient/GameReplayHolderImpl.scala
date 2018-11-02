package com.neo.sk.tank.front.tankClient

import java.util.concurrent.atomic.AtomicInteger

import com.neo.sk.tank.front.common.Routes
import com.neo.sk.tank.front.model.{PlayerInfo, ReplayInfo}
import com.neo.sk.tank.front.utils.{JsFunc, Shortcut}
import com.neo.sk.tank.shared.game.GameContainerState
import com.neo.sk.tank.shared.model.Constants.GameState
import com.neo.sk.tank.shared.model.Point
import com.neo.sk.tank.shared.protocol.TankGameEvent
import org.scalajs.dom
import org.scalajs.dom.ext.{Color, KeyCode}
import org.scalajs.dom.html.Canvas
import org.scalajs.dom.raw.MouseEvent

import scala.collection.mutable

/**
  * User: sky
  * Date: 2018/10/29
  * Time: 13:00
  */
class GameReplayHolderImpl(name:String, playerInfoOpt: Option[PlayerInfo] = None) extends GameHolder(name) {
  webSocketClient.setWsReplay(true)

  override protected def drawGameRestart(): Unit = {
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
    } else{
      Shortcut.cancelSchedule(reStartTimer)
      countDownTimes = countDown
    }
//    startReplay()
  }

  def startReplay(option: Option[ReplayInfo]=None)={
    canvas.focus()
    if(firstCome){
      setGameState(GameState.loadingPlay)
      webSocketClient.setup(Routes.getReplaySocketUri(option.get))
      gameLoop()
    }else if(webSocketClient.getWsState){
      firstCome = true
      gameLoop()
    }else{
      JsFunc.alert("网络连接失败，请重新刷新")
    }
  }

  override protected def gameLoop():Unit = {
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
        drawGameStop()

      case GameState.relive =>
        /**
          * 在生命值之内死亡重玩，倒计时进入
          * */
        drawGameStop()

      case GameState.replayLoading =>
        drawGameLoading()

      case _ => println(s"state=${gameState} failed")
    }
  }

  override protected def wsMessageHandler(data:TankGameEvent.WsMsgServer):Unit = {
    println(data.getClass)
    data match {
      case e:TankGameEvent.YourInfo =>
        println("----Start!!!!!")
        //        timer = Shortcut.schedule(gameLoop, e.config.frameDuration)
        gameContainerOpt = Some(GameContainerClientImpl(ctx,e.config,e.userId,e.tankId,e.name, canvasBoundary, canvasUnit,setGameState))
        gameContainerOpt.get.getTankId(e.tankId)

      case e:TankGameEvent.SyncGameAllState =>
        if(firstCome){
          firstCome = false
          setGameState(GameState.replayLoading)
//          timer = Shortcut.schedule(gameLoop, gameContainerOpt.get.config.frameDuration)
          gameContainerOpt.foreach(_.receiveGameContainerAllState(e.gState))
          gameContainerOpt.foreach(_.update())
//          nextFrame = dom.window.requestAnimationFrame(gameRender())
        }else{
          //fixme 此处存在重复操作
          //remind here allState change into state
          gameContainerOpt.foreach(_.receiveGameContainerState(GameContainerState(e.gState.f,e.gState.tanks,e.gState.props,e.gState.obstacle,e.gState.tankMoveAction)))
        }


      case TankGameEvent.StartReplay =>
        println("start replay---")
        setGameState(GameState.play)
        timer = Shortcut.schedule(gameLoop, gameContainerOpt.get.config.frameDuration)
        nextFrame = dom.window.requestAnimationFrame(gameRender())





      case e:TankGameEvent.UserActionEvent =>
        //remind here only add preAction without rollback
        gameContainerOpt.get.preExecuteUserEvent(e)


      case e:TankGameEvent.GameEvent =>
        gameContainerOpt.foreach(_.receiveGameEvent(e))
        //remind 此处判断是否为用户进入，更新userMap
        e match {
          case t: TankGameEvent.UserJoinRoom =>
            if (t.tankState.userId == gameContainerOpt.get.myId) {
              gameContainerOpt.foreach(_.changeTankId(t.tankState.tankId))
//              gameContainerOpt.foreach(_.update())
              setGameState(GameState.play)
            }
          case _ =>
        }

      case e:TankGameEvent.EventData =>
        e.list.foreach(r=>wsMessageHandler(r))
        if(this.gameState == GameState.replayLoading){
          gameContainerOpt.foreach(_.update())
        }

      case e:TankGameEvent.PingPackage =>
        receivePingPackage(e)

      case e:TankGameEvent.DecodeError=>

      case e:TankGameEvent.InitReplayError=>
        drawReplayMsg(e.msg)

      case e:TankGameEvent.ReplayFinish=>
        drawReplayMsg("游戏回放完毕。。。")
        closeHolder

      case TankGameEvent.RebuildWebSocket=>
        drawReplayMsg("存在异地登录。。")
        closeHolder

      case _ => println(s"unknow msg={sss}")
    }
  }
}
