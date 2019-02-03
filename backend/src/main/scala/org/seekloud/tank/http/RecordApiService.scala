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

package org.seekloud.tank.http

import java.io.File

import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.FileIO
import org.seekloud.tank.protocol.EsheepProtocol._
import org.seekloud.tank.protocol.RecordApiProtocol._
import org.seekloud.tank.protocol.ReplayProtocol.ChangeRecordMsg
import org.seekloud.tank.protocol.{EsheepProtocol, ReplayProtocol}
import org.seekloud.tank.Boot.{esheepSyncClient, userManager}
import org.seekloud.tank.core.EsheepSyncClient
import org.seekloud.tank.models.DAO.RecordDAO
import org.seekloud.tank.protocol.{CommonErrorCode, EsheepProtocol, ReplayProtocol}
import org.seekloud.tank.shared.ptcl.{CommonRsp, ErrorRsp, SuccessRsp}
import org.seekloud.utils.SecureUtil.generateSignature
import org.slf4j.LoggerFactory

import concurrent.duration._
import org.seekloud.tank.Boot.{executor, scheduler, timeout}

import scala.concurrent.Future
import akka.actor.typed.scaladsl.AskPattern._

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  *
  * 提供获取游戏录像列表的接口
  * 提供code接口
  * 提供游戏下载接口
  */


trait RecordApiService extends ServiceUtils {

  import io.circe._
  import io.circe.generic.auto._

  private val log = LoggerFactory.getLogger(getClass)

  private def getGameRecErrorRsp(msg: String) = ErrorRsp(1000020, msg)

  private val getRecordList = (path("getRecordList") & post) {
    dealPostReq[GetGameRecReq] { req =>
      RecordDAO.queryAllRec(req.lastRecordId, req.count).map { r =>
        val temp = r.groupBy(_._1)
        var tempList = List.empty[GameRec]
        for ((k, v) <- temp) {
          if (v.head._2.nonEmpty)
            tempList = tempList :+ GameRec(k.recordId, k.roomId, k.startTime, k.endTime, v.length, v.map(r => (r._2.get.userId,r._2.get.userNickname)))
          else tempList = tempList :+ GameRec(k.recordId, k.roomId, k.startTime, k.endTime, 0, List())
        }
        complete(GetGameRecRsp(Some(tempList.sortBy(_.recordId))))
      }.recover {
        case e: Exception =>
          log.debug(s"获取游戏录像失败，recover error:$e")
          complete(getGameRecErrorRsp(s"获取游戏录像失败，recover error:$e"))
      }
    }
  }

  private val getRecordListByTime = (path("getRecordListByTime") & post) {
    dealPostReq[GetGameRecByTimeReq] { req =>
      RecordDAO.queryRecByTime(req.startTime, req.endTime, req.lastRecordId, req.count).map { r =>
        val temp = r.groupBy(_._1)
        var tempList = List.empty[GameRec]
        for ((k, v) <- temp) {
          if (v.head._2.nonEmpty)
            tempList = tempList :+ GameRec(k.recordId, k.roomId, k.startTime, k.endTime, v.length,  v.map(r => (r._2.get.userId,r._2.get.userNickname)))
          else tempList = tempList :+ GameRec(k.recordId, k.roomId, k.startTime, k.endTime, 0, List())
        }
        complete(GetGameRecRsp(Some(tempList.sortBy(_.recordId))))
      }.recover {
        case e: Exception =>
          log.debug(s"获取游戏录像失败，recover error:$e")
          complete(getGameRecErrorRsp(s"获取游戏录像失败，recover error:$e"))
      }
    }
  }

  private val getRecordListByPlayer = (path("getRecordListByPlayer") & post) {
    dealPostReq[GetGameRecByPlayerReq] { req =>
      RecordDAO.queryRecByPlayer(req.playerId, req.lastRecordId, req.count).map { r =>
        val temp = r.groupBy(_._1)
        var tempList = List.empty[GameRec]
        for ((k, v) <- temp) {
          if (v.head._2.nonEmpty)
            tempList = tempList :+ GameRec(k.recordId, k.roomId, k.startTime, k.endTime, v.length,  v.map(r => (r._2.get.userId,r._2.get.userNickname)))
          else tempList = tempList :+ GameRec(k.recordId, k.roomId, k.startTime, k.endTime, 0, List())
        }
        complete(GetGameRecRsp(Some(tempList.sortBy(_.recordId))))
      }.recover {
        case e: Exception =>
          log.debug(s"获取游戏录像失败，recover error:$e")
          complete(getGameRecErrorRsp(s"获取游戏录像失败，recover error:$e"))
      }
    }
  }

  private val downloadRecord = (path("downloadRecord")) {
    parameter('code) { code =>
      dealFutureResult(checkCode(code).map { rsp =>
        if (rsp) {
          dealPostReq[DownloadRecordReq] { req =>
            RecordDAO.getFilePath(req.recordId).map { r =>
              val fileName = r.head
              val f = new File(fileName)
              if (f.exists()) {
                val responseEntity = HttpEntity(
                  ContentTypes.`application/octet-stream`,
                  f.length,
                  FileIO.fromPath(f.toPath, chunkSize = 262144))
                complete(responseEntity)
              } else complete(getGameRecErrorRsp("file not exist"))
            }.recover {
              case e: Exception =>
                log.debug(s"获取游戏录像失败，recover error:$e")
                complete(getGameRecErrorRsp(s"获取游戏录像失败，recover error:$e"))
            }
          }
        } else {
          complete(getGameRecErrorRsp(s"code验证失败"))
        }
      })
    }
  }

  private def generateCode(appId: String, secureKey: String) = {
    val timeStamp = System.currentTimeMillis()
    val tokenInfo: Future[EsheepProtocol.GameServerKey2TokenRsp] = esheepSyncClient ? (e => EsheepSyncClient.VerifyToken(e))
    tokenInfo.flatMap { r =>
      val sList = appId :: r.data.get.token :: timeStamp.toString :: Nil
      val code = generateSignature(sList, secureKey)
      val deadline = timeStamp + 1000 * 60
      RecordDAO.insertCodeForDownload(deadline, code).map { r =>
        if (r > 0) code
        else ""
      }
    }
  }

  private def checkCode(code: String) = {
    val nowTime = System.currentTimeMillis()
    RecordDAO.selectCodeForDownload(nowTime).map { r =>
      if (r.contains(code)) true else false
    }
  }

  def deleteCode = {
    val nowTime = System.currentTimeMillis()
    RecordDAO.deleteCodeForDownload(nowTime).onComplete {
      case Success(v) => println("delete successfully")
      case Failure(ex) => println("delete not completed")
    }
  }

  private val token2Code = (path("token2code") & get & pathEndOrSingleSlash) {
    dealFutureResult(generateCode("tank", "sjdakhjskJHK7768G76sdksdkasHU").map(r =>
      if (r != "") complete(EsheepProtocol.CodeForDownloadRsp(r))
      else complete(ErrorRsp(100021, "generate code error"))
    ))
  }


  private val changeRecord = (path("changeRecord") & post) {
    entity(as[Either[Error, ChangeRecordReq]]) {
      case Right(req) =>
        userManager ! ChangeRecordMsg(req.recordId, req.watchId, req.playerId, req.frame)
        complete(SuccessRsp())
      case Left(e) =>
        complete(CommonErrorCode.parseJsonError)
    }
  }

  private val getRecordFrame = (path("getRecordFrame") & post) {
    dealPostReq[GetRecordFrameReq] { req =>
      val flowFuture: Future[CommonRsp] = userManager ? (ReplayProtocol.GetRecordFrameMsg(req.recordId, req.playerId, _))
      flowFuture.map {
        case r: GetRecordFrameRsp =>
          complete(r)
        case r: ErrorRsp =>
          complete(r)
        case _ =>
          complete(ErrorRsp(10003, "init error"))
      }.recover {
        case e: Exception =>
          log.debug(s"获取游戏录像失败，recover error:$e")
          complete(ErrorRsp(10004, "init error"))
      }
    }

    /*entity(as[Either[Error,GetRecordFrameReq]]){
      case Right(req)=>
        val flowFuture:Future[CommonRsp]=userManager ? (ReplayProtocol.GetRecordFrameMsg(req.recordId,req.playerId,_))
        dealFutureResult{
          flowFuture.map {
            case r: GetRecordFrameRsp =>
              complete(r)
            case _=>
              complete(ErrorRsp(10001,"init error"))
          }
        }

      case Left(e)=>
        complete(CommonErrorCode.parseJsonError)
    }*/
  }

  private val getRecordPlayerList = (path("getRecordPlayerList") & post) {
    dealPostReq[GetUserInRecordReq] { req =>
      val flowFuture: Future[CommonRsp] = userManager ? (ReplayProtocol.GetUserInRecordMsg(req.recordId, req.playerId, _))
      flowFuture.map {
        case r: GetUserInRecordRsp =>
          complete(r)
        case r: ErrorRsp =>
          complete(r)
        case _ =>
          complete(ErrorRsp(10003, "init error"))
      }.recover {
        case e: Exception =>
          log.debug(s"获取游戏录像失败，recover error:$e")
          complete(ErrorRsp(10004, "init error"))
      }
    }
    /*    entity(as[Either[Error,GetUserInRecordReq]]){
          case Right(req)=>
            val flowFuture:Future[CommonRsp]=userManager ? (ReplayProtocol.GetUserInRecordMsg(req.recordId,req.playerId,_))
            dealFutureResult(
            flowFuture.map(r=>complete(r))
            )
          case Left(e)=>
            complete(CommonErrorCode.parseJsonError)
        }*/
  }


  val GameRecRoutes: Route =
    getRecordList ~ getRecordListByTime ~ getRecordListByPlayer ~ downloadRecord ~ token2Code ~ changeRecord ~ getRecordFrame ~ getRecordPlayerList
}
