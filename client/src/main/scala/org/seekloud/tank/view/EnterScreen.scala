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

package org.seekloud.tank.view

import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.layout.{BorderPane, HBox}
import javafx.scene.{Group, Scene}
import org.seekloud.tank.common.{Constants, Context}

abstract class EnterSceneListener {
    def onBtnForMan()
    def onBtnForBot()
}

object EnterScreen{
  var enterScreen:EnterScreen=_
}

class EnterScreen(context:Context){
  private var enterSceneListener: EnterSceneListener = _
  private val group = new Group()
  private val scene = new Scene(group,Constants.SceneBound.weight,Constants.SceneBound.height)

  private val borderPane = new BorderPane()
  borderPane.prefHeightProperty().bind(scene.heightProperty())
  borderPane.prefWidthProperty().bind(scene.widthProperty())

  private val hBox = new HBox(10)
  hBox.setAlignment(Pos.CENTER)

  private val buttonForMan = new Button()
  private val buttonForBot = new Button()
  buttonForMan.setText("用户登录")
  buttonForBot.setText("Bot登录")
  buttonForMan.setOnAction(_ => enterSceneListener.onBtnForMan())
  buttonForBot.setOnAction(_ => enterSceneListener.onBtnForBot())

  hBox.getChildren.addAll(buttonForMan, buttonForBot)
  borderPane.setCenter(hBox)
  group.getChildren.add(borderPane)

  def show = context.switchScene(this.scene,resize = true)

  def setListener(listener:EnterSceneListener):Unit = {
    enterSceneListener = listener
  }

}

