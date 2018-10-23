package com.neo.sk.tank


import akka.actor.ActorSystem
import akka.dispatch.MessageDispatcher
import akka.event.{Logging, LoggingAdapter}
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.neo.sk.tank.common.Context
import com.neo.sk.tank.controller.LoginScreenController
import com.neo.sk.tank.view.LoginScreen
import javafx.animation.{Animation, AnimationTimer}
import javafx.application.Application
import javafx.scene.{Group, Scene}
import javafx.scene.canvas.Canvas
import javafx.stage.Stage

import concurrent.duration._
import javafx.application.Platform
/**
  * Created by hongruying on 2018/10/22
  */
class App extends Application{

  import App._

  scheduler.scheduleOnce(1000.millis){
    println("s")
  }





  override def start(primaryStage: Stage): Unit = {
    val context = new Context(primaryStage)
    val loginScreen = new LoginScreen(context)
    context.switchScene(loginScreen.sence)
    new LoginScreenController(context, loginScreen)
  }

}

object App{

  import concurrent.duration._

  implicit val system = ActorSystem("tankDemoSystem")
  // the executor should not be the default dispatcher.
  implicit val executor: MessageDispatcher =
    system.dispatchers.lookup("akka.actor.default-dispatcher")

  implicit val materializer = ActorMaterializer()

  implicit val scheduler = system.scheduler

  implicit val timeout:Timeout = Timeout(20 seconds) // for actor asks

  val log: LoggingAdapter = Logging(system, getClass)

  def pushStack2AppThread(fun: => Unit) = {
    Platform.runLater(() => fun)
  }


}
