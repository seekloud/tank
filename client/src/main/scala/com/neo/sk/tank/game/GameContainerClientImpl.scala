package com.neo.sk.tank.game

import com.neo.sk.tank.game.draw._
import com.neo.sk.tank.shared.`object`.{Tank, TankClientImpl}
import com.neo.sk.tank.shared.config.TankGameConfig
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
                                    var canvasUnit:Int,
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

  def updateClientSize(canvasSize:Point, cUnit:Int)={
    canvasBoundary=canvasSize
    canvasUnit = cUnit
    updateBackSize(canvasBoundary)
    updateBulletSize(canvasBoundary)
    updateFpsSize(canvasBoundary)
    updateObstacleSize(canvasBoundary)
    updateTankSize(canvasBoundary)
  }


  /**
    * 游戏画面绘制
    * */
  def drawGame(time:Long,networkLatency: Long):Unit = {
    val offsetTime = math.min(time,config.frameDuration)
//    val bounds = Screen.getPrimary.getVisualBounds
//    val h = bounds.getMaxY.toFloat
//    val w = bounds.getMaxX.toFloat
    val h = 800
    val w = 800
    val startTime = System.currentTimeMillis()
    if(!waitSyncData){
      ctx.setLineCap(StrokeLineCap.ROUND)
      ctx.setLineJoin(StrokeLineJoin.ROUND)
      tankMap.get(myTankId) match {
        case Some(tank) =>
          val offset = canvasBoundary / 2 - tank.asInstanceOf[TankClientImpl].getPosition4Animation(boundary, quadTree, offsetTime)
//          val t1=System.currentTimeMillis()
          drawBackground(offset)
//          val t2=System.currentTimeMillis()
          drawObstacles(offset,Point(w,h))
//          val t3=System.currentTimeMillis()
          drawEnvironment(offset,Point(w,h))
//          val t4=System.currentTimeMillis()
          drawProps(offset,Point(w,h))
//          val t5=System.currentTimeMillis()
          drawBullet(offset,offsetTime, Point(w,h))
//          val t6=System.currentTimeMillis()
          drawTank(offset,offsetTime,Point(w,h))
//          val t7=System.currentTimeMillis()
          drawObstacleBloodSlider(offset)
//          val t8=System.currentTimeMillis()
          drawMyTankInfo(tank.asInstanceOf[TankClientImpl])
//          val t9=System.currentTimeMillis()
          drawMinimap(tank)
//          val t10=System.currentTimeMillis()
          drawRank()
//          val t11=System.currentTimeMillis()
          renderFps(networkLatency)
//          val t12=System.currentTimeMillis()
          drawKillInformation()
//          val t13=System.currentTimeMillis()
          drawRoomNumber()
//          val t14=System.currentTimeMillis()
          drawCurMedicalNum(tank.asInstanceOf[TankClientImpl])
//          val t15=System.currentTimeMillis()
      /*    val l=List(t1,t2,t3,t4,t5,t6,t7,t8,t9,t10,t11,t12,t13,t14,t15)
          l.reduceLeft{(a,b)=> {
            print(s"---time--${b-a} ")
            b
          }}
          print("==============")*/
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
