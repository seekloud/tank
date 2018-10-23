package com.neo.sk.tank.protocol

import akka.actor.typed.ActorRef
import com.neo.sk.tank.protocol.EsheepProtocol.{GetRecordFrameRsp, GetUserInRecordRsp}
import com.neo.sk.tank.core._
import scala.collection.mutable

/**
  * User: sky
  * Date: 2018/10/18
  * Time: 14:54
  */
object ReplayProtocol {
  final case class EssfMapKey(
                               tankId: Int,
                               userId: String,
                               name:String
                             )
  final case class EssfMapJoinLeftInfo(
                                        joinF: Long,
                                        leftF: Long
                                      )
  final case class EssfMapInfo(m:List[(EssfMapKey,EssfMapJoinLeftInfo)])

  /**Actor间查询信息*/
  final case class GetUserInRecordMsg(recordId:Long, watchId:String, replyTo:ActorRef[GetUserInRecordRsp]) extends UserManager.Command with UserActor.Command with GamePlayer.Command
  final case class GetRecordFrameMsg(recordId:Long, watchId:String, replyTo:ActorRef[GetRecordFrameRsp]) extends UserManager.Command with UserActor.Command with GamePlayer.Command
}
