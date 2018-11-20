package com.neo.sk.tank.front.tankClient.game

//import com.neo.sk.tank.front.tankClient.view._
import com.neo.sk.tank.front.utils.Shortcut
import com.neo.sk.tank.front.utils.canvas.{MiddleContextInJs, MiddleFrameInJs}
import com.neo.sk.tank.shared.`object`.Tank
import com.neo.sk.tank.shared.config.TankGameConfig
import com.neo.sk.tank.shared.game.{GameContainerImpl, TankClientImpl}
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
                                    override val drawFrame:MiddleFrameInJs,
                                    override val ctx:MiddleContextInJs,
                                    override val config:TankGameConfig,
                                    myId:String,
                                    myTankId:Int,
                                    myName:String,
                                    canvasS:Point,
                                    canvasU:Int,
                                    setGameState:Int => Unit,
                                    isObserve: Boolean = false,
                                    setKillCallback: (String, Boolean, Int, Int) => Unit = {(_,_,_,_) =>} // killerName, live, killTankNum, damage
                                  ) extends GameContainerImpl(config,myId,myTankId,myName,canvasS,canvasU,ctx,drawFrame){


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


  private var canvasBoundary=canvasSize


  override protected def dropTankCallback(bulletTankId:Int, bulletTankName:String,tank:Tank) = {
    if(tank.tankId == tId){
      setKillCallback(bulletTankName, tank.lives > 1, tank.killTankNum, tank.damageStatistics)
      if (tank.lives <= 1) setGameState(GameState.stop)
    }
  }


  def drawDeadImg(s:String) = {
    ctx.setFill("rgb(0,0,0)")
    ctx.fillRec(0, 0, canvasBoundary.x * canvasUnit, canvasBoundary.y * canvasUnit)
    ctx.setFill("rgb(250, 250, 250)")
    ctx.setTextAlign("left")
    ctx.setTextBaseline("top")
    ctx.setFont("Helvetica","normal",36)
    ctx.fillText(s"$s", 150, 180)
  }



}
