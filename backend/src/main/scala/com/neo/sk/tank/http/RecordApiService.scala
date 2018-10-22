package com.neo.sk.tank.http

import org.slf4j.LoggerFactory
import com.neo.sk.utils.HttpUtil
import akka.http.scaladsl.server.Directives.{complete, _}
import akka.http.scaladsl.server.Route
import com.neo.sk.tank.protocol.CommonErrorCode
import com.neo.sk.tank.protocol.RecordApiProtocol.{getGameRecReq, getGameRecByTimeReq, getGameRecByPlayerReq, downloadRecordReq, getGameRecRsp, gameRec}
import scala.language.postfixOps
import com.neo.sk.tank.models.DAO.RecordDAO
import scala.concurrent.ExecutionContext.Implicits.global
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.stream.scaladsl.{FileIO, Source}
import java.io.File

/**
  *
  * 提供获取游戏录像列表的接口
  * 提供游戏下载接口
  */


trait RecordApiService extends ServiceUtils{
  import io.circe._
  import io.circe.generic.auto._

  private val log = LoggerFactory.getLogger(getClass)
  private def getGameRecErrorRsp(msg:String) = CommonErrorCode.CommonRsp(1000020,msg)

  private val getRecordList = (path("getRecordList") & post){
    dealPostReq[getGameRecReq]{req =>
      RecordDAO.queryAllRec(req.lastRecordId, req.count).map{r =>
        val temp = r.groupBy(_._1)
        var tempList = List.empty[gameRec]
        for((k, v) <- temp){
          if(v.head._2.nonEmpty)
            tempList = tempList :+ gameRec(k.recordId, k.roomId, k.startTime, k.endTime, v.length, v.map(r => r._2.get.userId))
          else tempList = tempList :+ gameRec(k.recordId, k.roomId, k.startTime, k.endTime, 0, List())
        }
        complete(getGameRecRsp(Some(tempList.sortBy(_.recordId))))
      }.recover{
        case e:Exception =>
          log.debug(s"获取游戏录像失败，recover error:$e")
          complete(getGameRecErrorRsp(s"获取游戏录像失败，recover error:$e"))
      }
    }
  }

  private val getRecordListByTime = (path("getRecordListByTime") & post){
    dealPostReq[getGameRecByTimeReq]{req =>
      RecordDAO.queryRecByTime(req.startTime, req.endTime, req.lastRecordId, req.count).map{r =>
        val temp = r.groupBy(_._1)
        var tempList = List.empty[gameRec]
        for((k, v) <- temp){
          if(v.head._2.nonEmpty)
            tempList = tempList :+ gameRec(k.recordId, k.roomId, k.startTime, k.endTime, v.length, v.map(r => r._2.get.userId))
          else tempList = tempList :+ gameRec(k.recordId, k.roomId, k.startTime, k.endTime, 0, List())
        }
        complete(getGameRecRsp(Some(tempList.sortBy(_.recordId))))
      }.recover{
        case e:Exception =>
          log.debug(s"获取游戏录像失败，recover error:$e")
          complete(getGameRecErrorRsp(s"获取游戏录像失败，recover error:$e"))
      }
    }
  }

  private val getRecordListByPlayer = (path("getRecordListByPlayer") & post){
    dealPostReq[getGameRecByPlayerReq]{req =>
      RecordDAO.getRecByUserId(req.playerId, req.lastRecordId, req.count).map{r =>
        val temp = r.groupBy(_._1)
        var tempList = List.empty[gameRec]
        for((k, v) <- temp){
          if(v.head._2.nonEmpty)
            tempList = tempList :+ gameRec(k.recordId, k.roomId, k.startTime, k.endTime, v.length, v.map(r => r._2.get.userId))
          else tempList = tempList :+ gameRec(k.recordId, k.roomId, k.startTime, k.endTime, 0, List())
        }
        complete(getGameRecRsp(Some(tempList.sortBy(_.recordId))))
      }.recover{
        case e:Exception =>
          log.debug(s"获取游戏录像失败，recover error:$e")
          complete(getGameRecErrorRsp(s"获取游戏录像失败，recover error:$e"))
      }
    }
  }

  private val downloadRecord = (path("downloadRecord")){
    parameter('token){token =>
      dealPostReq[downloadRecordReq]{req =>
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
    }
  }


  val GameRecRoutes: Route =
    getRecordList ~ getRecordListByTime ~ getRecordListByPlayer ~ downloadRecord
}