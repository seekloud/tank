package com.neo.sk.tank.http

import org.slf4j.LoggerFactory
import akka.http.scaladsl.server.Directives.{complete, _}
import akka.http.scaladsl.server.Route
import com.neo.sk.tank.protocol.{CommonErrorCode, EsheepProtocol, ReplayProtocol}
import com.neo.sk.tank.protocol.RecordApiProtocol._

import scala.language.postfixOps
import com.neo.sk.tank.models.DAO.RecordDAO

import scala.concurrent.ExecutionContext.Implicits.global
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.stream.scaladsl.{FileIO, Source}
import java.io.File

import com.neo.sk.tank.Boot.{esheepSyncClient, executor, scheduler, timeout, userManager}
import com.neo.sk.tank.core.EsheepSyncClient
import com.neo.sk.tank.shared.ptcl.ErrorRsp

import scala.concurrent.Future
import akka.actor.typed.scaladsl.AskPattern._
import com.neo.sk.tank.protocol.EsheepProtocol.{GetRecordFrameReq, GetRecordFrameRsp, GetUserInRecordReq, GetUserInRecordRsp}


/**
  *
  * 提供获取游戏录像列表的接口
  * 提供code接口
  * 提供游戏下载接口
  */


trait RecordApiService extends ServiceUtils{
  import io.circe._
  import io.circe.generic.auto._

  private val log = LoggerFactory.getLogger(getClass)
  private def getGameRecErrorRsp(msg:String) = ErrorRsp(1000020,msg)

  private val getRecordList = (path("getRecordList") & post){
    dealPostReq[GetGameRecReq]{req =>
      RecordDAO.queryAllRec(req.lastRecordId, req.count).map{r =>
        val temp = r.groupBy(_._1)
        var tempList = List.empty[GameRec]
        for((k, v) <- temp){
          if(v.head._2.nonEmpty)
            tempList = tempList :+ GameRec(k.recordId, k.roomId, k.startTime, k.endTime, v.length, v.map(r => r._2.get.userId))
          else tempList = tempList :+ GameRec(k.recordId, k.roomId, k.startTime, k.endTime, 0, List())
        }
        complete(GetGameRecRsp(Some(tempList.sortBy(_.recordId))))
      }.recover{
        case e:Exception =>
          log.debug(s"获取游戏录像失败，recover error:$e")
          complete(getGameRecErrorRsp(s"获取游戏录像失败，recover error:$e"))
      }
    }
  }

  private val getRecordListByTime = (path("getRecordListByTime") & post){
    dealPostReq[GetGameRecByTimeReq]{req =>
      RecordDAO.queryRecByTime(req.startTime, req.endTime, req.lastRecordId, req.count).map{r =>
        val temp = r.groupBy(_._1)
        var tempList = List.empty[GameRec]
        for((k, v) <- temp){
          if(v.head._2.nonEmpty)
            tempList = tempList :+ GameRec(k.recordId, k.roomId, k.startTime, k.endTime, v.length, v.map(r => r._2.get.userId))
          else tempList = tempList :+ GameRec(k.recordId, k.roomId, k.startTime, k.endTime, 0, List())
        }
        complete(GetGameRecRsp(Some(tempList.sortBy(_.recordId))))
      }.recover{
        case e:Exception =>
          log.debug(s"获取游戏录像失败，recover error:$e")
          complete(getGameRecErrorRsp(s"获取游戏录像失败，recover error:$e"))
      }
    }
  }

  private val getRecordListByPlayer = (path("getRecordListByPlayer") & post){
    dealPostReq[GetGameRecByPlayerReq]{req =>
      RecordDAO.queryRecByPlayer(req.playerId, req.lastRecordId, req.count).map{r =>
        val temp = r.groupBy(_._1)
        var tempList = List.empty[GameRec]
        for((k, v) <- temp){
          if(v.head._2.nonEmpty)
            tempList = tempList :+ GameRec(k.recordId, k.roomId, k.startTime, k.endTime, v.length, v.map(r => r._2.get.userId))
          else tempList = tempList :+ GameRec(k.recordId, k.roomId, k.startTime, k.endTime, 0, List())
        }
        complete(GetGameRecRsp(Some(tempList.sortBy(_.recordId))))
      }.recover{
        case e:Exception =>
          log.debug(s"获取游戏录像失败，recover error:$e")
          complete(getGameRecErrorRsp(s"获取游戏录像失败，recover error:$e"))
      }
    }
  }

  private val downloadRecord = (path("downloadRecord")){
    parameter('token){token =>
      val verifyTokenFutureRst: Future[EsheepProtocol.GameServerKey2TokenRsp] = esheepSyncClient ? (e => EsheepSyncClient.VerifyToken(e))
      dealFutureResult(verifyTokenFutureRst.map{rsp =>
        if(rsp.data.get.token == token){
          dealPostReq[DownloadRecordReq]{req =>
            RecordDAO.getFilePath(req.recordId).map{r =>
              val fileName = r.head
              val f = new File(fileName)
              if(f.exists()){
                val responseEntity = HttpEntity(
                  ContentTypes.`application/octet-stream`,
                  f.length,
                  FileIO.fromPath(f.toPath, chunkSize = 262144))
                complete(responseEntity)
              } else complete(getGameRecErrorRsp("file not exist"))
            }.recover{
              case e:Exception =>
                log.debug(s"获取游戏录像失败，recover error:$e")
                complete(getGameRecErrorRsp(s"获取游戏录像失败，recover error:$e"))
            }
          }
        }else{
          complete(getGameRecErrorRsp(s"token验证失败"))
        }
      })
      }
    }

  private val getRecordFrame=(path("getRecordFrame") & post){
    dealPostReq[GetRecordFrameReq]{req=>
      val flowFuture:Future[GetRecordFrameRsp]=userManager ? (ReplayProtocol.GetRecordFrameMsg(req.recordId,req.playerId,_))
      flowFuture.map(r=>complete(r))
    }
  }

  private val getRecordPlayerList=(path("getRecordPlayerList") & post){
    dealPostReq[GetUserInRecordReq]{req=>
      val flowFuture:Future[GetUserInRecordRsp]=userManager ? (ReplayProtocol.GetUserInRecordMsg(req.recordId,req.playerId,_))
      flowFuture.map(r=>complete(r))
    }
     /* entity(as[Either[Error,GetUserInRecordReq]]){
        case Right(req)=>
          val flowFuture:Future[GetUserInRecordRsp]=userManager ? (ReplayProtocol.GetUserInRecordMsg(req.recordId,req.playerId,_))
          dealFutureResult(
          flowFuture.map(r=>complete(r))
          )
        case Left(e)=>
          complete(CommonErrorCode.parseJsonError)
      }*/
  }



  val GameRecRoutes: Route =
    getRecordList ~ getRecordListByTime ~ getRecordListByPlayer ~ downloadRecord ~ getRecordFrame ~ getRecordPlayerList
}