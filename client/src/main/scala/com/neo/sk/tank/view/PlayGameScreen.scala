package com.neo.sk.tank.view

import com.neo.sk.tank.common.Context
import com.neo.sk.tank.shared.model.Point
import javafx.scene.{Group, Scene}
import javafx.scene.canvas.GraphicsContext
import javafx.scene.canvas.Canvas
import com.neo.sk.utils.JavaFxUtil.getCanvasUnit
/**
  * Created by hongruying on 2018/10/23
  * 玩游戏的view
  *
  */

class PlayGameScreen(context: Context) {

  //todo 此处目前为固定视野，之后修改为可放大
  protected var canvasWidth = 1200
  protected var canvasHeight = 800
  var canvasUnit = getCanvasUnit(canvasWidth)
  var canvasBoundary = Point(canvasWidth, canvasHeight) / canvasUnit
  val group = new Group()
  val canvas=new Canvas()
  canvas.setHeight(canvasHeight)
  canvas.setWidth(canvasWidth)

  def getScene():Scene = new Scene(group)

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
