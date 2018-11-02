package com.neo.sk.tank.view

import com.neo.sk.tank.App
import com.neo.sk.tank.common.Context
import com.neo.sk.tank.shared.model.Point
import javafx.scene.{Group, Scene}
import javafx.scene.canvas.GraphicsContext
import javafx.scene.canvas.Canvas
import javafx.scene.image.Image
import com.neo.sk.utils.JavaFxUtil.getCanvasUnit
import javafx.scene.ImageCursor
/**
  * Created by hongruying on 2018/10/23
  * 玩游戏的view
  *
  */

class PlayGameScreen(context: Context) {

  //todo 此处目前为固定视野，之后修改为可放大
  import javafx.stage.Screen

  val screen= Screen.getPrimary.getVisualBounds
  protected var canvasWidth = screen.getMaxX.toFloat
  protected var canvasHeight = screen.getMaxY.toFloat
  var canvasUnit = getCanvasUnit(canvasWidth)
  var canvasBoundary = Point(canvasWidth, canvasHeight) / canvasUnit
  val group = new Group()
  val canvas=new Canvas()
  canvas.setHeight(canvasHeight)
  canvas.setWidth(canvasWidth)
  val scene = new Scene(group)


  val image = new Image(App.getClass.getResourceAsStream("/img/瞄准.png"))
  scene.setCursor(new ImageCursor(image, image.getWidth / 10, image.getHeight / 10))

  def getScene():Scene = scene

  def getCanvasContext: GraphicsContext = canvas.getGraphicsContext2D

  group.getChildren.add(canvas)

  def drawGameLoading():Unit = {
    getCanvasContext.fillRect(0, 0, canvasBoundary.x * canvasUnit, canvasBoundary.y * canvasUnit)
    getCanvasContext.fillText("请稍等，正在连接服务器", 150, 180)
  }

  def drawGameStop(killerName:String):Unit = {
    getCanvasContext.fillRect(0, 0, canvasBoundary.x * canvasUnit, canvasBoundary.y * canvasUnit)
    getCanvasContext.fillText(s"您已经死亡,被玩家=${killerName}所杀", 150, 180)
  }

  def drawReplayMsg(m:String):Unit = {
    getCanvasContext.fillRect(0, 0, canvasBoundary.x * canvasUnit, canvasBoundary.y * canvasUnit)
    getCanvasContext.fillText(m, 150, 180)
  }

  def draw()={

  }



}
