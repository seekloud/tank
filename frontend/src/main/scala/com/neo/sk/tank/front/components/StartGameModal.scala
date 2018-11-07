package com.neo.sk.tank.front.components

import com.neo.sk.tank.front.common.{Component, Constants}
import com.neo.sk.tank.front.model.PlayerInfo
import com.neo.sk.tank.shared.model.Constants.GameState
import mhtml.Var
import org.scalajs.dom
import org.scalajs.dom.ext.KeyCode
import org.scalajs.dom.{Event, KeyboardEvent}
import org.scalajs.dom.raw.MouseEvent

import scala.xml.Elem
import com.neo.sk.tank.front.utils.Shortcut

/**
  * Created by hongruying on 2018/7/9
  */
class StartGameModal(gameState:Var[Int],startGame:(String,Option[Long]) => Unit, playerInfoOpt:Option[PlayerInfo]) extends Component{


  private val inputDisabled:Var[Boolean] = Var(playerInfoOpt.isDefined)
  private val inputValue:Var[String] = Var(playerInfoOpt.map(_.userName).getOrElse(""))
//  private val input4RoomIdValue:Var[Option[Long]] = Var(playerInfoOpt.map(_.roomIdOpt).get)



//  private var lives:Int = 3 // 默认第一次进入，生命值3
  private val title = gameState.map{
    case GameState.firstCome => "欢迎来到坦克大战io，请输入用户名进行游戏体验"
    case GameState.stop => "重新开始"
    case _ => ""
  }


  private val name = gameState.map{
    case GameState.firstCome => "名字"
    case GameState.stop => "名字"
    case _ => ""
  }

  private val roomId = gameState.map{
    case GameState.firstCome => "房间"
    case GameState.stop => "房间"
    case _ => ""
  }


  private val divStyle = gameState.map{
    case GameState.play => "display:none;"
    case GameState.loadingPlay => "display:none;"
    case GameState.relive => "display:none;"
//    case Constants.GameState.stop if lives != 1 => "display:none"
    case _ => "display:block;"
  }


  private val watchButtonDivStyle = inputDisabled.map{
    case true => "display:none;"
    case false => "display:block;"
  }


  private val inputElem = <input id ="TankGameNameInput" onkeydown ={e:KeyboardEvent => clickEnter(e)} disabled={inputDisabled} value ={inputValue}></input>
  private val inputElem4RoomId = <input id="TankGameRoomIdInput" onkeydown={e:KeyboardEvent => clickEnter(e)} disabled={inputDisabled}></input>
  private val button = <button id="start_button" class ="btn btn-info" onclick ={() => clickEnter()}>进入</button>
  private val watchButton = <button id="watch_button" class ="btn btn-info" onclick ={() => Shortcut.redirect("#/getGameRec")}>进入观看列表</button>



  def clickEnter(e:KeyboardEvent):Unit = {
    if(e.keyCode == KeyCode.Enter){
      val name = dom.document.getElementById("TankGameNameInput").asInstanceOf[dom.html.Input].value
      val roomIdString = dom.document.getElementById("TankGameRoomIdInput").asInstanceOf[dom.html.Input].value
      val roomIdOpt = if(roomIdString == "")None else Some(roomIdString.toLong)
      if(name.nonEmpty){
        startGame(name,roomIdOpt)
      }
      e.preventDefault()
    }

  }


  def clickEnter():Unit = {
    val name = dom.document.getElementById("TankGameNameInput").asInstanceOf[dom.html.Input].value
    val roomIdString = dom.document.getElementById("TankGameRoomIdInput").asInstanceOf[dom.html.Input].value
    val roomIdOpt = if(roomIdString == "")None else Some(roomIdString.toLong)
    if(name.nonEmpty){
      startGame(name,roomIdOpt)
    }
  }

  override def render: Elem = {
    <div style={divStyle}>
      <div class ="input_mask" onkeydown ={e:KeyboardEvent => clickEnter(e)}>
      </div>
      <div class ="input_div">
        <div id = "combat_gains">
        </div>
        <div class ="input_title">{title}</div>
        <div>
          <div class="input_inline">
            <div class ="input_des" style="display:inline-block">{name}</div>
            <div class ="input_elem" style="display: inline-block;">{inputElem}</div>
          </div>
          <div class="input_inline">
            <div class="input_des" style="display: inline-block;">{roomId}</div>
            <div class="input_elem" style="display: inline-block;">{inputElem4RoomId}</div>
          </div>
        </div>
        <div class ="input_button">{button}</div>
        <div class ="input_button" style={watchButtonDivStyle}>{watchButton}</div>
      </div>
    </div>

  }


}
