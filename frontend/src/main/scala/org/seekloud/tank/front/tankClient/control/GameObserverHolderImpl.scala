/*
 * Copyright 2018 seekloud (https://github.com/seekloud)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.seekloud.tank.front.tankClient.control

import org.seekloud.tank.front.common.Routes
import org.seekloud.tank.shared.`object`.Tank
import org.seekloud.tank.shared.protocol.TankGameEvent
import org.scalajs.dom
import org.scalajs.dom.ext.Color
import org.seekloud.tank.shared.game.GameContainerClientImpl
import org.seekloud.tank.shared.model.Constants.GameState
import org.seekloud.tank.front.common.{Constants, Routes}
import org.seekloud.tank.front.utils.Shortcut

/**
  * User: sky
  * Date: 2018/10/29
  * Time: 13:00
  */
class GameObserverHolderImpl(canvasObserver:String, roomId:Long, accessCode:String, playerId:Option[String]) extends GameHolder(canvasObserver) {

  override protected def gameLoop(): Unit = {
    checkScreenSize
    if(tickCount % rankCycle == 1){
      gameContainerOpt.foreach(_.updateRanks())
      gameContainerOpt.foreach(t => t.rankUpdated = true)
    }
    gameContainerOpt.foreach(_.update())
    logicFrameTime = System.currentTimeMillis()
    tickCount += 1
  }

  def watchGame() = {
    canvas.getCanvas.focus()
    webSocketClient.setup(Routes.getWsSocketUri(roomId, accessCode, playerId))
  }

  override protected def setKillCallback(tank: Tank) = {
    if (gameContainerOpt.nonEmpty&&tank.tankId ==gameContainerOpt.get.tankId) {
      if (tank.lives <= 1) setGameState(GameState.stop)
    }
  }

  override protected def wsMessageHandler(data:TankGameEvent.WsMsgServer):Unit = {
//    println(data.getClass)
    data match {
      case e:TankGameEvent.YourInfo =>
        //        setGameState(Constants.GameState.loadingPlay)
        gameContainerOpt = Some(GameContainerClientImpl(drawFrame,canvas,e.config,e.userId,e.tankId,e.name, canvasBoundary, canvasUnit,setKillCallback,versionInfoOpt))
        gameContainerOpt.get.changeTankId(e.tankId)
        Shortcut.cancelSchedule(timer)
        timer = Shortcut.schedule(gameLoop, e.config.frameDuration / e.config.playRate)

      case e:TankGameEvent.TankFollowEventSnap =>
        gameContainerOpt.foreach(_.receiveTankFollowEventSnap(e))

      case e:TankGameEvent.PlayerLeftRoom =>
        Shortcut.cancelSchedule(timer)
        gameContainerOpt.foreach(_.drawDeadImg(s"玩家已经离开了房间，请重新选择观战对象"))

      case e:TankGameEvent.SyncGameAllState =>
        gameContainerOpt.foreach(_.receiveGameContainerAllState(e.gState))
        dom.window.cancelAnimationFrame(nextFrame)
        nextFrame = dom.window.requestAnimationFrame(gameRender())

      case e:TankGameEvent.SyncGameState =>
        gameContainerOpt.foreach(_.receiveGameContainerState(e.state))

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
        gameContainerOpt.foreach(_.updateDamageInfo(e.killTankNum,e.name,e.damageStatistics))
        if(e.hasLife && Constants.supportLiveLimit){
          gameContainerOpt.foreach(_.drawDeadImg(s"玩家死亡，生命值未用尽，等待玩家复活"))
        }else{
          gameContainerOpt.foreach(_.drawDeadImg(s"玩家死亡，生命值已经用完啦！可以在此界面等待玩家重新进入房间"))

        }
        dom.window.cancelAnimationFrame(nextFrame)
//        Shortcut.cancelSchedule(timer)

      case TankGameEvent.RebuildWebSocket=>
        gameContainerOpt.foreach(_.drawReplayMsg("存在异地登录。。"))
        closeHolder


      case _ =>
    }

  }
}
