package org.seekloud.tank.view

import javafx.scene.image.Image
import javafx.scene.{Group, ImageCursor, Scene}
import org.seekloud.tank.App
import org.seekloud.tank.common.Context
import org.seekloud.tank.shared.model.Point
import org.seekloud.utils.JavaFxUtil.getCanvasUnit
import org.seekloud.utils.canvas.MiddleFrameInFx
/**
  * Created by hongruying on 2018/10/23
  * 玩游戏的view
  *
  */
class PlayGameScreen(context: Context) {

  import javafx.stage.Screen

  val screen= Screen.getPrimary.getVisualBounds
  println(s"----width--${screen.getMaxX.toFloat}")
  println(s"----heght--${screen.getMaxY.toFloat}")
  protected var canvasWidth = screen.getMaxX.toFloat
  protected var canvasHeight = screen.getMaxY.toFloat
  var canvasUnit = getCanvasUnit(canvasWidth)
  var canvasBoundary = Point(canvasWidth, canvasHeight) / canvasUnit
  val drawFrame=new MiddleFrameInFx

  val canvas = drawFrame.createCanvas(canvasWidth,canvasHeight)
//  canvas.setHeight(canvasHeight)
//  canvas.setWidth(canvasWidth)
  val group = new Group()
  val scene = new Scene(group)
//  var listener : CanvasListener = _
//  canvas.widthProperty().bind(scene.widthProperty())
//  canvas.heightProperty()bind(scene.heightProperty())
//  canvas.widthProperty().addListener(e => listener.updateCanvasBoundary())
//  canvas.heightProperty().addListener(e => listener.updateCanvasBoundary())
//  protected var canvasWidth = canvas.getWidth.toFloat
//  protected var canvasHeight = canvas.getHeight.toFloat
//  var canvasUnit = getCanvasUnit(canvas.getWidth.toFloat)
//  var canvasBoundary = Point(canvas.getWidth.toFloat, canvas.getHeight.toFloat) / canvasUnit
  def setCursor={
  println(App.getClass)
    val image = new Image(App.getClass.getResourceAsStream("/img/瞄准_2.png"))
    scene.setCursor(new ImageCursor(image, image.getWidth / 10, image.getHeight / 10))
  }

  def updateSize={
    canvasWidth = screen.getMaxX.toFloat
    canvasHeight = screen.getMaxY.toFloat
  }

  setCursor

  def getScene():Scene = scene

  def getCanvasContext = canvas.getCtx

  group.getChildren.add(canvas.getCanvas)

  def checkScreenSize() = {
    val newCanvasWidth = context.getStageWidth.toFloat
    val newCanvasHeight = if(context.isFullScreen) context.getStageHeight.toFloat else context.getStageHeight.toFloat - 20
    if(canvasWidth != newCanvasWidth || canvasHeight != newCanvasHeight){
      println("the screen size has changed")
      canvasWidth = newCanvasWidth
      canvasHeight = newCanvasHeight
      canvasUnit = getCanvasUnit(newCanvasWidth)
      canvasBoundary = Point(canvasWidth, canvasHeight) / canvasUnit
      canvas.setWidth(newCanvasWidth)
      canvas.setHeight(newCanvasHeight)
      (canvasBoundary, canvasUnit)
    }else (Point(0,0), 0)
  }

  //todo 考虑本部分代码移到shared
  def drawCombatGains(killNum:Int, damageNum:Int, killerList:List[String]):Unit = {
    getCanvasContext.setFont("楷体", "normal", 5 * canvasUnit)
    getCanvasContext.setFill("rgb(0,0,0)")
    getCanvasContext.fillRec(0,0,canvasBoundary.x * canvasUnit,canvasBoundary.y * canvasUnit)
    val img = drawFrame.createImage("/img/dead.png")
    getCanvasContext.drawImage(img,450,250,Some(370,2700))
    getCanvasContext.setTextAlign("left")
    getCanvasContext.setFill("#FFFFFF")
    getCanvasContext.fillText(s"击杀者：", 500, 300)
    getCanvasContext.fillText(s"伤害量：", 500, 350)
    getCanvasContext.fillText(s"击杀者ID：", 500, 400)
    getCanvasContext.setFill("rgb(255,0,0)")
    getCanvasContext.fillText(s"${killNum}", 650, 300)
    getCanvasContext.fillText(s"${damageNum}", 650, 350)
    var pos = 700
    killerList.foreach{r =>
      getCanvasContext.fillText(s"${r}", pos, 400)
      pos = pos + 4 * canvasUnit * r.length}
  }

}

abstract class CanvasListener{
  def updateCanvasBoundary()
}
