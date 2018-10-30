package com.neo.sk.tank.view


import com.neo.sk.tank.common.Context
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
  val canvas = new Canvas(1000, 800)
  val gc = canvas.getGraphicsContext2D

  group.getChildren.add(canvas)
  val sence = new Scene(group)

  var startTime = System.currentTimeMillis()
  var lastRenders = 0
  var renders = 0



  def showScanUrl(scanUrl:String) = {
    val url = scanUrl
    val browser = new WebView()
    val webEngine = browser.getEngine
    webEngine.load(scanUrl)
    val text = new Text(500, 500, "请扫码登录")

    val groupNew = new Group()
    groupNew.getChildren.add(text)
    //groupNew.getChildren.add(browser)
    val senceNew = new Scene(groupNew)
    context.switchScene(senceNew)
  }






}

