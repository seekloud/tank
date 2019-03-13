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

package org.seekloud.tank.core

import akka.actor.ActorSystem
import akka.actor.typed.{ActorRef, Behavior}
import com.google.protobuf.ByteString
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import org.slf4j.LoggerFactory
import org.seekloud.pb.api.ObservationRsp
import org.seekloud.pb.observations.{ImgData, LayeredObservation}
import org.seekloud.tank.common.AppSettings
import org.seekloud.tank.common.Constants
import org.seekloud.tank.core.PlayGameActor.DispatchMsg
import org.seekloud.tank.model.JoinRoomRsp
import org.seekloud.tank.game.control.BotPlayController
import org.seekloud.tank.shared.protocol.TankGameEvent
import org.seekloud.tank.shared.protocol.TankGameEvent.CreateRoom
/**
  * Created by sky
  * Date on 2019/3/11
  * Time at 上午10:44
  * 管理bot observation 数据
  */
object BotViewActor {
  private val log = LoggerFactory.getLogger(this.getClass)

  sealed trait Command

  case class GetByte(locationByte: Array[Byte], mapByte: Array[Byte], immutableByte: Array[Byte], mutableByte: Array[Byte], bodiesByte: Array[Byte], stateByte: Array[Byte], viewByte: Option[Array[Byte]]) extends Command

  case class GetViewByte(viewByte: Array[Byte]) extends Command

  case class GetObservation(sender:ActorRef[ObservationRsp]) extends Command

  case class CreateRoomReq(password:String,sender:ActorRef[JoinRoomRsp]) extends Command

  case class JoinRoomReq(roomId:Long,password:String,sender:ActorRef[JoinRoomRsp]) extends Command


  def create(): Behavior[Command] = {
    Behaviors.setup[Command] {
      _ =>
        idle(Array[Byte](), Array[Byte](), Array[Byte](), Array[Byte](), Array[Byte](), Array[Byte](), Some(Array[Byte]()))
    }
  }

  def idle(locationByte: Array[Byte], mapByte: Array[Byte], immutableByte: Array[Byte], mutableByte: Array[Byte], bodiesByte: Array[Byte], stateByte: Array[Byte], viewByte: Option[Array[Byte]]): Behavior[Command] = {
    Behaviors.receive[Command] {
      (ctx, msg) =>
        msg match {
          case x =>
            println(s"get unKnow msg $x")
            Behaviors.unhandled
          case t: CreateRoomReq =>
            BotPlayController.SDKReplyTo = t.sender
            BotPlayController.serverActors.foreach(
              a =>
                a ! DispatchMsg(TankGameEvent.CreateRoom(None, Some(t.password)))
            )
            Behaviors.same
          case t: JoinRoomReq =>
            BotPlayController.SDKReplyTo = t.sender
            BotPlayController.serverActors.foreach(
              a =>
                a ! DispatchMsg(TankGameEvent.StartGame(Some(t.roomId),Some(t.password)))
            )
            Behaviors.same
          case t: GetObservation =>
            val pixel = if (mapByte.isEmpty) 0 else if (AppSettings.isGray) 1 else 4
            val layer = LayeredObservation(
              Some(ImgData(400,200, pixel, ByteString.copyFrom(locationByte))),
              Some(ImgData(400,200, pixel, ByteString.copyFrom(mapByte))),
              Some(ImgData(400,200, pixel, ByteString.copyFrom(immutableByte))),
              Some(ImgData(400,200, pixel, ByteString.copyFrom(mutableByte))),
              Some(ImgData(400,200, pixel, ByteString.copyFrom(bodiesByte))),
              Some(ImgData(400,200, pixel, ByteString.copyFrom(stateByte)))
            )
            val observation = ObservationRsp(Some(layer), Some(ImgData(400, 200, pixel, ByteString.copyFrom(viewByte.get))))

            t.sender ! observation
            Behaviors.same


        }
    }
  }
}
