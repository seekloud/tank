package com.neo.sk.utils

import com.neo.sk.tank.model._
import com.neo.sk.tank.shared.ptcl.ErrorRsp
import org.slf4j.LoggerFactory
import com.neo.sk.tank.App.{executor}
import scala.concurrent.Future

/**
  * Created by hongruying on 2018/10/16
  */
object EsheepClient extends HttpUtil {
  import io.circe.generic.auto._
  import io.circe.parser.decode
  import io.circe.syntax._

  private val log = LoggerFactory.getLogger(this.getClass)

//  private val baseUrl = s"${AppSettings.esheepProtocol}://${AppSettings.esheepHost}:${AppSettings.esheepPort}"
  private val baseUrl = s"http://10.1.29.250:30374"
  println(baseUrl)
//  private val appId = AppSettings.esheepAppId
//  private val secureKey = AppSettings.esheepSecureKey

//  private val gameId = AppSettings.esheepGameId
  private val gameId = 1000000002
//  private val gameServerKey = AppSettings.esheepGameKey

  def getLoginInfo(): Future[Either[ErrorRsp,LoginInfo]] = {
    val methodName = s"login"
    val url = s"${baseUrl}/esheep/api/gameAgent/login"

    getRequestSend(methodName,url,Nil).map{
      case Right(jsonStr) =>
        decode[LoginResponse](jsonStr) match {
          case Right(rsp) =>
            if(rsp.errCode == 0){
              Right(rsp.data)
            }else{
              println(s"${methodName} failed,error:${rsp.msg}")
              Left(ErrorRsp(rsp.errCode, rsp.msg))
            }
          case Left(error) =>
            log.info(s"${methodName} parse json error:${error.getMessage}")
            Left(ErrorRsp(-1, error.getMessage))
        }
      case Left(error) =>
        log.info(s"${methodName}  failed,error:${error.getMessage}")
        Left(ErrorRsp(-1,error.getMessage))
    }
  }


  def linkGameAgent(token:String, playerId: String): Future[Either[ErrorRsp,GameServerInfo]] = {
    val methodName = s"joinGame"
    val url = s"${baseUrl}/esheep/api/gameAgent/joinGame?token=$token"

    val data = JoinGameReq(gameId, playerId).asJson.noSpaces


    postJsonRequestSend(methodName,url,Nil,data).map{
      case Right(jsonStr) =>
        println(jsonStr)
        decode[JoinGameRsp](jsonStr) match {
          case Right(rsp) =>
            if(rsp.errCode == 0){
              Right(rsp.data.gsPrimaryInfo)
            }else{
              log.debug(s"${methodName} failed,error:${rsp.msg}")
              Left(ErrorRsp(rsp.errCode, rsp.msg))
            }
          case Left(error) =>
            println(s"${methodName} parse json error:${error.getMessage}")
            Left(ErrorRsp(-1, error.getMessage))
        }
      case Left(error) =>
        println(s"${methodName}  failed,error:${error.getMessage}")
        Left(ErrorRsp(-1,error.getMessage))
    }
  }




}
