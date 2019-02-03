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

package org.seekloud.tank.controller

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.adapter._
import javafx.animation.{Animation, AnimationTimer, KeyFrame, Timeline}
import javafx.scene.control.{Alert, ButtonBar, ButtonType}
import javafx.scene.input.KeyCode
import javafx.scene.media.{AudioClip, Media, MediaPlayer}
import javafx.util.Duration
import org.seekloud.tank.App
import org.seekloud.tank.App.{executor, scheduler, system, timeout, tokenActor}
import org.seekloud.tank.actor.PlayGameActor.DispatchMsg
import org.seekloud.tank.actor.{PlayGameActor, TokenActor}
import org.seekloud.tank.common.{Constants, Context}
import org.seekloud.tank.game.NetworkInfo
import org.seekloud.tank.model.{GameServerInfo, PlayerInfo, TokenAndAcessCode, UserInfo}
import org.seekloud.tank.shared.`object`.Tank
import org.seekloud.tank.shared.game.GameContainerClientImpl
import org.seekloud.tank.shared.model.Constants.GameState
import org.seekloud.tank.shared.model.Point
import org.seekloud.tank.shared.protocol.TankGameEvent
import org.seekloud.tank.view.{GameHallScreen, PlayGameScreen}
import org.seekloud.utils.JavaFxUtil.{changeKeys, keyCode2Int}
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.concurrent.Future


/**
  * Created by hongruying on 2018/10/23
  * 1.PlayScreenController 一实例化后启动PlayScreenController 连接gameServer的websocket
  * 然后使用AnimalTime来绘制屏幕，使用TimeLine来做gameLoop的更新
  *
  * @author sky
  */
class PlayScreenController(
                            playerInfo: PlayerInfo,
                            gameServerInfo: GameServerInfo,
                            context: Context,
                            playGameScreen: PlayGameScreen,
                            roomInfo:Option[String] = None,
                            roomPwd:Option[String] = None,
                            isCreated:Boolean
                          ) extends NetworkInfo {
  private val log = LoggerFactory.getLogger(this.getClass)
  val playGameActor = system.spawn(PlayGameActor.create(this), "PlayGameActor")

  protected var firstCome = true
//  protected var killerName:String = ""
//  protected var killNum:Int = 0
//  protected var damageNum:Int = 0
//  protected var killerList = List.empty[String]


  private val actionSerialNumGenerator = new AtomicInteger(0)
  private var spaceKeyUpState = true
  private var lastMouseMoveAngle: Byte = 0
  private val perMouseMoveFrame = 2
  private var lastMoveFrame = -1L
  private val poKeyBoardMoveTheta = 2 * math.Pi / 72 //炮筒顺时针转
  private val neKeyBoardMoveTheta = -2 * math.Pi / 72 //炮筒逆时针转
  private var poKeyBoardFrame = 0L
  private var eKeyBoardState4AddBlood = true
  private val preExecuteFrameOffset = org.seekloud.tank.shared.model.Constants.PreExecuteFrameOffset

  private var recvYourInfo: Boolean = false
  private var recvSyncGameAllState: Option[TankGameEvent.SyncGameAllState] = None

  private val gameMusic = new Media(getClass.getResource("/music/bgm.mp3").toString)
  private val gameMusicPlayer = new MediaPlayer(gameMusic)
  gameMusicPlayer.setCycleCount(MediaPlayer.INDEFINITE)
  private val bulletMusic = new AudioClip(getClass.getResource("/music/bullet.mp3").toString)
  private val deadMusic = new AudioClip(getClass.getResource("/music/fail.mp3").toString)
  private var needBgm = true

  protected var gameContainerOpt: Option[GameContainerClientImpl] = None // 这里存储tank信息，包括tankId
  private var gameState = GameState.loadingPlay
  private var logicFrameTime = System.currentTimeMillis()

  private var tickCount = 1//更新排行榜信息计时器
  private val rankCycle = 20

  /**阻塞时间*/
  private val timeline = new Timeline()
  timeline.setCycleCount(Animation.INDEFINITE)
  val keyFrame = new KeyFrame(Duration.millis(5000), { _ =>
    App.pushStack2AppThread{
//      killerList = List.empty[String]
      val gameHallScreen = new GameHallScreen(context, playerInfo)
      context.switchScene(gameHallScreen.getScene,resize = true)
      val accessCodeInfo: Future[TokenAndAcessCode] = tokenActor ? TokenActor.GetAccessCode
      accessCodeInfo.map{
        info =>
          if(info.token != ""){
            val newUserInfo = UserInfo(playerInfo.userInfo.userId,playerInfo.userInfo.nickname,info.token, info.expireTime)
            val newPlayerInfo = PlayerInfo(newUserInfo,playerInfo.playerId, playerInfo.nickName, info.accessCode)
            new HallScreenController(context, gameHallScreen, gameServerInfo, newPlayerInfo)
          }
      }
    }
    timeline.stop()
  })
  timeline.getKeyFrames.add(keyFrame)

  private val animationTimer = new AnimationTimer() {
    override def handle(now: Long): Unit = {
      drawGame(System.currentTimeMillis() - logicFrameTime)
    }
  }

  private val watchKeys = Set(
    KeyCode.LEFT,
    KeyCode.DOWN,
    KeyCode.RIGHT,
    KeyCode.UP
  )

  private val gunAngleAdjust = Set(
    KeyCode.K,
    KeyCode.L
  )

  private val myKeySet = mutable.HashSet[KeyCode]()

  protected def setGameState(s:Int):Unit = {
    gameState = s
  }

  protected def setKillCallback(tank: Tank) = {
    if (gameContainerOpt.nonEmpty&&tank.tankId ==gameContainerOpt.get.tankId) {
      if (tank.lives <= 1) setGameState(GameState.stop)
    }
  }

  def getActionSerialNum: Byte = actionSerialNumGenerator.getAndIncrement().toByte

  def start = {
    if(firstCome){
      firstCome=false
      println("start!!!!!!!")
      playGameActor ! PlayGameActor.ConnectGame(playerInfo,gameServerInfo,roomInfo)
      addUserActionListenEvent
      logicFrameTime = System.currentTimeMillis()
    }else{
      gameContainerOpt.foreach{r=>
        playGameActor ! DispatchMsg(TankGameEvent.RestartGame(Some(r.myTankId),r.myName))
        setGameState(GameState.loadingPlay)
        playGameActor ! PlayGameActor.StartGameLoop
      }
    }
  }

  private def drawGame(offsetTime: Long) = {
//    gameContainerOpt.foreach(_.drawGame(offsetTime, getNetworkLatency,Constants.supportLiveLimit))
    gameContainerOpt.foreach(_.drawGame(offsetTime, getNetworkLatency, Nil,Constants.supportLiveLimit))
  }

  def logicLoop() = {
    App.pushStack2AppThread{
      val (bounDary, unit) = playGameScreen.checkScreenSize()
      if(unit != 0){
        gameContainerOpt.foreach{r =>
          r.updateClientSize(bounDary, unit)
        }
      }
      gameState match {
        case GameState.loadingPlay =>
          //        println(s"等待同步数据")
          gameContainerOpt.foreach(_.drawGameLoading())
        case GameState.play =>
          /** */
          if(tickCount % rankCycle == 1){
            gameContainerOpt.foreach(_.updateRanks())
            gameContainerOpt.foreach(t => t.rankUpdated = true)
          }
          gameContainerOpt.foreach(_.update())
          logicFrameTime = System.currentTimeMillis()
          ping()
          tickCount += 1

        case GameState.stop =>
          closeHolder
          gameContainerOpt.foreach(_.drawCombatGains())
          timeline.play()


        case _ => log.info(s"state=${gameState} failed")
      }
    }
  }

  private def addUserActionListenEvent: Unit = {
    playGameScreen.canvas.getCanvas.requestFocus()
    /**
      * 增加鼠标移动操作
      **/
    playGameScreen.canvas.getCanvas.setOnMouseMoved{ e =>
      val point = Point(e.getX.toFloat, e.getY.toFloat) + Point(24,24)
      val theta = point.getTheta(playGameScreen.canvasBoundary * playGameScreen.canvasUnit / 2).toFloat
      val angle = point.getAngle(playGameScreen.canvasBoundary * playGameScreen.canvasUnit / 2)
      //remind tank自身流畅显示
      //fixme 此处序列号是否存疑
      val preMMFAction = TankGameEvent.UserMouseMove(gameContainerOpt.get.myTankId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, theta, getActionSerialNum)
      gameContainerOpt.get.preExecuteUserEvent(preMMFAction)
      if (gameContainerOpt.nonEmpty && gameState == GameState.play && lastMoveFrame < gameContainerOpt.get.systemFrame) {
        if (lastMouseMoveAngle!=angle) {
          lastMouseMoveAngle = angle
          lastMoveFrame = gameContainerOpt.get.systemFrame
          val preMMBAction = TankGameEvent.UM(gameContainerOpt.get.myTankId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, angle, getActionSerialNum)
          playGameActor ! DispatchMsg(preMMBAction) //发送鼠标位置
          println(preMMBAction)
        }
      }
    }
    /**
      * 增加鼠标点击操作
      **/
    playGameScreen.canvas.getCanvas.setOnMouseClicked{ e=>
      if (gameContainerOpt.nonEmpty && gameState == GameState.play) {
        val point = Point(e.getX.toFloat, e.getY.toFloat) + Point(24,24)
        val theta = point.getTheta(playGameScreen.canvasBoundary * playGameScreen.canvasUnit / 2).toFloat
        bulletMusic.play()
        val preExecuteAction = TankGameEvent.UC(gameContainerOpt.get.myTankId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, theta, getActionSerialNum)
        gameContainerOpt.get.preExecuteUserEvent(preExecuteAction)
        playGameActor ! DispatchMsg(preExecuteAction)
      }
    }
    /**
      * 增加按下按键操作
      **/
    playGameScreen.canvas.getCanvas.setOnKeyPressed{ e =>
      if (gameContainerOpt.nonEmpty && gameState == GameState.play) {
        val keyCode = changeKeys(e.getCode)
        if (watchKeys.contains(keyCode) && !myKeySet.contains(keyCode)) {
          myKeySet.add(keyCode)
          println(s"key down: [${e.getCode.getName}]")
          val preExecuteAction = TankGameEvent.UserPressKeyDown(gameContainerOpt.get.myTankId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, keyCode2Int(keyCode).toByte, getActionSerialNum)
          gameContainerOpt.get.preExecuteUserEvent(preExecuteAction)
          playGameActor ! DispatchMsg(preExecuteAction)
          if (org.seekloud.tank.shared.model.Constants.fakeRender) {
            gameContainerOpt.get.addMyAction(preExecuteAction)
          }
        }
        if (gunAngleAdjust.contains(keyCode) && poKeyBoardFrame != gameContainerOpt.get.systemFrame) {
          myKeySet.remove(keyCode)
          println(s"key down: [${e.getCode.getName}]")
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
        else if(keyCode == KeyCode.M){
          if(needBgm){
            gameMusicPlayer.pause()
            needBgm = false
          }else{
            gameMusicPlayer.play()
            needBgm = true
          }
        }
      }
    }

    /**
      * 增加松开按键操作
      **/
    playGameScreen.canvas.getCanvas.setOnKeyReleased { e =>
      if (gameContainerOpt.nonEmpty && gameState == GameState.play) {
        val keyCode = changeKeys(e.getCode)
        if (watchKeys.contains(keyCode)) {
          myKeySet.remove(keyCode)
          println(s"key up: [${e.getCode}]")
          val preExecuteAction = TankGameEvent.UserPressKeyUp(gameContainerOpt.get.myTankId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, keyCode2Int(keyCode).toByte, getActionSerialNum)
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

  /**
    * 此处处理消息*/
  def wsMessageHandler(data: TankGameEvent.WsMsgServer):Unit = {
//    println(data.getClass)
    App.pushStack2AppThread{
      data match {

        case e:TankGameEvent.WsSuccess =>
          if(isCreated) playGameActor ! DispatchMsg(TankGameEvent.CreateRoom(e.roomId,roomPwd))
          else playGameActor ! DispatchMsg(TankGameEvent.StartGame(e.roomId,roomPwd))

        case e: TankGameEvent.YourInfo =>
          /**
            * 更新游戏数据
            **/
          println("start------------")
          gameMusicPlayer.play()
          try {
            gameContainerOpt = Some(GameContainerClientImpl(playGameScreen.drawFrame,playGameScreen.getCanvasContext,e.config,e.userId,e.tankId,e.name, playGameScreen.canvasBoundary, playGameScreen.canvasUnit,setKillCallback))
            gameContainerOpt.get.changeTankId(e.tankId)
            recvYourInfo = true
            recvSyncGameAllState.foreach(t => wsMessageHandler(t))
          }catch {
            case e:Exception=>
              closeHolder
              println(e.getMessage)
              println(e.printStackTrace())
              println("client is stop!!!")
          }

        case e:TankGameEvent.TankFollowEventSnap =>
          gameContainerOpt.foreach(_.receiveTankFollowEventSnap(e))

        case e: TankGameEvent.YouAreKilled =>

          /**
            * 死亡重玩
            **/
          println(s"you are killed")
          gameContainerOpt.foreach(_.updateDamageInfo(e.killTankNum,e.name,e.damageStatistics))
//          killNum = e.killTankNum
//          damageNum = e.damageStatistics
//          killerList = killerList :+ e.name
//          killerName = e.name
//          animationTimer.stop()
          gameContainerOpt.foreach(_.drawGameStop())
          if(!e.hasLife || !Constants.supportLiveLimit){
            setGameState(GameState.stop)
            gameMusicPlayer.pause()
            deadMusic.play()
          }else animationTimer.stop()

//        case e:TankGameEvent.TankReliveInfo =>
//          animationTimer.start()

        case e: TankGameEvent.SyncGameState =>
          gameContainerOpt.foreach(_.receiveGameContainerState(e.state))

        case e: TankGameEvent.SyncGameAllState =>
          if(!recvYourInfo){
            println("----发生预料事件")
            recvSyncGameAllState = Some(e)
          } else {
            gameContainerOpt.foreach(_.receiveGameContainerAllState(e.gState))
            logicFrameTime = System.currentTimeMillis()
            animationTimer.start()
            playGameActor ! PlayGameActor.StartGameLoop
            setGameState(GameState.play)
          }

        case e: TankGameEvent.UserActionEvent =>
          gameContainerOpt.foreach(_.receiveUserEvent(e))



        case e: TankGameEvent.GameEvent =>
          e match {
            case e:TankGameEvent.UserRelive =>
              gameContainerOpt.foreach(_.receiveGameEvent(e))
              if(e.userId == gameContainerOpt.get.myId){
                animationTimer.start()
//                dom.window.cancelAnimationFrame(nextFrame)
//                nextFrame = dom.window.requestAnimationFrame(gameRender())
              }
            case ee:TankGameEvent.GenerateBullet =>
              gameContainerOpt.foreach(_.receiveGameEvent(e))
            case _ => gameContainerOpt.foreach(_.receiveGameEvent(e))
          }

        case e: TankGameEvent.PingPackage =>
          receivePingPackage(e)


        case TankGameEvent.RebuildWebSocket =>
          gameContainerOpt.foreach(_.drawReplayMsg("存在异地登录。。"))
          closeHolder

        case _:TankGameEvent.DecodeError=>
          log.info("hahahha")

        case e:TankGameEvent.WsMsgErrorRsp =>
          if(e.errCode == 10001){
            val warn = new Alert(Alert.AlertType.WARNING,"您输入的房间密码错误",new ButtonType("确定",ButtonBar.ButtonData.YES))
            warn.setTitle("警示")
            val buttonType = warn.showAndWait()
            if(buttonType.get().getButtonData.equals(ButtonBar.ButtonData.YES)) warn.close()
            val gameHallScreen = new GameHallScreen(context, playerInfo)
            context.switchScene(gameHallScreen.getScene,resize = true)
            new HallScreenController(context, gameHallScreen, gameServerInfo, playerInfo)
            closeHolder
          }
        case _ =>
          log.info(s"unknow msg={sss}")
      }
    }
  }

  private def closeHolder={
    animationTimer.stop()
    //remind 此处关闭WebSocket
    playGameActor ! PlayGameActor.StopGameActor
  }



}
