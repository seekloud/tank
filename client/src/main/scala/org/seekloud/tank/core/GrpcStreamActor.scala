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

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import io.grpc.stub.StreamObserver
import org.seekloud.pb.api.{Credit, CurrentFrameRsp, ObservationRsp, ObservationWithInfoRsp}
import org.slf4j.LoggerFactory

/**
  * Created by sky
  * Date on 2019/3/10
  * Time at 下午4:47
  * 处理grpc传输
  */
object GrpcStreamActor {
  private[this] val log = LoggerFactory.getLogger(this.getClass)

  sealed trait Command

  case class NewFrame(frame: Long) extends Command

  case class FrameObserver(frameObserver: StreamObserver[CurrentFrameRsp]) extends Command

  case class ObservationObserver(observationObserver: StreamObserver[ObservationWithInfoRsp]) extends Command

  case class NewObservation(observation: ObservationRsp) extends Command

  case object LeaveRoom extends Command

  def create(): Behavior[Command] = {
    Behaviors.setup[Command] { ctx =>
      val fStream = new StreamObserver[CurrentFrameRsp] {
        override def onNext(value: CurrentFrameRsp): Unit = {}
        override def onCompleted(): Unit = {}
        override def onError(t: Throwable): Unit = {}
      }
      val oStream = new StreamObserver[ObservationWithInfoRsp] {
        override def onNext(value: ObservationWithInfoRsp): Unit = {}
        override def onCompleted(): Unit = {}
        override def onError(t: Throwable): Unit = {}
      }
//      working(gameController,fStream, oStream)
      Behaviors.same
    }
  }
}
