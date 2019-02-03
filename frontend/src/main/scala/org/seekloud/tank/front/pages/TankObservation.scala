package org.seekloud.tank.front.pages

import org.seekloud.tank.front.tankClient.control.GameObserverHolderImpl
import org.seekloud.tank.front.common.Page
import org.seekloud.tank.front.utils.Shortcut

import scala.xml.Elem

class TankObservation(roomId:Long, accessCode:String, playerIdOpt: Option[String] = None) extends Page{
//  dom.window.location.hash = s"#/watchGame/${roomId}/${playerId}"
  private val canvas = <canvas id="GameWatch" tabindex="1"></canvas>
  private val audio_1 = <audio id="GameAudioForBgm" src="/tank/static/music/tank.mp3" loop="loop" preload="auto" style="display:none"></audio>
  private val audio_2 = <audio id="GameAudioForDead" src="/tank/static/music/fail.mp3" preload="auto" style="display:none"></audio>
  private val audio_3 = <audio id="GameAudioForBullet" src="/tank/static/music/bullet.mp3" preload="auto" style="display:none"></audio>
  def init() = {
    val gameObservation = new GameObserverHolderImpl("GameWatch",roomId, accessCode, playerIdOpt)
    gameObservation.watchGame()
  }

  override def render: Elem = {
    Shortcut.scheduleOnce(() => init(),0)
    <div>
      {canvas}
      {audio_1}
      {audio_2}
      {audio_3}
    </div>
  }

}
