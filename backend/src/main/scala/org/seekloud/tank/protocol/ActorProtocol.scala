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
import org.seekloud.tank.core._
import org.seekloud.tank.shared.ptcl.CommonRsp

/**
  * Created by sky
  * Date on 2019/3/14
  * Time at 下午9:18
  */
object ActorProtocol {
  /**加入游戏*/
  case class JoinRoom(uid:String,tankIdOpt:Option[Int],name:String,startTime:Long,userActor:ActorRef[UserActor.Command], roomIdOpt:Option[Long] = None, passwordOpt:Option[String] = None) extends RoomManager.Command with RoomActor.Command


  /** 回放 */
  final case class GetUserInRecordMsg(recordId: Long, watchId: String, replyTo: ActorRef[CommonRsp]) extends UserManager.Command with UserActor.Command with GamePlayer.Command

  final case class GetRecordFrameMsg(recordId: Long, watchId: String, replyTo: ActorRef[CommonRsp]) extends UserManager.Command with UserActor.Command with GamePlayer.Command

  final case class ChangeRecordMsg(rid: Long, watchId: String, playerId: String, f: Int) extends UserManager.Command with UserActor.Command
}
