/*
 * Copyright 2018 seekloud (https://github.com/seekloud)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.seekloud.tank.front.components

import mhtml.Var
import org.scalajs.dom
import org.scalajs.dom.KeyboardEvent
import org.scalajs.dom.ext.KeyCode
import org.seekloud.tank.front.common.Component
import org.seekloud.tank.front.model.PlayerInfo
import org.seekloud.tank.front.utils.Shortcut
import org.seekloud.tank.shared.model.Constants.GameState

import scala.xml.Elem

/**
  * Created by hongruying on 2018/7/9
  */
class StartGameModal(gameState:Var[Int],startGame:(String,Option[Long]) => Unit, playerInfoOpt:Option[PlayerInfo]) extends Component{


  private val inputDisabled:Var[Boolean] = Var(playerInfoOpt.isDefined)
  private val inputValue:Var[String] = Var(playerInfoOpt.map(_.userName).getOrElse(""))
//  private val input4RoomIdValue:Var[Option[Long]] = Var(playerInfoOpt.map(_.roomIdOpt).get)
  private var inputName = ""



//  private var lives:Int = 3 // 默认第一次进入，生命值3
  private val title = gameState.map{
    case GameState.firstCome => "欢迎来到坦克大战io，请输入用户名进行游戏体验"
    case GameState.stop => "重新开始"
    case _ => ""
  }

  private val divStyle = gameState.map{
    case GameState.firstCome => "display:block;"
    case GameState.stop => "display:block;"
    case _ => "display:none;"
  }

  private val inputDivStyle = gameState.map{
    case GameState.firstCome => "display:block;"
    case _ => "display:none;"
  }
//
//  private val combatGainsStyle = gameState.map{
//    case GameState.stop => "display:block"
//    case _ => "display:none"
//  }


  private val watchButtonDivStyle = inputDisabled.map{
    case true => "display:none;"
    case false => "display:inline;"
  }


  private val inputElem = <input id ="TankGameNameInput" onkeydown ={e:KeyboardEvent => clickEnter(e)} disabled={inputDisabled} value ={inputValue}></input>
  private val inputElem4RoomId = <input id="TankGameRoomIdInput" onkeydown={e:KeyboardEvent => clickEnter(e)} disabled={inputDisabled}></input>
  private val button = <button id="start_button" class ="btn btn-info" onclick ={() => clickEnter()}>进入游戏</button>
  private val watchButton = <button id="watch_button" class ="btn btn-info" onclick ={() => Shortcut.redirect("#/getGameRec")}>观看列表</button>



  def clickEnter(e:KeyboardEvent):Unit = {
    if(e.keyCode == KeyCode.Enter){
      val name = dom.document.getElementById("TankGameNameInput").asInstanceOf[dom.html.Input].value
      val roomIdString = dom.document.getElementById("TankGameRoomIdInput").asInstanceOf[dom.html.Input].value
      val roomIdOpt = if(roomIdString == "") None else Some(roomIdString.toLong)
      if(name.nonEmpty){
        inputName = name
        startGame(name,roomIdOpt)
      }
      e.preventDefault()
    }
    if(e.keyCode == KeyCode.Space){
      if(inputName != "") startGame(inputName,None)
    }
  }


  def clickEnter():Unit = {
    val name = dom.document.getElementById("TankGameNameInput").asInstanceOf[dom.html.Input].value
    val roomIdString = dom.document.getElementById("TankGameRoomIdInput").asInstanceOf[dom.html.Input].value
    val roomIdOpt = if(roomIdString == "") None else Some(roomIdString.toLong)
    if(name.nonEmpty){
      inputName = name
      startGame(name,roomIdOpt)
    }
  }

  override def render: Elem = {
    <div style={divStyle}>
      <div class ="input_mask" id="input_mask_id" tabindex="-1" onkeydown ={e:KeyboardEvent => clickEnter(e)}></div>
      <div class ="input_div" style={inputDivStyle}>
        <div class ="input_title">{title}</div>
        <div>
          <p class="input_inline"><span class="input_des">名字</span>{inputElem}</p>
          <p class="input_inline"><span class="input_des">房间</span>{inputElem4RoomId}</p>
        </div>
        <div class ="input_button">
          <span>{button}</span>
          <span style={watchButtonDivStyle}>{watchButton}</span>
        </div>
      </div>
    </div>

  }


}
