package com.neo.sk.tank.front.pages

import com.neo.sk.tank.front.common.Page
import com.neo.sk.tank.front.model.PlayerInfo
import com.neo.sk.tank.front.tankClient.GameHolder
import com.neo.sk.tank.front.utils.Shortcut
import mhtml.{Var, emptyHTML}

import scala.xml.Elem

/**
  * Created by hongruying on 2018/10/18
  */
case class PlayPage(
              playerInfoSeq:List[String]
              ) extends Page{


  private def parsePlayerInfoSeq: Option[PlayerInfo] = playerInfoSeq match {
    case userId :: userName :: roomId :: accessCode :: Nil => Some(PlayerInfo(userId, userName, accessCode, Some(roomId.toLong)))
    case userId :: userName  :: accessCode :: Nil => Some(PlayerInfo(userId, userName, accessCode, None))
    case _ => None
  }




  private val cannvas = <canvas id ="GameView" tabindex="1"></canvas>


  private val modal = Var(emptyHTML)

  def init(playerInfo: PlayerInfo) = {
    val gameHolder = new GameHolder("GameView", Some(playerInfo))
    val startGameModal = gameHolder.getStartGameModal()
    modal := startGameModal
  }





  override def render: Elem ={
    parsePlayerInfoSeq match {
      case Some(playerInfo) =>
        Shortcut.scheduleOnce(() =>init(playerInfo),0)
        <div>
          <div >{modal}</div>
          {cannvas}
        </div>
      case None =>
        <div>
          <div >链接不符合标准</div>
        </div>
    }

  }

}
