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

package org.seekloud.utils

import java.io._

import org.seekloud.tank.model.Ws4AgentRsp

import io.circe.parser.decode
import io.circe.syntax._
import io.circe._
import io.circe.generic.auto._
/**
  * Created by sky
  * Date on 2019/3/21
  * Time at 上午10:53
  * 文件存储
  */
object FileUtil {
  def saveFile(info: String, filePath:String) = {
    try {
      val out = new FileOutputStream(filePath)
      val outWriter = new OutputStreamWriter(out, "UTF-8")
      val bufWrite = new BufferedWriter(outWriter)
      bufWrite.append(info)
      bufWrite.close()
      out.close()
    }catch{
      case e:Exception=>
        println("save file exception:"+e.getStackTrace)
    }
  }

  def readFile(filePath:String) = {
    val file = new File(filePath)
    if (file.isFile && file.exists) {
      try {
        val in = new FileInputStream(filePath)
        val inReader = new InputStreamReader(in, "UTF-8")
        val bufferedReader = new BufferedReader(inReader)
        decode[Ws4AgentRsp](bufferedReader.readLine()) match {
          case Right(value)=>
            Some(value)
          case Left(e)=>
            None
        }
      } catch {
        case e: Exception =>
          println("get exception:" + e.getStackTrace)
          None
      }
    }else{
      println(s"file--$filePath isn't exists.")
      None
    }
  }
}
