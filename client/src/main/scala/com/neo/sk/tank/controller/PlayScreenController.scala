package com.neo.sk.tank.controller

import java.util.concurrent.atomic.AtomicInteger

import com.neo.sk.tank.App.system
import com.neo.sk.tank.actor.PlayGameActor
import com.neo.sk.tank.common.Context
import com.neo.sk.tank.game.{GameContainerClientImpl, NetworkInfo}
import com.neo.sk.tank.model.{GameServerInfo, PlayerInfo}
import com.neo.sk.tank.view.PlayGameScreen
import akka.actor.typed.scaladsl.adapter._
import com.neo.sk.tank.actor.PlayGameActor.{DispatchMsg, log}
import com.neo.sk.tank.shared.model.Constants.GameState
import com.neo.sk.tank.shared.model.Point
import com.neo.sk.tank.shared.protocol.TankGameEvent
import javafx.scene.input.KeyCode
import org.slf4j.LoggerFactory

import scala.collection.mutable
/**
  * Created by hongruying on 2018/10/23
  * 1.PlayScreenController 一实例化后启动PlayScreenController 连接gameServer的websocket
  * 然后使用AnimalTime来绘制屏幕，使用TimeLine来做gameLoop的更新
  * @author sky
  */
class PlayScreenController(
                            playerInfo: PlayerInfo,
                            gameServerInfo: GameServerInfo,
                            context: Context,
                            playGameScreen: PlayGameScreen
                          ) extends NetworkInfo {
  private val log = LoggerFactory.getLogger(this.getClass)
  private val playGameActor=system.spawn(PlayGameActor.create(this),"PlayGameActor")
  protected var firstCome = true
  private val actionSerialNumGenerator = new AtomicInteger(0)
  private var spaceKeyUpState = true
  private var lastMouseMoveTheta:Float = 0
  private val mouseMoveThreshold = math.Pi / 180
  private val poKeyBoardMoveTheta = 2* math.Pi / 72 //炮筒顺时针转
  private val neKeyBoardMoveTheta = -2* math.Pi / 72 //炮筒逆时针转
  private var poKeyBoardFrame = 0L
  private var eKeyBoardState4AddBlood = true
  private val preExecuteFrameOffset = com.neo.sk.tank.shared.model.Constants.PreExecuteFrameOffset
  protected var gameContainerOpt : Option[GameContainerClientImpl] = None // 这里存储tank信息，包括tankId
  private var gameState=GameState.loadingPlay

  private val watchKeys = Set(
    KeyCode.LEFT,
    KeyCode.DOWN,
    KeyCode.RIGHT,
    KeyCode.UP
  )

  private val watchBakKeys = Set(
    KeyCode.W,
    KeyCode.S,
    KeyCode.A,
    KeyCode.D
  )

  private val gunAngleAdjust = Set(
    KeyCode.K,
    KeyCode.L

  )

  private val myKeySet = mutable.HashSet[KeyCode]()

  private def changeKeys(k:KeyCode) = k match {
    case KeyCode.W => KeyCode.UP
    case KeyCode.S => KeyCode.DOWN
    case KeyCode.A => KeyCode.LEFT
    case KeyCode.D => KeyCode.RIGHT
    case origin => origin
  }

  def getActionSerialNum:Int = actionSerialNumGenerator.getAndIncrement()

  def start={

  }

  private def addUserActionListenEvent():Unit = {
    playGameScreen.canvas.setOnKeyPressed{ e =>
      if (gameContainerOpt.nonEmpty && gameState == GameState.play) {
        /**
          * 增加按键操作
          * */
        val keyCode = changeKeys(e.getCode)
        if (watchKeys.contains(keyCode) && !myKeySet.contains(keyCode)) {
          myKeySet.add(keyCode)
          println(s"key down: [${e.getCode.getName}]")
          val preExecuteAction = TankGameEvent.UserPressKeyDown(gameContainerOpt.get.myTankId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, keyCode, getActionSerialNum)
          gameContainerOpt.get.preExecuteUserEvent(preExecuteAction)
          playGameActor ! DispatchMsg(preExecuteAction)
          if(com.neo.sk.tank.shared.model.Constants.fakeRender){
            gameContainerOpt.get.addMyAction(preExecuteAction)
          }
        }
        if (gunAngleAdjust.contains(keyCode) && poKeyBoardFrame != gameContainerOpt.get.systemFrame) {
          myKeySet.remove(keyCode)
          println(s"key down: [${e.getCode.getName}]")
          poKeyBoardFrame = gameContainerOpt.get.systemFrame
          val Theta =
            if(keyCode == KeyCode.K) poKeyBoardMoveTheta
            else neKeyBoardMoveTheta
          val preExecuteAction = TankGameEvent.UserKeyboardMove(gameContainerOpt.get.myTankId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset,Theta.toFloat , getActionSerialNum)
          gameContainerOpt.get.preExecuteUserEvent(preExecuteAction)
          playGameActor ! DispatchMsg(preExecuteAction)
        }
        else if (keyCode == KeyCode.SPACE && spaceKeyUpState) {
          spaceKeyUpState = false
          val preExecuteAction = TankGameEvent.UserMouseClick(gameContainerOpt.get.myTankId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, System.currentTimeMillis(), getActionSerialNum)
          gameContainerOpt.get.preExecuteUserEvent(preExecuteAction)
          playGameActor ! DispatchMsg(preExecuteAction) //发送鼠标位置
        }
        else if(keyCode == KeyCode.E){
          /**
            * 吃道具
            * */
          eKeyBoardState4AddBlood = false
          val preExecuteAction = TankGameEvent.UserPressKeyMedical(gameContainerOpt.get.myTankId,gameContainerOpt.get.systemFrame + preExecuteFrameOffset,serialNum = getActionSerialNum)
          gameContainerOpt.get.preExecuteUserEvent(preExecuteAction)
          playGameActor ! DispatchMsg(preExecuteAction)
        }
      }
    }

    playGameScreen.canvas.setOnKeyReleased{ e =>
      if (gameContainerOpt.nonEmpty && gameState == GameState.play) {
        val keyCode = changeKeys(e.getCode)
        if (watchKeys.contains(keyCode)) {
          myKeySet.remove(keyCode)
          println(s"key up: [${e.getCode}]")
          val preExecuteAction = TankGameEvent.UserPressKeyUp(gameContainerOpt.get.myTankId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, keyCode, getActionSerialNum)
          gameContainerOpt.get.preExecuteUserEvent(preExecuteAction)
          playGameActor ! DispatchMsg(preExecuteAction)
          if(com.neo.sk.tank.shared.model.Constants.fakeRender) {
            gameContainerOpt.get.addMyAction(preExecuteAction)
          }

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
        else if (e.getCode == KeyCode.SPACE) {
          spaceKeyUpState = true
          e.preventDefault()
        }
        else if(e.keyCode == KeyCode.E){
          eKeyBoardState4AddBlood = true
          e.preventDefault()
        }
      }
    }
  }

  /**
    * 此处处理消息*/
  def wsMessageHandler(data:TankGameEvent.WsMsgServer)={
    data match {
      case e:TankGameEvent.YourInfo =>
      /**
        * 更新游戏数据
        * */
      case e:TankGameEvent.YouAreKilled =>
        /**
          * 死亡重玩
          * */
        log.info(s"you are killed")

      case e:TankGameEvent.Ranks =>
      /**
        * 游戏排行榜
        * */

      case e:TankGameEvent.SyncGameState =>


      case e:TankGameEvent.SyncGameAllState =>



      case e:TankGameEvent.UserActionEvent =>



      case e:TankGameEvent.GameEvent =>
        e match {
          case ee:TankGameEvent.GenerateBullet =>

          case _ =>
        }

      case e:TankGameEvent.PingPackage =>


      case TankGameEvent.RebuildWebSocket=>


      case _ =>
        log.info(s"unknow msg={sss}")
    }
  }


}
