/*
 *  Copyright 2018 seekloud (https://github.com/seekloud)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.seekloud.tank

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{Behaviors, StashBuffer, TimerScheduler}
import io.grpc.stub.StreamObserver
import io.grpc.{Server, ServerBuilder}
import org.seekloud.pb.api._
import org.seekloud.pb.service.EsheepAgentGrpc
import org.seekloud.pb.service.EsheepAgentGrpc.EsheepAgent
import org.seekloud.tank.core.GrpcStreamActor
import org.seekloud.tank.game.control.{BotPlayController, GameController}
import org.slf4j.LoggerFactory
import akka.actor.typed.scaladsl.adapter._
import org.seekloud.tank.App.{system,executor}
import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by hongruying on 2018/11/29
  *
  * @author sky
  *         管理server服务
  *         对接grpcAPI
  */
object BotServer {
  val log = LoggerFactory.getLogger(this.getClass)

  trait Command

  case class BuildServer(port: Int,
                         executionContext: ExecutionContext,
                         gameController: BotPlayController) extends Command

  case object Shutdown extends Command

  var streamSender: Option[ActorRef[GrpcStreamActor.Command]] = None
  var state: State = State.unknown
  var isObservationConnect = false
  var isFrameConnect = false

  private def build(
                     port: Int,
                     executionContext: ExecutionContext,
                     gameController: BotPlayController
                   ): Server = {
    log.info("tank gRPC Sever is building..")
    val service = new BotServer(gameController)
    ServerBuilder.forPort(port).addService(
      EsheepAgentGrpc.bindService(service, executionContext)
    ).build
  }

  def create(): Behavior[Command] = {
    Behaviors.setup[Command] { ctx =>
      Behaviors.withTimers[Command] { implicit timer =>
        implicit val stashBuffer: StashBuffer[Command] = StashBuffer[Command](Int.MaxValue)
        idle()
      }
    }
  }

  private def idle()
                  (implicit stashBuffer: StashBuffer[Command],
                   timer: TimerScheduler[Command]
                  ): Behavior[Command] = {
    Behaviors.receive { (ctx, msg) =>
      msg match {
        case BuildServer(port, executor, gController) =>
          val server = build(port, executor, gController)
          server.start()
          log.info(s"Server started at $port")
          sys.addShutdownHook {
            log.info("JVM SHUT DOWN.")
            server.shutdown()
            log.info("SHUT DOWN.")
          }
          working(server)
      }
    }
  }

  private def working(server: Server)
                     (implicit stashBuffer: StashBuffer[Command],
                      timer: TimerScheduler[Command]
                     ): Behavior[Command] = {
    Behaviors.receive { (ctx, msg) =>
      msg match {
        case Shutdown =>
          server.shutdown()
          Behaviors.stopped
      }
    }
  }


}


class BotServer(
                 gameController: BotPlayController
               ) extends EsheepAgent {

  import BotServer._
  import org.seekloud.utils.SecureUtil.botAuth

  override def createRoom(request: CreateRoomReq): Future[CreateRoomRsp] = {
    if (request.credit.nonEmpty && botAuth(request.credit.get.apiToken)) {
      println(s"createRoom Called by [$request")
      state = State.init_game
      Future.successful(CreateRoomRsp(errCode = 101, state = state, msg = "ok"))
    } else {
      Future.successful(CreateRoomRsp(errCode = 101, state = State.unknown, msg = "auth error"))
    }
  }

  override def joinRoom(request: JoinRoomReq): Future[SimpleRsp] = {
    if (request.credit.nonEmpty && botAuth(request.credit.get.apiToken)) {
      println(s"joinRoom Called by [$request")
      state = State.in_game
      Future.successful(SimpleRsp(errCode = 102, state = state, msg = "ok"))
    } else {
      Future.successful(SimpleRsp(errCode = 101, state = State.unknown, msg = "auth error"))
    }
  }

  override def leaveRoom(request: Credit): Future[SimpleRsp] = {
    if (botAuth(request.apiToken)) {
      println(s"leaveRoom Called by [$request")
      state = State.ended
      Future.successful(SimpleRsp(errCode = 103, state = state, msg = "ok"))
    } else {
      Future.successful(SimpleRsp(errCode = 101, state = State.unknown, msg = "auth error"))
    }
  }

  override def actionSpace(request: Credit): Future[ActionSpaceRsp] = {
    if (botAuth(request.apiToken)) {
      println(s"actionSpace Called by [$request")
      val rsp = ActionSpaceRsp()
      Future.successful(rsp)
    } else {
      Future.successful(ActionSpaceRsp(errCode = 101, state = State.unknown, msg = "auth error"))
    }
  }

  override def action(request: ActionReq): Future[ActionRsp] = {
    if (request.credit.nonEmpty && botAuth(request.credit.get.apiToken)) {
      println(s"action Called by [$request")
      val rsp = ActionRsp()
      Future.successful(rsp)
    } else {
      Future.successful(ActionRsp(errCode = 101, state = State.unknown, msg = "auth error"))
    }
  }

  override def observation(request: Credit): Future[ObservationRsp] = {
    if (botAuth(request.apiToken)) {
      println(s"action Called by [$request")
      val rsp = ObservationRsp()
      Future.successful(rsp)
    } else {
      Future.successful(ObservationRsp(errCode = 101, state = State.unknown, msg = "auth error"))
    }
  }

  override def inform(request: Credit): Future[InformRsp] = {
    if (botAuth(request.apiToken)) {
      println(s"action Called by [$request")
      val rsp = InformRsp()
      Future.successful(rsp)
    } else {
      Future.successful(InformRsp(errCode = 101, state = State.unknown, msg = "auth error"))
    }
  }

  override def reincarnation(request: Credit): Future[SimpleRsp] = {
    if (botAuth(request.apiToken)) {
      Future.successful(SimpleRsp())
    } else {
      Future.successful(SimpleRsp(errCode = 101, state = State.unknown, msg = "auth error"))
    }
  }

  override def systemInfo(request: Credit): Future[SystemInfoRsp] = {
    if (botAuth(request.apiToken)) {
      Future.successful(SystemInfoRsp())
    } else {
      Future.successful(SystemInfoRsp(errCode = 101, state = State.unknown, msg = "auth error"))
    }
  }


  override def currentFrame(request: Credit, responseObserver: StreamObserver[CurrentFrameRsp]): Unit = {
    if (botAuth(request.apiToken)) {
      isFrameConnect=true
      if (streamSender.isDefined) {
        streamSender.get ! GrpcStreamActor.FrameObserver(responseObserver)
      } else {
        streamSender = Some(system.spawn(GrpcStreamActor.create(gameController), "GrpcStreamActor"))
        streamSender.get ! GrpcStreamActor.FrameObserver(responseObserver)
      }
    } else {
      responseObserver.onCompleted()
    }
  }

  override def observationWithInfo(request: Credit, responseObserver: StreamObserver[ObservationWithInfoRsp]): Unit = {
    if (botAuth(request.apiToken)) {
      isObservationConnect=true
      if(streamSender.isDefined){
        streamSender.get ! GrpcStreamActor.ObservationObserver(responseObserver)
      }else{
        streamSender = Some(system.spawn(GrpcStreamActor.create(gameController), "GrpcStreamActor"))
        streamSender.get ! GrpcStreamActor.ObservationObserver(responseObserver)
      }
    } else {
      responseObserver.onCompleted()
    }
  }
}
