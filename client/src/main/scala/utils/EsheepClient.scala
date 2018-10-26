package utils

import com.neo.sk.tank.common.AppSettings
import com.neo.sk.tank.model.{LoginInfo, LoginResponse}
import com.neo.sk.tank.shared.ptcl.{ComRsp, ErrorRsp}
import org.slf4j.LoggerFactory

import scala.concurrent.Future

/**
  * Created by hongruying on 2018/10/16
  */
object EsheepClient extends HttpUtil {
  import io.circe._
  import io.circe.generic.auto._
  import io.circe.parser.decode
  import io.circe.syntax._

  private val log = LoggerFactory.getLogger(this.getClass)

  private val baseUrl = s"${AppSettings.esheepProtocol}://${AppSettings.esheepHost}:${AppSettings.esheepPort}"
  private val appId = AppSettings.esheepAppId
  private val secureKey = AppSettings.esheepSecureKey

  private val gameId = AppSettings.esheepGameId
  private val gameServerKey = AppSettings.esheepGameKey

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




}
