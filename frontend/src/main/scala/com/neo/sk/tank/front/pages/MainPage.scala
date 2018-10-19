package com.neo.sk.tank.front.pages

import com.neo.sk.tank.front.common.{Page, PageSwitcher}
import mhtml.{Cancelable, Rx, Var, mount}
import org.scalajs.dom
import com.neo.sk.tank.front.pages.TankObservation

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
    case "replay":: name :: uid :: rid :: wid :: f :: Nil => new ReplayPage(name, uid.toLong, rid.toLong,wid.toLong, f.toInt).render
    case "test" :: Nil => <div>TO BE CONTINUE...</div>
  private val currentPage: Rx[Elem] = currentPageName.map {
    case Nil => TankDemo.render
    case "首页"::Nil => TankDemo.render
    case "test"::Nil => <div>TO BE CONTINUE...</div>
    case "watchGame" :: roomId::playerId::Nil =>
      new TankObservation(roomId.toInt,playerId.toLong).render
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
