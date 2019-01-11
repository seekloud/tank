package com.neo.sk.tank.core.bot

import java.util.concurrent.atomic.AtomicLong

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import com.neo.sk.tank.common.AppSettings.{nameList, needSpecialName}
import com.neo.sk.tank.core.game.GameContainerServerImpl
import com.neo.sk.tank.shared.protocol.TankGameEvent.WsMsgSource
import org.slf4j.LoggerFactory

/**
  * Created by sky
  * Date on 2019/1/10
  * Time at 下午8:59
  */
object BotManager {
  object Stopmap{
    val stop=1
    val delete=2
  }

  trait Command

  final case class CreateBot(count:Int, roomId:Long, gameContainer: GameContainerServerImpl) extends Command
  final case class DeleteBot(size:Int, roomId:Long) extends Command
  final case class StopBot(bid:String,state:Byte) extends Command with BotActor.Command
  final case class ChildDead[U](name: String, childRef: ActorRef[U]) extends Command

  private val log = LoggerFactory.getLogger(this.getClass)

  def create(): Behavior[Command] = {
    log.debug(s"BotManager start...")
    Behaviors.setup[Command] {
      ctx =>
        Behaviors.withTimers[Command] {
          implicit timer =>
            val bidGenerator = new AtomicLong(1L)
            idle(bidGenerator,List[(String,Int)]())
        }
    }
  }

  private def idle(bidGenerator: AtomicLong,
                   botMap:List[(String,Int)]
                  )
                  (
                    implicit timer: TimerScheduler[Command]
                  ): Behavior[Command] = {
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case CreateBot(count,roomId,gameContainer) =>
          for(i <- 1 to count){
            val botId = bidGenerator.getAndIncrement()
            getBotActor(ctx,botId.toString,Some(gameContainer),Some(roomId))
          }
          Behaviors.same

        case msg:DeleteBot =>
          //todo 删除bot
          Behaviors.same

        case msg:StopBot=>
          getBotActor(ctx,msg.bid) ! msg
          Behaviors.same

        case ChildDead(child, childRef) =>
          ctx.unwatch(childRef)
          Behaviors.same

        case unknow =>
          log.error(s"${ctx.self.path} recv a unknow msg when idle:${unknow}")
          Behaviors.same
      }
    }
  }

  private def getBotActor(ctx: ActorContext[Command], id:String, gameContainer: Option[GameContainerServerImpl]=None,roomId:Option[Long]=None):ActorRef[BotActor.Command] = {
    val childName = s"BotActor-$id"
    val botName = if(needSpecialName) generateName() else s"tankBot-$id"
    ctx.child(childName).getOrElse{
      val actor = ctx.spawn(BotActor.create(s"tankBot-$id",botName,gameContainer.get,roomId.get), childName)
      ctx.watchWith(actor, ChildDead(childName, actor))
      actor
    }.upcast[BotActor.Command]
  }

  private def generateName():String = {
    val idx = (new util.Random).nextInt(nameList.size())
    nameList.get(idx)
  }

}
