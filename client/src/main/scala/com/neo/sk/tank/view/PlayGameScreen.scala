package com.neo.sk.tank.view

import com.neo.sk.tank.App
import com.neo.sk.tank.common.Context
import com.neo.sk.tank.shared.model.Point
import javafx.scene.{Group, Scene}
import javafx.scene.canvas.GraphicsContext
import javafx.scene.canvas.Canvas
import javafx.scene.image.Image
import com.neo.sk.utils.JavaFxUtil.getCanvasUnit
import javafx.animation.{Animation, KeyFrame, Timeline}
import javafx.scene.ImageCursor
import javafx.scene.paint.Color
import javafx.scene.text.{Font, FontWeight, TextAlignment}
import javafx.util.Duration
/**
  * Created by hongruying on 2018/10/23
  * 玩游戏的view
  *
  */
class PlayGameScreen(context: Context) {

  //todo 此处目前为固定视野，之后修改为可放大
  import javafx.stage.Screen

  //todo 此处涉及到显卡的最大纹理尺寸
  val screen= Screen.getPrimary.getVisualBounds
  println(s"----width--${screen.getMaxX.toFloat}")
  println(s"----width--${screen.getMaxY.toFloat}")
  protected var canvasWidth = screen.getMaxX.toFloat
  protected var canvasHeight = screen.getMaxY.toFloat
//  protected var canvasWidth = 1440
//  protected var canvasHeight = 900
  var canvasUnit = getCanvasUnit(canvasWidth)
  var canvasBoundary = Point(canvasWidth, canvasHeight) / canvasUnit
  val group = new Group()
  val canvas=new Canvas()
  canvas.setHeight(canvasHeight)
  canvas.setWidth(canvasWidth)
  val scene = new Scene(group)

  def setCursor={
    val image = new Image(App.getClass.getResourceAsStream("/img/瞄准.png"))
    scene.setCursor(new ImageCursor(image, image.getWidth / 10, image.getHeight / 10))
  }

  def getScene():Scene = scene

  def getCanvasContext: GraphicsContext = canvas.getGraphicsContext2D

  group.getChildren.add(canvas)

  def drawGameLoading():Unit = {
    getCanvasContext.setFill(Color.web("#006699"))
    getCanvasContext.fillRect(0, 0, canvasBoundary.x * canvasUnit, canvasBoundary.y * canvasUnit)
    getCanvasContext.setTextAlign(TextAlignment.CENTER)
    getCanvasContext.setFont(Font.font("楷体", FontWeight.NORMAL, 5 * canvasUnit))
    getCanvasContext.setFill(Color.BLACK)
    getCanvasContext.fillText("请稍等，正在连接服务器", 300, 180)
  }

  def drawGameStop(killerName:String):Unit = {
    getCanvasContext.setFill(Color.web("#006699"))
    getCanvasContext.fillRect(0, 0, canvasBoundary.x * canvasUnit, canvasBoundary.y * canvasUnit)
    getCanvasContext.setTextAlign(TextAlignment.CENTER)
    getCanvasContext.setFont(Font.font("楷体", FontWeight.NORMAL, 5 * canvasUnit))
    getCanvasContext.setFill(Color.BLACK)
    getCanvasContext.fillText(s"您已经死亡,被玩家=${killerName}所杀", 300, 180)
  }

  def drawReplayMsg(m:String):Unit = {
    getCanvasContext.setFill(Color.web("#006699"))
    getCanvasContext.fillRect(0, 0, canvasBoundary.x * canvasUnit, canvasBoundary.y * canvasUnit)
    getCanvasContext.setTextAlign(TextAlignment.CENTER)
    getCanvasContext.setFont(Font.font("楷体", FontWeight.NORMAL, 5 * canvasUnit))
    getCanvasContext.setFill(Color.BLACK)
    getCanvasContext.fillText(m, 300, 180)
  }

  def drawGameRestart(countDownTimes:Int,killerName:String): Unit = {
    getCanvasContext.setFill(Color.web("#006699"))
    getCanvasContext.setTextAlign(TextAlignment.CENTER)
    getCanvasContext.setFont(Font.font("楷体", FontWeight.NORMAL, 5 * canvasUnit))
    getCanvasContext.fillRect(0, 0, canvasBoundary.x * canvasUnit, canvasBoundary.y * canvasUnit)
    getCanvasContext.setFill(Color.BLACK)
    getCanvasContext.fillText(s"重新进入房间，倒计时：${countDownTimes}", 300, 100)
    getCanvasContext.fillText(s"您已经死亡,被玩家=${killerName}所杀", 300, 180)
  }

  def drawCombatGains(killNum:Int, damageNum:Int, killerList:List[String]):Unit = {
    getCanvasContext.setFont(Font.font("楷体", FontWeight.NORMAL, 5 * canvasUnit))
    getCanvasContext.setFill(Color.BLACK)
    getCanvasContext.setTextAlign(TextAlignment.LEFT)
    getCanvasContext.fillText(s"击杀者：", 500, 300)
    getCanvasContext.fillText(s"伤害量：", 500, 350)
    getCanvasContext.fillText(s"击杀者ID：", 500, 400)
    getCanvasContext.setFill(Color.RED)
    getCanvasContext.fillText(s"${killNum}", 650, 300)
    getCanvasContext.fillText(s"${damageNum}", 650, 350)
    var pos = 700
    killerList.foreach{r =>
      getCanvasContext.fillText(s"${r}", pos, 400)
      pos = pos + 4 * canvasUnit * r.length}
  }


}
