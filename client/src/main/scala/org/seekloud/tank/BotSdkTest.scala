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

import io.grpc.{ManagedChannel, ManagedChannelBuilder}
import org.seekloud.esheepapi.pb.api._
import org.seekloud.esheepapi.pb.service.EsheepAgentGrpc
import org.seekloud.esheepapi.pb.service.EsheepAgentGrpc.EsheepAgentStub

import scala.concurrent.Future

/**
  * Created by sky
  * Date on 2019/3/14
  * Time at 上午11:00
  */
object BotSdkTest {
  val host = "127.0.0.1"
  val port = 8008
  val pId = "test"
  val apiToken = "test"

  private[this] val channel: ManagedChannel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build
  private val esheepStub: EsheepAgentStub = EsheepAgentGrpc.stub(channel)
  val credit = Credit( apiToken = apiToken)

  def createRoom(password:String): Future[CreateRoomRsp] = esheepStub.createRoom(CreateRoomReq(Some(credit),password))

  def joinRoom():Future[SimpleRsp]= esheepStub.joinRoom(JoinRoomReq(Some(credit),"3","test"))


  def test: Unit = {
    createRoom("test")
//    joinRoom()
  }

  def main(args: Array[String]): Unit = {
    createRoom("test")
  }
}
