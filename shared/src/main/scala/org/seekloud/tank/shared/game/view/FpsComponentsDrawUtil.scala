/*
 * Copyright 2018 seekloud (https://github.com/seekloud)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.seekloud.tank.shared.game.view

import org.seekloud.tank.shared.game.GameContainerClientImpl
import org.seekloud.tank.shared.model.Constants.LittleMap
import org.seekloud.tank.shared.model.Point

/**
  * Created by hongruying on 2018/8/29
  */
trait FpsComponentsDrawUtil{ this:GameContainerClientImpl =>
  private var lastRenderTime = System.currentTimeMillis()
  private var lastRenderTimes = 0
  private var renderTimes = 0
  private val isRenderFps:Boolean = true
  private var canvasBoundary:Point=canvasSize / canvasUnit

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

  protected def renderFps(networkLatency: Long, dataSizeList:List[String]) = {
    addFps()
    if(isRenderFps){
      viewCtx.setFont("Helvetica", "normal",2 * canvasUnit)
      viewCtx.setFill("rgb(0,0,0)")
      viewCtx.setTextAlign("left")
      val fpsString = s"fps : $lastRenderTimes,  ping : ${networkLatency}ms"
      viewCtx.fillText(fpsString,0,(canvasBoundary.y - LittleMap.h - 2) * canvasUnit)
      viewCtx.setTextAlign("right")
      var i=18
      dataSizeList.foreach{ r=>
        viewCtx.fillText(r,canvasBoundary.x*canvasUnit,(canvasBoundary.y - i) * canvasUnit)
        i+=2
      }
      }

  }

}