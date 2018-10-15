package com.neo.sk.tank.front.pages

import com.neo.sk.tank.front.common.Page
import com.neo.sk.tank.front.tankClient.GameHolder
import com.neo.sk.tank.front.utils.Shortcut
import mhtml.{Var, emptyHTML}

import scala.xml.Elem

/**
  * User: sky
  * Date: 2018/10/15
  * Time: 12:35
  */
class ReplayPage(name:String,uid:Long,rid:Long) extends Page{

  private val cannvas = <canvas id ="GameReplay" tabindex="1"></canvas>

  //  private val can = cannvas.asInstanceOf[Canvas]
  ////
  //  private var ctx:dom.CanvasRenderingContext2D = null

  private val modal = Var(emptyHTML)

  def init() = {

    val gameHolder = new GameHolder("GameReplay",true)
    println("------1")
    gameHolder.getStartReplayModel(name,uid,rid)
    modal := <div>Please Wait...</div>
  }


  override def render: Elem ={
    Shortcut.scheduleOnce(() =>init(),0)
    <div>
      <div >{modal}</div>
      {cannvas}
    </div>
  }

}
