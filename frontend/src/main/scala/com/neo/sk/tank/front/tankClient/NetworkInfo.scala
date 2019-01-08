package com.neo.sk.tank.front.tankClient

import com.neo.sk.tank.front.tankClient.control.GameHolder
import com.neo.sk.tank.front.utils.{JsFunc, Shortcut}
import com.neo.sk.tank.shared.protocol.TankGameEvent

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

  val dataSizeMap:HashMap[String,Double]= HashMap[String,Double]()
  var dataSizeList:List[String]=Nil

  def ping(): Unit = {
    val curTime = System.currentTimeMillis()
    if (curTime - lastPingTime > 1000) {
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

  Shortcut.schedule(()=>{
    dataSizeList=dataSizeMap.toList.sortBy(_._1).map(r=>r._1+":"+{r._2/1000}.formatted("%.2f")+"kb/s")
    dataSizeMap.foreach(r=>dataSizeMap.update(r._1,0))
  },1000)

  protected def setDateSize(d: String,s:Double) = {
    dataSizeMap.get(d) match {
      case Some(m)=>
        dataSizeMap.update(d,m+s)
      case None=>
        dataSizeMap.put(d,s)
    }
  }

}
