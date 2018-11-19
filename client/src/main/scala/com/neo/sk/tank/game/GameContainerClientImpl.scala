package com.neo.sk.tank.game

//import com.neo.sk.tank.game.draw._
import com.neo.sk.tank.shared.`object`.Tank
import com.neo.sk.tank.shared.config.TankGameConfig
import com.neo.sk.tank.shared.model.Constants.{GameAnimation, GameState, PropGenerateType}
import com.neo.sk.tank.shared.model.Point
import com.neo.sk.tank.shared.protocol.TankGameEvent
import com.neo.sk.utils.canvas.{MiddleContextInFx, MiddleFrameInFx}
import javafx.scene.canvas.GraphicsContext
//import com.neo.sk.tank.common.Constants.GameState
import javafx.geometry.VPos
import javafx.scene.paint.Color
import javafx.scene.shape.{StrokeLineCap, StrokeLineJoin}
import javafx.scene.text.{Font, TextAlignment}
import javafx.stage.Screen
import java.util.Timer
import java.util.TimerTask

import com.neo.sk.tank.App.scheduler
import com.neo.sk.tank.shared.game.{GameContainerImpl, TankClientImpl}

import concurrent.duration._
import scala.collection.mutable

/**
  * Created by hongruying on 2018/10/23
  */
case class GameContainerClientImpl(
                                    override val drawFrame:MiddleFrameInFx,
                                    override val ctx:MiddleContextInFx,
                                    override val config:TankGameConfig,
                                    myId:String,
                                    myTankId:Int,
                                    myName:String,
                                    canvasS:Point,
                                    canvasU:Int,
                                    setGameState:Int => Unit
                                  ) extends GameContainerImpl(config, myId, myTankId, myName, canvasS,canvasU,ctx = ctx,drawFrame = drawFrame) {

  protected val obstacleAttackedAnimationMap = mutable.HashMap[Int,Int]()
  protected val tankAttackedAnimationMap = mutable.HashMap[Int,Int]()
  protected val tankDestroyAnimationMap = mutable.HashMap[Int,Int]() //prop ->

  private var canvasBoundary=canvasSize
  private var renderTime:Long = 0
  private var renderTimes = 0

  private var isPlayMusic = true

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
    if(tank.tankId == myTankId && tank.lives <= 1)
      setGameState(GameState.stop)
  }

/*  def updateClientSize(canvasSize:Point, cUnit:Int)={
    canvasBoundary=canvasSize
    canvasUnit = cUnit
    updateBackSize(canvasBoundary)
//    updateBulletSize(canvasBoundary)
//    updateFpsSize(canvasBoundary)
//    updateObstacleSize(canvasBoundary)
//    updateTankSize(canvasBoundary)
  }*/


  /**
    * 游戏画面绘制
    * */
  def drawGame(time:Long,networkLatency: Long):Unit = {
    val offsetTime = math.min(time,config.frameDuration)
    val h = 800
    val w = 800
    val startTime = System.currentTimeMillis()
    if(!waitSyncData){
      ctx.setLineCap("round")
      ctx.setLineJoin("round")
      tankMap.get(myTankId) match {
        case Some(tank) =>
//          println(s"---------------------------------------------------------${tank}")
          val offset = canvasBoundary / 2 - tank.asInstanceOf[TankClientImpl].getPosition4Animation(boundary, quadTree, offsetTime)
//          val t1=System.currentTimeMillis()
          println("----drawBack---")
          drawBackground(offset)
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
    ctx.setFill("black")
    ctx.fillRec(0, 0, canvasBoundary.x * canvasUnit, canvasBoundary.y * canvasUnit)
    ctx.setFill("red")
    ctx.setTextAlign("left")
    ctx.setTextBaseline("top")
    ctx.setFont("Helvetica","normal",2)
    ctx.fillText(s"$s", 150, 180)
  }
}
