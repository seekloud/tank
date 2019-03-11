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

/**
  * Created by sky
  * Date on 2019/3/11
  * Time at 上午12:09
  * bot游玩控制
  */
class BotPlayController(
                         playerInfo: PlayerInfo,
                         gameServerInfo: GameServerInfo,
                         roomPwd: Option[String] = None
                       ) extends GameController(800, 400, true, roomPwd) {

  val botViewActor= system.spawn(BotViewActor.create(), "BotViewActor")

  override protected def checkScreenSize: Unit = {}

  override protected def gameStopCallBack: Unit = {}

  //remind canvas2Byte示例
  protected def getView={
    gameContainerOpt.foreach(r=>r.mapCanvas.asInstanceOf[MiddleCanvasInFx].canvas2byteArray)
  }

}
