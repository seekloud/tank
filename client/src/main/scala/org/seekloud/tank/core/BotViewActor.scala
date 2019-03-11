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
import org.slf4j.LoggerFactory

/**
  * Created by sky
  * Date on 2019/3/11
  * Time at 上午10:44
  * 管理bot observation 数据
  */
object BotViewActor {
  private val log = LoggerFactory.getLogger(this.getClass)

  sealed trait Command

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
        }
    }
  }
}
