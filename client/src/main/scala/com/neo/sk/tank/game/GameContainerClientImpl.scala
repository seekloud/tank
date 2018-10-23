package com.neo.sk.tank.game


import com.neo.sk.tank.game.draw._
import com.neo.sk.tank.shared.`object`.Tank
import com.neo.sk.tank.shared.config.TankGameConfig
import com.neo.sk.tank.shared.game.GameContainerImpl
import com.neo.sk.tank.shared.model.Constants.{GameAnimation, PropGenerateType}
import com.neo.sk.tank.shared.model.Point
import com.neo.sk.tank.shared.protocol.TankGameEvent
import javafx.scene.canvas.GraphicsContext
import com.neo.sk.tank.common.Constants.GameState

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
                                    canvasBoundary:Point,
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

  }






}
