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

package org.seekloud.tank.shared.game.botView

import org.seekloud.tank.shared.`object`.Tank
import org.seekloud.tank.shared.game.GameContainerClientImpl
import org.seekloud.tank.shared.game.view.BackgroundDrawUtil
import org.seekloud.tank.shared.model.{Constants, Point}

/**
  * Created by sky
  * Date on 2019/3/18
  * Time at 上午11:26
  */
trait BotBackgroundDrawUtil extends BackgroundDrawUtil{ this:GameContainerClientImpl=>
  def drawBackground4Bot(offset:Point)={
    val backColor="#000000"
    clearScreen(backColor,1, layerCanvasSize.x, layerCanvasSize.y, immutableCtx)
    clearScreen(backColor,1, layerCanvasSize.x, layerCanvasSize.y, mutableCtx)
    clearScreen(backColor,1, layerCanvasSize.x, layerCanvasSize.y, bodiesCtx)
    clearScreen(backColor,1, layerCanvasSize.x, layerCanvasSize.y, ownerShipCtx)
    clearScreen(backColor,1, layerCanvasSize.x, layerCanvasSize.y, selfCtx)
    clearScreen(backColor,1, layerCanvasSize.x, layerCanvasSize.y, locationCtx)
  }

  private val fullSize=config.boundary
  private val locationSize=Point(layerCanvasSize.x-20,layerCanvasSize.y-10)
  private val locationUnit=fullSize/locationSize
  private val realSize=Point(Constants.WindowView.x,layerCanvasSize.y/canvasUnit)/locationUnit
  protected def drawLocation4Bot(tank:Tank):Unit={
    locationCtx.beginPath()
    locationCtx.setStrokeStyle("white")
    locationCtx.moveTo(10, 5)
    locationCtx.lineTo(layerCanvasSize.x-10, 5)
    locationCtx.lineTo(layerCanvasSize.x-10, layerCanvasSize.y-5)
    locationCtx.lineTo(10, layerCanvasSize.y-5)
    locationCtx.lineTo(10, 5)
    locationCtx.stroke()
    locationCtx.closePath()
    val loP = tank.getPosition/locationUnit
    locationCtx.beginPath()
    locationCtx.setFill("white")
    locationCtx.moveTo(loP.x-realSize.x/2+10,loP.y-realSize.y/2+5)
    locationCtx.lineTo(loP.x+realSize.x/2+10,loP.y-realSize.y/2+5)
    locationCtx.lineTo(loP.x+realSize.x/2+10,loP.y+realSize.y/2+5)
    locationCtx.lineTo(loP.x-realSize.x/2+10,loP.y+realSize.y/2+5)
    locationCtx.lineTo(loP.x-realSize.x/2+10,loP.y-realSize.y/2+5)
    locationCtx.fill()
    locationCtx.stroke()
    locationCtx.closePath()
  }
}
