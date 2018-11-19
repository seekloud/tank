//package com.neo.sk.tank.front.tankClient.view
//
//import com.neo.sk.tank.front.tankClient.NetworkLatency
//import com.neo.sk.tank.front.tankClient.game.GameContainerClientImpl
//import com.neo.sk.tank.shared.model.Constants.LittleMap
//import com.neo.sk.tank.shared.model.Point
//import org.scalajs.dom.ext.Color
//
///**
//  * Created by hongruying on 2018/8/29
//  */
//trait FpsComponents{ this:GameContainerClientImpl =>
//  private var lastRenderTime = System.currentTimeMillis()
//  private var lastRenderTimes = 0
//
//  private var renderTimes = 0
//
//  private val isRenderFps:Boolean = true
//
//  private var canvasBoundary:Point=canvasSize
//
//  def updateFpsSize(canvasSize:Point)={
//    canvasBoundary=canvasSize
//  }
//
//  private def addFps() ={
//    val time = System.currentTimeMillis()
//    renderTimes += 1
//    if(time - lastRenderTime > 1000){
//      lastRenderTime = time
//      lastRenderTimes = renderTimes
//      renderTimes = 0
//    }
//  }
//
//  protected def renderFps(networkLatency: Long) = {
//    addFps()
//    if(isRenderFps){
//      ctx.font = s"${1.4 * canvasUnit}px Helvetica"
//      ctx.textAlign = "start"
//      ctx.fillStyle = Color.Black.toString()
//      val fpsString = s"fps : $lastRenderTimes,  ping : ${networkLatency}ms"
//      ctx.fillText(fpsString,canvasBoundary.x * canvasUnit - ctx.measureText(fpsString).width - 10,(canvasBoundary.y - LittleMap.h - 2) * canvasUnit)
////      ctx.fillText(s"ping: ${networkLatency}ms",canvasBoundary.x * canvasUnit - ctx.measureText(),(canvasBoundary.y - LittleMap.h - 2) * canvasUnit,10 * canvasUnit)
//    }
//  }
//
//
//}