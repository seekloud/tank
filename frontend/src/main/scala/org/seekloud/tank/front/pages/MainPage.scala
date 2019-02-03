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

import mhtml.{Cancelable, Rx, mount}
import org.scalajs.dom
import org.seekloud.tank.front.common.PageSwitcher
import org.seekloud.tank.front.model.ReplayInfo

import scala.xml.Elem

/**
  * Created by hongruying on 2018/4/8
  */
object MainPage extends PageSwitcher {




  override def switchPageByHash(): Unit = {
    val tokens = {
      val t = getCurrentHash.split("/").toList
      if (t.nonEmpty) {
        t.tail
      } else Nil
    }

    println(tokens)
    switchToPage(tokens)
  }


  private val currentPage: Rx[Elem] = currentPageHash.map {
    case Nil => TankDemo.render
    case "playGame" :: playInfoSeq => PlayPage(playInfoSeq).render
    case "watchRecord":: rid :: wid :: f :: accessCode :: Nil => {
      ReplayPage.setParam(ReplayInfo(rid.toLong,wid, f.toInt, accessCode))
      ReplayPage.render
    }
    case "watchGame" :: roomId :: playerId :: accessCode ::Nil => new TankObservation(roomId.toLong, accessCode, Some(playerId)).render
    case "watchGame" :: roomId :: accessCode :: Nil => new TankObservation(roomId.toLong, accessCode).render
    case "getGameRec" :: Nil => GameRecordPage.render

    case "test" :: Nil => TankTest.render
    case _ => <div>Error Page</div>
  }

  def gotoPage(i: String) = {
    dom.document.location.hash = "" + i
  }


  def show(): Cancelable = {
    switchPageByHash()
    val page =
      <div>
        {currentPage}
      </div>
    mount(dom.document.body, page)
  }

}
