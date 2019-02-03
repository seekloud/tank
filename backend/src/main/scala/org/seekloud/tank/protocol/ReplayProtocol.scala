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

package org.seekloud.tank.protocol

import akka.actor.typed.ActorRef
import org.seekloud.tank.core.{GamePlayer, UserActor, UserManager}
import org.seekloud.tank.shared.ptcl.CommonRsp

/**
  * User: sky
  * Date: 2018/10/18
  * Time: 14:54
  */
object ReplayProtocol {

  final case class EssfMapKey(
                               tankId: Int,
                               userId: String,
                               name: String
                             )

  final case class EssfMapJoinLeftInfo(
                                        joinF: Long,
                                        leftF: Long
                                      )

  final case class EssfMapInfo(m: List[(EssfMapKey, EssfMapJoinLeftInfo)])

  /** Actor间信息 */
  final case class GetUserInRecordMsg(recordId: Long, watchId: String, replyTo: ActorRef[CommonRsp]) extends UserManager.Command with UserActor.Command with GamePlayer.Command

  final case class GetRecordFrameMsg(recordId: Long, watchId: String, replyTo: ActorRef[CommonRsp]) extends UserManager.Command with UserActor.Command with GamePlayer.Command

  final case class ChangeRecordMsg(rid: Long, watchId: String, playerId: String, f: Int) extends UserManager.Command with UserActor.Command

}