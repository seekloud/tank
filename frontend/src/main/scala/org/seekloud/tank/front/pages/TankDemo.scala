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

import org.seekloud.tank.front.tankClient.control.GamePlayHolderImpl
import mhtml.{Var, emptyHTML}
import org.seekloud.tank.front.common.Page
import org.seekloud.tank.front.utils.Shortcut

import scala.xml.Elem

/**
  * Created by hongruying on 2018/7/7
  */
object TankDemo extends Page{

  private val canvas = <canvas id="GameView" tabindex="1"></canvas>
//  private val audio_1 = <audio id="GameAudioForBgm" src="/tank/static/music/tank.mp3" loop="loop" preload="auto" style="display:none"></audio>
//  private val audio_2 = <audio id="GameAudioForDead" src="/tank/static/music/fail.mp3" preload="auto" style="display:none"></audio>
//  private val audio_3 = <audio id="GameAudioForBullet" src="/tank/static/music/bullet.mp3" preload="auto" style="display:none"></audio>
//  private val audio_3 = <audio id="GameAudioForBullet" src="/tank/static/music/fail.mp3" preload="auto" style="display:none"></audio>
//  private val can = cannvas.asInstanceOf[Canvas]
////
//  private var ctx:dom.CanvasRenderingContext2D = null

  private val modal = Var(emptyHTML)

  def init() = {
    print("------1-111")
    val gameHolder = new GamePlayHolderImpl("GameView")
    val startGameModal = gameHolder.getStartGameModal()
    modal := startGameModal
    //    val canvas = dom.document.getElementById("GameView").asInstanceOf[Canvas]
//    val ctx = canvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
//    println("ssssssssssss")
////    ctx = can.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
//    canvas.width = 100
//    canvas.height = 100
//    ctx.fillStyle = Color.Green.toString()
//    ctx.fillRect(0,0,20,20)
  }



  override def render: Elem ={
    Shortcut.scheduleOnce(() =>init(),0)
    <div>
      <div >{modal}</div>
      {canvas}
    </div>
  }



}
