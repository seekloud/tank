/*
 * Copyright 2018 seekloud (https://github.com/seekloud)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.seekloud.tank.view

import java.io.ByteArrayInputStream

import javafx.geometry.{Insets, Pos}
import javafx.scene.control._
import javafx.scene.image.{Image, ImageView}
import javafx.scene.layout.{GridPane, HBox, _}
import javafx.scene.text.{Font, FontWeight, Text}
import javafx.scene.{Group, Scene}
import org.seekloud.tank.common.{Constants, Context}
import org.seekloud.tank.view.LoginScene.LoginSceneListener
import sun.misc.BASE64Decoder
import org.seekloud.tank.common.AppSettings.framePeriod

/**
  * Created by hongruying on 2018/10/23
  */

object LoginScene {
  trait LoginSceneListener {
    def onButtonConnect()
    def onButtonEmail(mail:String, pwd:String)
    def onLinkToEmail()
    def onLinkToQr()
    def onButtonBot(id:String,key:String,frame:Int)
  }
}

class LoginScreen(context: Context) {
  var loginSceneListener: LoginSceneListener = _

  def showScanUrl(scanUrl:String):Unit = {
    println(scanUrl)
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
    imageView.setY(60)

    val label = new Label("请扫码登录")
    label.setFont(Font.font("Cambria", 32))
    label.setLayoutX(170)
    label.setLayoutY(330)

    val emailLink = new Hyperlink()
    emailLink.setText("邮箱登录")
    emailLink.setFont(Font.font("Cambria", FontWeight.NORMAL, 15))
    emailLink.setOnAction(_ => loginSceneListener.onLinkToEmail())
    emailLink.setLayoutX(220)
    emailLink.setLayoutY(370)


    root.getChildren.add(imageView)
    root.getChildren.add(label)
    root.getChildren.add(emailLink)

    val senceNew = new Scene(root,Constants.SceneBound.weight,Constants.SceneBound.height)
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
    val label = new Label(s"$error")
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


  def emailLogin():Unit = {
    val grid = new GridPane()
    grid.setAlignment(Pos.CENTER)
    grid.setHgap(10)
    grid.setVgap(10)
    grid.setPadding(new Insets(30, 30, 30, 30))

    val sceneTitle = new Text("邮箱登录")
    sceneTitle.setFont(Font.font("Cambria", FontWeight.NORMAL, 20))
    grid.add(sceneTitle, 0, 0, 2, 1)

    val emailLabel = new Label("邮箱")
    grid.add(emailLabel, 0, 1)

    val emailField = new TextField()
    grid.add(emailField, 1, 1)

    val pwdLabel = new Label("密码")
    grid.add(pwdLabel, 0, 2)

    val pwBox = new PasswordField
    grid.add(pwBox, 1, 2)

    val qrLink = new Hyperlink()
    qrLink.setText("二维码登录")
    qrLink.setFont(Font.font("Cambria", FontWeight.NORMAL, 15))
    qrLink.setOnAction(_ => loginSceneListener.onLinkToQr())

    val btn = new Button("登录")
    btn.setOnAction(_ => loginSceneListener.onButtonEmail(emailField.getText, pwBox.getText))

    val hbBtn = new HBox(10)
    hbBtn.setAlignment(Pos.BOTTOM_RIGHT)
    hbBtn.getChildren.addAll(qrLink, btn)
    grid.add(hbBtn, 1, 4)

    val senceNew = new Scene(grid,Constants.SceneBound.weight,Constants.SceneBound.height)
    context.switchScene(senceNew)
  }

  def botLogin() :Unit={

    val grid = new GridPane()
    grid.setAlignment(Pos.CENTER)
    grid.setHgap(10)
    grid.setVgap(10)
    grid.setPadding(new Insets(30, 30, 30, 30))

    val sceneTitle = new Text("bot登录")
    sceneTitle.setFont(Font.font("Cambria", FontWeight.NORMAL, 20))
    grid.add(sceneTitle, 0, 0, 2, 1)

    val warningText = new Text()

    val botIdLabel = new Label("botId")
    grid.add(botIdLabel, 0, 1)

    val botIdField = new TextField()
    grid.add(botIdField, 1, 1)

    val botKeyLabel = new Label("botKey")
    grid.add(botKeyLabel, 0, 2)

    val botKeyField = new TextField()
    grid.add(botKeyField, 1, 2)

    val botFrameLable = new Label("botFrame")
    grid.add(botFrameLable,0,3)

    val botFrameField = new TextField()
    grid.add(botFrameField,1,3)


    val btn = new Button("BotJion")
    btn.setOnAction{_ =>
      val id = botIdField.getText()
      val key = botKeyField.getText()
      val frame = botFrameField.getText().toInt
      if(frame< framePeriod){
        warningText.setText("帧率最小为40")
      }else if(id.trim() == ""){
        warningText.setText("botId不可为空")
      }else if(key.trim() == ""){
        warningText.setText("botKey不可为空")
      }else{
        loginSceneListener.onButtonBot(id,key,frame)
      }
    }

    val hbBtn = new HBox(10)
    hbBtn.setAlignment(Pos.BOTTOM_RIGHT)
    hbBtn.getChildren.addAll( btn)
    grid.add(hbBtn, 1, 4)

    val senceNew = new Scene(grid,Constants.SceneBound.weight,Constants.SceneBound.height)
    context.switchScene(senceNew)
  }

  def setLoginSceneListener(listener: LoginSceneListener) {
    loginSceneListener = listener
  }

}


