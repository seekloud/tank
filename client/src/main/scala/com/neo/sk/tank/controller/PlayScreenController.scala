package com.neo.sk.tank.controller

import java.util.concurrent.atomic.AtomicInteger

import com.neo.sk.tank.App.{executor, materializer, scheduler, system, timeout, tokenActor}
import com.neo.sk.tank.actor.{PlayGameActor, TokenActor}
import com.neo.sk.tank.common.Context
import com.neo.sk.tank.game.{GameContainerClientImpl, NetworkInfo}
import com.neo.sk.tank.model.{GameServerInfo, PlayerInfo}
import com.neo.sk.tank.view.{GameHallScreen, PlayGameScreen}
import akka.actor.typed.scaladsl.adapter._
import com.neo.sk.tank.actor.PlayGameActor.{DispatchMsg, log}
import com.neo.sk.tank.game.GameContainerClientImpl
import com.neo.sk.tank.shared.model.Constants.GameState
import com.neo.sk.tank.shared.model.Point
import com.neo.sk.tank.shared.protocol.TankGameEvent
import com.neo.sk.utils.JavaFxUtil.{changeKeys, keyCode2Int}
import javafx.animation.{Animation, AnimationTimer, KeyFrame, Timeline}
import javafx.scene.input.KeyCode
import akka.actor.typed.scaladsl.AskPattern._
import org.slf4j.LoggerFactory
import com.neo.sk.tank.App
import javafx.util.Duration

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
                            roomInfo:Option[String]=None
                          ) extends NetworkInfo {
  private val log = LoggerFactory.getLogger(this.getClass)
  val playGameActor = system.spawn(PlayGameActor.create(this), "PlayGameActor")

  protected var firstCome = true
  protected var killerName:String = ""
  protected var killNum:Int = 0
  protected var damageNum:Int = 0
  protected var killerList = List.empty[String]


  private val actionSerialNumGenerator = new AtomicInteger(0)
  private var spaceKeyUpState = true
  private var lastMouseMoveTheta: Float = 0
  private val mouseMoveThreshold = math.Pi / 180
  private val poKeyBoardMoveTheta = 2 * math.Pi / 72 //炮筒顺时针转
  private val neKeyBoardMoveTheta = -2 * math.Pi / 72 //炮筒逆时针转
  private var poKeyBoardFrame = 0L
  private var eKeyBoardState4AddBlood = true
  private val preExecuteFrameOffset = com.neo.sk.tank.shared.model.Constants.PreExecuteFrameOffset

  private var recvYourInfo: Boolean = false
  private var recvSyncGameAllState: Option[TankGameEvent.SyncGameAllState] = None


  protected var gameContainerOpt: Option[GameContainerClientImpl] = None // 这里存储tank信息，包括tankId
  private var gameState = GameState.loadingPlay
  private var logicFrameTime = System.currentTimeMillis()
  private val animationTimer = new AnimationTimer() {
    override def handle(now: Long): Unit = {
      drawGame(System.currentTimeMillis() - logicFrameTime)
    }
  }
  private val timeline = new Timeline()
  private var countDownTimes=3
  timeline.setCycleCount(Animation.INDEFINITE)
  val keyFrame = new KeyFrame(Duration.millis(1000), { _ =>
    if(countDownTimes>0){
      playGameScreen.drawGameRestart(countDownTimes,killerName)
      countDownTimes-=1
    }else{
      timeline.stop()
      countDownTimes=3
      start
    }
  })
  timeline.getKeyFrames.add(keyFrame)

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

  def getActionSerialNum: Int = actionSerialNumGenerator.getAndIncrement()

  def start = {
    if(firstCome){
      firstCome=false
      println("start!!!!!!!")
      playGameActor ! PlayGameActor.ConnectGame(playerInfo,gameServerInfo,roomInfo)
      addUserActionListenEvent
      logicFrameTime = System.currentTimeMillis()
    }else{
      gameContainerOpt.foreach{r=>
        playGameActor ! DispatchMsg(TankGameEvent.RestartGame(Some(r.myTankId),r.myName,gameState))
        setGameState(GameState.loadingPlay)
        playGameActor ! PlayGameActor.StartGameLoop
      }
    }

  }

  private def drawGame(offsetTime: Long) = {
    gameContainerOpt.foreach(_.drawGame(offsetTime, getNetworkLatency))
  }

  def logicLoop() = {
    App.pushStack2AppThread{
      gameState match {
        case GameState.loadingPlay =>
          //        println(s"等待同步数据")
          playGameScreen.drawGameLoading()
        case GameState.play =>

          /** */
          gameContainerOpt.foreach(_.update())
          logicFrameTime = System.currentTimeMillis()
          ping()

        case GameState.stop =>
          closeHolder
          playGameScreen.drawGameStop(killerName)
          //todo 死亡结算
          playGameScreen.drawCombatGains(killNum, damageNum, killerList)
          killerList = List.empty[String]
          Thread.sleep(5000)
          val gameHallScreen = new GameHallScreen(context, playerInfo)
          context.switchScene(gameHallScreen.getScene,resize = true)
          val accessCodeInfo: Future[String] = tokenActor ? TokenActor.GetAccessCode
          accessCodeInfo.map{
            accessCode =>
              if(accessCode != "error"){
                val newPlayerInfo = PlayerInfo(playerInfo.playerId, playerInfo.nickName, accessCode)
                new HallScreenController(context, gameHallScreen, gameServerInfo, newPlayerInfo)
              }
          }

        case GameState.relive =>

          /**
            * 在生命值之内死亡重玩，倒计时进入
            **/
          //        dom.window.cancelAnimationFrame(nextFrame)
          //        Shortcut.cancelSchedule(timer)
          animationTimer.stop()
          playGameActor ! PlayGameActor.StopGameLoop
          timeline.play()

        case _ => log.info(s"state=${gameState} failed")
      }
    }
  }

  private def addUserActionListenEvent: Unit = {
    playGameScreen.canvas.requestFocus()
    /**
      * 增加鼠标移动操作
      **/
    playGameScreen.canvas.setOnMouseMoved{ e =>
      val point = Point(e.getX.toFloat, e.getY.toFloat) + Point(16,16)
      val theta = point.getTheta(playGameScreen.canvasBoundary * playGameScreen.canvasUnit / 2).toFloat
      if (gameContainerOpt.nonEmpty && gameState == GameState.play) {
        if(math.abs(theta - lastMouseMoveTheta) >= mouseMoveThreshold){
          lastMouseMoveTheta = theta
          val preExecuteAction = TankGameEvent.UserMouseMove(gameContainerOpt.get.myTankId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, theta, getActionSerialNum)
          gameContainerOpt.get.preExecuteUserEvent(preExecuteAction)
          playGameActor ! DispatchMsg(preExecuteAction) //发送鼠标位置
        }
      }
    }
    /**
      * 增加鼠标点击操作
      **/
    playGameScreen.canvas.setOnMouseClicked{ e=>
      if (gameContainerOpt.nonEmpty && gameState == GameState.play) {
        val preExecuteAction = TankGameEvent.UserMouseClick(gameContainerOpt.get.myTankId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, System.currentTimeMillis(), getActionSerialNum)
        gameContainerOpt.get.preExecuteUserEvent(preExecuteAction)
        playGameActor ! DispatchMsg(preExecuteAction)
      }
    }
    /**
      * 增加按下按键操作
      **/
    playGameScreen.canvas.setOnKeyPressed{ e =>
      if (gameContainerOpt.nonEmpty && gameState == GameState.play) {
        val keyCode = changeKeys(e.getCode)
        if (watchKeys.contains(keyCode) && !myKeySet.contains(keyCode)) {
          myKeySet.add(keyCode)
          println(s"key down: [${e.getCode.getName}]")
          val preExecuteAction = TankGameEvent.UserPressKeyDown(gameContainerOpt.get.myTankId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, keyCode2Int(keyCode), getActionSerialNum)
          gameContainerOpt.get.preExecuteUserEvent(preExecuteAction)
          playGameActor ! DispatchMsg(preExecuteAction)
          if (com.neo.sk.tank.shared.model.Constants.fakeRender) {
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
          val preExecuteAction = TankGameEvent.UserMouseClick(gameContainerOpt.get.myTankId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, System.currentTimeMillis(), getActionSerialNum)
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
      }
    }

    /**
      * 增加松开按键操作
      **/
    playGameScreen.canvas.setOnKeyReleased { e =>
      if (gameContainerOpt.nonEmpty && gameState == GameState.play) {
        val keyCode = changeKeys(e.getCode)
        if (watchKeys.contains(keyCode)) {
          myKeySet.remove(keyCode)
          println(s"key up: [${e.getCode}]")
          val preExecuteAction = TankGameEvent.UserPressKeyUp(gameContainerOpt.get.myTankId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, keyCode2Int(keyCode), getActionSerialNum)
          gameContainerOpt.get.preExecuteUserEvent(preExecuteAction)
          playGameActor ! DispatchMsg(preExecuteAction)
          if (com.neo.sk.tank.shared.model.Constants.fakeRender) {
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
    App.pushStack2AppThread{
      data match {
        case e: TankGameEvent.YourInfo =>
          /**
            * 更新游戏数据
            **/
          println("start------------")
          try {
            gameContainerOpt = Some(GameContainerClientImpl(playGameScreen.getCanvasContext,e.config,e.userId,e.tankId,e.name, playGameScreen.canvasBoundary, playGameScreen.canvasUnit,setGameState))
            gameContainerOpt.get.getTankId(e.tankId)
            recvYourInfo = true
            recvSyncGameAllState.foreach(t => wsMessageHandler(t))
          }catch {
            case e:Exception=>
              closeHolder
              println(e.getMessage)
              print("client is stop!!!")
          }


        case e: TankGameEvent.YouAreKilled =>

          /**
            * 死亡重玩
            **/
          println(s"you are killed")
          killNum = e.killTankNum
          damageNum = e.damageStatistics
          killerList = killerList :+ e.name
          killerName = e.name
          if(e.hasLife){
            setGameState(GameState.relive)
          } else setGameState(GameState.stop)

        case e: TankGameEvent.Ranks =>

          /**
            * 游戏排行榜
            **/
          gameContainerOpt.foreach{ t =>
            t.currentRank = e.currentRank
            t.historyRank = e.historyRank
            t.rankUpdated = true
          }


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
            case ee:TankGameEvent.GenerateBullet =>
              gameContainerOpt.foreach(_.receiveGameEvent(e))
            case _ => gameContainerOpt.foreach(_.receiveGameEvent(e))
          }

        case e: TankGameEvent.PingPackage =>
          receivePingPackage(e)


        case TankGameEvent.RebuildWebSocket =>
          playGameScreen.drawReplayMsg("存在异地登录。。")
          closeHolder

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
