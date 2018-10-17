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
class ReplayPage(n:String, u:Long, r:Long, frame:Int) extends Page{

  private val cannvas = <canvas id ="GameReplay" tabindex="1"></canvas>

  //  private val can = cannvas.asInstanceOf[Canvas]
  ////
  //  private var ctx:dom.CanvasRenderingContext2D = null

  private val modal = Var(emptyHTML)
/*  private var name:String=""
  private var uid:Long=0l
  private var rid:Long=0l
  private var f:Int=0
  private var gameHolderOpt:Option[GameHolder]=None

  def setParam(n:String, u:Long, r:Long, frame:Int)={
    name=n
    uid=u
    rid=r
    f=frame
    gameHolderOpt.foreach(g=>g.closeWsConnect)
  }*/
  private def init() = {
    println("-----new holder------")
    val gameHolder = new GameHolder("GameReplay",true)
    gameHolder.getStartReplayModel(n,u,r,frame)
//    gameHolderOpt=Some(gameHolder)
    modal := <div>观看中...</div>
  }


  override def render: Elem ={
    Shortcut.scheduleOnce(() =>init(),0)
    <div>
      <div >{modal}</div>
      {cannvas}
    </div>
  }

}
