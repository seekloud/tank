package com.neo.sk.tank.front.tankClient.game

import com.neo.sk.tank.front.tankClient.view._
import com.neo.sk.tank.front.utils.Shortcut
import com.neo.sk.tank.shared.`object`.Tank
import com.neo.sk.tank.shared.config.TankGameConfig
import com.neo.sk.tank.shared.model.Constants.{GameAnimation, GameState, PropGenerateType}
import com.neo.sk.tank.shared.model.Point
import com.neo.sk.tank.shared.protocol.TankGameEvent
import com.neo.sk.tank.shared.protocol.TankGameEvent.ObstacleAttacked
import org.scalajs.dom
import org.scalajs.dom.ext.Color

import scala.collection.mutable

/**
  * Created by hongruying on 2018/8/29
  */
case class GameContainerClientImpl(
                                    ctx:dom.CanvasRenderingContext2D,
                                    override val config:TankGameConfig,
                                    myId:String,
                                    myTankId:Int,
                                    myName:String,
                                    canvasSize:Point,
                                    var canvasUnit:Int,
                                    setGameState:Int => Unit,
                                    isObserve: Boolean = false,
                                    setKillCallback: (String, Boolean, Int, Int) => Unit = {(_,_,_,_) =>} // killerName, live, killTankNum, damage
                                  ) extends GameContainerImpl(config,myId,myTankId,myName)
  with Background
  with ObstacleDrawUtil
  with PropDrawUtil
  with TankDrawUtil
  with FpsComponents
  with BulletDrawUtil{


  private var renderTime:Long = 0
  private var renderTimes = 0

  Shortcut.schedule( () =>{
    if(renderTimes != 0){
      println(s"render page use avg time:${renderTime / renderTimes}ms")
    }else{
      println(s"render page use avg time:0 ms")
    }
    renderTime = 0
    renderTimes = 0
  }, 5000L)



  protected val obstacleAttackedAnimationMap = mutable.HashMap[Int,Int]()
  protected val tankAttackedAnimationMap = mutable.HashMap[Int,Int]()
  protected val tankDestroyAnimationMap = mutable.HashMap[Int,Int]() //prop ->

  private var canvasBoundary=canvasSize

  def updateClientSize(canvasSize:Point, cUnit:Int)={
    canvasBoundary=canvasSize
    canvasUnit = cUnit
    updateBackSize(canvasBoundary)
    updateBulletSize(canvasBoundary)
    updateFpsSize(canvasBoundary)
    updateObstacleSize(canvasBoundary)
    updateTankSize(canvasBoundary)
  }


  override protected def handleObstacleAttacked(e: ObstacleAttacked): Unit = {
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
    if(tank.tankId == tId){
      setKillCallback(bulletTankName, tank.lives > 1, tank.killTankNum, tank.damageStatistics)
      if (tank.lives > 1) setGameState(GameState.relive)
      else setGameState(GameState.stop)
    }
  }


  def drawGame(time:Long,networkLatency: Long):Unit = {
    val offsetTime = math.min(time,config.frameDuration)
    val h = dom.window.innerHeight.toFloat
    val w = dom.window.innerWidth.toFloat
    val startTime = System.currentTimeMillis()
    if(!waitSyncData){
      ctx.lineCap = "round"
      ctx.lineJoin = "round"
      tankMap.get(tId) match {
        case Some(tank) =>
          val offset = canvasBoundary / 2 - tank.asInstanceOf[TankClientImpl].getPosition4Animation(boundary, quadTree, offsetTime)
          drawBackground(offset)
          drawObstacles(offset,Point(w,h))
          drawEnvironment(offset,Point(w,h))
          drawProps(offset,Point(w,h))
          drawBullet(offset,offsetTime, Point(w,h))
          drawTank(offset,offsetTime,Point(w,h))
          drawObstacleBloodSlider(offset)
          drawMyTankInfo(tank.asInstanceOf[TankClientImpl])
          drawMinimap(tank)
          drawRank()
          renderFps(networkLatency)
          drawKillInformation()
          drawRoomNumber()
          drawCurMedicalNum(tank.asInstanceOf[TankClientImpl])

          if(tank.cavasFrame >=1) {
            tank.cavasFrame += 1
          }
          val endTime = System.currentTimeMillis()
          renderTimes += 1
          renderTime += endTime - startTime



        case None =>
//          info(s"tankid=${myTankId} has no in tankMap.....................................")
//          setGameState(GameState.stop)
//          if(isObserve) drawDeadImg()
      }
    }
  }

  def drawDeadImg(s:String) = {
    ctx.fillStyle = Color.Black.toString()
    ctx.fillRect(0, 0, canvasBoundary.x * canvasUnit, canvasBoundary.y * canvasUnit)
    ctx.fillStyle = "rgb(250, 250, 250)"
    ctx.textAlign = "left"
    ctx.textBaseline = "top"
    ctx.font = "36px Helvetica"
    ctx.fillText(s"$s", 150, 180)
  }



}