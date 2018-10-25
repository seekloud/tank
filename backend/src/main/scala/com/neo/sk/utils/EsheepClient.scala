package com.neo.sk.utils

import com.neo.sk.tank.common.AppSettings
import com.neo.sk.utils.SecureUtil.{PostEnvelope, genPostEnvelope}
import org.slf4j.LoggerFactory
import com.neo.sk.tank.Boot.executor
import com.neo.sk.tank.shared.ptcl.{ComRsp, CommonRsp, ErrorRsp}

import scala.concurrent.Future
/**
  * Created by hongruying on 2018/10/16
  */
object EsheepClient extends HttpUtil {
  import io.circe.parser.decode
  import io.circe.syntax._
  import io.circe._
  import io.circe.generic.auto._
  import com.neo.sk.tank.protocol.EsheepProtocol

  private val log = LoggerFactory.getLogger(this.getClass)

  private val baseUrl = s"${AppSettings.esheepProtocol}://${AppSettings.esheepHost}:${AppSettings.esheepPort}"
  private val appId = AppSettings.esheepAppId
  private val secureKey = AppSettings.esheepSecureKey

  private val gameId = AppSettings.esheepGameId
  private val gameServerKey = AppSettings.esheepGameKey

  def gsKey2Token(): Future[Either[ErrorRsp,EsheepProtocol.GameServerKey2TokenInfo]] = {
    val methodName = s"gsKey2Token"
    val url = s"${baseUrl}/esheep/api/gameServer/gsKey2Token"

    val data = EsheepProtocol.GameServerKey2TokenReq(gameId,gameServerKey).asJson.noSpaces



    postJsonRequestSend(methodName,url,Nil,data).map{
      case Right(jsonStr) =>
        decode[EsheepProtocol.GameServerKey2TokenRsp](jsonStr) match {
          case Right(rsp) =>
            if(rsp.errCode == 0 && rsp.data.nonEmpty){
              Right(rsp.data.get)
            }else{
              log.debug(s"${methodName} failed,error:${rsp.msg}")
              Left(ErrorRsp(rsp.errCode, rsp.msg))
            }
          case Left(error) =>
            log.warn(s"${methodName} parse json error:${error.getMessage}")
            Left(ErrorRsp(-1, error.getMessage))
        }
      case Left(error) =>
        log.debug(s"${methodName}  failed,error:${error.getMessage}")
        Left(ErrorRsp(-1,error.getMessage))
    }
  }

  def verifyAccessCode(accessCode:String,token:String): Future[Either[ErrorRsp,EsheepProtocol.PlayerInfo]] = {
    val methodName = s"verifyAccessCode"
    val url = s"${baseUrl}/esheep/api/gameServer/verifyAccessCode?token=$token"

    val data = Json.obj(
      ("gameId",gameId.asJson),
      ("accessCode",accessCode.asJson)
    ).noSpaces

    val sn = appId + System.currentTimeMillis()
    val (timestamp, noce, signature) = SecureUtil.generateSignatureParameters(List(appId, sn, data), secureKey)
    val postData = PostEnvelope(appId,sn,timestamp,noce,data,signature).asJson.noSpaces

    postJsonRequestSend(methodName,url,Nil,postData).map{
      case Right(jsonStr) =>
        decode[EsheepProtocol.VerifyAccessCodeRsp](jsonStr) match {
          case Right(rsp) =>
            if(rsp.errCode == 0 && rsp.data.nonEmpty){
              Right(rsp.data.get)
            }else{
              log.debug(s"${methodName} failed,error:${rsp.msg}")
              Left(ErrorRsp(rsp.errCode, rsp.msg))
            }
          case Left(error) =>
            log.warn(s"${methodName} parse json error:${error.getMessage}")
            Left(ErrorRsp(-1, error.getMessage))
        }
      case Left(error) =>
        log.debug(s"${methodName}  failed,error:${error.getMessage}")
        Left(ErrorRsp(-1,error.getMessage))
    }
  }


  def inputBatRecoder(token:String,playerId: String, nickname: String, killing: Int, killed: Int, score: Int, gameExtent: String, startTime: Long, endTime: Long): Future[Either[String,String]]={
    val methodName = s"addPlayerRecord"
    val url = s"${baseUrl}/esheep/api/gameServer/addPlayerRecord?token=${token}"

    val info = EsheepProtocol.BatRecordInfo(playerId,gameId,nickname,killing,killed,score,gameExtent,startTime,endTime)
    val data = EsheepProtocol.BatRecord(info).asJson.noSpaces


    postJsonRequestSend(methodName,url,Nil,data).map{
      case Right(jsonStr) =>
        decode[ComRsp](jsonStr) match {
          case Right(rsp) =>
            if(rsp.errCode == 0){
              Right(s"${methodName} success")
            }else{
              log.debug(s"${methodName} failed,error:${rsp.msg}")
              Left("error")
            }
          case Left(error) =>
            log.warn(s"${methodName} parse json error:${error.getMessage}")
            Left("error")
        }
      case Left(error) =>
        log.debug(s"${methodName}  failed,error:${error.getMessage}")
        Left("error")
    }



  }




}
