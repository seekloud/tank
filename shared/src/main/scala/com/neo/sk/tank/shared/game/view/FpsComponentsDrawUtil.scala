package com.neo.sk.tank.shared.game.view

import com.neo.sk.tank.shared.game.GameContainerClientImpl
import com.neo.sk.tank.shared.model.Constants.LittleMap
import com.neo.sk.tank.shared.model.Point

/**
  * Created by hongruying on 2018/8/29
  */
trait FpsComponentsDrawUtil{ this:GameContainerClientImpl =>
  private var lastRenderTime = System.currentTimeMillis()
  private var lastRenderTimes = 0
  private var renderTimes = 0
  private val isRenderFps:Boolean = true
  private var canvasBoundary:Point=canvasSize

  def updateFpsSize(canvasSize:Point)={
    canvasBoundary = canvasSize
  }


  private def addFps() ={
    val time = System.currentTimeMillis()
    renderTimes += 1
    if(time - lastRenderTime > 1000){
      lastRenderTime = time
      lastRenderTimes = renderTimes
      renderTimes = 0
    }
  }

  protected def renderFps(networkLatency: Long,dataSize:String) = {
    addFps()
    if(isRenderFps){
      ctx.setFont("Helvetica", "normal",2 * canvasUnit)
//      ctx.setTextAlign(TextAlignment.JUSTIFY)
      ctx.setFill("rgb(0,0,0)")
      val fpsString = s"fps : $lastRenderTimes,  ping : ${networkLatency}ms"
      ctx.fillText(fpsString,canvasBoundary.x * canvasUnit - fpsString.length * canvasUnit/1.5,(canvasBoundary.y - LittleMap.h - 2) * canvasUnit)
      ctx.fillText(dataSize,canvasBoundary.x * canvasUnit - fpsString.length * canvasUnit/1.5,(canvasBoundary.y - LittleMap.h - 4) * canvasUnit)
      //      ctx.fillText(s"ping: ${networkLatency}ms",canvasBoundary.x * canvasUnit - ctx.measureText(),(canvasBoundary.y - LittleMap.h - 2) * canvasUnit,10 * canvasUnit)
    }

  }

}