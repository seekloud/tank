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
import org.seekloud.tank.shared.model.Point

/**
  * Created by sky
  * Date on 2019/3/18
  * Time at 上午11:26
  */
trait BotBackgroundDrawUtil extends BackgroundDrawUtil{ this:GameContainerClientImpl=>
  def drawBackground4Bot(offset:Point)={
    clearScreen("#000000",1, layerCanvasSize.x, layerCanvasSize.y, immutableCtx)
    clearScreen("#000000",1, layerCanvasSize.x, layerCanvasSize.y, mutableCtx)
    clearScreen("#000000",1, layerCanvasSize.x, layerCanvasSize.y, bodiesCtx)
    clearScreen("#000000",1, layerCanvasSize.x, layerCanvasSize.y, ownerShipCtx)
    clearScreen("#000000",1, layerCanvasSize.x, layerCanvasSize.y, selfCtx)
  }

  protected def drawLocation4Bot(tank:Tank):Unit={
    if(isBot){
      locationCtx.setFill("black")
      locationCtx.fillRec(0,0,220,200)
      locationCtx.beginPath()
      locationCtx.setStrokeStyle("white")
      val x = tank.getPosition.x * 0.5 - 48
      val y = tank.getPosition.y * 0.5 - 27
      val w = 96
      val h = 54
      locationCtx.setFill("white")
      locationCtx.fillRec(x,y,w,h)
      locationCtx.moveTo(x,y)
      locationCtx.lineTo(x,y+h)
      locationCtx.lineTo(x+w,y+h)
      locationCtx.lineTo(x+w,y)
      locationCtx.lineTo(x,y)
      locationCtx.stroke()
      locationCtx.closePath()
    }
  }
}
