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

package org.seekloud.tank.front.utils.canvas

import org.seekloud.tank.shared.util.canvas.MiddleImage
import javafx.scene.SnapshotParameters
import javafx.scene.paint.Color
import org.scalajs.dom.html.Image
import org.scalajs.dom
import org.scalajs.dom.html
import org.seekloud.tank.front.common.Routes

/**
  * Created by sky
  * Date on 2018/11/16
  * Time at 下午4:51
  */
object MiddleImageInJs {
  def apply(url: String): MiddleImageInJs = new MiddleImageInJs(url)
}

class MiddleImageInJs extends MiddleImage {
  private[this] var image: Image = _

  def this(url: String) = {
    this()
    image = dom.document.createElement("img").asInstanceOf[html.Image]
    image.setAttribute("src", Routes.base + "/static" + url)
  }

  def getImage = image

  override def isComplete: Boolean = image.complete

}
