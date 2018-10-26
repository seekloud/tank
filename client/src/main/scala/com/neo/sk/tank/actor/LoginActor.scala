package com.neo.sk.tank.actor

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import com.neo.sk.tank.App
import com.neo.sk.tank.controller.LoginScreenController

/**
  * Created by hongruying on 2018/10/23
  */
object LoginActor {

  sealed trait Command

  final case object Login extends Command

  final case class Request(m: String) extends Command


  def create(controller: LoginScreenController): Behavior[Command] = {
    Behaviors.receive[Command]{ (ctx, msg) =>
      msg match {
        case Request(m) =>





          Behaviors.same


        case _ =>
          Behaviors.same

      }

    }
  }



}
