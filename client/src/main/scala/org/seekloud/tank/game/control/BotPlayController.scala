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

package org.seekloud.tank.game.control

import akka.actor.typed.scaladsl.adapter._
import org.seekloud.tank.App.system
import org.seekloud.tank.core.BotViewActor
import org.seekloud.tank.model.{GameServerInfo, PlayerInfo}
import org.seekloud.utils.canvas.MiddleCanvasInFx
import org.seekloud.tank.shared.model
import org.seekloud.tank.shared.util.canvas.MiddleCanvas

/**
  * Created by sky
  * Date on 2019/3/11
  * Time at 上午12:09
  * bot游玩控制
  */
class BotPlayController(
                         playerInfo:
                         roomPwd: Option[String] = None
                       ) extends GameController(800, 400, true, roomPwd) {

  val botViewActor = system.spawn(BotViewActor.create(), "BotViewActor")

  override protected def checkScreenSize: Unit = {}

  override protected def gameStopCallBack: Unit = {}

  override protected def canvas2Byte4Bot: Unit = {
    implicit def canvas2Fx(m:MiddleCanvas):MiddleCanvasInFx=m.asInstanceOf[MiddleCanvasInFx]
    gameContainerOpt.foreach(r =>
      botViewActor ! BotViewActor.GetByte(
        r.locationCanvas.canvas2byteArray,
        r.mapCanvas.canvas2byteArray,
        r.immutableCanvas.canvas2byteArray,
        r.mutableCanvas.canvas2byteArray,
        r.bodiesCanvas.canvas2byteArray,
        r.statusCanvas.canvas2byteArray,
        Some(canvas.canvas2byteArray)
      )
    )
  }

  override protected def initGameContainerCallBack: Unit = {}

  def getBotScore = gameContainerOpt.map { r =>
    r.currentRank.find(_.id == r.myTankId).getOrElse(model.Score(r.myTankId, "", 0, 0, 0))
  }.getOrElse(model.Score(0, "", 0, 0, 0))

  def getCurFrame=gameContainerOpt.map(_.systemFrame).getOrElse(0l)

}
