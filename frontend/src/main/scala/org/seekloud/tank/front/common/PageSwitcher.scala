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

package org.seekloud.tank.front.common

import mhtml.Var
import org.scalajs.dom
import org.scalajs.dom.HashChangeEvent

/**
  * User: Taoz
  * Date: 6/3/2017
  * Time: 1:46 PM
  */
trait PageSwitcher {

  import scalatags.JsDom.short._

  protected var currentPageHash: Var[List[String]] = Var(Nil)

  private val bodyContent = div(*.height := "100%").render

  def getCurrentHash: String = dom.window.location.hash


  private[this] var internalTargetHash = ""


  //init.
  {
    val func = {
      e: HashChangeEvent =>
        //only handler browser history hash changed.
        if (internalTargetHash != getCurrentHash) {
          println(s"hash changed, new hash: $getCurrentHash")
          internalTargetHash =getCurrentHash
          switchPageByHash()
        }
    }
    dom.window.addEventListener("hashchange", func, useCapture = false)
  }


  protected def switchToPage(pageName: List[String]): Unit = {
    currentPageHash.update(_ => pageName)
  }

  def getCurrentPageName: Var[List[String]] = currentPageHash

  def switchPageByHash(): Unit

}
