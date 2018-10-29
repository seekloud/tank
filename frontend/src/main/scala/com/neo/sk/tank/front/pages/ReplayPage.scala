package com.neo.sk.tank.front.pages

import com.neo.sk.tank.front.common.Page
import com.neo.sk.tank.front.tankClient.GameReplayHolderImpl
import com.neo.sk.tank.front.utils.Shortcut
import com.neo.sk.tank.front.model.ReplayInfo
import mhtml.{Var, emptyHTML}

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
