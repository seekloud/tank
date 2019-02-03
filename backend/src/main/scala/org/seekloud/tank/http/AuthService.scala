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

import java.util.concurrent.atomic.AtomicLong
import scala.language.implicitConversions
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives.complete
import org.seekloud.tank.Boot.esheepSyncClient
import org.seekloud.tank.common.AppSettings
import org.seekloud.tank.core.EsheepSyncClient
import org.seekloud.tank.protocol.EsheepProtocol
import org.seekloud.tank.shared.ptcl.ErrorRsp
import org.slf4j.LoggerFactory
import concurrent.duration._
import org.seekloud.tank.Boot.{executor, timeout, scheduler, materializer}
import scala.concurrent.Future
import akka.actor.typed.scaladsl.AskPattern._

/**
  * Created by hongruying on 2018/10/18
  */
trait AuthService extends ServiceUtils{

  import io.circe.generic.auto._

  private val log = LoggerFactory.getLogger(this.getClass)

  private def AuthUserErrorRsp(msg: String) = ErrorRsp(10001001, msg)

  val uid = new AtomicLong(1L)

  protected def authPlatUser(accessCode:String)(f: EsheepProtocol.PlayerInfo => server.Route):server.Route = {
    if(AppSettings.esheepAuthToken) {
      val verifyAccessCodeFutureRst: Future[EsheepProtocol.VerifyAccessCodeRsp] = esheepSyncClient ? (e => EsheepSyncClient.VerifyAccessCode(accessCode, e))
      dealFutureResult{
        verifyAccessCodeFutureRst.map{ rsp =>
          if(rsp.errCode == 0 && rsp.data.nonEmpty){
            f(rsp.data.get)
          } else{
            complete(AuthUserErrorRsp(rsp.msg))
          }
        }.recover{
          case e:Exception =>
            log.warn(s"verifyAccess code failed, code=${accessCode}, error:${e.getMessage}")
            complete(AuthUserErrorRsp(e.getMessage))
        }
      }
    } else {
      val id = uid.getAndIncrement()
      f(EsheepProtocol.PlayerInfo(s"test_${id}",s"test_${id}"))
    }
  }

}
