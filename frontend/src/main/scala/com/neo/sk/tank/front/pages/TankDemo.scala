package com.neo.sk.tank.front.pages

import com.neo.sk.tank.front.common.Page
import com.neo.sk.tank.front.tankClient.GameHolder
import com.neo.sk.tank.front.utils.Shortcut
import mhtml.Var
import org.scalajs.dom
import org.scalajs.dom.ext.Color
import org.scalajs.dom.html.Canvas
import mhtml._
import scala.xml.Elem

/**
  * Created by hongruying on 2018/7/7
  */
object TankDemo extends Page{

  private val cannvas = <canvas id ="GameView" tabindex="1"></canvas>

//  private val can = cannvas.asInstanceOf[Canvas]
////
//  private var ctx:dom.CanvasRenderingContext2D = null

  private val modal = Var(emptyHTML)

  def init() = {

    val gameHolder = new GameHolder("GameView")
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
      {cannvas}
    </div>
  }



}
