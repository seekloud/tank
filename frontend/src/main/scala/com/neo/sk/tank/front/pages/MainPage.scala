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

    val pageName =
      tokens match {
        case Nil => "首页"
        case "test" :: Nil => "test"
        case "postManager"::Nil => "帖子管理"
        case "versionManager"::Nil =>"版本管理"
        case "register" :: Nil => "管理员登录"
        case "sticky" :: Nil => "置顶管理"
        case "recommendBoardManager":: Nil => "推荐版面管理"
        case "getGameRec" :: Nil => "游戏记录查看"
        case x =>
          println(s"unknown hash: $x")
          "unknow"
      }
    println(tokens)
    println(pageName)
    switchToPage(pageName)
  }


  private val currentPage: Rx[Elem] = currentPageName.map {
    case "首页" => TankDemo.render
    case "游戏记录查看" => GameRecordList.render
    case "test" => <div>TO BE CONTINUE...</div>
    case _ => <div>Error Page</div>
  }

  def gotoPage(i: String) = {
    dom.document.location.hash = "" + i
  }


  def show(): Cancelable = {
    val page =
      <div>
        {currentPage}
      </div>
    mount(dom.document.body, page)
  }

}
