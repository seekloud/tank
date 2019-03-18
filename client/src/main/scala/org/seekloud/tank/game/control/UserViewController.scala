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

package org.seekloud.tank.game.control


import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.adapter._
import javafx.animation.{Animation, KeyFrame, Timeline}
import javafx.scene.control.{Alert, ButtonBar, ButtonType}
import javafx.scene.input.KeyCode
import javafx.scene.media.{AudioClip, Media, MediaPlayer}
import javafx.util.Duration
import org.seekloud.tank.ClientApp
import org.seekloud.tank.ClientApp.{executor, scheduler, system, timeout, tokenActor}
import org.seekloud.tank.common.AppSettings.{viewHeight, viewWidth}
import org.seekloud.tank.core.PlayGameActor.DispatchMsg
import org.seekloud.tank.core.{BotViewActor, PlayGameActor, TokenActor}
import org.seekloud.tank.common.{AppSettings, Context}
import org.seekloud.tank.controller.HallScreenController
import org.seekloud.tank.model.{GameServerInfo, PlayerInfo, TokenAndAcessCode, UserInfo}
import org.seekloud.tank.shared.model.Constants.GameState
import org.seekloud.tank.shared.model.Point
import org.seekloud.tank.shared.protocol.TankGameEvent
import org.seekloud.tank.view.{GameHallScreen, PlayGameScreen}
import org.seekloud.utils.JavaFxUtil.{changeKeys, getCanvasUnit, getMoveStateByKeySet}
import org.seekloud.utils.canvas.MiddleCanvasInFx

import scala.concurrent.Future


/**
  * 用户游玩控制
  *
  * @author sky
  */
class UserViewController(
                          playerInfo: PlayerInfo,
                          gameServerInfo: GameServerInfo,
                          context: Context,
                          playGameScreen: PlayGameScreen,
                          roomInfo: Option[String] = None,
                          roomPwd: Option[String] = None,
                          isCreated: Boolean
                        ) extends GameController( /*context.getStageWidth.toFloat, context.getStageHeight.toFloat, false*/viewWidth*4, viewHeight*4, true) {
  private var spaceKeyUpState = true
  private var lastMouseMoveAngle: Byte = 0
  private val perMouseMoveFrame = 2
  private var lastMoveFrame = -1L
  private val poKeyBoardMoveTheta = 2 * math.Pi / 72 //炮筒顺时针转
  private val neKeyBoardMoveTheta = -2 * math.Pi / 72 //炮筒逆时针转
  private var poKeyBoardFrame = 0L
  private var eKeyBoardState4AddBlood = true

  private val gameMusic = new Media(getClass.getResource("/music/bgm.mp3").toString)
  private val gameMusicPlayer = new MediaPlayer(gameMusic)
  gameMusicPlayer.setCycleCount(MediaPlayer.INDEFINITE)
  private val bulletMusic = new AudioClip(getClass.getResource("/music/bullet.mp3").toString)
  private val deadMusic = new AudioClip(getClass.getResource("/music/fail.mp3").toString)
  private var needBgm = true

  /** 阻塞时间 */
  private val timeline = new Timeline()
  timeline.setCycleCount(Animation.INDEFINITE)
  val keyFrame = new KeyFrame(Duration.millis(5000), { _ =>
    ClientApp.pushStack2AppThread {
      //      killerList = List.empty[String]
      val gameHallScreen = new GameHallScreen(context, playerInfo)
      context.switchScene(gameHallScreen.getScene, resize = true)
      val accessCodeInfo: Future[TokenAndAcessCode] = tokenActor ? TokenActor.GetAccessCode
      accessCodeInfo.map {
        info =>
          if (info.token != "") {
            val newUserInfo = UserInfo(playerInfo.userInfo.userId, playerInfo.userInfo.nickname, info.token, info.expireTime)
            val newPlayerInfo = PlayerInfo(newUserInfo, playerInfo.playerId, playerInfo.nickName, info.accessCode)
            new HallScreenController(context, gameHallScreen, gameServerInfo, newPlayerInfo)
          }
      }
    }
    timeline.stop()
  })
  timeline.getKeyFrames.add(keyFrame)

  def startGame = {
    if (firstCome) {
      firstCome = false
      println("start!!!!!!!")
      playGameActor ! PlayGameActor.ConnectGame(playerInfo, gameServerInfo, roomInfo)
      logicFrameTime = System.currentTimeMillis()
    } else {
      gameContainerOpt.foreach { r =>
        playGameActor ! DispatchMsg(TankGameEvent.RestartGame(Some(r.myTankId), r.myName))
        setGameState(GameState.loadingPlay)
        playGameActor ! PlayGameActor.StartGameLoop(r.config.frameDuration)
      }
    }
  }

  private def getScreenSize() = {
    val newCanvasWidth = context.getStageWidth.toFloat
    val newCanvasHeight = if (context.isFullScreen) context.getStageHeight.toFloat else context.getStageHeight.toFloat - 20
    if (canvasWidth != newCanvasWidth || canvasHeight != newCanvasHeight) {
      println("the screen size has changed")
      canvasWidth = newCanvasWidth
      canvasHeight = newCanvasHeight
      canvasUnit = getCanvasUnit(newCanvasWidth)
      canvasBoundary = Point(canvasWidth, canvasHeight)
      canvas.setWidth(newCanvasWidth)
      canvas.setHeight(newCanvasHeight)
      (canvasBoundary / canvasUnit, canvasUnit)
    } else (Point(0, 0), 0)
  }

  override protected def checkScreenSize: Unit = {
    //fixme 测试阶段
    /*val (boundary, unit) = getScreenSize()
    if (unit != 0) {
      gameContainerOpt.foreach { r =>
        r.updateClientSize(boundary, unit)
      }
    }*/
  }

  override protected def gameStopCallBack: Unit = timeline.play()

  override protected def canvas2Byte4Bot: Unit = {

  }

  private def addUserActionListenEvent: Unit = {
    canvas.getCanvas.requestFocus()

    /**
      * 增加鼠标移动操作
      **/

    canvas.getCanvas.setOnMouseMoved { e =>
      if (gameContainerOpt.nonEmpty) {
        val point = Point(e.getX.toFloat, e.getY.toFloat) + Point(24, 24)
        val theta = point.getTheta(canvasBoundary  / 2).toFloat
        val angle = point.getAngle(canvasBoundary  / 2)
        val preMMFAction = TankGameEvent.UserMouseMove(gameContainerOpt.get.myTankId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, theta, getActionSerialNum)
        gameContainerOpt.get.preExecuteUserEvent(preMMFAction)
        if (gameContainerOpt.nonEmpty && gameState == GameState.play && lastMoveFrame < gameContainerOpt.get.systemFrame) {
          if (lastMouseMoveAngle != angle) {
            lastMouseMoveAngle = angle
            lastMoveFrame = gameContainerOpt.get.systemFrame
            val preMMBAction = TankGameEvent.UM(gameContainerOpt.get.myTankId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, angle, getActionSerialNum)
            playGameActor ! DispatchMsg(preMMBAction) //发送鼠标位置
            println(preMMBAction)
          }
        }
      }
    }

    /**
      * 增加鼠标点击操作
      **/
    canvas.getCanvas.setOnMouseClicked { e =>
      if (gameContainerOpt.nonEmpty && gameState == GameState.play) {
        val point = Point(e.getX.toFloat, e.getY.toFloat) + Point(24, 24)
        val theta = point.getTheta(canvasBoundary  / 2).toFloat
        bulletMusic.play()
        val preExecuteAction = TankGameEvent.UC(gameContainerOpt.get.myTankId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, theta, getActionSerialNum)
        gameContainerOpt.get.preExecuteUserEvent(preExecuteAction)
        playGameActor ! DispatchMsg(preExecuteAction)
      }
    }

    /**
      * 增加按下按键操作
      **/
    canvas.getCanvas.setOnKeyPressed { e =>
      if (gameContainerOpt.nonEmpty && gameState == GameState.play) {
        val keyCode = changeKeys(e.getCode)
        if (watchKeys.contains(keyCode) && !myKeySet.contains(keyCode)) {
          myKeySet.add(keyCode)
          val preExecuteAction = TankGameEvent.UserMoveState(gameContainerOpt.get.myTankId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, getMoveStateByKeySet(myKeySet.toSet), getActionSerialNum)
          gameContainerOpt.get.preExecuteUserEvent(preExecuteAction)
          playGameActor ! DispatchMsg(preExecuteAction)
          if (org.seekloud.tank.shared.model.Constants.fakeRender) {
            gameContainerOpt.get.addMyAction(preExecuteAction)
          }
        }
        if (gunAngleAdjust.contains(keyCode) && poKeyBoardFrame != gameContainerOpt.get.systemFrame) {
          myKeySet.remove(keyCode)
          poKeyBoardFrame = gameContainerOpt.get.systemFrame
          val Theta =
            if (keyCode == KeyCode.K) poKeyBoardMoveTheta
            else neKeyBoardMoveTheta
          val preExecuteAction = TankGameEvent.UserKeyboardMove(gameContainerOpt.get.myTankId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, Theta.toFloat, getActionSerialNum)
          gameContainerOpt.get.preExecuteUserEvent(preExecuteAction)
          playGameActor ! DispatchMsg(preExecuteAction)
        }
        else if (keyCode == KeyCode.SPACE && spaceKeyUpState) {
          spaceKeyUpState = false
          val preExecuteAction = TankGameEvent.UC(gameContainerOpt.get.myTankId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, gameContainerOpt.get.tankMap(gameContainerOpt.get.myTankId).getGunDirection(), getActionSerialNum)
          gameContainerOpt.get.preExecuteUserEvent(preExecuteAction)
          playGameActor ! DispatchMsg(preExecuteAction) //发送鼠标位置
        }
        else if (keyCode == KeyCode.E) {
          /**
            * 吃道具
            **/
          eKeyBoardState4AddBlood = false
          val preExecuteAction = TankGameEvent.UserPressKeyMedical(gameContainerOpt.get.myTankId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, serialNum = getActionSerialNum)
          gameContainerOpt.get.preExecuteUserEvent(preExecuteAction)
          playGameActor ! DispatchMsg(preExecuteAction)
        }
        else if (keyCode == KeyCode.M) {
          if (needBgm) {
            gameMusicPlayer.pause()
            needBgm = false
          } else {
            gameMusicPlayer.play()
            needBgm = true
          }
        }
      }
    }

    /**
      * 增加松开按键操作
      **/
    canvas.getCanvas.setOnKeyReleased { e =>
      if (gameContainerOpt.nonEmpty && gameState == GameState.play) {
        val keyCode = changeKeys(e.getCode)
        if (watchKeys.contains(keyCode)) {
          myKeySet.remove(keyCode)
          val preExecuteAction = TankGameEvent.UserMoveState(gameContainerOpt.get.myTankId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, getMoveStateByKeySet(myKeySet.toSet), getActionSerialNum)
          gameContainerOpt.get.preExecuteUserEvent(preExecuteAction)
          playGameActor ! DispatchMsg(preExecuteAction)
          if (org.seekloud.tank.shared.model.Constants.fakeRender) {
            gameContainerOpt.get.addMyAction(preExecuteAction)
          }
        } else if (e.getCode == KeyCode.SPACE) {
          spaceKeyUpState = true
        }
        else if (e.getCode == KeyCode.E) {
          eKeyBoardState4AddBlood = true
        }
      }
    }
  }

  override protected def handleWsSuccess(e: TankGameEvent.WsSuccess): Unit = {
    if (isCreated) playGameActor ! DispatchMsg(TankGameEvent.CreateRoom(e.roomId, roomPwd))
    else playGameActor ! DispatchMsg(TankGameEvent.JoinRoom(e.roomId, roomPwd))
  }

  override protected def handleWsMsgErrorRsp(e: TankGameEvent.WsMsgErrorRsp): Unit = {
    if (e.errCode == 10001) {
      val warn = new Alert(Alert.AlertType.WARNING, "您输入的房间密码错误", new ButtonType("确定", ButtonBar.ButtonData.YES))
      warn.setTitle("警示")
      val buttonType = warn.showAndWait()
      if (buttonType.get().getButtonData.equals(ButtonBar.ButtonData.YES)) warn.close()
      val gameHallScreen = new GameHallScreen(context, playerInfo)
      context.switchScene(gameHallScreen.getScene, resize = true)
      new HallScreenController(context, gameHallScreen, gameServerInfo, playerInfo)
      closeHolder
    }
  }

  override protected def initGameContainerCallBack: Unit = {
    gameContainerOpt.foreach { r =>
      ClientApp.pushStack2AppThread {
//        playGameScreen.group.getChildren.add(canvas.getCanvas)
//        addUserActionListenEvent
        //fixme 测试阶段
        canvas.getCanvas.setLayoutX(0)
        canvas.getCanvas.setLayoutY(0)
        r.locationCanvas.asInstanceOf[MiddleCanvasInFx].getCanvas.setLayoutX(viewWidth*4+5)
        r.locationCanvas.asInstanceOf[MiddleCanvasInFx].getCanvas.setLayoutY(0)
        r.immutableCanvas.asInstanceOf[MiddleCanvasInFx].getCanvas.setLayoutX(viewWidth*5+10)
        r.immutableCanvas.asInstanceOf[MiddleCanvasInFx].getCanvas.setLayoutY(0)
        r.mutableCanvas.asInstanceOf[MiddleCanvasInFx].getCanvas.setLayoutX(viewWidth*4+5)
        r.mutableCanvas.asInstanceOf[MiddleCanvasInFx].getCanvas.setLayoutY(viewHeight+5)
        r.bodiesCanvas.asInstanceOf[MiddleCanvasInFx].getCanvas.setLayoutX(viewWidth*5+10)
        r.bodiesCanvas.asInstanceOf[MiddleCanvasInFx].getCanvas.setLayoutY(viewHeight+5)
        r.ownerShipCanvas.asInstanceOf[MiddleCanvasInFx].getCanvas.setLayoutX(viewWidth*4+5)
        r.ownerShipCanvas.asInstanceOf[MiddleCanvasInFx].getCanvas.setLayoutY(viewHeight*2+10)
        r.selfCanvas.asInstanceOf[MiddleCanvasInFx].getCanvas.setLayoutX(viewWidth*5+10)
        r.selfCanvas.asInstanceOf[MiddleCanvasInFx].getCanvas.setLayoutY(viewHeight*2+10)
        r.statusCanvas.asInstanceOf[MiddleCanvasInFx].getCanvas.setLayoutX(viewWidth*5+10)
        r.statusCanvas.asInstanceOf[MiddleCanvasInFx].getCanvas.setLayoutY(viewHeight*3+15)
        playGameScreen.group.getChildren.add(canvas.getCanvas)
        playGameScreen.group.getChildren.add(r.locationCanvas.asInstanceOf[MiddleCanvasInFx].getCanvas)
        playGameScreen.group.getChildren.add(r.immutableCanvas.asInstanceOf[MiddleCanvasInFx].getCanvas)
        playGameScreen.group.getChildren.add(r.mutableCanvas.asInstanceOf[MiddleCanvasInFx].getCanvas)
        playGameScreen.group.getChildren.add(r.bodiesCanvas.asInstanceOf[MiddleCanvasInFx].getCanvas)
        playGameScreen.group.getChildren.add(r.ownerShipCanvas.asInstanceOf[MiddleCanvasInFx].getCanvas)
        playGameScreen.group.getChildren.add(r.selfCanvas.asInstanceOf[MiddleCanvasInFx].getCanvas)
        playGameScreen.group.getChildren.add(r.statusCanvas.asInstanceOf[MiddleCanvasInFx].getCanvas)
        addUserActionListenEvent
      }
    }
  }

}
