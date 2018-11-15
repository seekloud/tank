package com.neo.sk.tank.core

import java.util.concurrent.atomic.AtomicLong

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.stream.{ActorAttributes, Supervision}
import akka.stream.scaladsl.Flow
import akka.util.ByteString
import com.neo.sk.tank.models.TankGameUserInfo
import com.neo.sk.tank.protocol.EsheepProtocol
import com.neo.sk.tank.protocol.ReplayProtocol.{GetRecordFrameMsg, GetUserInRecordMsg}
import com.neo.sk.tank.shared.protocol.TankGameEvent
import io.circe.{Decoder, Encoder}
import org.slf4j.LoggerFactory
import com.neo.sk.tank.Boot.{executor, scheduler, timeout}
import com.neo.sk.tank.common.Constants
import com.neo.sk.tank.core.RoomActor.TankRelive
import com.neo.sk.tank.core.UserActor.{ChangeUserInfo, TankRelive4UserActor}
/**
  * Created by hongruying on 2018/7/9
  */
object UserManager {

  import io.circe.generic.auto._
  import io.circe.syntax._
  import org.seekloud.byteobject.ByteObject._
  import org.seekloud.byteobject.MiddleBufferInJvm

  trait Command

  final case class ChildDead[U](name: String, childRef: ActorRef[U]) extends Command

  final case class GetWebSocketFlow(name:String,replyTo:ActorRef[Flow[Message,Message,Any]], playerInfo:Option[EsheepProtocol.PlayerInfo] = None, roomId:Option[Long] = None) extends Command

  final case class GetReplaySocketFlow(name: String, uid: String, rid: Long, wid:String, f:Int, replyTo: ActorRef[Flow[Message, Message, Any]]) extends Command

  final case class GetWebSocketFlow4WatchGame(roomId:Long, watchedUserId:String, replyTo:ActorRef[Flow[Message,Message,Any]], playerInfo:Option[EsheepProtocol.PlayerInfo] = None) extends Command

  private val log = LoggerFactory.getLogger(this.getClass)

  def create(): Behavior[Command] = {
    log.debug(s"UserManager start...")
    Behaviors.setup[Command] {
      ctx =>
        Behaviors.withTimers[Command] {
          implicit timer =>
            val uidGenerator = new AtomicLong(1L)
            idle(uidGenerator)
        }
    }
  }

  private def idle(uidGenerator: AtomicLong)
                  (
                    implicit timer: TimerScheduler[Command]
                  ): Behavior[Command] = {
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
//        case GetWebSocketFlow(name, replyTo) =>
//          replyTo ! getWebSocketFlow(0,getUserActor(ctx, uidGenerator.getAndIncrement(), name))
//          Behaviors.same

        case GetReplaySocketFlow(name, uid, rid, wid, f, replyTo) =>
          getUserActorOpt(ctx, uid) match {
            case Some(userActor) =>
              //remind 进入等待状态
              userActor ! UserActor.ChangeBehaviorToInit
            case None =>
          }
          val userActor = getUserActor(ctx, uid, TankGameUserInfo(uid, name, name, true))
          replyTo ! getWebSocketFlow(userActor)
          userActor ! ChangeUserInfo(TankGameUserInfo(uid,name,name,true))
          userActor ! UserActor.StartReplay(rid, wid, f)
          Behaviors.same



        case GetWebSocketFlow(name,replyTo, playerInfoOpt, roomIdOpt) =>
          println(s"ssssss${playerInfoOpt},${roomIdOpt}")
          val playerInfo = playerInfoOpt match {
            case Some(p) => TankGameUserInfo(p.playerId, p.nickname, name, true)
            case None => TankGameUserInfo(Constants.TankGameUserIdPrefix + s"-${uidGenerator.getAndIncrement()}", s"guest:${name}", name, false)
          }
          getUserActorOpt(ctx, playerInfo.userId) match {
            case Some(userActor) =>
              userActor ! UserActor.ChangeBehaviorToInit
            case None =>
          }
          val userActor = getUserActor(ctx, playerInfo.userId, playerInfo)
          replyTo ! getWebSocketFlow(userActor)
          userActor ! ChangeUserInfo(playerInfo)
          userActor ! UserActor.StartGame(roomIdOpt)
          Behaviors.same


        case GetWebSocketFlow4WatchGame(roomId, watchedUserId, replyTo, playerInfoOpt) =>
          //观战用户建立Actor由userManager监管，消息来自HttpService
          val playerInfo = playerInfoOpt match {
            case Some(p) => TankGameUserInfo(p.playerId, p.nickname, p.nickname, true)
            case None => TankGameUserInfo(Constants.TankGameUserIdPrefix + s"-${uidGenerator.getAndIncrement()}", s"guest:observer", s"guest:observer", false)
          }
          getUserActorOpt(ctx, playerInfo.userId) match {
            case Some(userActor) =>
              userActor ! UserActor.ChangeBehaviorToInit
            case None =>
          }
          val userActor = getUserActor(ctx, playerInfo.userId, playerInfo)
          replyTo ! getWebSocketFlow(userActor)
          userActor ! ChangeUserInfo(playerInfo)
          //发送用户观战命令
          userActor ! UserActor.StartObserve(roomId, watchedUserId)
          Behaviors.same

        case msg:GetUserInRecordMsg=>
          getUserActor(ctx,msg.watchId,
            TankGameUserInfo(msg.watchId,
              msg.watchId.toString,msg.watchId.toString,false)
          ) ! msg
          Behaviors.same

        case msg:GetRecordFrameMsg=>
          getUserActor(ctx,msg.watchId,
            TankGameUserInfo(msg.watchId,msg.watchId.toString,msg.watchId.toString,false)
          ) ! msg
          Behaviors.same

        case msg:TankRelive4UserActor =>
          //todo
          getUserActor(ctx,msg.userId,
            TankGameUserInfo(msg.userId,msg.name,msg.name,msg.userId.take(4) == "user")) ! TankRelive4UserActor(msg.tank,msg.userId,msg.name,msg.roomActor,msg.config)
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

  private def getWebSocketFlow(userActor: ActorRef[UserActor.Command]): Flow[Message, Message, Any] = {
    import scala.language.implicitConversions
    import org.seekloud.byteobject.ByteObject._


    implicit def parseJsonString2WsMsgFront(s: String): Option[TankGameEvent.WsMsgFront] = {
      import io.circe.generic.auto._
      import io.circe.parser._

      try {
        val wsMsg = decode[TankGameEvent.WsMsgFront](s).right.get
        Some(wsMsg)
      } catch {
        case e: Exception =>
          log.warn(s"parse front msg failed when json parse,s=${s}")
          None
      }
    }

    Flow[Message]
      .collect {
        case TextMessage.Strict(m) =>
          UserActor.WebSocketMsg(m)

        case BinaryMessage.Strict(m) =>
          val buffer = new MiddleBufferInJvm(m.asByteBuffer)
          bytesDecode[TankGameEvent.WsMsgFront](buffer) match {
            case Right(req) => UserActor.WebSocketMsg(Some(req))
            case Left(e) =>
              log.error(s"decode binaryMessage failed,error:${e.message}")
              UserActor.WebSocketMsg(None)
          }
      }.via(UserActor.flow(userActor))
      .map {
        case t: TankGameEvent.Wrap =>
          BinaryMessage.Strict(ByteString(t.ws))

        case t: TankGameEvent.ReplayFrameData =>
          BinaryMessage.Strict(ByteString(t.ws))

        case x =>
          log.debug(s"akka stream receive unknown msg=${x}")
          TextMessage.apply("")
      }.withAttributes(ActorAttributes.supervisionStrategy(decider))

  }

  private val decider: Supervision.Decider = {
    e: Throwable =>
      e.printStackTrace()
      log.error(s"WS stream failed with $e")
      Supervision.Resume
  }





  private def getUserActor(ctx: ActorContext[Command],id:String, userInfo: TankGameUserInfo):ActorRef[UserActor.Command] = {
    val childName = s"UserActor-${id}"
    ctx.child(childName).getOrElse{
      val actor = ctx.spawn(UserActor.create(id, userInfo),childName)
      ctx.watchWith(actor,ChildDead(childName,actor))
      actor
    }.upcast[UserActor.Command]
  }

  private def getUserActorOpt(ctx: ActorContext[Command],id:String):Option[ActorRef[UserActor.Command]] = {
    val childName = s"UserActor-${id}"
    ctx.child(childName).map(_.upcast[UserActor.Command])
  }

}
