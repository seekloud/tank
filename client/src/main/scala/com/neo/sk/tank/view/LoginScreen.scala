package com.neo.sk.tank.view

import com.neo.sk.tank.common.Context
import com.neo.sk.tank.shared.model.Point
import javafx.scene.{Group, Scene}
import javafx.scene.canvas.Canvas
import javafx.scene.image.{Image, ImageView}
import javafx.scene.web.WebEngine
import javafx.scene.web.WebView
import javafx.scene.control.ScrollPane
import javafx.scene.text.Text

/**
  * Created by hongruying on 2018/10/23
  */
class LoginScreen(context: Context) {

  val group = new Group()


  def showScanUrl(scanUrl:String) = {
    println(scanUrl)
    val url = scanUrl
    val browser = new WebView()
    val webEngine = browser.getEngine
    webEngine.load(scanUrl)
    val text = new Text(100, 100, "请扫码登录")

    val groupNew = new Group()
    groupNew.getChildren.add(browser)
    groupNew.getChildren.add(text)
    val senceNew = new Scene(groupNew)
    context.switchScene(senceNew)
  }






}

