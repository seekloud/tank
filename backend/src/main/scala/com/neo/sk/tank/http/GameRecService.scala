//package com.neo.sk.tank.http
//
//import org.slf4j.LoggerFactory
//import com.neo.sk.utils.HttpUtil
//import akka.http.scaladsl.server.Directives.{complete, _}
//import akka.http.scaladsl.server.Route
//import scala.language.postfixOps
//import com.neo.sk.tank.models.DAO.RecordDAO
//import scala.concurrent.ExecutionContext.Implicits.global
//import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
//import com.neo.sk.tank.protocol.RecordApiProtocol.{getGameRecReq, getGameRecByTimeReq, getGameRecByPlayerReq, getGameRecRsp, gameRec}
//import com.neo.sk.tank.protocol.CommonErrorCode.parseJsonError
//import com.neo.sk.tank.shared.ptcl.ErrorRsp
//
//
//
//
///**
//  *
//  * 提供获取游戏录像列表的本地接口
//  */
//
//
//trait GameRecService {
//  import io.circe._
//  import io.circe.generic.auto._
//
//  private val log = LoggerFactory.getLogger(getClass)
//  private def getGameRecErrorRsp(msg:String) = ErrorRsp(1000020,msg)
//
//  private val getRecordList = (path("getRecordList") & post){
//    entity(as[Either[Error, getGameRecReq]]){
//      case Right(req) =>
//        dealFutureResult(
//          RecordDAO.queryAllRec(req.lastRecordId, req.count).map{r =>
//            val temp = r.groupBy(_._1)
//            var tempList = List.empty[gameRec]
//            for((k, v) <- temp){
//              if(v.head._2.nonEmpty)
//                tempList = tempList :+ gameRec(k.recordId, k.roomId, k.startTime, k.endTime, v.length, v.map(r => r._2.get.userId))
//              else tempList = tempList :+ gameRec(k.recordId, k.roomId, k.startTime, k.endTime, 0, List())
//            }
//            complete(getGameRecRsp(Some(tempList.sortBy(_.recordId))))
//          }.recover{
//            case e:Exception =>
//              log.debug(s"获取游戏录像失败，recover error:$e")
//              complete(getGameRecErrorRsp(s"获取游戏录像失败，recover error:$e"))
//          }
//        )
//      case Left(e) =>
//        log.error(s"json parse PostEnvelope error: $e")
//        complete(parseJsonError)
//    }
//  }
//
//  private val getRecordListByTime = (path("getRecordListByTime") & post){
//    entity(as[Either[Error, getGameRecByTimeReq]]){req =>
//      RecordDAO.queryRecByTime(req.startTime, req.endTime, req.lastRecordId, req.count).map{r =>
//        val temp = r.groupBy(_._1)
//        var tempList = List.empty[gameRec]
//        for((k, v) <- temp){
//          if(v.head._2.nonEmpty)
//            tempList = tempList :+ gameRec(k.recordId, k.roomId, k.startTime, k.endTime, v.length, v.map(r => r._2.get.userId))
//          else tempList = tempList :+ gameRec(k.recordId, k.roomId, k.startTime, k.endTime, 0, List())
//        }
//        complete(getGameRecRsp(Some(tempList.sortBy(_.recordId))))
//      }.recover{
//        case e:Exception =>
//          log.debug(s"获取游戏录像失败，recover error:$e")
//          complete(getGameRecErrorRsp(s"获取游戏录像失败，recover error:$e"))
//      }
//    }
//  }
//
//  private val getRecordListByPlayer = (path("getRecordListByPlayer") & post){
//    entity(as[Either[Error, getGameRecByPlayerReq]]){req =>
//      RecordDAO.getRecByUserId(req.playerId, req.lastRecordId, req.count).map{r =>
//        val temp = r.groupBy(_._1)
//        var tempList = List.empty[gameRec]
//        for((k, v) <- temp){
//          if(v.head._2.nonEmpty)
//            tempList = tempList :+ gameRec(k.recordId, k.roomId, k.startTime, k.endTime, v.length, v.map(r => r._2.get.userId))
//          else tempList = tempList :+ gameRec(k.recordId, k.roomId, k.startTime, k.endTime, 0, List())
//        }
//        complete(getGameRecRsp(Some(tempList.sortBy(_.recordId))))
//      }.recover{
//        case e:Exception =>
//          log.debug(s"获取游戏录像失败，recover error:$e")
//          complete(getGameRecErrorRsp(s"获取游戏录像失败，recover error:$e"))
//      }
//    }
//  }
//
//  val GameRecRoutes: Route =
//    getRecordList ~ getRecordListByTime ~ getRecordListByPlayer
//
//}
