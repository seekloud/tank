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

package org.seekloud.tank.shared.game

/**
  * Created by hongruying on 2018/8/28
  */
trait KillInformation{ this:GameContainer =>
  private var killInfoList = List[(String,String,Long)]() //killerName,killedName, startTimer
  private val maxDisplaySize = 5
  private val displayDuration = 3 //s

  private val displayFrameNum:Long = displayDuration * 1000 / this.config.frameDuration

  protected def addKillInfo(killerName:String,killedName:String) = {
    println(s"killInfo----------,$killedName,$killerName,${this.systemFrame}")
    killInfoList = (killerName,killedName,this.systemFrame) :: killInfoList
  }



  protected def updateKillInformation():Unit = {
    killInfoList = killInfoList.filterNot(_._3 + displayFrameNum < this.systemFrame)
  }

  protected def getDisplayKillInfo():List[(String,String,Long)] = {
    val curDisplayNum = math.min(maxDisplaySize,killInfoList.size)
    killInfoList.take(curDisplayNum)
  }

  protected def removeKillInfoByRollback(frame:Long) = {
    killInfoList = killInfoList.filterNot(_._3 >= frame)
  }


}
