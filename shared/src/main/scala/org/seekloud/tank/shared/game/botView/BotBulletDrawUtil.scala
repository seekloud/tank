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

import org.seekloud.tank.shared.game.GameContainerClientImpl
import org.seekloud.tank.shared.game.view.BulletDrawUtil
import org.seekloud.tank.shared.model.Point

/**
  * Created by sky
  * Date on 2019/3/18
  * Time at 上午11:40
  */
trait BotBulletDrawUtil extends BulletDrawUtil{this:GameContainerClientImpl=>
  protected def drawBullet4bot(offset:Point, view:Point) = {
    bulletMap.values.foreach{ bullet =>
      val p = bullet.getPosition + offset
      if(p.in(view,Point(bullet.getRadius * 4 ,bullet.getRadius *4))) {
        val cacheCanvas = canvasCacheMap.getOrElseUpdate((bullet.getBulletLevel(),Some(bullet.tankId)), generateCanvas(bullet,Some(bullet.tankId)))
        val radius = bullet.getRadius
        mutableCtx.drawImage(cacheCanvas, (p.x - bullet.getRadius) * layerCanvasUnit - radius * layerCanvasUnit / 2.5, (p.y - bullet.getRadius) * layerCanvasUnit - radius * layerCanvasUnit / 2.5)
        ownerShipCtx.drawImage(cacheCanvas, (p.x - bullet.getRadius) * layerCanvasUnit - radius * layerCanvasUnit / 2.5, (p.y - bullet.getRadius) * layerCanvasUnit - radius * layerCanvasUnit / 2.5)
        if(bullet.tankId==myTankId){
          selfCtx.drawImage(cacheCanvas, (p.x - bullet.getRadius) * layerCanvasUnit - radius * layerCanvasUnit / 2.5, (p.y - bullet.getRadius) * layerCanvasUnit - radius * layerCanvasUnit / 2.5)
        }
      }
    }
  }
}
