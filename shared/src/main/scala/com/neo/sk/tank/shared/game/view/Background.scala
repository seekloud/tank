package com.neo.sk.tank.shared.game.view

import com.neo.sk.tank.shared.`object`.Tank
import com.neo.sk.tank.shared.game.GameContainerImpl
import com.neo.sk.tank.shared.util.canvas.{MiddleCanvas, MiddleContext, MiddleFrame, MiddleImage}
import com.neo.sk.tank.shared.model.Constants.LittleMap
import com.neo.sk.tank.shared.model.{Point, Score}

import scala.collection.mutable

/**
  * Created by hongruying on 2018/8/29
  */
trait Background{ this:GameContainerImpl =>

  val drawFrame:MiddleFrame
  //fixme 将此处map暴露给子类
  private val cacheCanvasMap = mutable.HashMap.empty[String, MiddleCanvas]
  private var canvasBoundary:Point=canvasSize

  private val rankWidth = 26
  private val rankHeight = 24
  private val currentRankCanvas=drawFrame.createCanvas(math.max(rankWidth * canvasUnit, 26 * 10),math.max(rankHeight * canvasUnit, 24 * 10))
  private val historyRankCanvas=drawFrame.createCanvas(math.max(rankWidth * canvasUnit, 26 * 10),math.max(rankHeight * canvasUnit, 24 * 10))
  var rankUpdated: Boolean = true
  private val goldImg=drawFrame.createImage("")
  private val silverImg=drawFrame.createImage("")
  private val bronzeImg=drawFrame.createImage("")

  private val minimapCanvas=drawFrame.createCanvas(LittleMap.w * canvasUnit + 6,LittleMap.h * canvasUnit + 6)
  private val minimapCanvasCtx=minimapCanvas.getCtx

  var minimapRenderFrame = 0L


  def updateBackSize(canvasSize:Point)={
    cacheCanvasMap.clear()
    canvasBoundary=canvasSize

    rankUpdated = true
    minimapRenderFrame = systemFrame - 1
    currentRankCanvas.setWidth(math.max(rankWidth * canvasUnit, 26 * 10))
    currentRankCanvas.setHeight(math.max(rankHeight * canvasUnit, 24 * 10))
    historyRankCanvas.setWidth(math.max(rankWidth * canvasUnit, 26 * 10))
    historyRankCanvas.setHeight(math.max(rankHeight * canvasUnit, 24 * 10))
    minimapCanvas.setWidth(LittleMap.w * canvasUnit + 6)
    minimapCanvas.setHeight(LittleMap.h * canvasUnit + 6)
  }


  private def generateBackgroundCanvas() = {
    val blackBackground = "rgba(0, 0, 0 ,0.05)"
    val cacheCanvas = drawFrame.createCanvas(((boundary.x + canvasBoundary.x) * canvasUnit).toInt,((boundary.y + canvasBoundary.y) * canvasUnit).toInt)
    val cacheCanvasCtx=cacheCanvas.getCtx
    clearScreen("#BEBEBE", 1, boundary.x + canvasBoundary.x, boundary.y + canvasBoundary.y, cacheCanvasCtx)
    clearScreen("#E8E8E8",1, boundary.x, boundary.y, cacheCanvas.getCtx, canvasBoundary / 2)

    cacheCanvasCtx.setLineWidth(1)
    cacheCanvasCtx.setStrokeStyle(blackBackground)
    for(i <- 0  to((boundary.x + canvasBoundary.x).toInt,2)){
      drawLine(Point(i,0), Point(i, boundary.y + canvasBoundary.y), cacheCanvasCtx)
    }

    for(i <- 0  to((boundary.y + canvasBoundary.y).toInt,2)){
      drawLine(Point(0 ,i), Point(boundary.x + canvasBoundary.x, i), cacheCanvasCtx)
    }
    cacheCanvas
  }


  private def clearScreen(color:String, alpha:Double, width:Float = canvasBoundary.x, height:Float = canvasBoundary.y, middleCanvas:MiddleContext , start:Point = Point(0,0)):Unit = {
    middleCanvas.setFill(color)
    middleCanvas.setGlobalAlpha(alpha)
    middleCanvas.fillRec(start.x * canvasUnit, start.y * canvasUnit,  width * this.canvasUnit, height * this.canvasUnit)
    middleCanvas.setGlobalAlpha(1)
  }

  protected def drawLine(start:Point,end:Point, middleCanvas:MiddleContext):Unit = {
    middleCanvas.beginPath
    middleCanvas.moveTo(start.x * canvasUnit, start.y * canvasUnit)
    middleCanvas.lineTo(end.x * canvasUnit, end.y * canvasUnit)
    middleCanvas.stroke()
    middleCanvas.closePath()
  }



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
    ctx.setStrokeStyle("")

    for(i <- (64 - canvasStart.x % 64) to canvasBoundary.x by 64f){
      drawLine(Point(i,0), Point(i, canvasBoundary.y), ctx)
    }
    for(i <- (64 - canvasStart.y % 64) to canvasBoundary.y by 64f){
      drawLine(Point(0, i), Point(canvasBoundary.x, i), ctx)
    }

  }

  protected def drawRank():Unit = {
    def drawTextLine(str: String, x: Double, y: Double, context:MiddleContext) = {
      context.fillText(str, x, y)
    }

    def refreshCacheCanvas(middleCanvas:MiddleContext, header: String, rank: List[Score],historyRank:Boolean): Unit ={
      //绘制当前排行榜
      val unit = currentRankCanvas.getWidth() / rankWidth

      println(s"rank =${historyRankCanvas.getWidth()}, canvasUnit=${canvasUnit}, unit=${unit}")

      val leftBegin = 4 * unit
      middleCanvas.setFont(s"bold ${12}px Arial")
      middleCanvas.clearRect(0,0,currentRankCanvas.getWidth(), currentRankCanvas.getHeight())

      var index = 0
      middleCanvas.setFill("black")
      middleCanvas.setTextAlign("center")
      minimapCanvas.getCtx.setTextBaseline("middle")
      middleCanvas.setLineCap("round")
      drawTextLine(header, currentRankCanvas.getWidth() / 2 , 1 * unit, middleCanvas)
      rank.foreach{ score =>
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
          middleCanvas.drawImage(img, leftBegin - 4 * unit, (2 * index) * unit, 2 * unit, 2 * unit)
        }
        middleCanvas.setStrokeStyle(drawColor)
        middleCanvas.setLineWidth(1.8 * unit)
        middleCanvas.beginPath()
        middleCanvas.moveTo(leftBegin,(2 * index + 1) * unit)
        middleCanvas.lineTo((rankWidth - 2) * unit,(2 * index + 1) * unit)
        middleCanvas.stroke()
        middleCanvas.closePath()
        middleCanvas.setTextAlign("start")
        if(historyRank) drawTextLine(s"[$index]: ${score.n.+("   ").take(3)} kill=${score.k} damage=${score.d}", leftBegin, (2 * index + 1) * unit, context)
        else drawTextLine(s"[$index]: ${score.n.+("   ").take(3)} kill=${score.k} damage=${score.d} lives=${score.l}", leftBegin, (2 * index + 1) * unit, context)
      }
//      drawTextLine(s"当前房间人数 ${index}", 28*canvasUnit, (2 * index + 1) * canvasUnit, context)

    }



    def refresh():Unit = {
      refreshCacheCanvas(currentRankCanvas.getCtx, " --- Current Rank --- ", currentRank,false)
      refreshCacheCanvas(historyRankCanvas.getCtx, " --- History Rank --- ", historyRank,true)
    }


    if(rankUpdated){
      refresh()
      rankUpdated = false
    }
    ctx.setGlobalAlpha(0.8)
    ctx.drawImage(currentRankCanvas,0,0)
    ctx.drawImage(historyRankCanvas, canvasBoundary.x * canvasUnit - historyRankCanvas.getWidth(),0)
    ctx.setGlobalAlpha(1)
  }


  protected def drawMinimap(tank:Tank) = {
    def drawTankMinimap(position:Point,color:String, context:MiddleContext) = {
      val offset = Point(position.x / boundary.x * LittleMap.w, position.y / boundary.y * LittleMap.h)
      context.beginPath()
      context.setFill(color)
      context.arc(offset.x * canvasUnit + 3, offset.y * canvasUnit + 3, 0.5 * canvasUnit,0,2*Math.PI)
      context.fill()
      context.closePath()
    }


    def refreshMinimap():Unit = {
      val mapColor = "rgba(255,245,238,0.5)"
      val myself = "#000080"
      val otherTankColor = "#CD5C5C"
      val bolderColor = "#8F8F8F"

      minimapCanvasCtx.clearRect(0, 0, minimapCanvas.getWidth(), minimapCanvas.getHeight())
      minimapCanvasCtx.setFill(mapColor)
      minimapCanvasCtx.fillRec(3, 3, LittleMap.w * canvasUnit ,LittleMap.h * canvasUnit)
      minimapCanvasCtx.setStrokeStyle(bolderColor)
      minimapCanvasCtx.setLineWidth(6)
      minimapCanvasCtx.beginPath()
      minimapCanvasCtx.setFill(mapColor)
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

    ctx.drawImage(minimapCanvas, (canvasBoundary.x - LittleMap.w) * canvasUnit - 6, (canvasBoundary.y - LittleMap.h) * canvasUnit - 6)

  }


  protected def drawKillInformation():Unit = {
    val killInfoList = getDisplayKillInfo()
    if(killInfoList.nonEmpty){
      var offsetY = canvasBoundary.y - 30
      ctx.beginPath()
      ctx.setStrokeStyle("black")
      ctx.setTextAlign("start")
      ctx.setFont(s"bold ${2.5 * canvasUnit}px 微软雅黑")
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
    ctx.setStrokeStyle("black")
    ctx.setTextAlign("left")
    ctx.setFont(s" ${3 * canvasUnit}px Arial")
    ctx.setLineWidth(1)
    val offsetX = canvasBoundary.x - 20

    ctx.strokeText(s"当前在线人数： ${tankMap.size}", offsetX*canvasUnit,(canvasBoundary.y - LittleMap.h -6) * canvasUnit , 20 * canvasUnit)


  }


}
