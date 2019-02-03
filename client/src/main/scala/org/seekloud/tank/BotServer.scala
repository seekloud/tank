//package org.seekloud.tank
//
//import scala.concurrent.Future
//
///**
//  * Created by hongruying on 2018/11/29
//  */
//object BotServer {
//
//}
//
//
//class BotService() extends EsheepAgent {
//  override def createRoom(request: Credit): Future[CreateRoomRsp] = {
//    println(s"createRoom Called by [$request")
//    val state = State.init_game
//    Future.successful(CreateRoomRsp(errCode = 101, state = state, msg = "ok"))
//  }
//
//  override def joinRoom(request: JoinRoomReq): Future[SimpleRsp] = {
//    println(s"joinRoom Called by [$request")
//    val state = State.in_game
//    Future.successful(SimpleRsp(errCode = 102, state = state, msg = "ok"))
//  }
//
//  override def leaveRoom(request: Credit): Future[SimpleRsp] = {
//    println(s"leaveRoom Called by [$request")
//    val state = State.ended
//    Future.successful(SimpleRsp(errCode = 103, state = state, msg = "ok"))
//  }
//
//  override def actionSpace(request: Credit): Future[ActionSpaceRsp] = {
//    println(s"actionSpace Called by [$request")
//    val rsp = ActionSpaceRsp()
//    Future.successful(rsp)
//  }
//
//  override def action(request: ActionReq): Future[ActionRsp] = {
//    println(s"action Called by [$request")
//    val rsp = ActionRsp()
//    Future.successful(rsp)
//  }
//
//  override def observation(request: Credit): Future[ObservationRsp] = {
//    println(s"action Called by [$request")
//    val rsp = ObservationRsp()
//    Future.successful(rsp)
//  }
//
//  override def inform(request: Credit): Future[InformRsp] = {
//    println(s"action Called by [$request")
//    val rsp = InformRsp()
//    Future.successful(rsp)
//  }
//}
