package com.neo.sk.tank.front.tankClient.control

import com.neo.sk.tank.front.common.Routes
import com.neo.sk.tank.front.tankClient.game.GameContainerClientImpl
import com.neo.sk.tank.front.utils.Shortcut
import com.neo.sk.tank.shared.protocol.TankGameEvent
import org.scalajs.dom
import org.scalajs.dom.ext.Color

/**
  * User: sky
  * Date: 2018/10/29
  * Time: 13:00
  */
class GameObserverHolderImpl(canvasObserver:String, roomId:Long, accessCode:String, playerId:Option[String]) extends GameHolder(canvasObserver) {

  override protected def gameLoop(): Unit = {
    checkScreenSize
    gameContainerOpt.foreach(_.update())
    logicFrameTime = System.currentTimeMillis()
  }

  def watchGame() = {
    canvas.getCanvas.focus()
    webSocketClient.setup(Routes.getWsSocketUri(roomId, accessCode, playerId))
  }

  override protected def wsMessageHandler(data:TankGameEvent.WsMsgServer):Unit = {
//    println(data.getClass)
    data match {
      case e:TankGameEvent.YourInfo =>
        //        setGameState(Constants.GameState.loadingPlay)
        gameContainerOpt = Some(GameContainerClientImpl(drawFrame,ctx,e.config,e.userId,e.tankId,e.name, canvasBoundary, canvasUnit,setGameState, true))
        gameContainerOpt.get.getTankId(e.tankId)
        Shortcut.cancelSchedule(timer)
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
          case e:TankGameEvent.UserRelive =>
            println(e.getClass)
            gameContainerOpt.foreach(_.receiveGameEvent(e))
            playerId match{
              case Some(id) =>
                if(id == e.userId){
                  dom.window.cancelAnimationFrame(nextFrame)
                  nextFrame = dom.window.requestAnimationFrame(gameRender())
                }
              case None =>
            }

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
//        Shortcut.cancelSchedule(timer)

      case TankGameEvent.RebuildWebSocket=>
        drawReplayMsg("存在异地登录。。")
        closeHolder


      case _ =>
    }

  }
}
