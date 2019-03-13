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
import org.seekloud.tank.shared.game.GameContainerClientImpl
import org.seekloud.tank.front.utils.Shortcut
import org.seekloud.tank.shared.`object`.Tank
import org.seekloud.tank.shared.game.TankClientImpl
import org.seekloud.tank.shared.model.Constants.{GameState, ObstacleType}
import org.seekloud.tank.shared.model.Point
import org.seekloud.tank.shared.protocol.TankGameEvent
import org.scalajs.dom
import org.scalajs.dom.ext.KeyCode
import org.scalajs.dom.raw.HTMLElement
import org.seekloud.tank.front.common.{Constants, Routes}
import org.seekloud.tank.front.components.StartGameModal
import org.seekloud.tank.front.utils.{JsFunc, Shortcut}
import sun.text.resources.sr.FormatData_sr_Latn

import scala.collection.mutable
import scala.util.Random
import scala.xml.Elem

class GameTestHolderImpl(name:String, playerInfoOpt: Option[PlayerInfo] = None) extends GameHolder(name){
  private[this] val actionSerialNumGenerator = new AtomicInteger(0)
  private var spaceKeyUpState = true
  private var enterKeyUpstate = true
  private var lastMouseMoveTheta:Float = 0
  private var currentMouseMOveTheta:Float = 0
  private val mouseMoveThreshold = math.Pi / 60
  private val poKeyBoardMoveTheta = 2* math.Pi / 72 //炮筒顺时针转
  private val neKeyBoardMoveTheta = -2* math.Pi / 72 //炮筒逆时针转
  private var poKeyBoardFrame = 0L
  private var eKeyBoardState4AddBlood = true
  private val preExecuteFrameOffset = org.seekloud.tank.shared.model.Constants.PreExecuteFrameOffset
  private val startGameModal = new StartGameModal(gameStateVar,start, playerInfoOpt)
  private var timerForClick = 0
  private var thisTankId = 0
  private val random = new Random(System.currentTimeMillis())

  private var thisTank = gameContainerOpt.getOrElse(None)

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

  private def changeKeys(k:Int):Int = k match {
    case KeyCode.W => KeyCode.Up
    case KeyCode.S => KeyCode.Down
    case KeyCode.A => KeyCode.Left
    case KeyCode.D => KeyCode.Right
    case origin => origin
  }

  def getActionSerialNum:Byte = actionSerialNumGenerator.getAndIncrement().toByte

  def getStartGameModal():Elem = {
    startGameModal.render
  }

  private def start(name:String,roomIdOpt:Option[Long]):Unit = {
    canvas.getCanvas.focus()
    dom.window.cancelAnimationFrame(nextFrame)
    Shortcut.cancelSchedule(timer)
    Shortcut.scheduleOnce(() => userAction,1000)
    if(firstCome){
      firstCome = false
      setGameState(GameState.loadingPlay)
      webSocketClient.setup(Routes.getJoinGameWebSocketUri(name, playerInfoOpt,roomIdOpt))
      gameLoop()
    }else if(webSocketClient.getWsState){
      gameContainerOpt match {
        case Some(gameContainer) =>
          gameContainerOpt.foreach(_.changeTankId(gameContainer.myTankId))
          if(Constants.supportLiveLimit){
            webSocketClient.sendMsg(TankGameEvent.RestartGame(Some(gameContainer.myTankId),name))
          }else{
            webSocketClient.sendMsg(TankGameEvent.RestartGame(None,name))
          }
        case None =>
          webSocketClient.sendMsg(TankGameEvent.RestartGame(None,name))
      }
      setGameState(GameState.loadingPlay)
      gameLoop()
    }else{
      JsFunc.alert("网络连接失败，请重新刷新")
    }
  }

  private def userAction:Unit = {
    Shortcut.cancelSchedule(timerForClick)
    println("sdsddsds")
    if(gameContainerOpt.nonEmpty && gameState == GameState.play){

      val r = random.nextInt(3)
      println("sss,=",r)
      if(r % 3 == 1){
        val theta = random.nextFloat() * math.Pi * 2
        userClick(theta.toFloat)
      } else{
        userMove

      }
      Shortcut.scheduleOnce(() => userAction, 300)
    }
    else if(gameState == GameState.stop){
      Shortcut.scheduleOnce(() => start(name,None), 3000)
    }
  }

  private def findTarget = {

    val gameContainer = gameContainerOpt.get

    val tankListOpt = gameContainer.findAllTank(thisTankId)
    val tankList = tankListOpt.getOrElse(List())

    if(tankList.nonEmpty){
      val thisTank = tankList.filter(_.tankId == thisTankId).head

      val obstacleList = gameContainer.findOtherObstacle(thisTank)
      val airDropList = obstacleList.filter(r => r.obstacleType == ObstacleType.airDropBox)
      val brickList = obstacleList.filter(r => r.obstacleType == ObstacleType.brick)

      val offset = canvasBoundary / 2 - thisTank.getTankState().position

      if(tankList.exists(r => r.tankId != thisTankId && jugeTheDistance(r.getTankState().position + offset))){
        val attackTank = tankList.filter(_.tankId != thisTankId).find(r => jugeTheDistance(r.getTankState().position + offset)).get
        val pos = (attackTank.getTankState().position + offset) * canvasUnit
        currentMouseMOveTheta = pos.getTheta(canvasBoundary * canvasUnit / 2).toFloat
        true
      }
      else if(airDropList.exists(r => jugeTheDistance(r.getObstacleState().p + offset))){
        val attackAir = airDropList.find(r => jugeTheDistance(r.getObstacleState().p + offset)).get
        val pos = (attackAir.getObstacleState().p + offset) * canvasUnit
        currentMouseMOveTheta = pos.getTheta(canvasBoundary * canvasUnit / 2).toFloat
        true
      }
      else if(brickList.exists(r => jugeTheDistance(r.getObstacleState().p + offset))){
        val attackBrick = brickList.find(r => jugeTheDistance(r.getObstacleState().p + offset)).get
        val pos = (attackBrick.getObstacleState().p + offset) * canvasUnit
        currentMouseMOveTheta = pos.getTheta(canvasBoundary * canvasUnit / 2).toFloat
        true
      }
      else false
    }
    else false
  }

  private def jugeTheDistance(p:Point) = {
    if((p * canvasUnit).distance(canvasBoundary * canvasUnit / 2) <= 70 * canvasUnit)
      true
    else
      false
  }

  //模拟坦克移动
  private def userMove:Unit = {
    val randomKeyCode = (new util.Random).nextInt(4) + 37
    val keyCode = changeKeys(randomKeyCode)
    if (watchKeys.contains(keyCode) && !myKeySet.contains(keyCode)) {
      myKeySet.add(keyCode)
      val preExecuteAction = TankGameEvent.UserPressKeyDown(gameContainerOpt.get.myTankId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, keyCode.toByte, getActionSerialNum)
      gameContainerOpt.get.preExecuteUserEvent(preExecuteAction)
      sendMsg2Server(preExecuteAction)
      if (org.seekloud.tank.shared.model.Constants.fakeRender) {
        gameContainerOpt.get.addMyAction(preExecuteAction)
      }
    }
    Shortcut.scheduleOnce(() => fakeUserKeyUp(keyCode),1000)
  }

  private def userClick(theta:Float):Unit = {
    if(math.abs(theta - lastMouseMoveTheta) >= mouseMoveThreshold) {
      lastMouseMoveTheta = theta
      fakeUserMouseMove(theta)
    }
    fakeUserMouseClick
  }

  //模拟鼠标点击
  private def fakeUserMouseClick = {
    val preExecuteAction = TankGameEvent.UC(gameContainerOpt.get.myTankId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, gameContainerOpt.get.tankMap(gameContainerOpt.get.myTankId).getGunDirection(), getActionSerialNum)
    gameContainerOpt.get.preExecuteUserEvent(preExecuteAction)
    sendMsg2Server(preExecuteAction)
  }

  private def fakeUserKeyUp(keyCode:Int) = {
    if (watchKeys.contains(keyCode)){
      myKeySet.remove(keyCode)
      val preExecuteAction = TankGameEvent.UserPressKeyUp(gameContainerOpt.get.myTankId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, keyCode.toByte, getActionSerialNum)
      gameContainerOpt.get.preExecuteUserEvent(preExecuteAction)
      sendMsg2Server(preExecuteAction)
      if (org.seekloud.tank.shared.model.Constants.fakeRender) {
        gameContainerOpt.get.addMyAction(preExecuteAction)
      }
    }
  }

  private def fakeUserMouseMove(theta:Float) = {
    val preMMFAction = TankGameEvent.UserMouseMove(gameContainerOpt.get.myTankId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, theta, getActionSerialNum)
    gameContainerOpt.get.preExecuteUserEvent(preMMFAction)
  }

  override protected def setKillCallback(tank: Tank) = {
    if (gameContainerOpt.nonEmpty&&tank.tankId ==gameContainerOpt.get.tankId) {
      if (tank.lives <= 1) setGameState(GameState.stop)
    }
  }


  override protected def wsMessageHandler(data:TankGameEvent.WsMsgServer):Unit = {
    data match {
      case e: TankGameEvent.WsSuccess =>
        webSocketClient.sendMsg(TankGameEvent.JoinRoom(e.roomId, None))

      case e:TankGameEvent.YourInfo =>
        timer = Shortcut.schedule(gameLoop, e.config.frameDuration)
        /**
          * 更新游戏数据
          * */
        gameContainerOpt = Some(GameContainerClientImpl(drawFrame,canvas,e.config,e.userId,e.tankId,e.name, canvasBoundary, canvasUnit,setKillCallback,versionInfoOpt))
        gameContainerOpt.get.changeTankId(e.tankId)
        thisTankId = e.tankId

      case e:TankGameEvent.YouAreKilled =>
        /**
          * 死亡重玩
          * */
        println(s"you are killed")
        gameContainerOpt.foreach(_.updateDamageInfo(e.killTankNum,e.name,e.damageStatistics))
//        dom.window.cancelAnimationFrame(nextFrame)
//        gameContainerOpt.foreach(_.drawGameStop())
        if((Constants.supportLiveLimit && ! e.hasLife) || (! Constants.supportLiveLimit)){
          setGameState(GameState.stop)
          gameContainerOpt.foreach(_.changeTankId(e.tankId))
        }

      case e:TankGameEvent.TankFollowEventSnap =>
        println(s"game TankFollowEventSnap =${e} systemFrame=${gameContainerOpt.get.systemFrame} tankId=${gameContainerOpt.get.myTankId} ")
        gameContainerOpt.foreach(_.receiveTankFollowEventSnap(e))

      case e:TankGameEvent.SyncGameState =>
        gameContainerOpt.foreach(_.receiveGameContainerState(e.state))

      case e:TankGameEvent.SyncGameAllState =>
        gameContainerOpt.foreach(_.receiveGameContainerAllState(e.gState))
        dom.window.cancelAnimationFrame(nextFrame)
        nextFrame = dom.window.requestAnimationFrame(gameRender())
        setGameState(GameState.play)

      case e:TankGameEvent.UserActionEvent =>
        //        Shortcut.scheduleOnce(() => gameContainerOpt.foreach(_.receiveUserEvent(e)),100)
        gameContainerOpt.foreach(_.receiveUserEvent(e))


      case e:TankGameEvent.GameEvent =>
        e match {
          case e:TankGameEvent.UserRelive =>
            gameContainerOpt.foreach(_.receiveGameEvent(e))
            if(e.userId == gameContainerOpt.get.myId){
              dom.window.cancelAnimationFrame(nextFrame)
              nextFrame = dom.window.requestAnimationFrame(gameRender())
            }

          case ee:TankGameEvent.GenerateBullet =>
            gameContainerOpt.foreach(_.receiveGameEvent(e))
          case _ => gameContainerOpt.foreach(_.receiveGameEvent(e))
        }

      case e:TankGameEvent.PingPackage =>
        receivePingPackage(e)

      case TankGameEvent.RebuildWebSocket=>
        gameContainerOpt.foreach(_.drawReplayMsg("存在异地登录。。"))
        closeHolder

      case _ => println(s"unknow msg={sss}")
    }
  }

}
