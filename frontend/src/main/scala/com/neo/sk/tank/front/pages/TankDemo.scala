package com.neo.sk.tank.front.pages

import com.neo.sk.tank.front.common.Page
import com.neo.sk.tank.front.tankClient.control.GamePlayHolderImpl
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
  private val audio_1 = <audio id = "GameAudioForBgm"></audio>
  private val audio_2 = <audio id = "GameAudioForDead"></audio>

//  private val can = cannvas.asInstanceOf[Canvas]
////
//  private var ctx:dom.CanvasRenderingContext2D = null

  private val modal = Var(emptyHTML)

  def init() = {
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
      {cannvas}
      {audio_1}
      {audio_2}
    </div>
  }



}
