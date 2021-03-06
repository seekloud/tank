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

import org.seekloud.tank.shared.protocol.TankGameEvent.{GameContainerAllState, GameEvent, UserActionEvent}

import scala.collection.mutable

/**
  * Created by hongruying on 2018/8/24
  * 中间会记录所有的事件数据和快照数据，但是当同步全量数据时会将历史的事件数据更新掉，并且丢掉快照数据
  * 可支持回溯
  */
trait EsRecover { this:GameContainerClientImpl =>

  private val gameEventHistoryMap = mutable.HashMap[Long,List[GameEvent]]()
  private val actionEventHistoryMap = mutable.HashMap[Long,List[UserActionEvent]]()
  private val gameSnapshotMap = mutable.HashMap[Long,GameContainerAllState]()

  def addEventHistory(frame:Long,gameEvents:List[GameEvent],actionEvents:List[UserActionEvent]):Unit = {
    gameEventHistoryMap.put(frame,gameEvents)
    actionEventHistoryMap.put(frame,actionEvents)
  }



  def addGameSnapShot(frame:Long,gameState:GameContainerAllState) = {
    gameSnapshotMap.put(frame,gameState)
  }

  def clearEsRecoverData():Unit = {
    gameEventHistoryMap.clear()
    actionEventHistoryMap.clear()
    gameSnapshotMap.clear()
  }

  def rollback(frame:Long) = {
    require(frame < this.systemFrame)

    gameSnapshotMap.get(frame) match {
      case Some(gameContainerAllState) =>
        val startTime = System.currentTimeMillis()
        val curFrame = this.systemFrame
        handleGameContainerAllState(gameContainerAllState)
        //同步所有数据
        removeKillInfoByRollback(frame)
        reSetFollowEventMap(frame)
        (frame until curFrame).foreach{ f =>
          this.addGameEvents(f,gameEventHistoryMap.getOrElse(f,Nil),actionEventHistoryMap.getOrElse(f,Nil))
          this.rollbackUpdate()
        }
        val endTime = System.currentTimeMillis()
        this.info(s"roll back to frame=${frame},nowFrame=${curFrame} use Time:${endTime - startTime}")
      case None => this.info(s"there are not snapshot frame=${frame}")
    }
  }

  def rollback4GameEvent(e:GameEvent) = {
    this.info(s"roll back4GameEvent to frame=${e.frame},nowFrame=${systemFrame} because event:${e}")
    gameEventHistoryMap.put(e.frame, e :: gameEventHistoryMap.getOrElse(e.frame, Nil))
    rollback(e.frame)
  }

  def rollback4UserActionEvent(e:UserActionEvent) = {
    this.info(s"roll back4UserAction to frame=${e.frame},nowFrame=${systemFrame} because event:${e}")
    actionEventHistoryMap.put(e.frame, e :: actionEventHistoryMap.getOrElse(e.frame, Nil))
    rollback(e.frame)
  }


  def removePreEventHistory(frame:Long, tankId:Int, serialNum:Byte):Unit = {
    actionEventHistoryMap.get(frame).foreach{ actions =>
      actionEventHistoryMap.put(frame,actions.filterNot(t => t.tankId == tankId && t.serialNum == serialNum))
    }
  }

  def addUserActionHistory(e:UserActionEvent) = {
    actionEventHistoryMap.put(e.frame, e :: actionEventHistoryMap.getOrElse(e.frame, Nil))
  }

}
