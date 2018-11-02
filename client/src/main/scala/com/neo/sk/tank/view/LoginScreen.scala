package com.neo.sk.tank.view

import java.io.ByteArrayInputStream

import com.neo.sk.tank.common.Context
import com.neo.sk.tank.shared.model.Point
import javafx.scene.{Group, Scene}
import javafx.scene.canvas.Canvas
import javafx.scene.image.{Image, ImageView}
import javafx.scene.web.WebEngine
import javafx.scene.web.WebView
import javafx.scene.control.{Label, ScrollPane}
import javafx.scene.layout.{BorderPane, HBox}
import javafx.scene.text.Text
import sun.misc.BASE64Decoder

/**
  * Created by hongruying on 2018/10/23
  */
class LoginScreen(context: Context) {

  val group = new Group()
  val sence = new Scene(group)


  def showScanUrl(scanUrl:String) = {
    println(scanUrl)
    val url = scanUrl
    val decoder = new BASE64Decoder()
    val bytes = decoder.decodeBuffer(scanUrl.split(",")(1))
    val root = new BorderPane


    val out = new ByteArrayInputStream(bytes)
    val image = new Image(out)
    println(image.getHeight)
    val imageView = new ImageView()
    imageView.setImage(image)
    val label = new Label("testsssssssssssssss")

    root.setCenter(imageView)
    root.setBottom(label)

    val senceNew = new Scene(root,400,400)
    context.switchScene(senceNew)
  }






}

