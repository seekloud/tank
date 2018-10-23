package com.neo.sk.tank.http

import org.slf4j.LoggerFactory
import com.neo.sk.utils.HttpUtil
import akka.http.scaladsl.server.Directives.{complete, _}
import akka.http.scaladsl.server.Route
import scala.language.postfixOps
import com.neo.sk.tank.models.DAO.RecordDAO
import scala.concurrent.ExecutionContext.Implicits.global
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import com.neo.sk.tank.shared.ptcl.GameRecPtcl.{GetGameRecReq, GetGameRecByRoomReq, GetGameRecByPlayerReq, GetGameRecByIdReq, GetGameRecRsp, GameRec}
import com.neo.sk.tank.protocol.CommonErrorCode.parseJsonError
import com.neo.sk.tank.shared.ptcl.ErrorRsp
import scala.concurrent.Future



/**
  *
  * 提供获取游戏录像列表的本地接口
  */


trait GameRecService extends ServiceUtils{
  import io.circe._
  import io.circe.generic.auto._

  private val log = LoggerFactory.getLogger(getClass)
  private def getGameRecErrorRsp(msg:String) = ErrorRsp(1000020,msg)

  private val getGameRec = (path("getGameRec") & post){
    entity(as[Either[Error, GetGameRecReq]]){
      case Right(req) =>
        dealFutureResult(
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
        )
      case Left(e) =>
        log.error(s"json parse error: $e")
        complete(parseJsonError)
    }
  }

  private val getGameRecByRoom = (path("getGameRecByRoom") & post){
    entity(as[Either[Error, GetGameRecByRoomReq]]){
      case Right(req) =>
        dealFutureResult(
          RecordDAO.queryRecByRoom(req.roomId, req.lastRecordId, req.count).map{r =>
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
        )
      case Left(e) =>
        log.error(s"json parse error: $e")
        complete(parseJsonError)
    }
  }

  private val getGameRecByPlayer = (path("getGameRecByPlayer") & post){
    entity(as[Either[Error, GetGameRecByPlayerReq]]){
      case Right(req) =>
        dealFutureResult(
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
        )
      case Left(e) =>
        log.error(s"json parse error: $e")
        complete(parseJsonError)
    }
  }

  private val getGameRecByPId = (path("getGameRecById") & post){
    entity(as[Either[Error, GetGameRecByIdReq]]){
      case Right(req) =>
        dealFutureResult(
          RecordDAO.queryRecById(req.recordId).map{r =>
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
        )
      case Left(e) =>
        log.error(s"json parse error: $e")
        complete(parseJsonError)
    }
  }

  val GameRecRoutesLocal: Route =
    getGameRec ~ getGameRecByRoom ~ getGameRecByPlayer ~ getGameRecByPId

}
