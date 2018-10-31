package com.neo.sk.tank.game

import com.neo.sk.tank.actor.PlayGameActor
import com.neo.sk.tank.controller.PlayScreenController
import com.neo.sk.tank.shared.protocol.TankGameEvent

/**
  * Created by hongruying on 2018/8/29
  * 计算网络时延
  */
final case class NetworkLatency(latency: Long)

trait NetworkInfo { this:PlayScreenController =>

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
