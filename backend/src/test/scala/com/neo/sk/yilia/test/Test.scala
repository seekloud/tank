package com.neo.sk.yilia.test

import java.util.concurrent.atomic.AtomicLong

import akka.actor.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import akka.dispatch.MessageDispatcher
import akka.stream.ActorMaterializer
import akka.util.Timeout
import org.slf4j.LoggerFactory
import concurrent.duration._
import scala.concurrent.Future
import concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.FiniteDuration

/**
  * Created by hongruying on 2018/3/11
  */
object Test {

  private val log = LoggerFactory.getLogger(this.getClass)

//  val x = Future{
//    Thread.sleep(2000)
//    println("sssss")
//  }

//  val y = (0 to 10).toList.map(t => Future{
//    println("start",t)
//    Thread.sleep((10-t)*1000);
//    println("end",t)
//  t}.onComplete(t =>println(t)))


  sealed trait Command

  case class Hello(id:Long) extends Command

  case class TimeOut(msg:String) extends Command

  final case class SwitchBehavior(
                                   name: String,
                                   behavior: Behavior[Command],
                                   durationOpt: Option[FiniteDuration] = None,
                                   timeOut: TimeOut = TimeOut("busy time error")
                                 ) extends Command

  private val autoLonger = new AtomicLong(100L)

  private[this] def switchBehavior(ctx: ActorContext[Command],
                                   behaviorName: String, behavior: Behavior[Command], durationOpt: Option[FiniteDuration] = None,timeOut: TimeOut  = TimeOut("busy time error"))
                                  (implicit stashBuffer: StashBuffer[Command],
                                   timer:TimerScheduler[Command]) = {
    log.debug(s"${ctx.self.path} becomes $behaviorName behavior.")
    stashBuffer.unstashAll(ctx,behavior)
  }


  def create() = {
    Behaviors.setup[Command] {
      ctx =>
        log.debug(s"${ctx.self.path} App is starting...")
        implicit val stashBuffer = StashBuffer[Command](Int.MaxValue)
        Behaviors.withTimers[Command] { implicit timer =>
          busy()
        }
    }
  }

  def idle() = {
    Behaviors.receive[Command]{ (ctx,msg) =>
      msg match {
        case Hello(id) =>
          log.debug(s"${ctx.self.path} recv a msg=${msg}")
          Behaviors.same
      }

    }
  }

  private def busy()(
    implicit stashBuffer:StashBuffer[Command],
    timer:TimerScheduler[Command]
  ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case SwitchBehavior(name, behavior,durationOpt,timeOut) =>
          Thread.sleep(1000L)
          println("--------------")
          switchBehavior(ctx,name,behavior,durationOpt,timeOut)

        case TimeOut(m) =>
          log.debug(s"${ctx.self.path} is time out when busy,msg=${m}")
          switchBehavior(ctx,"stop",Behaviors.stopped)

        case unknowMsg =>
          stashBuffer.stash(unknowMsg)
          Behavior.same
      }
    }


  def main(args: Array[String]): Unit = {

    import akka.actor.typed.scaladsl.adapter._

    implicit val system = ActorSystem("mySystem")
    // the executor should not be the default dispatcher.
    implicit val executor: MessageDispatcher =
    system.dispatchers.lookup("akka.actor.my-blocking-dispatcher")

    implicit val materializer = ActorMaterializer()

    implicit val scheduler = system.scheduler

    implicit val timeout:Timeout = Timeout(20 seconds) // for actor asks

    val actor = system.spawn(create(),"test")

    actor ! Hello(id = autoLonger.getAndIncrement())
    actor ! Hello(id = autoLonger.getAndIncrement())
    actor ! Hello(id = autoLonger.getAndIncrement())
    actor ! SwitchBehavior("idle",idle())
    actor ! Hello(id = autoLonger.getAndIncrement())
    actor ! Hello(id = autoLonger.getAndIncrement())



//    val xxx = Future.sequence(List(Future{Thread.sleep(2000);println("sssss")},Future{Thread.sleep(1000);println("------")})).onComplete{
//      _ =>
//      println("end")
//    }
//    Thread.sleep(100000)

  }

}
