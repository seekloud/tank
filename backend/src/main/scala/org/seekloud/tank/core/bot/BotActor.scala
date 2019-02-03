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

package org.seekloud.tank.core.bot

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import org.seekloud.byteobject.MiddleBufferInJvm
import org.seekloud.tank.Boot.{botManager, roomManager}
import org.seekloud.tank.core.RoomActor
import org.seekloud.tank.core.game.{GameContainerServerImpl, TankServerImpl}
import org.slf4j.LoggerFactory

import concurrent.duration._
import org.seekloud.tank.Boot.executor
import org.seekloud.tank.core.bot.BotManager.{ReliveBot, StopBot, StopMap}
import org.seekloud.tank.shared.model.Constants.GameState

import scala.concurrent.duration.FiniteDuration

/**
  * Created by sky
  * Date on 2019/1/10
  * Time at 下午8:59
  */
object BotActor {
  object Keymap{
    val move="move"
    val key="key"
    val click="click"
  }

  private val log = LoggerFactory.getLogger(this.getClass)
  private final val InitTime = Some(5.minutes)
  private final val moveTime = 1.seconds
  private final val keyTime = 30.seconds
  private final val clickTime = 30.seconds

  trait Command

  case class JoinRoomSuccess(tank: TankServerImpl,roomActor:ActorRef[RoomActor.Command]) extends Command

  private final case object BehaviorChangeKey
  private final case object MoveTimeKey
  private final case object KeyTimeKey
  private final case object ClickTimeKey


  final case class SwitchBehavior(
                                   name: String,
                                   behavior: Behavior[Command],
                                   durationOpt: Option[FiniteDuration] = None,
                                   timeOut: TimeOut = TimeOut("busy time error")
                                 ) extends Command

  case class TimeOut(msg: String) extends Command

  private[this] def switchBehavior(ctx: ActorContext[Command],
                                   behaviorName: String, behavior: Behavior[Command], durationOpt: Option[FiniteDuration] = None, timeOut: TimeOut = TimeOut("busy time error"))
                                  (implicit stashBuffer: StashBuffer[Command],
                                   timer: TimerScheduler[Command]) = {
    log.debug(s"${ctx.self.path} becomes $behaviorName behavior.")
    timer.cancel(BehaviorChangeKey)
    durationOpt.foreach(timer.startSingleTimer(BehaviorChangeKey, timeOut, _))
    stashBuffer.unstashAll(ctx, behavior)
  }

  def create(bId: String,
             name: String,
             gameContainer: GameContainerServerImpl,
             roomId: Long): Behavior[Command] = {
    Behaviors.setup[Command] { ctx =>
      log.debug(s"BotActor is starting...${bId} ")
      implicit val stashBuffer = StashBuffer[Command](Int.MaxValue)
      Behaviors.withTimers[Command] { implicit timer =>
        implicit val sendBuffer = new MiddleBufferInJvm(8192)
        roomManager ! RoomActor.BotJoinRoom(bId,None,name,System.currentTimeMillis(),ctx.self,roomId)
        switchBehavior(ctx, "init", init(bId,name,gameContainer,roomId), InitTime, TimeOut("init"))
      }
    }
  }

  def init(bId: String,
           name: String,
           gameContainer: GameContainerServerImpl,
           roomId: Long)(
            implicit stashBuffer: StashBuffer[Command],
            sendBuffer: MiddleBufferInJvm,
            timer: TimerScheduler[Command]
          ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case msg:TimeOut=>
          log.debug(s"create bot error ${msg.msg} after $InitTime")
          Behaviors.stopped
        case msg: JoinRoomSuccess =>
          log.info(s"bot $bId add success")
          botManager ! BotManager.AddBotSuccess(roomId,bId)
          //fixme 之后将动作分离
          timer.startPeriodicTimer(MoveTimeKey,TimeOut(Keymap.move),moveTime)
//          timer.startPeriodicTimer(KeyTimeKey,TimeOut(Keymap.key),keyTime)
//          timer.startPeriodicTimer(ClickTimeKey,TimeOut(Keymap.click),clickTime)
          switchBehavior(ctx, "play", play(bId,name,BotControl(bId,msg.tank.tankId,name,roomId,msg.roomActor,gameContainer),roomId,msg.tank))
        case unknowMsg =>
          log.info(s"botactpr get unknow init $unknowMsg")
          stashBuffer.stash(unknowMsg)
          Behavior.same
      }
    }

  def play(id: String,
           name: String,
           bot:BotControl,
           roomId: Long,
           tank: TankServerImpl)(
            implicit stashBuffer: StashBuffer[Command],
            sendBuffer: MiddleBufferInJvm,
            timer: TimerScheduler[Command]
          ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case msg:TimeOut=>
          msg.msg match {
            //todo 动作定时操作
            case Keymap.move=>
              bot.sendMsg2Actor

            case Keymap.key=>

            case Keymap.click=>

            case _ =>
              log.debug(s"timeout match error$msg")

          }
          Behaviors.same

        case msg:StopBot=>
          msg.state match {
            case StopMap.stop=>
//              log.info(s"bot $id in room $roomId stop")
              bot.setGameState(GameState.stop)
              Behaviors.same
            case StopMap.delete=>
//              log.info(s"bot $id in room $roomId delete")
              bot.leftRoom
              Behaviors.stopped
          }

        case msg:ReliveBot=>
          bot.setGameState(GameState.play)
          Behaviors.same

        case unknowMsg =>
          stashBuffer.stash(unknowMsg)
          Behavior.same
      }
    }


}
