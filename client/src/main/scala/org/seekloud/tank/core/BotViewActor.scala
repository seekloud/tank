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

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import com.google.protobuf.ByteString
import org.seekloud.tank.BotServer
import org.slf4j.LoggerFactory
import org.seekloud.pb.api.ObservationRsp
import org.seekloud.pb.observations.{ImgData, LayeredObservation}
import org.seekloud.tank.common.AppSettings
import org.seekloud.tank.core.PlayGameActor.DispatchMsg
import org.seekloud.tank.model.JoinRoomRsp
import org.seekloud.tank.game.control.BotViewController
import org.seekloud.tank.shared.protocol.TankGameEvent
/**
  * Created by sky
  * Date on 2019/3/11
  * Time at 上午10:44
  * 管理bot observation 数据
  */
object BotViewActor {
  private val log = LoggerFactory.getLogger(this.getClass)
  //fixme 此处需要放到配置文件
  private val windowWidth = 800
  private val windowHeight = 400
  sealed trait Command

  case class GetByte(locationByte: Array[Byte], mapByte: Array[Byte], immutableByte: Array[Byte], mutableByte: Array[Byte], bodiesByte: Array[Byte], stateByte: Array[Byte], viewByte: Option[Array[Byte]]) extends Command

  case class GetObservation(sender:ActorRef[ObservationRsp]) extends Command

  case class GetViewByte(viewByte: Array[Byte]) extends Command

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
          case m:GetByte=>
            val pixel = if (AppSettings.isGray) 1 else 4
            val layer = LayeredObservation(
              Some(ImgData(windowWidth, windowHeight, pixel, ByteString.copyFrom(locationByte))),
              Some(ImgData(windowWidth, windowHeight, pixel, ByteString.copyFrom(mapByte))),
              Some(ImgData(windowWidth, windowHeight, pixel, ByteString.copyFrom(immutableByte))),
              Some(ImgData(windowWidth, windowHeight, pixel, ByteString.copyFrom(mutableByte))),
              Some(ImgData(windowWidth, windowHeight, pixel, ByteString.copyFrom(bodiesByte))),
              Some(ImgData(windowWidth, windowHeight, pixel, ByteString.copyFrom(stateByte)))
            )
            val observation = ObservationRsp(Some(layer), if(viewByte.isDefined) Some(ImgData(windowWidth, windowHeight, pixel, ByteString.copyFrom(viewByte.get))) else None)
            if(BotServer.isObservationConnect) {
              BotServer.streamSender.get ! GrpcStreamActor.NewObservation(observation)
            }
            idle(m.locationByte,m.mapByte,m.immutableByte,m.mutableByte,m.bodiesByte,m.stateByte,m.viewByte)

          case t: GetObservation =>
            val pixel = if (AppSettings.isGray) 1 else 4
            val layer = LayeredObservation(
              Some(ImgData(windowWidth, windowHeight, pixel, ByteString.copyFrom(locationByte))),
              Some(ImgData(windowWidth, windowHeight, pixel, ByteString.copyFrom(mapByte))),
              Some(ImgData(windowWidth, windowHeight, pixel, ByteString.copyFrom(immutableByte))),
              Some(ImgData(windowWidth, windowHeight, pixel, ByteString.copyFrom(mutableByte))),
              Some(ImgData(windowWidth, windowHeight, pixel, ByteString.copyFrom(bodiesByte))),
              Some(ImgData(windowWidth, windowHeight, pixel, ByteString.copyFrom(stateByte)))
            )
            val observation = ObservationRsp(Some(layer), if(viewByte.isDefined) Some(ImgData(windowWidth, windowHeight, pixel, ByteString.copyFrom(viewByte.get))) else None)
            t.sender ! observation
            Behaviors.same

          case x =>
            log.debug(s"get unKnow msg $x")
            Behaviors.unhandled

        }
    }
  }
}
