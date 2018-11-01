package com.neo.sk.tank


import akka.actor.ActorSystem
import akka.dispatch.MessageDispatcher
import akka.event.{Logging, LoggingAdapter}
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.neo.sk.tank.common.Context
import com.neo.sk.tank.view.PlayGameScreen
import javafx.animation.{Animation, AnimationTimer}
import javafx.application.Application
import javafx.scene.{Group, Scene}
import javafx.scene.canvas.Canvas
import javafx.stage.Stage
import com.neo.sk.tank.controller.PlayScreenController
import concurrent.duration._
import javafx.application.Platform
import com.neo.sk.tank.model.{GameServerInfo, PlayerInfo}
/**
  * Created by hongruying on 2018/10/22
  */
class App extends Application{

  import App._

  scheduler.scheduleOnce(1000.millis){
    println("s")
  }

  val playerInfo = PlayerInfo("1", "1", "hahhahahha")
  val gameServerInfo = GameServerInfo("1", "1", "1")




  override def start(primaryStage: Stage): Unit = {
    val context = new Context(primaryStage)
    val playScreen = new PlayGameScreen(context)
    context.switchScene(playScreen.getScene())
    val l = new PlayScreenController(playerInfo, gameServerInfo, context, playScreen)
    l.start
  }

}

object App{

  import concurrent.duration._
  import scala.language.postfixOps

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
