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

import akka.actor.typed.scaladsl.adapter._

import akka.actor.typed.{ActorRef, Behavior}
import org.seekloud.tank.ClientApp.system
import org.seekloud.tank.core.{BotViewActor, PlayGameActor}
import org.seekloud.tank.model._
import org.seekloud.utils.canvas.MiddleCanvasInFx

import org.seekloud.esheepapi.pb.actions._

import org.seekloud.esheepapi.pb.api.ActionReq
import org.seekloud.tank.{BotSdkTest, ClientApp}
import org.seekloud.tank.core.PlayGameActor.DispatchMsg
import org.seekloud.tank.shared.model.Constants.GameState
import org.seekloud.tank.shared.model.Point
import org.seekloud.tank.shared.protocol.TankGameEvent
import org.seekloud.tank.shared.model
import org.seekloud.tank.shared.util.canvas.MiddleCanvas
import org.seekloud.tank.view.PlayGameScreen

import scala.language.implicitConversions
/**
  * Created by sky
  * Date on 2019/3/11
  * Time at 上午12:09
  * bot游玩控制
  */
object BotViewController{
  var SDKReplyTo:ActorRef[JoinRoomRsp] = _
}
import org.seekloud.tank.common.AppSettings.{viewWidth,viewHeight}
class BotViewController(
                         playerInfo: PlayerInfo,
                         gameServerInfo: GameServerInfo,
                         isView:Boolean=false,
                         playGameScreenOpt: Option[PlayGameScreen]=None,
                       ) extends GameController(if(isView) viewWidth*4 else viewWidth, if(isView) viewHeight*4 else viewHeight, true) {
  val botViewActor= system.spawn(BotViewActor.create(), "BotViewActor")

  val pointerCanvas=drawFrame.createCanvas(viewWidth, viewHeight)
  val pointerCtx=pointerCanvas.getCtx

  var mousePlace = Point(viewWidth/2,viewHeight/2)
  drawPointer

  val actionMsgCanvas = drawFrame.createCanvas(viewWidth*4,viewHeight*4)
  val actionMsgCtx = actionMsgCanvas.getCtx
  var actionMsgList: List[String] = Nil

  private var lastMoveFrame = -1L
  private var lastMouseMoveAngle: Byte = 0

  private var moveStateNow:Byte = 8

  private def move2Byte(move: Move) :Byte = {
    move match {
      case Move.right => 0
      case Move.r_down => 1
      case Move.down => 2
      case Move.l_down => 3
      case Move.left => 4
      case Move.l_up => 5
      case Move.up => 6
      case Move.r_up => 7
      case _ => 8
    }
  }

  def startGame: Unit = {
    if(firstCome){
      if(!isView){
        firstCome = false
      }
      playGameActor ! PlayGameActor.ConnectGame(playerInfo, gameServerInfo, None)
      logicFrameTime = System.currentTimeMillis()
    }else{
      gameContainerOpt.foreach { r =>
        playGameActor ! DispatchMsg(TankGameEvent.RestartGame(Some(r.myTankId), r.myName))
        setGameState(GameState.loadingPlay)
//        playGameActor ! PlayGameActor.StartGameLoop(r.config.frameDuration)
      }
    }
  }

  def closeBotHolder = closeHolder
  override protected def checkScreenSize: Unit = {}

  override protected def gameStopCallBack: Unit = {}

  override protected def canvas2Byte4Bot: Unit = {
    implicit def canvas2Fx(m:MiddleCanvas):MiddleCanvasInFx=m.asInstanceOf[MiddleCanvasInFx]
    gameContainerOpt.foreach(r =>
      botViewActor ! BotViewActor.GetByte(
        r.locationCanvas.canvas2byteArray,
        r.immutableCanvas.canvas2byteArray,
        r.mutableCanvas.canvas2byteArray,
        r.bodiesCanvas.canvas2byteArray,
        r.ownerShipCanvas.canvas2byteArray,
        r.selfCanvas.canvas2byteArray,
        pointerCanvas.canvas2byteArray,
        r.statusCanvas.canvas2byteArray,
        Some(canvas.canvas2byteArray)
      )
    )
  }

  override protected def handleWsSuccess(e: TankGameEvent.WsSuccess): Unit = {
    //fixme 此处测试需要
//    BotSdkTest.test
  }

  override protected def initGameContainerCallBack: Unit = {
    if(isView && playGameScreenOpt.nonEmpty && firstCome){
      firstCome = false
      ClientApp.pushStack2AppThread(
        gameContainerOpt.foreach{r=>
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
          pointerCanvas.getCanvas.setLayoutX(viewWidth*4+5)
          pointerCanvas.getCanvas.setLayoutY(viewHeight*3+15)
          r.statusCanvas.asInstanceOf[MiddleCanvasInFx].getCanvas.setLayoutX(viewWidth*5+10)
          r.statusCanvas.asInstanceOf[MiddleCanvasInFx].getCanvas.setLayoutY(viewHeight*3+15)
          actionMsgCanvas.getCanvas.setLayoutX(0)
          actionMsgCanvas.getCanvas.setLayoutY(viewHeight*4 + 20)
          playGameScreenOpt.get.group.getChildren.add(canvas.getCanvas)
          playGameScreenOpt.get.group.getChildren.add(r.locationCanvas.asInstanceOf[MiddleCanvasInFx].getCanvas)
          playGameScreenOpt.get.group.getChildren.add(r.immutableCanvas.asInstanceOf[MiddleCanvasInFx].getCanvas)
          playGameScreenOpt.get.group.getChildren.add(r.mutableCanvas.asInstanceOf[MiddleCanvasInFx].getCanvas)
          playGameScreenOpt.get.group.getChildren.add(r.bodiesCanvas.asInstanceOf[MiddleCanvasInFx].getCanvas)
          playGameScreenOpt.get.group.getChildren.add(r.ownerShipCanvas.asInstanceOf[MiddleCanvasInFx].getCanvas)
          playGameScreenOpt.get.group.getChildren.add(r.selfCanvas.asInstanceOf[MiddleCanvasInFx].getCanvas)
          playGameScreenOpt.get.group.getChildren.add(pointerCanvas.getCanvas)
          playGameScreenOpt.get.group.getChildren.add(r.statusCanvas.asInstanceOf[MiddleCanvasInFx].getCanvas)
          playGameScreenOpt.get.group.getChildren.add(actionMsgCanvas.getCanvas)
        }
      )
    }
  }

  def getBotScore = gameContainerOpt.map { r =>
    r.currentRank.find(_.id == r.myTankId).getOrElse(model.Score(r.myTankId, "", 0, 0, 0))
  }.getOrElse(model.Score(0, "", 0, 0, 0))

  def getCurFrame=gameContainerOpt.map(_.systemFrame).getOrElse(0l)

  def getGameState = gameState

  def getInform = {
    val gameContainer = gameContainerOpt.get
    val myTankId = gameContainer.myTankId
    val myTankInfo = gameContainer.tankMap(myTankId)
    (myTankInfo.damageStatistics,myTankInfo.killTankNum,myTankInfo.lives)
  }

  private def drawPointer={
    pointerCtx.setFill("black")
    pointerCtx.fillRec(0,0,viewWidth,viewHeight)
    pointerCtx.beginPath()
    pointerCtx.setStrokeStyle("white")
    pointerCtx.setFill("white")
    pointerCtx.arc(mousePlace.x,mousePlace.y,2,0,360)
    pointerCtx.stroke()
    pointerCtx.closePath()
  }

  private def drawActionMsg() = {
    actionMsgCtx.setFill("white")
    actionMsgCtx.fillRec(0,0,viewWidth*4,viewHeight*4)
    actionMsgCtx.setFill("black")
    actionMsgCtx.setTextAlign("left")
    actionMsgCtx.setTextBaseline("top")
    actionMsgCtx.setFont(s"Helvetica","normal",14)
    var i = 0
    val len = actionMsgList.length
    while(i < len){
      actionMsgCtx.fillText(actionMsgList(i),10, 18 * i)
      i += 1
    }
  }

  def gameActionReceiver(key: ActionReq) = {
    actionMsgList = (key.toString :: actionMsgList).take(10)
    drawActionMsg()
    if(key.swing.nonEmpty && gameContainerOpt.nonEmpty && gameState == GameState.play){
      /**
        * 鼠标移动
        **/
        //todo 增加鼠标位置绘画
      val d = key.swing.get.distance
      val r = key.swing.get.radian
      mousePlace  += Point(d * math.cos(r).toFloat,d * math.sin(r).toFloat)
      drawPointer
      val point = mousePlace  * 4
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
    if(key.fire == 1 && gameContainerOpt.nonEmpty && gameState == GameState.play){
      /**
        * 鼠标点击，开火
        **/
      val point = mousePlace * 4
      val theta = point.getTheta(canvasBoundary  / 2).toFloat
      val preExecuteAction = TankGameEvent.UC(gameContainerOpt.get.myTankId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, theta, getActionSerialNum)
      gameContainerOpt.get.preExecuteUserEvent(preExecuteAction)
      playGameActor ! DispatchMsg(preExecuteAction)
    }
    if(key.apply == 1 && gameContainerOpt.nonEmpty && gameState == GameState.play){
      /**
        * 吃道具
        **/
      val preExecuteAction = TankGameEvent.UserPressKeyMedical(gameContainerOpt.get.myTankId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, serialNum = getActionSerialNum)
      gameContainerOpt.get.preExecuteUserEvent(preExecuteAction)
      playGameActor ! DispatchMsg(preExecuteAction)
    }
    val moveStateReceive = move2Byte(key.move)
    if (moveStateReceive != moveStateNow) {
      log.info(s"move state change to: [$moveStateReceive]")
      val preExecuteAction = TankGameEvent.UserMoveState(gameContainerOpt.get.myTankId, gameContainerOpt.get.systemFrame + preExecuteFrameOffset, moveStateReceive, getActionSerialNum)
      gameContainerOpt.get.preExecuteUserEvent(preExecuteAction)
      playGameActor ! DispatchMsg(preExecuteAction)
      if (org.seekloud.tank.shared.model.Constants.fakeRender) {
        gameContainerOpt.get.addMyAction(preExecuteAction)
      }
      moveStateNow = moveStateReceive
    }
  }

}
