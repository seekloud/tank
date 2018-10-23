package com.neo.sk.tank.front.pages

import com.neo.sk.tank.front.common.{Page, PageSwitcher}
import mhtml.{Cancelable, Rx, Var, mount}
import org.scalajs.dom
import com.neo.sk.tank.front.model.ReplayInfo
import scala.xml.Elem

/**
  * Created by hongruying on 2018/4/8
  */
object MainPage extends PageSwitcher {




  override def switchPageByHash(): Unit = {
    val tokens = {
      val t = getCurrentHash.split("/").toList
      if (t.nonEmpty) {
        t.tail
      } else Nil
    }

    println(tokens)
    switchToPage(tokens)
  }


  private val currentPage: Rx[Elem] = currentPageHash.map {
    case Nil => TankDemo.render
    case "playGame" :: playInfoSeq => PlayPage(playInfoSeq).render
    case "watchRecord":: rid :: wid :: f :: accessCode :: Nil => {
      ReplayPage.setParam(ReplayInfo(rid.toLong,wid.toLong, f.toInt, accessCode))
      ReplayPage.render
    }
    case "watchGame" :: roomId :: playerId :: accessCode ::Nil => new TankObservation(roomId.toLong, accessCode, Some(playerId)).render
    case "watchGame" :: roomId :: accessCode :: Nil => new TankObservation(roomId.toLong, accessCode).render
    case "getGameRec" :: Nil => GameRecordPage.render

    case "test" :: Nil => <div>TO BE CONTINUE...</div>
    case _ => <div>Error Page</div>
  }

  def gotoPage(i: String) = {
    dom.document.location.hash = "" + i
  }


  def show(): Cancelable = {
    switchPageByHash()
    val page =
      <div>
        {currentPage}
      </div>
    mount(dom.document.body, page)
  }

}
