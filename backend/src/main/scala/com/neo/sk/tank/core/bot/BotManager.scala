package com.neo.sk.tank.core.bot

import java.util.concurrent.atomic.AtomicLong

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import com.neo.sk.tank.common.AppSettings.{nameList, needSpecialName}
import com.neo.sk.tank.core.game.GameContainerServerImpl
import com.neo.sk.tank.shared.protocol.TankGameEvent.WsMsgSource
import org.slf4j.LoggerFactory

import scala.collection.mutable
import com.neo.sk.tank.Boot.{executor, roomManager, scheduler, timeout}
import com.neo.sk.tank.common.AppSettings
import com.neo.sk.tank.core.RoomActor
/**
  * Created by sky
  * Date on 2019/1/10
  * Time at 下午8:59
  */
object BotManager {

  object StopMap {
    val stop: Byte = 1
    val delete: Byte = 2
  }

  val minSize=AppSettings.botLimit

  trait Command

  final case class SysUserSize(roomId: Long, size: Int,gameContainer: GameContainerServerImpl) extends Command

  final case class StopBot(bid: String, state: Byte) extends Command with BotActor.Command

  final case class ReliveBot(bid: String) extends Command with BotActor.Command

  final case class ChildDead[U](name: String, childRef: ActorRef[U]) extends Command

  final case class AddBotSuccess(roomId: Long, bid: String) extends Command

  private val log = LoggerFactory.getLogger(this.getClass)

  def create(): Behavior[Command] = {
    log.debug(s"BotManager start...")
    Behaviors.setup[Command] {
      ctx =>
        Behaviors.withTimers[Command] {
          implicit timer =>
            val bidGenerator = new AtomicLong(1L)
            idle(bidGenerator, mutable.HashMap[String,(Long,Boolean)]())
        }
    }
  }

  private def idle(bidGenerator: AtomicLong,
                   botMap:mutable.HashMap[String,(Long,Boolean)]  //(bid,(room,success))
                  )
                  (
                    implicit timer: TimerScheduler[Command]
                  ): Behavior[Command] = {
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case msg: SysUserSize =>
          val botList=botMap.filter(r=>r._2._1== msg.roomId&&r._2._2)
          if(msg.size-botList.size>0){
            if(msg.size < minSize){
              for (i <- msg.size until minSize) {
                val botId = bidGenerator.getAndIncrement()
                log.info(s"room add BotActor ${msg.roomId} $botId")
                getBotActor(ctx, s"BotActor-$botId", Some(msg.gameContainer), Some(msg.roomId))
                botMap.put(s"BotActor-$botId",(msg.roomId,false))
              }
            }else{
              if(botList.nonEmpty){
                botList.take(msg.size-minSize).foreach{r=>
                  if(ctx.child(r._1).nonEmpty){
                    getBotActor(ctx,r._1) ! StopBot(r._1,StopMap.delete)
                  }else{
                    log.debug(s"here bot error ${r._1}")
                  }
                }
              }
            }
          }
          //remind 此处设置房间没有用户清除bot
          /*else{
            botList.foreach(r=>
              if(ctx.child(r._1).nonEmpty){
                getBotActor(ctx,r._1) ! StopBot(r._1,StopMap.delete)
              }else{
                log.debug(s"here bot error ${r._1}")
              })
          }*/
          Behaviors.same


        case msg: AddBotSuccess =>
          botMap.update(msg.bid,(msg.roomId,true))
          Behaviors.same

        case msg: StopBot =>
          if(ctx.child(msg.bid).nonEmpty){
            getBotActor(ctx, msg.bid) ! msg
          }else{
            log.debug(s"here bot error ${msg.bid}")
          }
          Behaviors.same

        case msg: ReliveBot =>
          if(ctx.child(msg.bid).nonEmpty) {
            getBotActor(ctx, msg.bid) ! msg
          }else{
            log.debug(s"here bot error ${msg.bid}")
          }
          Behaviors.same

        case ChildDead(child, childRef) =>
          log.info(s"botMap remove $child")
          botMap.remove(child)
          ctx.unwatch(childRef)
          Behaviors.same

        case unknow =>
          log.error(s"${ctx.self.path} recv a unknow msg when idle:${unknow}")
          Behaviors.same
      }
    }
  }

  private def getBotActor(ctx: ActorContext[Command], id: String, gameContainer: Option[GameContainerServerImpl] = None, roomId: Option[Long] = None): ActorRef[BotActor.Command] = {
  val botName = if (needSpecialName) generateName() else id
    ctx.child(id).getOrElse {
      val actor = ctx.spawn(BotActor.create(id, botName, gameContainer.get, roomId.get), id)
      ctx.watchWith(actor, ChildDead(id, actor))
      actor
    }.upcast[BotActor.Command]
  }

  private def generateName(): String = {
    val idx = (new util.Random).nextInt(nameList.size())
    nameList.get(idx)
  }

}
