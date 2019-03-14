/*
 *  Copyright 2018 seekloud (https://github.com/seekloud)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.seekloud.tank.game.control

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.typed.scaladsl.adapter._
import javafx.animation.AnimationTimer
import javafx.scene.input.KeyCode

import org.seekloud.tank.ClientApp
import org.seekloud.tank.ClientApp.system
import org.seekloud.tank.core.PlayGameActor
import org.seekloud.tank.core.PlayGameActor.DispatchMsg
import org.seekloud.tank.common.Constants
import org.seekloud.tank.game.NetworkInfo
import org.seekloud.tank.model.JoinRoomRsp
import org.seekloud.tank.shared.`object`.Tank
import org.seekloud.tank.shared.game.GameContainerClientImpl
import org.seekloud.tank.shared.model.Constants.GameState
import org.seekloud.tank.shared.model.Point
import org.seekloud.tank.shared.protocol.TankGameEvent
import org.seekloud.utils.JavaFxUtil.getCanvasUnit
import org.seekloud.utils.canvas.MiddleFrameInFx
import org.slf4j.LoggerFactory

import scala.collection.mutable

/**
  * Created by sky
  * Date on 2019/3/10
  * Time at 下午5:28
  * 游戏控制基类
  * 连接gameServer的websocket
  * 然后使用AnimalTime来绘制屏幕，使用actor定时来做gameLoop的更新
  */
abstract class GameController(
                               var canvasWidth: Float,
                               var canvasHeight: Float,
                               isBot:Boolean
                             ) extends NetworkInfo {
  protected val log = LoggerFactory.getLogger(this.getClass)

  val playGameActor = system.spawn(PlayGameActor.create(this), "PlayGameActor")

  val drawFrame = new MiddleFrameInFx
  val canvas = drawFrame.createCanvas(canvasWidth, canvasHeight)
  var canvasUnit = getCanvasUnit(canvasWidth)
  var canvasBoundary = Point(canvasWidth, canvasHeight)

  protected var firstCome = true


  protected val actionSerialNumGenerator = new AtomicInteger(0)
  protected val preExecuteFrameOffset = org.seekloud.tank.shared.model.Constants.PreExecuteFrameOffset

  protected var recvYourInfo: Boolean = false
  protected var recvSyncGameAllState: Option[TankGameEvent.SyncGameAllState] = None

  protected var gameContainerOpt: Option[GameContainerClientImpl] = None // 这里存储tank信息，包括tankId
  protected var gameState = GameState.loadingPlay
  protected var logicFrameTime = System.currentTimeMillis()

  private var tickCount = 1
  //更新排行榜信息计时器
  private val rankCycle = 20

  protected val animationTimer = new AnimationTimer() {
    override def handle(now: Long): Unit = {
      drawGame(System.currentTimeMillis() - logicFrameTime)
    }
  }

  protected val watchKeys = Set(
    KeyCode.LEFT,
    KeyCode.DOWN,
    KeyCode.RIGHT,
    KeyCode.UP
  )

  protected val gunAngleAdjust = Set(
    KeyCode.K,
    KeyCode.L
  )

  protected val myKeySet = mutable.HashSet[KeyCode]()

  protected def setGameState(s: Int): Unit = {
    gameState = s
  }

  protected def setKillCallback(tank: Tank) = {
    if (gameContainerOpt.nonEmpty && tank.tankId == gameContainerOpt.get.tankId) {
      if (tank.lives <= 1) setGameState(GameState.stop)
    }
  }

  protected def checkScreenSize: Unit

  //fixme 考虑与killCallBack合并
  @deprecated
  protected def gameStopCallBack: Unit

  protected def canvas2Byte4Bot:Unit

  def logicLoop() = {
    ClientApp.pushStack2AppThread {
      checkScreenSize
      gameState match {
        case GameState.loadingPlay =>
          //        println(s"等待同步数据")
          gameContainerOpt.foreach(_.drawGameLoading())
        case GameState.play =>

          /** */
          if (tickCount % rankCycle == 1) {
            gameContainerOpt.foreach(_.updateRanks())
            gameContainerOpt.foreach(t => t.rankUpdated = true)
          }
          gameContainerOpt.foreach(_.update())
          canvas2Byte4Bot
          logicFrameTime = System.currentTimeMillis()
          ping()
          tickCount += 1

        case GameState.stop =>
          closeHolder
          gameContainerOpt.foreach(_.drawCombatGains())
          gameStopCallBack

        case _ => log.info(s"state=${gameState} failed")
      }
    }
  }

  def getActionSerialNum: Byte = actionSerialNumGenerator.getAndIncrement().toByte


  private def drawGame(offsetTime: Long) = {
    gameContainerOpt.foreach(_.drawGame(offsetTime, getNetworkLatency, Nil, Constants.supportLiveLimit))
  }


  protected def handleWsSuccess(e: TankGameEvent.WsSuccess)

  protected def handleWsMsgErrorRsp(e: TankGameEvent.WsMsgErrorRsp) = {
    if (e.errCode == 10001) {
      if(BotViewController.SDKReplyTo != null){
        BotViewController.SDKReplyTo ! JoinRoomRsp(-1,e.errCode,e.msg)
      }
      closeHolder
    }
  }

  protected def initGameContainerCallBack:Unit

  /**
    * 此处处理消息*/
  final def wsMessageHandler(data: TankGameEvent.WsMsgServer): Unit = {
    ClientApp.pushStack2AppThread {
      data match {
        case e: TankGameEvent.WsSuccess =>
          handleWsSuccess(e)

        case e: TankGameEvent.YourInfo =>

          /**
            * 更新游戏数据
            **/
          println("start------------")
          try {
            gameContainerOpt = Some(GameContainerClientImpl(drawFrame, canvas, e.config, e.userId, e.tankId, e.name, canvasBoundary, canvasUnit, setKillCallback, isBot = isBot, logInfo = log.info,layerCanvasSize = 2))
            initGameContainerCallBack
            gameContainerOpt.get.changeTankId(e.tankId)
            recvYourInfo = true
            recvSyncGameAllState.foreach(t => wsMessageHandler(t))
            if(BotViewController.SDKReplyTo != null){
              BotViewController.SDKReplyTo ! JoinRoomRsp(e.roomId)
            }
          } catch {
            case e: Exception =>
              closeHolder
              println(e.getMessage)
              println(e.printStackTrace())
              println("client is stop!!!")
          }

        case e: TankGameEvent.TankFollowEventSnap =>
          gameContainerOpt.foreach(_.receiveTankFollowEventSnap(e))

        case e: TankGameEvent.YouAreKilled =>

          /**
            * 死亡重玩
            **/
          println(s"you are killed")
          gameContainerOpt.foreach(_.updateDamageInfo(e.killTankNum, e.name, e.damageStatistics))
          //          killNum = e.killTankNum
          //          damageNum = e.damageStatistics
          //          killerList = killerList :+ e.name
          //          killerName = e.name
          //          animationTimer.stop()
          gameContainerOpt.foreach(_.drawGameStop())
          if (!e.hasLife || !Constants.supportLiveLimit) {
            setGameState(GameState.stop)
          } else animationTimer.stop()

        case e: TankGameEvent.SyncGameState =>
          gameContainerOpt.foreach(_.receiveGameContainerState(e.state))

        case e: TankGameEvent.SyncGameAllState =>
          if (!recvYourInfo) {
            log.info("----发生预料事件")
            recvSyncGameAllState = Some(e)
          } else {
            gameContainerOpt.foreach(_.receiveGameContainerAllState(e.gState))
            logicFrameTime = System.currentTimeMillis()
            animationTimer.start()
            gameContainerOpt.foreach(t => playGameActor ! PlayGameActor.StartGameLoop(t.config.frameDuration))
            setGameState(GameState.play)
          }

        case e: TankGameEvent.UserActionEvent =>
          gameContainerOpt.foreach(_.receiveUserEvent(e))


        case e: TankGameEvent.GameEvent =>
          e match {
            case e: TankGameEvent.UserRelive =>
              gameContainerOpt.foreach(_.receiveGameEvent(e))
              if (e.userId == gameContainerOpt.get.myId) {
                animationTimer.start()
              }
            case ee: TankGameEvent.GenerateBullet =>
              gameContainerOpt.foreach(_.receiveGameEvent(e))
            case _ => gameContainerOpt.foreach(_.receiveGameEvent(e))
          }

        case e: TankGameEvent.PingPackage =>
          receivePingPackage(e)


        case TankGameEvent.RebuildWebSocket =>
          gameContainerOpt.foreach(_.drawReplayMsg("存在异地登录。。"))
          closeHolder

        case _: TankGameEvent.DecodeError =>
          log.info("hahahha")

        case e: TankGameEvent.WsMsgErrorRsp =>
          handleWsMsgErrorRsp(e)

        case _ =>
          log.info(s"unknow msg={sss}")
      }
    }
  }

  protected def closeHolder = {
    animationTimer.stop()
    //remind 此处关闭WebSocket
    playGameActor ! PlayGameActor.StopGameActor
  }

}
