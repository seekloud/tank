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
import akka.actor.typed.ActorRef
import io.grpc.{Server, ServerBuilder}
import org.seekloud.pb.api._
import org.seekloud.pb.service.EsheepAgentGrpc
import org.seekloud.pb.service.EsheepAgentGrpc.EsheepAgent
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by hongruying on 2018/11/29
  */
object BotServer {
  val log = LoggerFactory.getLogger(this.getClass)

/*  def build(
             port: Int,
             executionContext: ExecutionContext,
             wsClient: ActorRef[WSClient.WsCommand],
             gameController: GameController,
             gameMessageReceiver: ActorRef[WsMsgSource],
             stageCtx: StageContext
           ): Server = {
    log.info("Medusa gRPC Sever is building..")
    val service = new BotServer(wsClient, gameController, gameMessageReceiver, stageCtx)
    ServerBuilder.forPort(port).addService(
      EsheepAgentGrpc.bindService(service, executionContext)
    ).build
  }*/
}


class BotServer(

               ) extends EsheepAgent {
  override def createRoom(request: Credit): Future[CreateRoomRsp] = {
    println(s"createRoom Called by [$request")
    val state = State.init_game
    Future.successful(CreateRoomRsp(errCode = 101, state = state, msg = "ok"))
  }

  override def joinRoom(request: JoinRoomReq): Future[SimpleRsp] = {
    println(s"joinRoom Called by [$request")
    val state = State.in_game
    Future.successful(SimpleRsp(errCode = 102, state = state, msg = "ok"))
  }

  override def leaveRoom(request: Credit): Future[SimpleRsp] = {
    println(s"leaveRoom Called by [$request")
    val state = State.ended
    Future.successful(SimpleRsp(errCode = 103, state = state, msg = "ok"))
  }

  override def actionSpace(request: Credit): Future[ActionSpaceRsp] = {
    println(s"actionSpace Called by [$request")
    val rsp = ActionSpaceRsp()
    Future.successful(rsp)
  }

  override def action(request: ActionReq): Future[ActionRsp] = {
    println(s"action Called by [$request")
    val rsp = ActionRsp()
    Future.successful(rsp)
  }

  override def observation(request: Credit): Future[ObservationRsp] = {
    println(s"action Called by [$request")
    val rsp = ObservationRsp()
    Future.successful(rsp)
  }

  override def inform(request: Credit): Future[InformRsp] = {
    println(s"action Called by [$request")
    val rsp = InformRsp()
    Future.successful(rsp)
  }
}
