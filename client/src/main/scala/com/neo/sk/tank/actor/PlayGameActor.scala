package com.neo.sk.tank.actor

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{Behaviors, StashBuffer, TimerScheduler}
import akka.http.scaladsl.Http
/**
  * Created by hongruying on 2018/10/23
  * 连接游戏服务器的websocket Actor
  */
object PlayGameActor {
  sealed trait Command

  /**进入游戏连接参数*/
  def create()={
    Behaviors.setup[Command]{ctx=>
      implicit val stashBuffer = StashBuffer[Command](Int.MaxValue)
      Behaviors.withTimers[Command]{timer=>
        init()
      }

    }
  }

  def init()(
    implicit stashBuffer: StashBuffer[Command],
    timer:TimerScheduler[Command])={
    Behaviors.receive[Command]{(ctx,msg)=>
      msg match {
        case x=>
          Behaviors.unhandled
      }
    }
  }

  def play(implicit stashBuffer: StashBuffer[Command],
           timer:TimerScheduler[Command])={
    Behaviors.receive[Command]{(ctx,msg)=>
      msg match {
        case x=>
          Behaviors.unhandled
      }
    }
  }
}
