package com.neo.sk.tank.front.tankClient.draw

import com.neo.sk.tank.front.common.Routes
import com.neo.sk.tank.front.tankClient.GameContainerClientImpl
import com.neo.sk.tank.shared.`object`.Tank
import com.neo.sk.tank.shared.model.Constants.LittleMap
import com.neo.sk.tank.shared.model.{Point, Score}
import org.scalajs.dom
import org.scalajs.dom.ext.Color
import org.scalajs.dom.html

import scala.collection.mutable

/**
  * Created by hongruying on 2018/8/29
  */
trait Background{ this:GameContainerClientImpl =>

  //fixme 将此处map暴露给子类
  private val cacheCanvasMap = mutable.HashMap.empty[String, html.Canvas]
  private var canvasBoundary:Point=canvasSize

  private val rankWidth = 26
  private val rankHeight = 24
  private val currentRankCanvas = dom.document.createElement("canvas").asInstanceOf[html.Canvas]
  private val currentRankCanvasCtx = currentRankCanvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
  currentRankCanvas.width = rankWidth * canvasUnit
  currentRankCanvas.height = rankHeight * canvasUnit
  private val historyRankCanvas = dom.document.createElement("canvas").asInstanceOf[html.Canvas]
  private val historyRankCanvasCtx = historyRankCanvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
  historyRankCanvas.width = rankWidth * canvasUnit
  historyRankCanvas.height = rankHeight * canvasUnit
  var rankUpdated: Boolean = true
  private val goldImg = dom.document.createElement("img").asInstanceOf[html.Image]
  goldImg.setAttribute("src", s"${Routes.base}/static/img/金牌.png")
  private val silverImg = dom.document.createElement("img").asInstanceOf[html.Image]
  silverImg.setAttribute("src", s"${Routes.base}/static/img/银牌.png")
  private val bronzeImg = dom.document.createElement("img").asInstanceOf[html.Image]
  bronzeImg.setAttribute("src", s"${Routes.base}/static/img/铜牌.png")

  private val minimapCanvas = dom.document.createElement("canvas").asInstanceOf[html.Canvas]
  private val minimapCanvasCtx = minimapCanvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
  minimapCanvas.width = LittleMap.w * canvasUnit + 6
  minimapCanvas.height = LittleMap.h * canvasUnit + 6
  var minimapRenderFrame = 0L


  def updateBackSize(canvasSize:Point)={
    cacheCanvasMap.clear()
    canvasBoundary=canvasSize
  }


  private def generateBackgroundCanvas():html.Canvas = {
    val blackBackground = "rgba(0, 0, 0 ,0.05)"
    val cacheCanvas = dom.document.createElement("canvas").asInstanceOf[html.Canvas]
    val ctxCache = cacheCanvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
    cacheCanvas.width = ((boundary.x + canvasBoundary.x) * canvasUnit).toInt
    cacheCanvas.height = ((boundary.y + canvasBoundary.y) * canvasUnit).toInt
    clearScreen("#BEBEBE", 1, boundary.x + canvasBoundary.x, boundary.y + canvasBoundary.y, ctxCache)
    clearScreen("#E8E8E8",1, boundary.x, boundary.y, ctxCache, canvasBoundary / 2)

    ctxCache.lineWidth = 1
//    ctxCache.fillStyle = blackBackground
    ctxCache.strokeStyle = blackBackground
    for(i <- 0  to((boundary.x + canvasBoundary.x).toInt,2)){
      drawLine(Point(i,0), Point(i, boundary.y + canvasBoundary.y), ctxCache)
    }

    for(i <- 0  to((boundary.y + canvasBoundary.y).toInt,2)){
      drawLine(Point(0 ,i), Point(boundary.x + canvasBoundary.x, i), ctxCache)
    }
    cacheCanvas
  }


  private def clearScreen(color:String, alpha:Double, width:Float = canvasBoundary.x, height:Float = canvasBoundary.y, context:dom.CanvasRenderingContext2D = ctx , start:Point = Point(0,0)):Unit = {
    context.fillStyle = color
    context.globalAlpha = alpha
    context.fillRect(start.x * canvasUnit, start.y * canvasUnit,  width * this.canvasUnit, height * this.canvasUnit)
    context.globalAlpha = 1
  }

  protected def drawLine(start:Point,end:Point, context:dom.CanvasRenderingContext2D = ctx):Unit = {
    context.beginPath()
    context.moveTo(start.x * canvasUnit, start.y * canvasUnit)
    context.lineTo(end.x * canvasUnit, end.y * canvasUnit)
    context.stroke()
    context.closePath()
  }



  protected def drawBackground(offset:Point) = {
    clearScreen("#FCFCFC",1)
//    ctx.lineWidth = 1
//    ctx.fillStyle = Color.Black.toString()
//    ctx.strokeStyle = Color.Black.toString()
//    for(i <- 0 to(boundary.x.toInt,3)){
//      drawLine(Point(i,0) + offset, Point(i, boundary.y) + offset)
//    }
//
//    for(i <- 0 to(boundary.y.toInt,3)){
//      drawLine(Point(0,i) + offset, Point(boundary.x, i) + offset)
//    }
    val cacheCanvas = cacheCanvasMap.getOrElseUpdate("background",generateBackgroundCanvas())
    ctx.drawImage(cacheCanvas,(-offset.x + canvasBoundary.x/2) * canvasUnit,( -offset.y+canvasBoundary.y/2 )* canvasUnit,
      canvasBoundary.x * canvasUnit,canvasBoundary.y * canvasUnit, 0, 0,
      canvasBoundary.x * canvasUnit, canvasBoundary.y * canvasUnit)
  }

  protected def drawRank():Unit = {
    def drawTextLine(str: String, x: Float, y: Float, context:dom.CanvasRenderingContext2D) = {
      context.fillText(str, x, y)
    }

    def refreshCacheCanvas(context:dom.CanvasRenderingContext2D, header: String, rank: List[Score],historyRank:Boolean): Unit ={
      //绘制当前排行榜
      val leftBegin = 4 * canvasUnit
      context.font = "bold 12px Arial"
      context.clearRect(0,0,rankWidth * canvasUnit, rankHeight * canvasUnit)

      var index = 0
      context.fillStyle = Color.Black.toString()
      context.textAlign = "center"
      context.textBaseline = "middle"
      context.lineCap = "round"
      drawTextLine(header, rankWidth / 2 * canvasUnit, 1 * canvasUnit, context)
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
          context.drawImage(img, leftBegin - 4 * canvasUnit, (2 * index) * canvasUnit, 2 * canvasUnit, 2 * canvasUnit)
        }
        context.strokeStyle = drawColor
        context.lineWidth = 18
        context.beginPath()
        context.moveTo(leftBegin,(2 * index + 1) * canvasUnit)
        context.lineTo((rankWidth - 2) * canvasUnit,(2 * index + 1) * canvasUnit)
        context.stroke()
        context.closePath()
        context.textAlign = "start"
        if(historyRank) drawTextLine(s"[$index]: ${score.n.+("   ").take(3)} kill=${score.k} damage=${score.d}", leftBegin, (2 * index + 1) * canvasUnit, context)
        else drawTextLine(s"[$index]: ${score.n.+("   ").take(3)} kill=${score.k} damage=${score.d} lives=${score.l}", leftBegin, (2 * index + 1) * canvasUnit, context)
      }
//      drawTextLine(s"当前房间人数 ${index}", 28*canvasUnit, (2 * index + 1) * canvasUnit, context)

    }



    def refresh():Unit = {
      refreshCacheCanvas(currentRankCanvasCtx, " --- Current Rank --- ", currentRank,false)
      refreshCacheCanvas(historyRankCanvasCtx, " --- History Rank --- ", historyRank,true)
    }


    if(rankUpdated){
      refresh()
      rankUpdated = false
    }
    ctx.globalAlpha = 0.8
    ctx.drawImage(currentRankCanvas,0,0)
    ctx.drawImage(historyRankCanvas, (canvasBoundary.x - rankWidth) * canvasUnit,0)
    ctx.globalAlpha = 1
  }


  protected def drawMinimap(tank:Tank) = {
    def drawTankMinimap(position:Point,color:String, context: dom.CanvasRenderingContext2D) = {
      val offset = Point(position.x / boundary.x * LittleMap.w, position.y / boundary.y * LittleMap.h)
      context.beginPath()
      context.fillStyle = color
      context.arc(offset.x * canvasUnit + 3, offset.y * canvasUnit + 3, 0.5 * canvasUnit,0,2*Math.PI)
      context.fill()
      context.closePath()
    }


    def refreshMinimap():Unit = {
      val mapColor = "rgba(255,245,238,0.5)"
      val myself = "#000080"
      val otherTankColor = "#CD5C5C"
      val bolderColor = "#8F8F8F"

      minimapCanvasCtx.clearRect(0, 0, minimapCanvas.width, minimapCanvas.height)
      minimapCanvasCtx.fillStyle = mapColor
      minimapCanvasCtx.fillRect(3, 3, LittleMap.w * canvasUnit ,LittleMap.h * canvasUnit)
      minimapCanvasCtx.strokeStyle = bolderColor
      minimapCanvasCtx.lineWidth = 6
      minimapCanvasCtx.beginPath()
      minimapCanvasCtx.fillStyle = mapColor
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
      ctx.strokeStyle = Color.Black.toString()
      ctx.textAlign = "start"
      ctx.font="bold 25px 微软雅黑"
      ctx.lineWidth = 1

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
    ctx.strokeStyle = Color.Black.toString()
    ctx.textAlign = "left"
    ctx.font=" 30px Arial"
    ctx.lineWidth = 1
    val offsetX = canvasBoundary.x - 20

    ctx.strokeText(s"当前在线人数： ${tankMap.size}", offsetX*canvasUnit,(canvasBoundary.y - LittleMap.h -6) * canvasUnit , 20 * canvasUnit)


  }


}
