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

package org.seekloud.tank.front.tankClient

import org.seekloud.tank.front.tankClient.control.GameHolder
import org.seekloud.tank.front.utils.Shortcut
import org.seekloud.tank.shared.protocol.TankGameEvent
import org.seekloud.tank.front.utils.Shortcut

import scala.collection.mutable
import scala.collection.mutable.HashMap

/**
  * Created by hongruying on 2018/8/29
  * 计算网络时延
  */
final case class NetworkLatency(latency: Long)

trait NetworkInfo {
  this: GameHolder =>

  private var lastPingTime = System.currentTimeMillis()
  private val PingTimes = 10
  private var latency: Long = 0L
  private var receiveNetworkLatencyList: List[NetworkLatency] = Nil

  val dataSizeMap: HashMap[String, Double] = HashMap[String, Double]()
  var dataSizeList: List[String] = Nil

  def ping(): Unit = {
    val curTime = System.currentTimeMillis()
    if (curTime - lastPingTime > 10000) {
      startPing()
      lastPingTime = curTime
    }
  }

  private def startPing(): Unit = {
    this.sendMsg2Server(TankGameEvent.PingPackage(System.currentTimeMillis()))
  }

  protected def receivePingPackage(p: TankGameEvent.PingPackage): Unit = {
    receiveNetworkLatencyList = NetworkLatency(System.currentTimeMillis() - p.sendTime) :: receiveNetworkLatencyList
    if (receiveNetworkLatencyList.size < PingTimes) {
      Shortcut.scheduleOnce(() => startPing(), 10)
    } else {
      latency = receiveNetworkLatencyList.map(_.latency).sum / receiveNetworkLatencyList.size
      receiveNetworkLatencyList = Nil
    }
  }

  protected def getNetworkLatency = latency

  Shortcut.schedule(() => {
    println("allDataSize---" + (dataSizeMap.values.sum/1000).formatted("%.2f") + "kb/s")
    dataSizeList = dataSizeMap.toList.sortBy(_._1).map { r =>
      val s = r._1 + ":" + {
        r._2 / 1000
      }.formatted("%.2f") + "kb/s"
      println(s)
      s
    }
    dataSizeMap.foreach(r => dataSizeMap.update(r._1, 0))
  }, 1000)

  protected def setDateSize(d: String, s: Double) = {
    dataSizeMap.get(d) match {
      case Some(m) =>
        dataSizeMap.update(d, m + s)
      case None =>
        dataSizeMap.put(d, s)
    }
  }

}
