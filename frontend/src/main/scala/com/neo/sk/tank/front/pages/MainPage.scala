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

//    val pageName =
//      tokens match {
//        case Nil => "首页"
//        case "watchGame" :: p :: q :: Nil =>"watchGame" :: p :: q :: Nil
//        case "test" :: Nil => "test"
//        case "postManager"::Nil => "帖子管理"
//        case "versionManager"::Nil =>"版本管理"
//        case "register" :: Nil => "管理员登录"
//        case "sticky" :: Nil => "置顶管理"
//        case "recommendBoardManager":: Nil => "推荐版面管理"
//        case x =>
//          println(s"unknown hash: $x")
//          "unknow"
//      }
    println("tokens================"+tokens)
//    println("pageName==================="+pageName)
    switchToPage(tokens)
  }


  private val currentPage: Rx[Elem] = currentPageName.map {
    case Nil => TankDemo.render
    case "首页"::Nil => TankDemo.render
    case "test"::Nil => <div>TO BE CONTINUE...</div>
    case "watchGame" :: roomId::playerId::Nil =>
      new TankObservation(roomId.toInt,playerId.toLong).render
    case _ => <div>Error Page</div>
  }

  def gotoPage(i: String) = {
    dom.document.location.hash = "#" + i
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
