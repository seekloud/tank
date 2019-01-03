package com.neo.sk.tank.controller

import com.neo.sk.tank.App
import com.neo.sk.tank.common.Context
import com.neo.sk.tank.view.{EnterSceneListener, EnterScreen, LoginScreen}

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
