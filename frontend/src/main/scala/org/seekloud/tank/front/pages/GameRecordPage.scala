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

import mhtml.{Var, emptyHTML}
import org.seekloud.tank.front.common.Page
import org.seekloud.tank.front.components.GameListModal
import org.seekloud.tank.front.utils.Shortcut

import scala.xml.Elem

/**
  * Created by hongruying on 2019/2/1
  */
object GameRecordPage extends Page{

  private val modal = Var(emptyHTML)

  def init() = {
    modal := GameListModal
  }

  override def render: Elem ={
    Shortcut.scheduleOnce(() =>init(),0)
    <div>
      <div >{modal}</div>
    </div>
  }


}
