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

import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.BasicDirectives
import org.seekloud.tank.common.AppSettings
import org.seekloud.utils.SessionSupport
import org.slf4j.LoggerFactory


/**
  * Created by hongruying on 2018/3/11
  */

object SessionBase {

  private val SessionTypeKey = "STKey"
  object AccountSessionKeys {
    val sessionType = "user_session"
//    val userType = "userType"
//    val accountId = "accountId"
    val name = "name"
//    val nameCn = "nameCn"
    val timestamp = "hw1801a_user_timestamp"
  }



  val log = LoggerFactory.getLogger(this.getClass)
  case class AccountSession(
//                             userType:Int,
//                             accountId:Long,
                             name:String,
//                             nameCn:String,
                             time:Long
                           ){
    def toSessionMap = Map(
      SessionTypeKey -> AccountSessionKeys.sessionType,
//      AccountSessionKeys.accountId -> accountId.toString,
//      AccountSessionKeys.userType -> userType.toString,
      AccountSessionKeys.name -> name,
//      AccountSessionKeys.nameCn -> nameCn,
      AccountSessionKeys.timestamp -> time.toString
    )
  }

  object SessionKeys {
    val accountType = "yilia_account_type"
    val accountId = "yilia_accountId"
    val timestamp = "yilia_timestamp"
    val nickname = "yilia_nickname"
    val headImg = "yilia_headImg"
  }
  case class SessionCombine(
                             userType: Int,
                             id: String,
                             timestamp: Long,
                             nickname: String,
                             headImg: String="",
                           ) {
    def toSessionMap = Map(
      SessionKeys.accountType -> userType.toString,
      SessionKeys.accountId -> id,
      SessionKeys.timestamp -> timestamp.toString,
      SessionKeys.nickname -> nickname,
      SessionKeys.headImg -> headImg
    )
  }


}

trait SessionBase extends SessionSupport {

  import SessionBase._

  override val sessionEncoder = SessionSupport.PlaySessionEncoder
  override val sessionConfig = AppSettings.sessionConfig
  protected val sessionTimeout = 24 * 60 * 60 * 1000
  private val log = LoggerFactory.getLogger(this.getClass)

  implicit class SessionTransformer(sessionMap: Map[String, String]) {
    def toAccountSession:Option[AccountSession] = {
      //      log.debug(s"toAdminSession: change map to session, ${sessionMap.mkString(",")}")
      try {
        if (sessionMap.get(SessionTypeKey).exists(_.equals(AccountSessionKeys.sessionType))) {
          if(sessionMap(AccountSessionKeys.timestamp).toLong - System.currentTimeMillis() > sessionTimeout){
            None
          }else {
            Some(AccountSession(
//              sessionMap(AccountSessionKeys.userType).toInt,
//              sessionMap(AccountSessionKeys.accountId).toLong,
              sessionMap(AccountSessionKeys.name),
//              sessionMap(AccountSessionKeys.nameCn),
              sessionMap(AccountSessionKeys.timestamp).toLong
            ))
          }
        } else {
          log.debug("no session type in the session")
          None
        }
      } catch {
        case e: Exception =>
          e.printStackTrace()
          log.warn(s"toAdminSession: ${e.getMessage}")
          None
      }
    }
    def toUserSession: Option[SessionCombine] = {
      log.debug(s"toUserSession: change map to session, ${sessionMap.mkString(",")}")
      try {
        if (sessionMap.get(SessionKeys.accountType).nonEmpty) {
          Some(SessionCombine(
            sessionMap(SessionKeys.accountType).toInt,
            sessionMap(SessionKeys.accountId),
            sessionMap(SessionKeys.timestamp).toLong,
            sessionMap(SessionKeys.nickname),
            sessionMap(SessionKeys.headImg)
          ))
        }
        else {
          log.debug("no session type in the session")
          None
        }
      } catch {
        case e: Exception =>
          e.printStackTrace()
          log.warn(s"toUserSession: ${e.getMessage}")
          None
      }
    }
  }
  protected val optionalAccountSession: Directive1[Option[AccountSession]] = optionalSession.flatMap {
    case Right(sessionMap) => BasicDirectives.provide(sessionMap.toAccountSession)
    case Left(error) =>
      log.debug(error)
      BasicDirectives.provide(None)
  }
  protected val optionalUserSession: Directive1[Option[SessionCombine]] = optionalSession.flatMap {
    case Right(sessionMap) => BasicDirectives.provide(sessionMap.toUserSession)
    case Left(error) =>
      log.debug(error)
      BasicDirectives.provide(None)
  }
}
