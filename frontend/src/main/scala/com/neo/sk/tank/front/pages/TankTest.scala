package com.neo.sk.tank.front.pages

import com.neo.sk.tank.front.common.Page
import com.neo.sk.tank.front.tankClient.control.{GamePlayHolderImpl, GameTestHolderImpl}
import com.neo.sk.tank.front.utils.Shortcut
import mhtml.{Var, emptyHTML}

import scala.xml.Elem

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

