package org.seekloud.tank.front.pages

import mhtml.{Var, emptyHTML}
import org.seekloud.tank.front.common.Page
import org.seekloud.tank.front.components.GameListModal
import org.seekloud.tank.front.utils.Shortcut

import scala.xml.Elem

/**
  * Created by hongruying on 2019/2/1
  */
object GameRecordPage extends Page{

  private val modal = Var(emptyHTML)

  def init() = {
    modal := GameListModal
  }

  override def render: Elem ={
    Shortcut.scheduleOnce(() =>init(),0)
    <div>
      <div >{modal}</div>
    </div>
  }


}
