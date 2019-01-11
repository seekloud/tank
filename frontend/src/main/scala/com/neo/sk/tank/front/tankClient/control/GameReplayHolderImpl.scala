package com.neo.sk.tank.front.tankClient.control

import com.neo.sk.tank.front.common.Routes
import com.neo.sk.tank.front.model.{PlayerInfo, ReplayInfo}
import com.neo.sk.tank.front.utils.{JsFunc, Shortcut}
import com.neo.sk.tank.shared.game.GameContainerClientImpl
import com.neo.sk.tank.shared.protocol.TankGameEvent.GameContainerState
import com.neo.sk.tank.shared.model.Constants.GameState
import com.neo.sk.tank.shared.protocol.TankGameEvent
import org.scalajs.dom
import org.scalajs.dom.ext.Color

/**
  * User: sky
  * Date: 2018/10/29
  * Time: 13:00
  */
class GameReplayHolderImpl(name:String, playerInfoOpt: Option[PlayerInfo] = None) extends GameHolder(name) {
  webSocketClient.setWsReplay(true)

  def startReplay(option: Option[ReplayInfo]=None)={
    canvas.getCanvas.focus()
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
        gameContainerOpt.foreach(_.drawGameLoading())
      case GameState.play =>
        /***/
        if(tickCount % rankCycle == 1){
          gameContainerOpt.foreach(_.updateRanks())
          gameContainerOpt.foreach(t => t.rankUpdated = true)
        }
        gameContainerOpt.foreach(_.update())
        logicFrameTime = System.currentTimeMillis()
        ping()
        tickCount += 1

      case GameState.stop =>
        if(tickCount % rankCycle == 1){
          gameContainerOpt.foreach(_.updateRanks())
          gameContainerOpt.foreach(t => t.rankUpdated = true)
        }
        gameContainerOpt.foreach(_.update())
        logicFrameTime = System.currentTimeMillis()
        gameContainerOpt.foreach(_.drawGameStop())
        tickCount += 1

      case GameState.leave =>
        gameContainerOpt.foreach(_.update())
        logicFrameTime = System.currentTimeMillis()
        gameContainerOpt.foreach(_.drawUserLeftGame)

      case GameState.replayLoading =>
        gameContainerOpt.foreach(_.drawGameLoading())

      case _ => println(s"state=${gameState} failed")
    }
  }


  private def setKillCallback(name:String, hasLife:Boolean, killTankNum:Int, damage:Int) = {
    println(s"you are killed")
    gameContainerOpt.foreach(_.updateDamageInfo(killTankNum,name,damage))
  }

  override protected def wsMessageHandler(data:TankGameEvent.WsMsgServer):Unit = {
//    println(data.getClass)
    data match {
      case e:TankGameEvent.YourInfo =>
        println("----Start!!!!!")
        if(nextFrame!=0){
          dom.window.cancelAnimationFrame(nextFrame)
          Shortcut.cancelSchedule(timer)
          firstCome = true
        }
        //        timer = Shortcut.schedule(gameLoop, e.config.frameDuration)
        gameContainerOpt = Some(GameContainerClientImpl(drawFrame,ctx,e.config,e.userId,e.tankId,e.name, canvasBoundary, canvasUnit,setGameState, setKillCallback = setKillCallback, versionInfo = versionInfoOpt))
        gameContainerOpt.get.getTankId(e.tankId)

      case e:TankGameEvent.TankFollowEventSnap =>
        gameContainerOpt.foreach(_.receiveTankFollowEventSnap(e))

      case e:TankGameEvent.SyncGameAllState =>
        if(firstCome){
          firstCome = false
          setGameState(GameState.replayLoading)
          gameContainerOpt.foreach(_.receiveGameContainerAllState(e.gState))
          gameContainerOpt.foreach(_.update())
        }else{
          //remind here allState change into state
          gameContainerOpt.foreach(_.receiveGameContainerState(GameContainerState(e.gState.f,e.gState.tanks)))
        }

      case e:TankGameEvent.Ranks =>
        /**
          * 游戏排行榜
          * */
        gameContainerOpt.foreach{ t =>
          t.currentRank = e.currentRank
          t.historyRank = e.historyRank
          t.rankUpdated = true
        }


      case TankGameEvent.StartReplay =>
        println("start replay---")
        setGameState(GameState.play)
        timer = Shortcut.schedule(gameLoop, gameContainerOpt.get.config.frameDuration /  gameContainerOpt.get.config.playRate)
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

          case t: TankGameEvent.UserLeftRoom =>
            if(t.userId == gameContainerOpt.get.myId) {
              println(s"recv userLeft=${t},set stop")
              setGameState(GameState.leave)
            }

          case _ =>
        }

      case e:TankGameEvent.EventData =>
        e.list.foreach(r=>wsMessageHandler(r))
        //remind 快速播放
        if(this.gameState == GameState.replayLoading){
          gameContainerOpt.foreach(_.update())
        }

      case e:TankGameEvent.PingPackage =>
        receivePingPackage(e)

      case e:TankGameEvent.DecodeError=>

      case e:TankGameEvent.InitReplayError=>
        gameContainerOpt.foreach(_.drawReplayMsg(e.msg))

      case e:TankGameEvent.ReplayFinish=>
        gameContainerOpt.foreach(_.drawReplayMsg("游戏回放完毕。。。"))
        closeHolder

      case TankGameEvent.RebuildWebSocket=>
        gameContainerOpt.foreach(_.drawReplayMsg("存在异地登录。。"))
        closeHolder

      case _ => println(s"unknow msg={sss}")
    }
  }
}
