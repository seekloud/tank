package com.neo.sk.tank.front.tankClient

import com.neo.sk.tank.front.common.Constants.GameState
import com.neo.sk.tank.front.tankClient.draw._
import com.neo.sk.tank.shared.`object`.{Tank, TankImpl}
import com.neo.sk.tank.shared.config.TankGameConfig
import com.neo.sk.tank.shared.game.GameContainerImpl
import com.neo.sk.tank.shared.model.Constants.{GameAnimation, LittleMap, PropGenerateType}
import com.neo.sk.tank.shared.model.Point
import com.neo.sk.tank.shared.protocol.TankGameEvent
import com.neo.sk.tank.shared.protocol.TankGameEvent.ObstacleAttacked
import org.scalajs.dom

import scala.collection.mutable

/**
  * Created by hongruying on 2018/8/29
  */
case class GameContainerClientImpl(
                                    ctx:dom.CanvasRenderingContext2D,
                                    override val config:TankGameConfig,
                                    myId:Long,
                                    myTankId:Int,
                                    myName:String,
                                    canvasBoundary:Point,
                                    canvasUnit:Int,
                                    setGameState:Int => Unit
                                  ) extends GameContainerImpl(config,myId,myTankId,myName)
  with Background
  with ObstacleDrawUtil
  with PropDrawUtil
  with TankDrawUtil
  with FpsComponents
  with BulletDrawUtil{


  protected val obstacleAttackedAnimationMap = mutable.HashMap[Int,Int]()
  protected val tankAttackedAnimationMap = mutable.HashMap[Int,Int]()
  protected val tankDestroyAnimationMap = mutable.HashMap[Int,Int]() //prop ->


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
    if(tank.tankId == myTankId){
      setGameState(GameState.stop)
    }
  }


  def drawGame(time:Long):Unit = {
    val offsetTime = math.min(time,config.frameDuration)
    if(!waitSyncData){
      ctx.lineCap = "round"
      ctx.lineJoin = "round"
      tankMap.get(myTankId) match {
        case Some(tank) =>
          val offset = canvasBoundary / 2 - tank.asInstanceOf[TankImpl].getPosition4Animation(boundary, quadTree, offsetTime)
          drawBackground(offset)
          drawObstacles(offset)
          drawEnvironment(offset)
          drawProps(offset)
          drawBullet(offset,offsetTime)
          drawTank(offset,offsetTime)
          drawMyTankInfo(tank.asInstanceOf[TankImpl])
          drawMinimap(tank)
          drawRank()
          renderFps()
          drawKillInformation()



        case None =>
          setGameState(GameState.stop)
      }
    }
  }



}
