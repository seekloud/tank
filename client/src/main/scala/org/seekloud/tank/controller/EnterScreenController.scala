package org.seekloud.tank.controller

import org.seekloud.tank.App
import org.seekloud.tank.common.Context
import org.seekloud.tank.view.{EnterSceneListener, EnterScreen, LoginScreen}

class EnterScreenController(val context:Context, val enter:EnterScreen) {
  enter.setListener(new EnterSceneListener{
    override def onBtnForMan(): Unit = {
      App.pushStack2AppThread{
        val loginScreen = new LoginScreen(context)
        val l = new LoginScreenController(context, loginScreen)
        l.start
      }
    }

    override def onBtnForBot(): Unit = {

    }
  })



}
