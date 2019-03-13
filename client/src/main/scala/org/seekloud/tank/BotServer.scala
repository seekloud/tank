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
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.adapter._
import io.grpc.{Server, ServerBuilder}
import org.seekloud.pb.actions._
import org.seekloud.pb.api._
import org.seekloud.pb.service.EsheepAgentGrpc
import org.seekloud.pb.service.EsheepAgentGrpc.EsheepAgent
import org.seekloud.tank.common.AppSettings
import org.seekloud.tank.core.PlayGameActor.DispatchMsg
import org.seekloud.tank.core.{BotViewActor, GrpcStreamActor}
import org.seekloud.tank.game.control.{BotPlayController, GameController}
import org.seekloud.tank.model.JoinRoomRsp
import org.seekloud.tank.shared.model.Constants.GameState
import org.seekloud.tank.shared.protocol.TankGameEvent
import org.seekloud.tank.view.LoginScene
import org.slf4j.LoggerFactory
import org.seekloud.tank.App.system

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
  var isObservationConnect = false
  var isFrameConnect = false
  var state: State = State.unknown

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
  private[this] val log = LoggerFactory.getLogger(this.getClass)

  val botViewActor = gameController.botViewActor

  override def createRoom(request: CreateRoomReq): Future[CreateRoomRsp] = {
//    println(s"createRoom Called by [$request")
//    val state = State.init_game
//    Future.successful(CreateRoomRsp(errCode = 101, state = state, msg = "ok"))
    log.info(s"createRoom Called by [$request]")
    BotServer.state = State.in_game
    val getRoomIdRsp: Future[JoinRoomRsp] = botViewActor ? (BotViewActor.CreateRoomReq(request.password, _))
    getRoomIdRsp.map {
      rsp =>
        if (rsp.errCode == 0) CreateRoomRsp(rsp.roomId.toString, 0, BotServer.state, "ok")
        else CreateRoomRsp(rsp.roomId.toString, rsp.errCode, BotServer.state, rsp.msg)
    }

  }

  override def joinRoom(request: JoinRoomReq): Future[SimpleRsp] = {
//    println(s"joinRoom Called by [$request")
//    val state = State.in_game
//    Future.successful(SimpleRsp(errCode = 102, state = state, msg = "ok"))
    BotServer.state = State.in_game
    val joinRoomRsp: Future[JoinRoomRsp] = botViewActor ? (BotViewActor.JoinRoomReq(request.roomId.toLong,request.password, _))
    joinRoomRsp.map {
      rsp =>
        if (rsp.errCode == 0) SimpleRsp(0, BotServer.state, "ok")
        else SimpleRsp(rsp.errCode, BotServer.state, rsp.msg)
    }
  }

  override def leaveRoom(request: Credit): Future[SimpleRsp] = {
//    println(s"leaveRoom Called by [$request")
//    val state = State.ended
//    Future.successful(SimpleRsp(errCode = 103, state = state, msg = "ok"))
    BotServer.isFrameConnect = false
    BotServer.isObservationConnect = false
    BotServer.streamSender.foreach(s=> s ! GrpcStreamActor.LeaveRoom)
    BotServer.state = State.ended
    Future.successful {
      SimpleRsp(state = BotServer.state, msg = "ok")
    }
  }

  override def actionSpace(request: Credit): Future[ActionSpaceRsp] = {
//    println(s"actionSpace Called by [$request")
//    val rsp = ActionSpaceRsp()
//    Future.successful(rsp)
    val rsp = ActionSpaceRsp(move = List(Move.up, Move.down, Move.left, Move.right), fire = List(), apply = List(), state = BotServer.state, msg = "ok")
    Future.successful(rsp)
  }

  override def action(request: ActionReq): Future[ActionRsp] = {
//    println(s"action Called by [$request")
//    val rsp = ActionRsp()
//    Future.successful(rsp)
    gameController.gameActionReceiver(request)
    val rsp = ActionRsp(frameIndex = gameController.getFrameCount, state = BotServer.state, msg = "ok")
    Future.successful(rsp)
  }

  override def observation(request: Credit): Future[ObservationRsp] = {
//    println(s"action Called by [$request")
//    val rsp = ObservationRsp()
//    Future.successful(rsp)
    BotServer.state = if (gameController.getGameState == GameState.play) State.in_game else State.killed
    val observationRsp: Future[ObservationRsp] = botViewActor ? BotViewActor.GetObservation
    observationRsp.map {
      observation =>
        ObservationRsp(observation.layeredObservation, observation.humanObservation, gameController.getFrameCount, 0, BotServer.state, "ok")
    }
  }

  override def inform(request: Credit): Future[InformRsp] = {
//    println(s"action Called by [$request")
//    val rsp = InformRsp()
//    Future.successful(rsp)
    BotServer.state = if (gameController.getGameState == GameState.play) State.in_game else State.killed
    val rsp = InformRsp(gameController.getInform._1, gameController.getInform._2, gameController.getInform._3, gameController.getFrameCount, 0, BotServer.state, "ok")
    Future.successful(rsp)
  }

  override def reincarnation(request: Credit): Future[SimpleRsp] = {
    gameController.receiveReincarnation
    Future.successful(SimpleRsp(state = BotServer.state, msg = "ok"))
  }

  override def systemInfo(request: Credit): Future[SystemInfoRsp] = {
    val rsp = SystemInfoRsp(framePeriod = AppSettings.framePeriod, state = BotServer.state, msg = "ok")
    Future.successful(rsp)
  }


  override def currentFrame(request: Credit, responseObserver: StreamObserver[CurrentFrameRsp]): Unit = {
    BotServer.isFrameConnect = true
    if (BotServer.streamSender.isDefined) {
      BotServer.streamSender.get ! GrpcStreamActor.FrameObserver(responseObserver)
    } else {
      BotServer.streamSender = Some(system.spawn(GrpcStreamActor.create(gameController), "GrpcStreamSender"))
      MedusaServer.streamSender.get ! GrpcStreamSender.FrameObserver(responseObserver)
    }
  }

  override def observationWithInfo(request: Credit, responseObserver: StreamObserver[ObservationWithInfoRsp]): Unit = {

  }
}
