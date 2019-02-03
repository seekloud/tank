package org.seekloud.tank


import akka.actor.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.adapter._
import akka.dispatch.MessageDispatcher
import akka.event.{Logging, LoggingAdapter}
import akka.stream.ActorMaterializer
import akka.util.Timeout
import javafx.application.{Application, Platform}
import javafx.stage.Stage
import org.seekloud.tank.actor.TokenActor
import org.seekloud.tank.common.Context
import org.seekloud.tank.controller.EnterScreenController
import org.seekloud.tank.view.EnterScreen
/**
  * Created by hongruying on 2018/10/22
  */
class  App extends Application{

  override def start(primaryStage: Stage): Unit = {
    val context = new Context(primaryStage)
//    val playerInfo = PlayerInfo(UserInfo(100,"eee","101",100),"df","hahha","jasiohfis")
//    val gameHallScreen = new GameHallScreen(context,playerInfo)
//    context.switchScene(gameHallScreen.getScene)
//    val gameServerInfo = GameServerInfo("",30369,"flowdev.neoap.com")
//    new HallScreenController(context,gameHallScreen,gameServerInfo,playerInfo)
    val enterScreen = new EnterScreen(context)
    context.switchScene(enterScreen.getScene,resize = true)
    new EnterScreenController(context, enterScreen)
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

  val tokenActor:ActorRef[TokenActor.Command] = system.spawn(TokenActor.create,"esheepSyncClient")

  implicit val timeout:Timeout = Timeout(20 seconds) // for actor asks

  val log: LoggingAdapter = Logging(system, getClass)

  def pushStack2AppThread(fun: => Unit) = {
    Platform.runLater(() => fun)
  }



}
