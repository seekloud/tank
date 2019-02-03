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

import org.seekloud.tank.front.tankClient.control.GameTestHolderImpl
import mhtml.{Var, emptyHTML}
import org.seekloud.tank.front.common.Page
import org.seekloud.tank.front.utils.Shortcut

import scala.xml.Elem

/**
  * Created by hongruying on 2019/2/1
  */
object TankTest extends Page{

  private val cannvas = <canvas id="GameTest" tabindex="1"></canvas>
  private val audio_1 = <audio id="GameAudioForBgm" src="/tank/static/music/tank.mp3" loop="loop" preload="auto" style="display:none"></audio>
  private val audio_2 = <audio id="GameAudioForDead" src="/tank/static/music/fail.mp3" preload="auto" style="display:none"></audio>
  private val audio_3 = <audio id="GameAudioForBullet" src="/tank/static/music/bullet.mp3" preload="auto" style="display:none"></audio>
  private val modal = Var(emptyHTML)

  def init() = {
    val gameHolder = new GameTestHolderImpl("GameTest")
    val startGameModal = gameHolder.getStartGameModal()
    modal := startGameModal
  }

  override def render: Elem ={
    Shortcut.scheduleOnce(() =>init(),0)
    <div>
      <div >{modal}</div>
      {cannvas}
      {audio_1}
      {audio_2}
      {audio_3}
    </div>
  }



}
