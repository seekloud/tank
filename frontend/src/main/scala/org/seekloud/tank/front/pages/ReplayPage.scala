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

package org.seekloud.tank.front.pages

import org.seekloud.tank.front.tankClient.control.GameReplayHolderImpl
import mhtml.{Var, emptyHTML}
import org.seekloud.tank.front.common.Page
import org.seekloud.tank.front.model.ReplayInfo
import org.seekloud.tank.front.utils.Shortcut

import scala.xml.Elem

/**
  * User: sky
  * Date: 2018/10/15
  * Time: 12:35
  */
object ReplayPage extends Page {

  private val cannvas = <canvas id="GameReplay" tabindex="1"></canvas>

  //  private val can = cannvas.asInstanceOf[Canvas]
  ////
  //  private var ctx:dom.CanvasRenderingContext2D = null

  private val modal = Var(emptyHTML)
  private var infoOpt:Option[ReplayInfo]=None
  private var gameHolderOpt:Option[GameReplayHolderImpl]=None
  def setParam(r:ReplayInfo)={
    infoOpt=Some(r)
    gameHolderOpt.foreach(g=>g.closeHolder)
  }

  private def init() = {
    println("-----new holder------")
    val gameHolder = new GameReplayHolderImpl("GameReplay")
    gameHolder.startReplay(Some(infoOpt.get))
    gameHolderOpt=Some(gameHolder)
    modal := <div>观看回放中...</div>
  }


  override def render: Elem = {
    Shortcut.scheduleOnce(() => init(), 0)
    <div>
      <div>
        {modal}
      </div>{cannvas}
    </div>
  }

}
