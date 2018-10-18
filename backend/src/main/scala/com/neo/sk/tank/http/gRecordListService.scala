package com.neo.sk.tank.http

import org.slf4j.LoggerFactory
import com.neo.sk.tank.shared.ptcl.TankGameProtocol.{GameRecordReq, GameRecordRsp}
import com.neo.sk.utils.HttpUtil
import akka.http.scaladsl.server.Directives.{complete, _}
import akka.http.scaladsl.server.Route
import com.neo.sk.tank.protocol.CommonErrorCode.{parseJsonError, noMessageError}
import scala.language.postfixOps
import com.neo.sk.tank.models.DAO.RecordDAO
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

/**
  *
  * 提供获取游戏录像列表的接口
  * 提供下载游戏录像接口
  */


trait gRecordListService extends ServiceUtils with HttpUtil{
  import io.circe._
  import io.circe.generic.auto._

  private val log = LoggerFactory.getLogger(getClass)

  def groupTheLst(lst: List[(Long, Long, Long, Long, Option[Long])]) = {
    val RecMapUser = scala.collection.mutable.Map[Long,List[Long]]()
    for(one <- lst) {
      val userId = one._5.getOrElse(-1L)
      if (RecMapUser.contains(one._1) && userId > 0L)
        RecMapUser(one._1) = RecMapUser(one._1) :+ userId
      else if(userId > 0L)
        RecMapUser(one._1) = List(userId)
    }
    val temp = lst.map(r => (r._1, r._2, r._3, r._4, RecMapUser.getOrElse(r._1, List())))
    temp.toSet.toList
  }


  def getRecByUserId(userId:Long) = {
    RecordDAO.queryRecByUser(userId).flatMap{r =>
      Future.sequence(r.map{r1 => RecordDAO.queryRecByRec(r1)})
    }
  }

  private val getGameRec = (path("getGameRec") & post){
    entity(as[Either[Error,GameRecordReq]]) {
      case Left(error) =>
        log.warn(s"some error: $error")
        complete(parseJsonError)
      case Right(req) =>
        if(req.userId != 0){
          dealFutureResult(getRecByUserId(req.userId).map{s =>
            val temp = s.flatten
            if(temp == Seq()) complete(noMessageError)
            else complete(GameRecordRsp(Some(groupTheLst(temp.toList))))
          })
        }else if(req.recordId !=0){
          dealFutureResult(RecordDAO.queryRecByRec(req.recordId).map{s =>
            if(s == Seq()) complete(noMessageError)
            else complete(GameRecordRsp(Some(groupTheLst(s.toList))))
          })
        }else if(req.roomId != 0L){
          dealFutureResult(RecordDAO.queryRecByRoom(req.roomId).map{s =>
            if(s == Seq()) complete(noMessageError)
            else complete(GameRecordRsp(Some(groupTheLst(s.toList))))
          })
        }else{
          dealFutureResult(RecordDAO.queryAllRec(req.page-1).map{s =>
            if(s == Seq()) complete(noMessageError)
            else complete(GameRecordRsp(Some(groupTheLst(s.toList))))
          })
        }
    }
  }
  val GameRecRoutes: Route =
    pathPrefix("game"){
      getGameRec
    }
}
