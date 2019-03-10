/*
 *  Copyright 2018 seekloud (https://github.com/seekloud)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.seekloud.tank.actor

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import org.seekloud.tank.model.TokenAndAcessCode
import org.seekloud.utils.EsheepClient
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.util.{Failure, Success}
import org.seekloud.tank.App._

/**
  * Created by hongruying on 2019/2/1
  */
object TokenActor {

  sealed trait Command
  final case object RefreshToken extends Command
  final case class  InitToken(token: String, tokenExpireTime: Long, playerId: String) extends Command
  final case class GetAccessCode(rsp:ActorRef[TokenAndAcessCode]) extends Command
  private final case object GetNewToken extends Command
  private val log = LoggerFactory.getLogger(this.getClass)
  private final case object RefreshTokenKey
  private final case object BehaviorChangeKey
  private final val refreshTime = 5.minutes
  private final val GetTokenTime = Some(5.minutes)

  case class TimeOut(msg:String) extends Command

  final case class SwitchBehavior(
                                   name: String,
                                   behavior: Behavior[Command],
                                   durationOpt: Option[FiniteDuration] = None,
                                   timeOut: TimeOut = TimeOut("busy time error")
                                 ) extends Command

  private[this] def switchBehavior(ctx: ActorContext[Command],
                                   behaviorName: String, behavior: Behavior[Command], durationOpt: Option[FiniteDuration] = None,timeOut: TimeOut  = TimeOut("busy time error"))
                                  (implicit stashBuffer: StashBuffer[Command],
                                   timer:TimerScheduler[Command]) = {

    timer.cancel(BehaviorChangeKey)
    durationOpt.foreach(timer.startSingleTimer(BehaviorChangeKey,timeOut,_))
    stashBuffer.unstashAll(ctx,behavior)
  }

  def create(): Behavior[Command] = {
    Behaviors.receive[Command]{ (ctx, msg) =>
      implicit val stashBuffer = StashBuffer[Command](Int.MaxValue)
      log.debug(s"${ctx.self.path} is starting...")
      msg match {
        case t:InitToken =>
          Behaviors.withTimers[Command] { implicit timer =>
            ctx.self ! RefreshToken
            work(t.token,t.tokenExpireTime,t.playerId)
          }
        case _ =>
          Behaviors.same
      }
    }
  }

  def work(token: String, tokenExpireTime:Long,playerId: String)
          (implicit stashBuffer: StashBuffer[Command],
            timer:TimerScheduler[Command]
          ): Behavior[Command] = {
    Behaviors.receive[Command]{ (ctx, msg) =>
      println("idle")
      msg match {

        case RefreshToken=>
          println("playerId--------" + playerId)
          timer.startSingleTimer(RefreshTokenKey, RefreshToken, refreshTime)
          ctx.self ! GetNewToken
          //刷新token
          Behaviors.same

        case GetNewToken =>
          EsheepClient.refreshToken(token,playerId).onComplete{
            case Success(rst) =>
              rst match {
                case Right(value) =>
                  ctx.self !  SwitchBehavior("work",work(value.token,value.expireTime,playerId))
                case Left(error) =>
                  //异常
                  timer.startSingleTimer(RefreshTokenKey, RefreshToken, 5.minutes)
                  log.error(s"GetNewToken error,error is ${error}")
              }
            case Failure(exception) =>
              //异常
              timer.startSingleTimer(RefreshTokenKey, RefreshToken, 5.minutes)
              log.warn(s" linkGameAgent failed, error:${exception.getMessage}")
          }
          switchBehavior(ctx, "busy", busy(), GetTokenTime, TimeOut("Get Token"))

        case GetAccessCode(rsp) =>
          EsheepClient.linkGameAgent(token,playerId).onComplete{
            case Success(rst) =>
              rst match {
                case Right(value) =>
                  rsp ! TokenAndAcessCode(token,tokenExpireTime,value.accessCode)
                case Left(error) =>
                  //异常
                  rsp ! TokenAndAcessCode("", 0l,"")
              }
            case Failure(exception) =>
              //异常
              log.warn(s" linkGameAgent failed, error:${exception.getMessage}")
              rsp ! TokenAndAcessCode("", 0l,"")
          }

          Behaviors.same





        case _ =>
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
          switchBehavior(ctx,name,behavior,durationOpt,timeOut)

        case TimeOut(m) =>
          log.debug(s"${ctx.self.path} is time out when busy,msg=${m}")
          Behaviors.stopped

        case unknowMsg =>
          stashBuffer.stash(unknowMsg)
          Behavior.same
      }
    }

}
