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

package org.seekloud.tank.controller

import org.seekloud.tank.App
import org.seekloud.tank.common.Context
import org.seekloud.tank.view.{EnterSceneListener, EnterScreen, LoginScreen}

class EnterScreenController(val context:Context, val enter:EnterScreen) {
  enter.setListener(new EnterSceneListener{
    override def onBtnForMan(): Unit = {
      App.pushStack2AppThread{
        val loginScreen = new LoginScreen(context)
        LoginScreenController.loginScreenController = new LoginScreenController(context, loginScreen)
        LoginScreenController.loginScreenController.start
      }
    }

    override def onBtnForBot(): Unit = {

    }
  })



}
