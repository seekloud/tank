package com.neo.sk.tank

import akka.actor.ActorSystem
import akka.dispatch.MessageDispatcher
import akka.event.{Logging, LoggingAdapter}
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.neo.sk.tank.shared.model
import com.neo.sk.tank.shared.util.QuadTree
import org.seekloud.byteobject.MiddleBufferInJvm
import scala.language.implicitConversions
import org.seekloud.byteobject.ByteObject._
import akka.util.ByteString
/**
  * Created by hongruying on 2018/7/17
  */
import com.neo.sk.utils.SecureUtil.nonceStr
object Test {

  def main(args: Array[String]): Unit = {
    val a = nonceStr(1)
    val b = nonceStr(10)
    val c = nonceStr(30)
    val d = nonceStr(10000)
    val buffer = new MiddleBufferInJvm(204000)
    var before = System.currentTimeMillis()
    ByteString(a.fillMiddleBuffer(buffer).result())
    var after = System.currentTimeMillis()
    println(after - before)
    before = System.currentTimeMillis()
    ByteString(b.fillMiddleBuffer(buffer).result())
    after = System.currentTimeMillis()
    println(after - before)
    before = System.currentTimeMillis()
    ByteString(c.fillMiddleBuffer(buffer).result())
    after = System.currentTimeMillis()
    println(after - before)
    before = System.currentTimeMillis()
    ByteString(d.fillMiddleBuffer(buffer).result())
    after = System.currentTimeMillis()
    println(after - before)
  }

}
