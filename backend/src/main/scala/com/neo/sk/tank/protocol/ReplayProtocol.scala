package com.neo.sk.tank.protocol

import scala.collection.mutable

/**
  * User: sky
  * Date: 2018/10/18
  * Time: 14:54
  */
object ReplayProtocol {
  final case class EssfMapKey(
                               tankId: Int,
                               userId: Long,
                               name:String
                             )
  final case class EssfMapJoinLeftInfo(
                                        joinF: Long,
                                        leftF: Long
                                      )
  final case class EssfMapInfo(m:List[(EssfMapKey,EssfMapJoinLeftInfo)])
}
