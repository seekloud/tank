package com.neo.sk.tank.view

import java.io.ByteArrayInputStream
import javafx.geometry.Insets

import com.neo.sk.tank.common.Context
import com.neo.sk.tank.shared.model.Point
import javafx.scene.{Group, Scene}
import javafx.scene.canvas.Canvas
import javafx.scene.image.{Image, ImageView}
import javafx.scene.web.WebEngine
import javafx.scene.web.WebView
import javafx.scene.control.{Button, Label, ScrollPane}
import javafx.scene.layout.{BorderPane, HBox, VBox}
import javafx.scene.text.{Font, Text}

import com.neo.sk.tank.actor.LoginActor
import com.neo.sk.tank.view.LoginScene.LoginSceneListener
import sun.misc.BASE64Decoder

/**
  * Created by hongruying on 2018/10/23
  */

object LoginScene {
  trait LoginSceneListener {
    def onButtonConnect()
  }
}

class LoginScreen(context: Context) {

  val group = new Group()
  val sence = new Scene(group)
  var loginSceneListener: LoginSceneListener = _

  def showScanUrl(scanUrl:String) = {
    println(scanUrl)
    val url = scanUrl
    val decoder = new BASE64Decoder()
    val bytes = decoder.decodeBuffer(scanUrl.split(",")(1))
    val root = new Group()
    val out = new ByteArrayInputStream(bytes)
    val image = new Image(out)
    val imageView = new ImageView()
    imageView.setImage(image)
    imageView.setFitHeight(300)
    imageView.setFitWidth(300)
    imageView.setX(100)
    imageView.setY(100)
    val label = new Label("请扫码登录")
    label.setFont(Font.font("Cambria", 32))
    label.setLayoutX(170)
    label.setLayoutY(430)

    root.getChildren.add(imageView)
    root.getChildren.add(label)

    val senceNew = new Scene(root,500,500)
    context.switchScene(senceNew)
  }

  def loginSuccess() ={
    val root = new BorderPane
    val label = new Label("登录成功")
    label.setFont(Font.font("Cambria", 32))
    root.setCenter(label)
    val senceNew = new Scene(root,500,500)
    context.switchScene(senceNew)
  }


  def getImgError(error: String) ={
    val group = new Group()
    val label = new Label(s"${error}")
    label.setFont(Font.font("Cambria", 32))
    label.setLayoutX(130)
    label.setLayoutY(200)
    group.getChildren.add(label)
    val button = new Button("重新登录")
    button.setLayoutX(210)
    button.setLayoutY(300)
    group.getChildren.add(button)
    button.setOnAction(_ => loginSceneListener.onButtonConnect())
    val senceNew = new Scene(group,500,500)
    context.switchScene(senceNew)
  }

  def setLoginSceneListener(listener: LoginSceneListener) {
    loginSceneListener = listener
  }






}


