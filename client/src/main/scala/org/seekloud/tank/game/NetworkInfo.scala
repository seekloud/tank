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

package org.seekloud.tank.game

import org.seekloud.tank.actor.PlayGameActor
import org.seekloud.tank.controller.PlayScreenController
import org.seekloud.tank.shared.protocol.TankGameEvent

/**
  * Created by hongruying on 2018/8/29
  * 计算网络时延
  */
final case class NetworkLatency(latency: Long)

trait NetworkInfo { this:GameController =>

  private var lastPingTime = System.currentTimeMillis()
  private val PingTimes = 10
  private var latency : Long = 0L
  private var receiveNetworkLatencyList : List[NetworkLatency] = Nil

  def ping():Unit = {
    val curTime = System.currentTimeMillis()
    if(curTime - lastPingTime > 1000){
      startPing()
      lastPingTime = curTime
    }
  }

  private def startPing():Unit = {
    this.playGameActor ! PlayGameActor.DispatchMsg(TankGameEvent.PingPackage(System.currentTimeMillis()))
  }

  protected def receivePingPackage(p:TankGameEvent.PingPackage):Unit = {
    receiveNetworkLatencyList = NetworkLatency(System.currentTimeMillis() - p.sendTime) :: receiveNetworkLatencyList
    if(receiveNetworkLatencyList.size < PingTimes){
      // todo 延时操作
//      Shortcut.scheduleOnce(() => startPing(),10)
    }else{
      latency = receiveNetworkLatencyList.map(_.latency).sum / receiveNetworkLatencyList.size
      receiveNetworkLatencyList = Nil
    }
  }

  protected def getNetworkLatency = latency



}
