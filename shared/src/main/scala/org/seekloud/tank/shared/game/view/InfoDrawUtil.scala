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

package org.seekloud.tank.shared.game.view

import org.seekloud.tank.shared.game.GameContainerClientImpl

/**
  * Created by sky
  * Date on 2018/11/21
  * Time at 下午4:03
  * 本文件中实现canvas绘制提示信息
  */
trait InfoDrawUtil {this:GameContainerClientImpl =>
//  private val combatImg = this.drawFrame.createImage("/img/dead.png")

  def drawGameLoading():Unit = {
    viewCtx.setFill("rgb(0,0,0)")
    viewCtx.fillRec(0, 0, canvasSize.x , canvasSize.y )
    viewCtx.setFill("rgb(250, 250, 250)")
    viewCtx.setTextAlign("left")
    viewCtx.setTextBaseline("top")
    viewCtx.setFont(s"Helvetica","normal",3.6 * canvasUnit)
    viewCtx.fillText("请稍等，正在连接服务器", 150, 180)
  }

  def drawGameStop():Unit = {
    viewCtx.setFill("rgb(0,0,0)")
    viewCtx.fillRec(0, 0, canvasSize.x , canvasSize.y )
    viewCtx.setFill("rgb(250, 250, 250)")
    viewCtx.setTextAlign("left")
    viewCtx.setTextBaseline("top")
    viewCtx.setFont(s"Helvetica","normal",3.6 * canvasUnit)
    viewCtx.fillText(s"您已经死亡,被玩家=${this.killerName}所杀,等待倒计时进入游戏", 150, 180)
    println()
  }

  def drawUserLeftGame:Unit = {
    viewCtx.setFill("rgb(0,0,0)")
    viewCtx.fillRec(0, 0, canvasSize.x , canvasSize.y )
    viewCtx.setFill("rgb(250, 250, 250)")
    viewCtx.setTextAlign("left")
    viewCtx.setTextBaseline("top")
    viewCtx.setFont(s"Helvetica","normal",3.6 * canvasUnit)
    viewCtx.fillText(s"您已经离开该房间。", 150, 180)
    println()
  }

  def drawReplayMsg(m:String):Unit = {
    viewCtx.setFill("rgb(0,0,0)")
    viewCtx.fillRec(0, 0, canvasSize.x , canvasSize.y )
    viewCtx.setFill("rgb(250, 250, 250)")
    viewCtx.setTextAlign("left")
    viewCtx.setTextBaseline("top")
    viewCtx.setFont(s"Helvetica","normal",3.6 * canvasUnit)
    viewCtx.fillText(m, 150, 180)
    println()
  }

  def drawGameRestart(countDownTimes:Int,killerName:String): Unit = {
    viewCtx.setFill("rgb(0,0,0)")
    viewCtx.setTextAlign("center")
    viewCtx.setFont("楷体", "normal", 5 * canvasUnit)
    viewCtx.fillRec(0, 0, canvasSize.x , canvasSize.y )
    viewCtx.setFill("rgb(0,0,0)")
    viewCtx.fillText(s"重新进入房间，倒计时：${countDownTimes}", 300, 100)
    viewCtx.fillText(s"您已经死亡,被玩家=${killerName}所杀", 300, 180)
  }

  def drawDeadImg(s:String) = {
    viewCtx.setFill("rgb(0,0,0)")
    viewCtx.fillRec(0, 0, canvasSize.x , canvasSize.y )
    viewCtx.setFill("rgb(250, 250, 250)")
    viewCtx.setTextAlign("left")
    viewCtx.setTextBaseline("top")
    viewCtx.setFont("Helvetica","normal",36)
    viewCtx.fillText(s"$s", 150, 180)
  }

  def drawCombatGains():Unit = {
    viewCtx.setFont("Arial", "normal", 4 * canvasUnit)
    viewCtx.setGlobalAlpha(1)
    viewCtx.setTextAlign("left")
    viewCtx.setFill("rgb(0,0,0)")
    viewCtx.fillText(s"KillCount：",0.4 * canvasSize.x , 0.12 * canvasSize.y )
    viewCtx.fillText(s"Damage：", 0.4 * canvasSize.x , 0.2 * canvasSize.y )
    viewCtx.fillText(s"Killer：",0.4 * canvasSize.x , 0.26 * canvasSize.y )
    viewCtx.fillText(s"Press Space To Comeback!!!",0.4 * canvasSize.x , 0.32 * canvasSize.y )
    viewCtx.setFill("rgb(255,0,0)")
    viewCtx.fillText(s"${this.killNum}", 0.5 * canvasSize.x , 0.12 * canvasSize.y )
    viewCtx.fillText(s"${this.damageNum}",0.5 * canvasSize.x , 0.2 * canvasSize.y )
    var pos = 0.5 * canvasSize.x
    this.killerList.foreach{r =>
      viewCtx.fillText(s"【${r}】", pos, 0.26 * canvasSize.y / canvasUnit)
      pos = pos + 2 * canvasUnit * s"【${r}】".length + 1 * canvasUnit}
//    ctx.drawImage(combatImg,0.25 * canvasSize.x * canvasUnit,0.1 * canvasSize.y * canvasUnit,Some(pos - 0.25 * canvasSize.x * canvasUnit + 2 * canvasUnit,0.22 * canvasSize.y * canvasUnit))
    //    ctx.drawImage(combatImg,0.25 * canvasSize.x * canvasUnit,0.1 * canvasSize.y * canvasUnit,Some(0.5* canvasSize.x * canvasUnit,0.22 * canvasSize.y * canvasUnit))

  }
}
