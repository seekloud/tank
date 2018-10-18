package com.neo.sk.tank.front.pages

import com.neo.sk.tank.front.common.{Page, PageSwitcher}
import mhtml.{Cancelable, Rx, Var, mount}
import org.scalajs.dom

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
    case "playGame" :: playInfoSeq => new PlayPage(playInfoSeq).render
    case "test" :: Nil => <div>TO BE CONTINUE...</div>
  private val currentPage: Rx[Elem] = currentPageHash.map {
    case Nil => TankDemo.render
    case "replay"::name::uid::rid::wid::f::Nil => {
//      ReplayPage.setParam(name, uid.toLong, rid.toLong, f.toInt)
//      ReplayPage.render
      new ReplayPage(name, uid.toLong, rid.toLong,wid.toLong, f.toInt).render
    }
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
