package com.neo.sk.tank.front.tankClient

import com.neo.sk.tank.front.common.Routes
import com.neo.sk.tank.front.model.{PlayerInfo, ReplayInfo}
import com.neo.sk.tank.front.utils.{JsFunc, Shortcut}
import com.neo.sk.tank.shared.game.GameContainerState
import com.neo.sk.tank.shared.model.Constants.GameState
import com.neo.sk.tank.shared.protocol.TankGameEvent
import org.scalajs.dom
import org.scalajs.dom.ext.Color
import org.scalajs.dom.html.Canvas

/**
  * User: sky
  * Date: 2018/10/29
  * Time: 13:00
  */
class GameObserverHolderImpl(canvasObserver:String, roomId:Long, accessCode:String, playerId:Option[String]) extends GameHolder(canvasObserver) {

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
  }

  override protected def gameLoop(): Unit = {
    checkScreenSize
    gameContainerOpt.foreach(_.update())
    logicFrameTime = System.currentTimeMillis()
  }

  def watchGame() = {
    canvas.focus()
    webSocketClient.setup(Routes.getWsSocketUri(roomId, accessCode, playerId))
  }

  override protected def wsMessageHandler(data:TankGameEvent.WsMsgServer):Unit = {
    println(data.getClass)
    data match {
      case e:TankGameEvent.YourInfo =>
        //        setGameState(Constants.GameState.loadingPlay)
        gameContainerOpt = Some(GameContainerClientImpl(ctx,e.config,e.userId,e.tankId,e.name, canvasBoundary, canvasUnit,setGameState, true))
        gameContainerOpt.get.getTankId(e.tankId)
        timer = Shortcut.schedule(gameLoop, e.config.frameDuration)

      case e:TankGameEvent.PlayerLeftRoom =>
        Shortcut.cancelSchedule(timer)
        gameContainerOpt.foreach(_.drawDeadImg(s"玩家已经离开了房间，请重新选择观战对象"))

      case e:TankGameEvent.SyncGameAllState =>
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
        if(e.hasLife){
          gameContainerOpt.foreach(_.drawDeadImg(s"玩家死亡，生命值未用尽，等待玩家复活"))
        }else{
          gameContainerOpt.foreach(_.drawDeadImg(s"玩家死亡，生命值已经用完啦！可以在此界面等待玩家重新进入房间"))

        }
        dom.window.cancelAnimationFrame(nextFrame)
        Shortcut.cancelSchedule(timer)

      case TankGameEvent.RebuildWebSocket=>
        drawReplayMsg("存在异地登录。。")
        closeHolder


      case _ =>
    }

  }
}
