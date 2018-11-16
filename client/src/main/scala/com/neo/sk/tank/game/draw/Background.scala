package com.neo.sk.tank.game.draw

import java.io.{File, FileInputStream}

import com.neo.sk.tank.game.GameContainerClientImpl
import com.neo.sk.tank.shared.`object`.Tank
import com.neo.sk.tank.shared.model.Constants.LittleMap
import com.neo.sk.tank.shared.model.{Point, Score}
import javafx.geometry.VPos
import javafx.scene.{Camera, SnapshotParameters}
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.{Image, ImageView}
import javax.imageio._
import javafx.scene.paint.Color
import javafx.scene.shape.StrokeLineCap
import javafx.scene.text.{Font, FontWeight, TextAlignment}

import scala.collection.mutable
import com.neo.sk.tank.App
import javafx.embed.swing.SwingFXUtils
import javafx.scene.transform.Transform


/**
  * Created by hongruying on 2018/8/29
  */
trait Background{ this:GameContainerClientImpl =>

  private val cacheCanvasMap = mutable.HashMap.empty[String, Image]
  private val rankWidth = 26
  private val rankHeight = 24
  private val currentRankCanvas = new Canvas(math.max(rankWidth * 10, rankWidth * canvasUnit), math.max(rankHeight * 10, rankHeight * canvasUnit))
  private val currentRankCanvasCtx = currentRankCanvas.getGraphicsContext2D
  private val historyRankCanvas = new Canvas(rankWidth * 10, rankHeight * 10)
  private val historyRankCanvasCtx = historyRankCanvas.getGraphicsContext2D
  var rankUpdated: Boolean = true
  private val goldImg = new Image(App.getClass.getResourceAsStream("/img/金牌.png"))
  private val silverImg = new Image(App.getClass.getResourceAsStream("/img/银牌.png"))
  private val bronzeImg = new Image(App.getClass.getResourceAsStream("/img/铜牌.png"))
  private val minimapCanvas = new Canvas(LittleMap.w * canvasUnit + 6, LittleMap.h * canvasUnit + 6)
  private val minimapCanvasCtx = minimapCanvas.getGraphicsContext2D
  var minimapRenderFrame = 0L
  private var canvasBoundary:Point = canvasSize

  def updateBackSize(canvasSize:Point)={
    cacheCanvasMap.clear()
    canvasBoundary = canvasSize
    rankUpdated = true
    minimapRenderFrame = systemFrame - 1
    currentRankCanvas.setWidth(math.max(rankWidth * canvasUnit, rankWidth * 10))
    currentRankCanvas.setHeight(math.max(rankHeight * canvasUnit, rankHeight * 10))
    historyRankCanvas.setWidth(math.max(rankWidth * canvasUnit, rankWidth * 10))
    historyRankCanvas.setHeight(math.max(rankHeight * canvasUnit, rankHeight * 10))
    minimapCanvas.setWidth(LittleMap.w * canvasUnit + 6)
    minimapCanvas.setHeight(LittleMap.h * canvasUnit + 6)
  }

  private def scale(source: Image, targetWidth: Int, targetHeight: Int, preserveRatio: Boolean): Image = {
    val iv = new ImageView()
    iv.setImage(source)
    iv.setPreserveRatio(preserveRatio)
    iv.setFitWidth(targetWidth)
    iv.setFitHeight(targetHeight)
    iv.snapshot(null, null)
  }

  private def generateBackgroundCanvas() = {
    val cacheCanvas = new Canvas(((boundary.x + canvasBoundary.x) * canvasUnit).toInt, ((boundary.y + canvasBoundary.y) * canvasUnit).toInt)
    println(s"=====width==${cacheCanvas.getWidth}=====height====${cacheCanvas.getHeight}")
    val ctxCache = cacheCanvas.getGraphicsContext2D
    clearScreen("#BEBEBE", 1, boundary.x + canvasBoundary.x, boundary.y + canvasBoundary.y, ctxCache)
    clearScreen("#E8E8E8",1, boundary.x, boundary.y, ctxCache, canvasBoundary / 2)
    ctxCache.setLineWidth(1)
    ctxCache.setStroke(Color.rgb(0,0,0,0.05))
    for(i <- 0  to((boundary.x + canvasBoundary.x).toInt,2)){
      drawLine(Point(i,0), Point(i, boundary.y + canvasBoundary.y), ctxCache)
    }

    for(i <- 0  to((boundary.y + canvasBoundary.y).toInt,2)){
      drawLine(Point(0 ,i), Point(boundary.x + canvasBoundary.x, i), ctxCache)
    }
    val param = new SnapshotParameters()
    param.setTransform(Transform.scale(boundary.x + canvasBoundary.x, boundary.y + canvasBoundary.y))
    val img = cacheCanvas.snapshot(param, null)
//    saveToFile(img)
    img
  }

  private def saveToFile(image: Image) = {
    val file = new File("/Users/wang/IdeaProjects/tank/client/src/main/resources/img/background.png")
    if (!file.exists()) file.createNewFile()
    val bImage = SwingFXUtils.fromFXImage(image, null)
    ImageIO.write(bImage, "png", file)
  }

  private def clearScreen(color:String, alpha:Double, width:Float = canvasBoundary.x, height:Float = canvasBoundary.y, context:GraphicsContext = ctx , start:Point = Point(0,0)):Unit = {
    context.setFill(Color.web(color))
    context.setGlobalAlpha(alpha)
    context.fillRect(start.x * canvasUnit, start.y * canvasUnit,  width * this.canvasUnit, height * this.canvasUnit)
    context.setGlobalAlpha(1)
  }

//  protected def drawBackground(offset:Point) = {
//    clearScreen("#FCFCFC",1)
//    val cacheCanvas = cacheCanvasMap.getOrElseUpdate("background",generateBackgroundCanvas())
//    ctx.drawImage(cacheCanvas, (-offset.x + canvasBoundary.x/2), (-offset.y+canvasBoundary.y/2 ), canvasBoundary.x, canvasBoundary.y, 0, 0, canvasBoundary.x * canvasUnit, canvasBoundary.y * canvasUnit)
//  }

  protected def drawBackground(offset:Point) = {
    clearScreen("#BEBEBE",1, canvasBoundary.x, canvasBoundary.y, ctx)
    val boundStart = Point(canvasBoundary.x/2, canvasBoundary.y/2)
    val boundEnd = Point(canvasBoundary.x/2 + boundary.x, canvasBoundary.y/2 + boundary.y)
    val canvasStart = Point(-offset.x + canvasBoundary.x/2, -offset.y + canvasBoundary.y/2)
    val canvasEnd = Point(-offset.x + canvasBoundary.x/2 * 3, -offset.y + canvasBoundary.y/2 * 3)
    val start = Point(math.max(boundStart.x, canvasStart.x), math.max(boundStart.y, canvasStart.y))
    val end = Point(math.min(boundEnd.x, canvasEnd.x), math.min(boundEnd.y, canvasEnd.y))
    val width = end.x - start.x
    val height = end.y - start.y
    if(canvasStart.x < boundStart.x && canvasStart.y > boundStart.y){
      clearScreen("#E8E8E8", 1, width, height, ctx, Point(canvasBoundary.x - width, 0))
    }
    else if(canvasStart.x > boundStart.x && canvasStart.y < boundStart.y){
      clearScreen("#E8E8E8", 1, width, height, ctx, Point(0, canvasBoundary.y - height))
    }
    else if(canvasStart.x < boundStart.x && canvasStart.y < boundStart.y){
      clearScreen("#E8E8E8", 1, width, height, ctx, Point(canvasBoundary.x - width, canvasBoundary.y - height))
    }
    else{
      clearScreen("#E8E8E8", 1, width, height, ctx)
    }
    ctx.setLineWidth(3)
    ctx.setStroke(Color.rgb(0,0,0,0.05))

    for(i <- (64 - canvasStart.x % 64) to canvasBoundary.x by 64f){
      drawLine(Point(i,0), Point(i, canvasBoundary.y), ctx)
    }
    for(i <- (64 - canvasStart.y % 64) to canvasBoundary.y by 64f){
      drawLine(Point(0, i), Point(canvasBoundary.x, i), ctx)
    }

  }

  protected def drawLine(start:Point,end:Point, context:GraphicsContext = ctx):Unit = {
    context.beginPath()
    context.moveTo(start.x * canvasUnit, start.y * canvasUnit)
    context.lineTo(end.x * canvasUnit, end.y * canvasUnit)
    context.stroke()
    context.closePath()
  }

  protected def drawRank():Unit = {

      def drawTextLine(str: String, x: Float, y: Float, context: GraphicsContext):Unit = {
        context.fillText(str, x, y)
      }

      def refreshCacheCanvas(context: GraphicsContext, header: String, rank: List[Score], historyRank: Boolean): Unit = {
        //绘制当前排行榜
        val unit = currentRankCanvas.getWidth / rankWidth
        println(s"rank =${historyRankCanvas.getWidth}, canvasUnit=${canvasUnit}, unit=${unit}")
        val leftBegin = 5 * unit
        context.setFont(Font.font("Arial", FontWeight.BOLD, 12))
        context.clearRect(0, 0, currentRankCanvas.getWidth, currentRankCanvas.getHeight)

        var index = 0
        context.setFill(Color.BLACK)
        context.setTextAlign(TextAlignment.CENTER)
        context.setTextBaseline(VPos.CENTER)
        context.setLineCap(StrokeLineCap.ROUND)
        drawTextLine(header, (currentRankCanvas.getWidth/2).toFloat, unit.toFloat, context)
        rank.foreach { score =>
          index += 1
          val drawColor = index match {
            case 1 => "#FFD700"
            case 2 => "#D1D1D1"
            case 3 => "#8B5A00"
            case _ => "#CAE1FF"
          }
          val imgOpt = index match {
            case 1 => Some(goldImg)
            case 2 => Some(silverImg)
            case 3 => Some(bronzeImg)
            case _ => None
          }
          imgOpt.foreach{ img =>
            context.drawImage(img, leftBegin - 5 * unit, (2 * index) * unit, 2 * unit, 2 * unit)
          }
          context.setStroke(Color.web(drawColor))
          context.setLineWidth(1.8 * unit)
          context.beginPath()
          context.moveTo(leftBegin,(2 * index + 1) * unit)
          context.lineTo((rankWidth - 2) * unit,(2 * index + 1) * unit)
          context.stroke()
          context.closePath()
          context.setTextAlign(TextAlignment.LEFT)
          if(historyRank) drawTextLine(s"[$index]: ${score.n.+("   ").take(3)} kill=${score.k} damage=${score.d}", leftBegin.toFloat, (2 * index + 1) * unit.toFloat, context)
          else drawTextLine(s"[$index]: ${score.n.+("   ").take(3)} kill=${score.k} damage=${score.d} lives=${score.l}", leftBegin.toFloat, (2 * index + 1) * unit.toFloat, context)
        }
        //      drawTextLine(s"当前房间人数 ${index}", 28*canvasUnit, (2 * index + 1) * canvasUnit, context)

      }

      def refresh(): Unit = {
        refreshCacheCanvas(currentRankCanvasCtx, " --- Current Rank --- ", currentRank, false)
        refreshCacheCanvas(historyRankCanvasCtx, " --- History Rank --- ", historyRank, true)
      }


      if (rankUpdated) {
        refresh()
        rankUpdated = false
      }
      ctx.setGlobalAlpha(0.8)
      val params = new SnapshotParameters
      params.setFill(Color.TRANSPARENT)
      ctx.drawImage(currentRankCanvas.snapshot(params, null), 0, 0)
      val params1 = new SnapshotParameters
      params1.setFill(Color.TRANSPARENT)
      ctx.drawImage(historyRankCanvas.snapshot(params1, null), canvasBoundary.x * canvasUnit - rankWidth * 10, 0)
      ctx.setGlobalAlpha(1)

  }

  protected def drawMinimap(tank:Tank):Unit = {

      def drawTankMinimap(position:Point, color:String, context:GraphicsContext) = {
        val offset = Point(position.x / boundary.x * LittleMap.w, position.y / boundary.y * LittleMap.h)
        context.beginPath()
        context.setFill(Color.web(color))
        val centerX = offset.x * canvasUnit + 3
        val centerY = offset.y * canvasUnit + 3
        val radiusX =  0.5 * canvasUnit
        val radiusY =  0.5 * canvasUnit
        val startAngle = 0
        val lengthAngle = 360
        context.arc(centerX.toFloat, centerY.toFloat, radiusX.toFloat, radiusY.toFloat, startAngle.toFloat, lengthAngle.toFloat)
        context.fill()
        context.closePath()
      }


      def refreshMinimap():Unit = {
        val myself = "#000080"
        val otherTankColor = "#CD5C5C"
        val bolderColor = "#8F8F8F"

        minimapCanvasCtx.clearRect(0, 0, minimapCanvas.getWidth, minimapCanvas.getHeight)
        minimapCanvasCtx.setFill(Color.rgb(255, 245, 238, 0.5))
        minimapCanvasCtx.fillRect(3, 3, LittleMap.w * canvasUnit ,LittleMap.h * canvasUnit)
        minimapCanvasCtx.setStroke(Color.web(bolderColor))
        minimapCanvasCtx.setLineWidth(6)
        minimapCanvasCtx.beginPath()
        minimapCanvasCtx.setFill(Color.rgb(255, 245, 238, 0.5))
        minimapCanvasCtx.rect(3, 3 ,LittleMap.w * canvasUnit ,LittleMap.h * canvasUnit)
        minimapCanvasCtx.fill()
        minimapCanvasCtx.stroke()
        minimapCanvasCtx.closePath()

        drawTankMinimap(tank.getPosition,myself, minimapCanvasCtx)
        tankMap.filterNot(_._1 == tank.tankId).values.toList.foreach{ t =>
          drawTankMinimap(t.getPosition,otherTankColor, minimapCanvasCtx)
        }
      }

      if(minimapRenderFrame != systemFrame){
        refreshMinimap()
        minimapRenderFrame = systemFrame
      }

      ctx.drawImage(minimapCanvas.snapshot(new SnapshotParameters(), null), (canvasBoundary.x - LittleMap.w) * canvasUnit - 6, (canvasBoundary.y - LittleMap.h) * canvasUnit - 6)

  }


  protected def drawKillInformation():Unit = {
      val killInfoList = getDisplayKillInfo()
      if(killInfoList.nonEmpty){
        var offsetY = canvasBoundary.y - 30
        ctx.beginPath()
        ctx.setStroke(Color.BLACK)
        ctx.setTextAlign(TextAlignment.JUSTIFY)
        ctx.setFont(Font.font("微软雅黑", FontWeight.BOLD, 25))
        ctx.setLineWidth(1)
        killInfoList.foreach{
          case (killerName,killedName,_) =>
            ctx.strokeText(s"$killerName 击杀了 $killedName",3 * canvasUnit, offsetY * canvasUnit, 40 * canvasUnit)
            offsetY -= 3
        }
        ctx.closePath()
      }

  }

  protected def drawRoomNumber():Unit = {
    ctx.beginPath()
    ctx.setStroke(Color.BLACK)
    ctx.setTextAlign(TextAlignment.LEFT)
    ctx.setFont(Font.font("Arial",3 * canvasUnit))
    ctx.setLineWidth(1)
    val offsetX = canvasBoundary.x - 20
    ctx.strokeText(s"当前在线人数： ${tankMap.size}", offsetX*canvasUnit,(canvasBoundary.y - LittleMap.h -6) * canvasUnit , 20 * canvasUnit)
  }


}
