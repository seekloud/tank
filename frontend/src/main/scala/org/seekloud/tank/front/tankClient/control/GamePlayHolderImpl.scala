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

import java.util.concurrent.atomic.AtomicInteger

import org.seekloud.tank.front.common.Routes
import org.seekloud.tank.front.model.PlayerInfo
import org.seekloud.tank.front.utils.Shortcut
import org.seekloud.tank.shared.`object`.Tank
import org.seekloud.tank.shared.game.GameContainerClientImpl
import org.seekloud.tank.shared.model.Constants.GameState
import org.seekloud.tank.shared.model.Point
import org.seekloud.tank.shared.protocol.TankGameEvent
import org.scalajs.dom
import org.scalajs.dom.ext.{Color, KeyCode}
import org.scalajs.dom.raw.MouseEvent
import org.seekloud.tank.front.common.{Constants, Routes}
import org.seekloud.tank.front.components.StartGameModal
import org.seekloud.tank.front.utils.{JsFunc, Shortcut}

import scala.collection.mutable
import scala.xml.Elem

/**
  * User: sky
  * Date: 2018/10/29
  * Time: 13:00
  */
class GamePlayHolderImpl(name: String, playerInfoOpt: Option[PlayerInfo] = None) extends GameHolder(name) {
  private[this] val actionSerialNumGenerator = new AtomicInteger(0)
  private var spaceKeyUpState = true
  private var lastMouseMoveAngle: Byte = 0
  private val perMouseMoveFrame = 3
  private var lastMoveFrame = -1L
  private val poKeyBoardMoveTheta = 2 * math.Pi / 72 //炮筒顺时针转
  private val neKeyBoardMoveTheta = -2 * math.Pi / 72 //炮筒逆时针转
  private var poKeyBoardFrame = 0L
  private var eKeyBoardState4AddBlood = true
  private val preExecuteFrameOffset = org.seekloud.tank.shared.model.Constants.PreExecuteFrameOffset
  private val startGameModal = new StartGameModal(gameStateVar, start, playerInfoOpt)

  private val watchKeys = Set(
    KeyCode.Left,
    KeyCode.Up,
    KeyCode.Right,
    KeyCode.Down
  )

  private val gunAngleAdjust = Set(
    KeyCode.K,
    KeyCode.L
  )

  private val myKeySet = mutable.HashSet[Int]()

  private def changeKeys(k: Int): Int = k match {
    case KeyCode.W => KeyCode.Up
    case KeyCode.S => KeyCode.Down
    case KeyCode.A => KeyCode.Left
    case KeyCode.D => KeyCode.Right
    case origin => origin
  }

  private def getMoveStateByKeySet(actionSet:Set[Int]):Byte = {
    if(actionSet.contains(KeyCode.Left) && actionSet.contains(KeyCode.Up)){
      5
    }else if(actionSet.contains(KeyCode.Right) && actionSet.contains(KeyCode.Up)){
      7
    }else if(actionSet.contains(KeyCode.Left) && actionSet.contains(KeyCode.Down)){
      3
    }else if(actionSet.contains(KeyCode.Right) && actionSet.contains(KeyCode.Down)){
      1
    }else if(actionSet.contains(KeyCode.Right)){
      0
    }else if(actionSet.contains(KeyCode.Left)){
      4
    }else if(actionSet.contains(KeyCode.Up) ){
      6
    }else if(actionSet.contains(KeyCode.Down)){
      2
    }else 8
  }

  def getActionSerialNum: Byte = (actionSerialNumGenerator.getAndIncrement()%127).toByte

  def getStartGameModal(): Elem = {
    startGameModal.render
  }

  def start(name: String, roomIdOpt: Option[Long]): Unit = {
    canvas.getCanvas.focus()
    dom.window.cancelAnimationFrame(nextFrame)
    Shortcut.cancelSchedule(timer)
    if (firstCome) {
      firstCome = false
      addUserActionListenEvent()
      setGameState(GameState.loadingPlay)
      webSocketClient.setup(Routes.getJoinGameWebSocketUri(name, playerInfoOpt, roomIdOpt))
      //      webSocketClient.sendMsg(TankGameEvent.StartGame(roomIdOpt,None))
      gameLoop()

    } else if (webSocketClient.getWsState) {
      gameContainerOpt match {
        case Some(gameContainer) =>
          gameContainerOpt.foreach(_.changeTankId(gameContainer.myTankId))
          if (Constants.supportLiveLimit) {
            webSocketClient.sendMsg(TankGameEvent.RestartGame(Some(gameContainer.myTankId), name))
          } else {
            webSocketClient.sendMsg(TankGameEvent.RestartGame(None, name))
          }

        case None =>
          webSocketClient.sendMsg(TankGameEvent.RestartGame(None, name))
      }
      setGameState(GameState.loadingPlay)
      gameLoop()

    } else {
      JsFunc.alert("网络连接失败，请重新刷新")
    }
  }

  private def addUserActionListenEvent(): Unit = {
    canvas.getCanvas.focus()
    canvas.getCanvas.onmousemove = { e: dom.MouseEvent =>
      val point = Point(e.clientX.toFloat, e.clientY.toFloat) + Point(24, 24)
      val theta = point.getTheta(canvasSize  / 2).toFloat
      val angle = point.getAngle(canvasSize  / 2)
      val preMMFAction = TankGameEvent.UserMouseMove(gameContainerOpt.get.myTankId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, theta,-1)
      gameContainerOpt.get.preExecuteUserEvent(preMMFAction)
      if (gameContainerOpt.nonEmpty && gameState == GameState.play && lastMoveFrame+perMouseMoveFrame < gameContainerOpt.get.systemFrame) {
        if (lastMouseMoveAngle!=angle) {
          lastMouseMoveAngle = angle
          lastMoveFrame = gameContainerOpt.get.systemFrame
          val preMMBAction = TankGameEvent.UM(gameContainerOpt.get.myTankId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, angle, getActionSerialNum)
          sendMsg2Server(preMMBAction) //发送鼠标位置
          e.preventDefault()
        }
      }
    }
    canvas.getCanvas.onclick = { e: MouseEvent =>
      if (gameContainerOpt.nonEmpty && gameState == GameState.play) {
        val tank=gameContainerOpt.get.tankMap.get(gameContainerOpt.get.myTankId)
        if(tank.nonEmpty&&tank.get.getBulletSize()>0){
          val point = Point(e.clientX.toFloat, e.clientY.toFloat) + Point(24, 24)
          val theta = point.getTheta(canvasSize  / 2).toFloat
          val preExecuteAction = TankGameEvent.UC(gameContainerOpt.get.myTankId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, theta, getActionSerialNum)
          sendMsg2Server(preExecuteAction) //发送鼠标位置
          e.preventDefault()
        }
      }
    }

    canvas.getCanvas.onkeydown = { e: dom.KeyboardEvent =>
      if (gameContainerOpt.nonEmpty && gameState == GameState.play) {
        /**
          * 增加按键操作
          **/
        val keyCode = changeKeys(e.keyCode)
        if (watchKeys.contains(keyCode) && !myKeySet.contains(keyCode)) {
          myKeySet.add(keyCode)
          //          println(s"key down: [${e.keyCode}]")
          val preExecuteAction = TankGameEvent.UserMoveState(gameContainerOpt.get.myTankId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, getMoveStateByKeySet(myKeySet.toSet), getActionSerialNum)
          gameContainerOpt.get.preExecuteUserEvent(preExecuteAction)
          sendMsg2Server(preExecuteAction)
          if (org.seekloud.tank.shared.model.Constants.fakeRender) {
            gameContainerOpt.get.addMyAction(preExecuteAction)
          }
          e.preventDefault()
        }
        if (gunAngleAdjust.contains(keyCode) && poKeyBoardFrame != gameContainerOpt.get.systemFrame) {
          myKeySet.remove(keyCode)
          //          println(s"key down: [${e.keyCode}]")
          poKeyBoardFrame = gameContainerOpt.get.systemFrame
          val Theta =
            if (keyCode == KeyCode.K) poKeyBoardMoveTheta
            else neKeyBoardMoveTheta
          val preExecuteAction = TankGameEvent.UserKeyboardMove(gameContainerOpt.get.myTankId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, Theta.toFloat, getActionSerialNum)
          gameContainerOpt.get.preExecuteUserEvent(preExecuteAction)
          sendMsg2Server(preExecuteAction)
          e.preventDefault()
        }
        else if (keyCode == KeyCode.Space && spaceKeyUpState) {
          //          audioForBullet.play()
          spaceKeyUpState = false
          val preExecuteAction = TankGameEvent.UC(gameContainerOpt.get.myTankId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, gameContainerOpt.get.tankMap(gameContainerOpt.get.myTankId).getGunDirection(), getActionSerialNum)
          gameContainerOpt.get.preExecuteUserEvent(preExecuteAction)
          sendMsg2Server(preExecuteAction) //发送鼠标位置
          e.preventDefault()
        }
        else if (keyCode == KeyCode.E) {
          /**
            * 吃道具
            **/
          eKeyBoardState4AddBlood = false
          val preExecuteAction = TankGameEvent.UserPressKeyMedical(gameContainerOpt.get.myTankId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, serialNum = getActionSerialNum)
          gameContainerOpt.get.preExecuteUserEvent(preExecuteAction)
          sendMsg2Server(preExecuteAction)
          e.preventDefault()
        }
      }

    }

    canvas.getCanvas.onkeyup = { e: dom.KeyboardEvent =>
      if (gameContainerOpt.nonEmpty && gameState == GameState.play) {
        val keyCode = changeKeys(e.keyCode)
        if (watchKeys.contains(keyCode)) {
          myKeySet.remove(keyCode)
          //          println(s"key up: [${e.keyCode}]")
          val preExecuteAction = TankGameEvent.UserMoveState(gameContainerOpt.get.myTankId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, getMoveStateByKeySet(myKeySet.toSet), getActionSerialNum)
          gameContainerOpt.get.preExecuteUserEvent(preExecuteAction)
          sendMsg2Server(preExecuteAction)
          if (org.seekloud.tank.shared.model.Constants.fakeRender) {
            gameContainerOpt.get.addMyAction(preExecuteAction)
          }
          e.preventDefault()

        }
        //        if (gunAngleAdjust.contains(keyCode)) {
        //          myKeySet.remove(keyCode)
        //          println(s"key up: [${e.keyCode}]")
        //
        //          val Theta = if(keyCode == KeyCode.K){
        //             poKeyBoardMoveTheta
        //          }
        //          else {
        //            neKeyBoardMoveTheta
        //          }
        //          val preExecuteAction = TankGameEvent.UserKeyboardMove(gameContainerOpt.get.myTankId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset,Theta.toFloat , getActionSerialNum)
        //          gameContainerOpt.get.preExecuteUserEvent(preExecuteAction)
        //          sendMsg2Server(preExecuteAction)
        //          e.preventDefault()

        //        }
        else if (e.keyCode == KeyCode.Space) {
          spaceKeyUpState = true
          e.preventDefault()
        }
        else if (e.keyCode == KeyCode.E) {
          eKeyBoardState4AddBlood = true
          e.preventDefault()
        }
      }
    }
  }

  override protected def setKillCallback(tank: Tank) = {
    if (gameContainerOpt.nonEmpty&&tank.tankId ==gameContainerOpt.get.tankId) {
      if (tank.lives <= 1) setGameState(GameState.stop)
    }
  }

  override protected def wsMessageHandler(data: TankGameEvent.WsMsgServer): Unit = {
    data match {
      case e: TankGameEvent.WsSuccess =>
        webSocketClient.sendMsg(TankGameEvent.JoinRoom(e.roomId, None))

      case e: TankGameEvent.YourInfo =>
        println(s"new game the id is ${e.tankId}=====${e.name}")
        println(s"玩家信息${e}")
        timer = Shortcut.schedule(gameLoop, e.config.frameDuration / e.config.playRate)
        //        audioForBgm.play()
        /**
          * 更新游戏数据
          **/
        gameContainerOpt = Some(GameContainerClientImpl(drawFrame, canvas, e.config, e.userId, e.tankId, e.name, canvasSize, canvasUnit, setKillCallback, versionInfoOpt))
        gameContainerOpt.get.changeTankId(e.tankId)
      //        gameContainerOpt.foreach(e =>)

      case e: TankGameEvent.TankFollowEventSnap =>
        println(s"game TankFollowEventSnap =${e} systemFrame=${gameContainerOpt.get.systemFrame} tankId=${gameContainerOpt.get.myTankId} ")
        gameContainerOpt.foreach(_.receiveTankFollowEventSnap(e))

      case e: TankGameEvent.YouAreKilled =>

        /**
          * 死亡重玩
          **/
        gameContainerOpt.foreach(_.updateDamageInfo(e.killTankNum, e.name, e.damageStatistics))
        //        dom.window.cancelAnimationFrame(nextFrame)
        //        gameContainerOpt.foreach(_.drawGameStop())
        if ((Constants.supportLiveLimit && !e.hasLife) || (!Constants.supportLiveLimit)) {
          setGameState(GameState.stop)
          gameContainerOpt.foreach(_.changeTankId(e.tankId))
          //          audioForBgm.pause()
          //          audioForDead.play()
        }

      case e: TankGameEvent.SyncGameState =>
        gameContainerOpt.foreach(_.receiveGameContainerState(e.state))

      case e: TankGameEvent.SyncGameAllState =>
        gameContainerOpt.foreach(_.receiveGameContainerAllState(e.gState))
        dom.window.cancelAnimationFrame(nextFrame)
        nextFrame = dom.window.requestAnimationFrame(gameRender())
        setGameState(GameState.play)

      case e: TankGameEvent.UserActionEvent =>
        e match {
          case e:TankGameEvent.UM=>
            if(gameContainerOpt.nonEmpty){
              if(gameContainerOpt.get.myTankId!=e.tankId){
                gameContainerOpt.foreach(_.receiveUserEvent(e))
              }
            }
          case _=>
            gameContainerOpt.foreach(_.receiveUserEvent(e))
        }


      case e: TankGameEvent.GameEvent =>
        gameContainerOpt.foreach(_.receiveGameEvent(e))
        e match {
          case e: TankGameEvent.UserRelive =>
            if (e.userId == gameContainerOpt.get.myId) {
              dom.window.cancelAnimationFrame(nextFrame)
              nextFrame = dom.window.requestAnimationFrame(gameRender())
            }
          case _ =>
        }

      case e: TankGameEvent.PingPackage =>
        receivePingPackage(e)

      case TankGameEvent.RebuildWebSocket =>
        gameContainerOpt.foreach(_.drawReplayMsg("存在异地登录。。"))
        closeHolder

      case _ => println(s"unknow msg={sss}")
    }
  }
}
