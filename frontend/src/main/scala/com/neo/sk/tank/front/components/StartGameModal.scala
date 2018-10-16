package com.neo.sk.tank.front.components

import com.neo.sk.tank.front.common.{Component, Constants}
import mhtml.Var
import org.scalajs.dom
import org.scalajs.dom.ext.KeyCode
import org.scalajs.dom.{Event, KeyboardEvent}
import org.scalajs.dom.raw.MouseEvent

import scala.xml.Elem

/**
  * Created by hongruying on 2018/7/9
  */
class StartGameModal(gameState:Var[Int],startGame:(String) => Unit) extends Component{

  private var lives:Int = 3 // 默认第一次进入，生命值3
  private val title = gameState.map{
    case Constants.GameState.firstCome => "欢迎来到坦克大战io，请输入用户名进行游戏体验"
    case Constants.GameState.stop =>
      if(lives == 0) lives = 2
      else lives = lives - 1
      "重新开始"
    case _ => ""
  }

  private val divStyle = gameState.map{
    case Constants.GameState.play => "display:none;"
    case Constants.GameState.loadingPlay => "display:none;"
    case Constants.GameState.stop if lives != 1 => "display:none"

    case _ => "display:block;"
  }


  private val inputElem = <input id ="TankGameNameInput" onkeydown ={e:KeyboardEvent => clickEnter(e)}></input>
  private val button = <button id="start_button" class ="btn btn-info" onclick ={() => clickEnter()}>进入</button>


  def clickEnter(e:KeyboardEvent):Unit = {
    if(e.keyCode == KeyCode.Enter){
      val name = dom.document.getElementById("TankGameNameInput").asInstanceOf[dom.html.Input].value
      if(name.nonEmpty){
        startGame(name)
      }
      e.preventDefault()
    }

  }


  def clickEnter():Unit = {
    val name = dom.document.getElementById("TankGameNameInput").asInstanceOf[dom.html.Input].value
    if(name.nonEmpty){
      startGame(name)
    }
  }

  override def render: Elem = {
    <div style={divStyle}>
      <div class ="input_mask" onkeydown ={e:KeyboardEvent => clickEnter(e)}>
      </div>
      <div class ="input_div">
        <div class ="input_title">{title}</div>
        <div class ="input_elem">{inputElem}</div>
        <div class ="input_button">{button}</div>
      </div>
    </div>

  }


}
