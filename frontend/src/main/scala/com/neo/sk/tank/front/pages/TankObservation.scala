package com.neo.sk.tank.front.pages

import com.neo.sk.tank.front.common.Page
import com.neo.sk.tank.front.tankClient.control.GameObserverHolderImpl
import com.neo.sk.tank.front.utils.Shortcut
import org.scalajs.dom

import scala.xml.Elem

class TankObservation(roomId:Long, accessCode:String, playerIdOpt: Option[String] = None) extends Page{
//  dom.window.location.hash = s"#/watchGame/${roomId}/${playerId}"
  private val canvas = <canvas id="GameWatch" tabindex="1"></canvas>
  def init() = {
    val gameObservation = new GameObserverHolderImpl("GameWatch",roomId, accessCode, playerIdOpt)
    gameObservation.watchGame()
  }

  override def render: Elem = {
    Shortcut.scheduleOnce(() => init(),0)
    <div>
    {canvas}
    </div>
  }

}
