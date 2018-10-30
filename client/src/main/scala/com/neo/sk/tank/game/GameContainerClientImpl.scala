package com.neo.sk.tank.game


import com.neo.sk.tank.game.draw._
import com.neo.sk.tank.shared.`object`.{Tank, TankImpl}
import com.neo.sk.tank.shared.config.TankGameConfig
import com.neo.sk.tank.shared.game.GameContainerImpl
import com.neo.sk.tank.shared.model.Constants.{GameAnimation, PropGenerateType}
import com.neo.sk.tank.shared.model.Point
import com.neo.sk.tank.shared.protocol.TankGameEvent
import javafx.scene.canvas.GraphicsContext
import com.neo.sk.tank.common.Constants.GameState
import javafx.geometry.VPos
import javafx.scene.paint.Color
import javafx.scene.shape.{StrokeLineCap, StrokeLineJoin}
import javafx.scene.text.{Font, TextAlignment}
import javafx.stage.Screen
import java.util.Timer
import java.util.TimerTask

import com.neo.sk.tank.App.scheduler
import concurrent.duration._

import scala.collection.mutable

/**
  * Created by hongruying on 2018/10/23
  */
case class GameContainerClientImpl(
                                    ctx:GraphicsContext,
                                    override val config:TankGameConfig,
                                    myId:String,
                                    myTankId:Int,
                                    myName:String,
                                    canvasSize:Point,
                                    canvasUnit:Int,
                                    setGameState:Int => Unit
                                  ) extends GameContainerImpl(config, myId, myTankId, myName)
  with Background
  with ObstacleDrawUtil
  with PropDrawUtil
  with TankDrawUtil
  with FpsComponents
  with BulletDrawUtil{

  protected val obstacleAttackedAnimationMap = mutable.HashMap[Int,Int]()
  protected val tankAttackedAnimationMap = mutable.HashMap[Int,Int]()
  protected val tankDestroyAnimationMap = mutable.HashMap[Int,Int]() //prop ->

  private var canvasBoundary=canvasSize
  private var renderTime:Long = 0
  private var renderTimes = 0

  val timer = new Timer()
  timer.schedule(new TimerTask {
    override def run(): Unit = {
      if(renderTimes != 0){
        println(s"render page use avg time:${renderTime / renderTimes}ms")
      }else{
        println(s"render page use avg time:0 ms")
      }
      renderTime = 0
      renderTimes = 0
    }
  }, 0, 5000)

  override protected def handleObstacleAttacked(e: TankGameEvent.ObstacleAttacked): Unit = {
    super.handleObstacleAttacked(e)
    if(obstacleMap.get(e.obstacleId).nonEmpty || environmentMap.get(e.obstacleId).nonEmpty){
      obstacleAttackedAnimationMap.put(e.obstacleId, GameAnimation.bulletHitAnimationFrame)
    }
  }


  override protected def handleTankAttacked(e: TankGameEvent.TankAttacked): Unit = {
    super.handleTankAttacked(e)
    if(tankMap.get(e.tankId).nonEmpty){
      tankAttackedAnimationMap.put(e.tankId,GameAnimation.bulletHitAnimationFrame)
    }
  }

  override protected def handleGenerateProp(e: TankGameEvent.GenerateProp): Unit = {
    super.handleGenerateProp(e)
    if(e.generateType == PropGenerateType.tank){
      tankDestroyAnimationMap.put(e.propState.pId,GameAnimation.tankDestroyAnimationFrame)
    }
  }


  override protected def dropTankCallback(bulletTankId:Int, bulletTankName:String,tank:Tank) = {
    if(tank.tankId == myTankId){
      if (tank.lives > 1) setGameState(GameState.relive)
      else setGameState(GameState.stop)
    }
  }


  /**
    * 游戏画面绘制
    * */
  def drawGame(time:Long,networkLatency: Long):Unit = {
    val offsetTime = math.min(time,config.frameDuration)
    val bounds = Screen.getPrimary.getVisualBounds
    val h = bounds.getMaxY.toFloat
    val w = bounds.getMaxX.toFloat
    val startTime = System.currentTimeMillis()
    if(!waitSyncData){
      ctx.setLineCap(StrokeLineCap.ROUND)
      ctx.setLineJoin(StrokeLineJoin.ROUND)
      tankMap.get(myTankId) match {
        case Some(tank) =>
          val offset = canvasBoundary / 2 - tank.asInstanceOf[TankImpl].getPosition4Animation(boundary, quadTree, offsetTime)
          drawBackground(offset)
          drawObstacles(offset,Point(w,h))
          drawEnvironment(offset,Point(w,h))
          drawProps(offset,Point(w,h))
          drawBullet(offset,offsetTime, Point(w,h))
          drawTank(offset,offsetTime,Point(w,h))
          drawObstacleBloodSlider(offset)
          drawMyTankInfo(tank.asInstanceOf[TankImpl])
          drawMinimap(tank)
          drawRank()
          renderFps(networkLatency)
          drawKillInformation()
          drawRoomNumber()
          drawCurMedicalNum(tank.asInstanceOf[TankImpl])

          if(tank.cavasFrame >=1) {
            tank.cavasFrame += 1
          }
          val endTime = System.currentTimeMillis()
          renderTimes += 1
          renderTime += endTime - startTime
        case None =>
      }
    }
  }

  def drawDeadImg(s:String) = {
    ctx.setFill(Color.BLACK)
    ctx.fillRect(0, 0, canvasBoundary.x * canvasUnit, canvasBoundary.y * canvasUnit)
    ctx.setFill(Color.rgb(250, 250, 250))
    ctx.setTextAlign(TextAlignment.LEFT)
    ctx.setTextBaseline(VPos.TOP)
    ctx.setFont(Font.font("Helvetica", 36))
    ctx.fillText(s"$s", 150, 180)
  }
}
