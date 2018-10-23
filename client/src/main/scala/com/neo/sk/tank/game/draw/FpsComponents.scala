package com.neo.sk.tank.game.draw

import com.neo.sk.tank.game.GameContainerClientImpl
import com.neo.sk.tank.shared.model.Constants.LittleMap

/**
  * Created by hongruying on 2018/8/29
  */
trait FpsComponents{ this:GameContainerClientImpl =>
  private var lastRenderTime = System.currentTimeMillis()
  private var lastRenderTimes = 0

  private var renderTimes = 0

  private val isRenderFps:Boolean = true

  private def addFps() ={
    val time = System.currentTimeMillis()
    renderTimes += 1
    if(time - lastRenderTime > 1000){
      lastRenderTime = time
      lastRenderTimes = renderTimes
      renderTimes = 0
    }
  }

  protected def renderFps(networkLatency: Long) = {
    addFps()

  }


}