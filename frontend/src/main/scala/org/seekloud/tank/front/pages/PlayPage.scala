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

package org.seekloud.tank.front.pages

import org.seekloud.tank.front.tankClient.control.GamePlayHolderImpl
import org.seekloud.tank.front.utils.Shortcut
import mhtml.{Var, emptyHTML}
import org.seekloud.tank.front.common.Page
import org.seekloud.tank.front.model.PlayerInfo
import org.seekloud.tank.front.utils.{JsFunc, Shortcut}

import scala.xml.Elem

/**
  * Created by hongruying on 2018/10/18
  */
case class PlayPage(
              playerInfoSeq:List[String]
              ) extends Page{


  private def parsePlayerInfoSeq: Option[PlayerInfo] = playerInfoSeq match {
    case userId :: userName :: roomId :: accessCode :: Nil => Some(PlayerInfo(userId, JsFunc.decodeURI(userName), accessCode, Some(roomId.toLong)))
    case userId :: userName  :: accessCode :: Nil => Some(PlayerInfo(userId, JsFunc.decodeURI(userName), accessCode, None))
    case _ => None
  }




  private val cannvas = <canvas id ="GameView" tabindex="1"></canvas>


  private val modal = Var(emptyHTML)

  def init(playerInfo: PlayerInfo) = {
    val gameHolder = new GamePlayHolderImpl("GameView", Some(playerInfo))
    val startGameModal = gameHolder.getStartGameModal()
    modal := startGameModal
  }





  override def render: Elem ={
    parsePlayerInfoSeq match {
      case Some(playerInfo) =>
        Shortcut.scheduleOnce(() =>init(playerInfo),0)
        <div>
          <div >{modal}</div>
          {cannvas}
        </div>
      case None =>
        <div>
          <div >链接不符合标准</div>
        </div>
    }

  }

}
