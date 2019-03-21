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
import org.seekloud.esheepapi.pb.actions._
import org.seekloud.esheepapi.pb.api._
import org.seekloud.esheepapi.pb.service.EsheepAgentGrpc
import org.seekloud.esheepapi.pb.service.EsheepAgentGrpc.EsheepAgent
import org.seekloud.tank.common.AppSettings
import org.seekloud.tank.core.{BotViewActor, GrpcStreamActor, LoginActor, PlayGameActor}
import org.seekloud.tank.game.control.BotViewController
import org.seekloud.tank.model.{BotKeyReq, GameServerInfo, JoinRoomRsp, PlayerInfo}
import org.seekloud.tank.shared.model.Constants.GameState
import org.slf4j.LoggerFactory
import org.seekloud.tank.ClientApp.{executor, scheduler, system, timeout}

import scala.concurrent.{ExecutionContext, Future}
/**
  * Created by hongruying on 2018/11/29
  *
  * @author sky
  *         管理server服务
  *         对接grpcAPI
  */
object BotServer {
  private val log = LoggerFactory.getLogger(this.getClass)

  trait Command

  case class BuildServer(port: Int,
                         executionContext: ExecutionContext,
                         gameController: BotViewController) extends Command

  case object Shutdown extends Command

  var streamSender: Option[ActorRef[GrpcStreamActor.Command]] = None
  var isObservationConnect = false
  var isFrameConnect = false
  var state: State = State.unknown
  var frameDuration=AppSettings.framePeriod

  private def build(
                     port: Int,
                     executionContext: ExecutionContext,
                     gameController: BotViewController
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
          System.exit(0)
          Behaviors.stopped
      }
    }
  }
}


class BotServer(
                 gameController: BotViewController
               ) extends EsheepAgent {

  import BotServer._
  import org.seekloud.utils.SecureUtil.botAuth

  private[this] val log = LoggerFactory.getLogger(this.getClass)

  val botViewActor = gameController.botViewActor

  override def createRoom(request: CreateRoomReq): Future[CreateRoomRsp] = {
    if (request.credit.nonEmpty && botAuth(request.credit.get.apiToken)) {
      log.info(s"createRoom Called by [$request]")
      state = State.in_game
      val getRoomIdRsp: Future[JoinRoomRsp] = gameController.playGameActor ? (PlayGameActor.CreateRoomReq(request.password, frameDuration,_))
      getRoomIdRsp.map {
        rsp =>
          if (rsp.errCode == 0) {
            log.info("createRoomSuccess")
            CreateRoomRsp(rsp.roomId.toString, 0, state, "ok")
          }
          else CreateRoomRsp(rsp.roomId.toString, rsp.errCode, state, rsp.msg)
      }
    } else {
      Future.successful(CreateRoomRsp(errCode = 101, state = State.unknown, msg = "auth error"))
    }
  }

  override def joinRoom(request: JoinRoomReq): Future[SimpleRsp] = {
    log.info(s"joinRoom Called by [$request]")
    if (request.credit.nonEmpty && botAuth(request.credit.get.apiToken)) {
      state = State.in_game
      val joinRoomRsp: Future[JoinRoomRsp] = gameController.playGameActor ? (PlayGameActor.JoinRoomReq(request.roomId.toLong, request.password, _))
      joinRoomRsp.map {
        rsp =>
          if (rsp.errCode == 0) {
            log.info("joinRoomSuccess")
            SimpleRsp(0, state, "ok")
          }
          else {
            println(rsp)
            log.info(s"joinRoomFailed:${rsp.msg}")
            SimpleRsp(rsp.errCode, state, rsp.msg)
          }
      }
    } else {
      Future.successful(SimpleRsp(errCode = 101, state = State.unknown, msg = "auth error"))
    }

  }

  override def leaveRoom(request: Credit): Future[SimpleRsp] = {
    log.info(s"leaveRoom Called by [$request]")
    if (botAuth(request.apiToken)) {
      isFrameConnect = false
      isObservationConnect = false
      streamSender.foreach(s => s ! GrpcStreamActor.LeaveRoom)
      state = State.ended
      Future.successful {
        gameController.closeBotHolder
        ClientApp.botServer ! BotServer.Shutdown
        SimpleRsp(state = state, msg = "ok")
      }
    } else {
      Future.successful(SimpleRsp(errCode = 101, state = State.unknown, msg = "auth error"))
    }
  }

  override def actionSpace(request: Credit): Future[ActionSpaceRsp] = {
    log.info(s"actionSpace Called by [$request]")
    if (botAuth(request.apiToken)) {
      val rsp = ActionSpaceRsp(move = List(Move.up, Move.down, Move.left, Move.right, Move.r_up, Move.r_down, Move.l_up, Move.l_down, Move.noop), swing = true, fire = List(0,1), apply = List(0,1), state = BotServer.state, msg = "ok")
      Future.successful(rsp)
    } else {
      Future.successful(ActionSpaceRsp(errCode = 101, state = State.unknown, msg = "auth error"))
    }
  }

  override def action(request: ActionReq): Future[ActionRsp] = {
    log.info(s"action Called by [$request]")
    if (request.credit.nonEmpty && botAuth(request.credit.get.apiToken)) {
      gameController.gameActionReceiver(request)
      val rsp = ActionRsp(frameIndex = gameController.getCurFrame.toInt, state = state, msg = "ok")
      Future.successful(rsp)
    } else {
      Future.successful(ActionRsp(errCode = 101, state = State.unknown, msg = "auth error"))
    }
  }

  override def observation(request: Credit): Future[ObservationRsp] = {
    log.info(s"observation Called by [$request]")
    if (botAuth(request.apiToken)) {
      state = if (gameController.getGameState == GameState.play) State.in_game else State.killed
      val observationRsp: Future[ObservationRsp] = botViewActor ? BotViewActor.GetObservation
      observationRsp.map {
        observation =>
          ObservationRsp(observation.layeredObservation, observation.humanObservation, gameController.getCurFrame.toInt, 0, BotServer.state, "ok")
      }
    } else {
      Future.successful(ObservationRsp(errCode = 101, state = State.unknown, msg = "auth error"))
    }
  }

  override def inform(request: Credit): Future[InformRsp] = {
    if (botAuth(request.apiToken)) {
      state = if (gameController.getGameState == GameState.play) State.in_game else State.killed
      val rsp = InformRsp(gameController.getInform._1, gameController.getInform._2, gameController.getInform._3, gameController.getCurFrame.toInt, 0, BotServer.state, "ok")
      Future.successful(rsp)
    } else {
      Future.successful(InformRsp(errCode = 101, state = State.unknown, msg = "auth error"))
    }
  }

  override def reincarnation(request: Credit): Future[SimpleRsp] = {
    if (botAuth(request.apiToken)) {
      gameController.receiveReincarnation
      Future.successful(SimpleRsp(state = BotServer.state, msg = "ok"))
    } else {
      Future.successful(SimpleRsp(errCode = 101, state = State.unknown, msg = "auth error"))
    }
  }

  override def systemInfo(request: Credit): Future[SystemInfoRsp] = {
    if (botAuth(request.apiToken)) {
      Future.successful(SystemInfoRsp(framePeriod = frameDuration, state = BotServer.state, msg = "ok"))
    } else {
      Future.successful(SystemInfoRsp(errCode = 101, state = State.unknown, msg = "auth error"))
    }
  }


  override def currentFrame(request: Credit, responseObserver: StreamObserver[CurrentFrameRsp]): Unit = {
    if (botAuth(request.apiToken)) {
      isFrameConnect = true
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
      isObservationConnect = true
      if (streamSender.isDefined) {
        streamSender.get ! GrpcStreamActor.ObservationObserver(responseObserver)
      } else {
        streamSender = Some(system.spawn(GrpcStreamActor.create(gameController), "GrpcStreamActor"))
        streamSender.get ! GrpcStreamActor.ObservationObserver(responseObserver)
      }
    } else {
      responseObserver.onCompleted()
    }
  }
}
