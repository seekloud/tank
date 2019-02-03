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

package org.seekloud.tank

import akka.actor.ActorSystem
import akka.actor.typed.ActorRef
import akka.dispatch.MessageDispatcher
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.util.Timeout
import org.seekloud.tank.core.{RoomManager, UserManager}
import org.seekloud.tank.core.bot.BotManager
import org.seekloud.tank.core.{EsheepSyncClient, RoomManager, UserManager}
import org.seekloud.tank.http.HttpService
import akka.actor.typed.scaladsl.adapter._
import scala.util.{Failure, Success}
import scala.language.postfixOps

/**
  * Created by hongruying on 2018/3/11
  */
object Boot extends HttpService {

  import org.seekloud.tank.common.AppSettings._

  import concurrent.duration._

  override implicit val system = ActorSystem("tankDemoSystem", config)
  // the executor should not be the default dispatcher.
  override implicit val executor: MessageDispatcher =
    system.dispatchers.lookup("akka.actor.my-blocking-dispatcher")

  override implicit val materializer = ActorMaterializer()

  override implicit val scheduler = system.scheduler

  override implicit val timeout:Timeout = Timeout(20 seconds) // for actor asks

  val log: LoggingAdapter = Logging(system, getClass)


//  val roomActor:ActorRef[RoomActor.Command] = system.spawn(RoomActor.create(),"roomActor")
  val roomManager:ActorRef[RoomManager.Command] = system.spawn(RoomManager.create(),"roomManager")

  val userManager:ActorRef[UserManager.Command] = system.spawn(UserManager.create(),"userManager")

  val botManager:ActorRef[BotManager.Command] = system.spawn(BotManager.create(),"BotManager")

  val esheepSyncClient:ActorRef[EsheepSyncClient.Command] = system.spawn(EsheepSyncClient.create,"esheepSyncClient")


//  var testTime = System.currentTimeMillis()
//  scheduler.schedule(0.millis,120.millis){
//    val startTime = System.currentTimeMillis()
//    println(s"test time delay =${startTime - testTime}")
//    testTime = startTime
//  }

  scheduler.schedule(10 minutes, 10 minutes){deleteCode}








  def main(args: Array[String]) {
    log.info("Starting.")
    val binding = Http().bindAndHandle(routes, httpInterface, httpPort)
    binding.onComplete {
      case Success(b) ⇒
        val localAddress = b.localAddress
        println(s"Server is listening on ${localAddress.getHostName}:${localAddress.getPort}")
      case Failure(e) ⇒
        println(s"Binding failed with ${e.getMessage}")
        system.terminate()
        System.exit(-1)
    }
  }


}
