package com.neo.sk.tank.front.tankClient.draw

import com.neo.sk.tank.front.tankClient.GameContainerClientImpl
import com.neo.sk.tank.shared.`object`.Tank
import com.neo.sk.tank.shared.model.Constants.LittleMap
import com.neo.sk.tank.shared.model.Point
import org.scalajs.dom.ext.Color

/**
  * Created by hongruying on 2018/8/29
  */
trait Background{ this:GameContainerClientImpl =>


  private def clearScreen(color:String, alpha:Double):Unit = {
    ctx.fillStyle = color
    ctx.globalAlpha = alpha
    ctx.fillRect(0, 0, this.canvasBoundary.x * this.canvasUnit, this.canvasBoundary.y * this.canvasUnit)
    ctx.globalAlpha = 1
  }

  protected def drawLine(start:Point,end:Point):Unit = {
    ctx.beginPath()
    ctx.moveTo(start.x * canvasUnit, start.y * canvasUnit)
    ctx.lineTo(end.x * canvasUnit, end.y * canvasUnit)
    ctx.stroke()
    ctx.closePath()
  }



  protected def drawBackground(offset:Point) = {
    clearScreen("#FCFCFC",1)
    ctx.lineWidth = 1
    ctx.fillStyle = Color.Black.toString()
    ctx.strokeStyle = Color.Black.toString()
    for(i <- 0 to(boundary.x.toInt,3)){
      drawLine(Point(i,0) + offset, Point(i, boundary.y) + offset)
    }

    for(i <- 0 to(boundary.y.toInt,3)){
      drawLine(Point(0,i) + offset, Point(boundary.x, i) + offset)
    }

  }

  protected def drawRank():Unit = {
    //绘制当前排行榜
    val textLineHeight = 18
    val leftBegin =7 * canvasUnit
    val rightBegin = (canvasBoundary.x.toInt-8) * canvasUnit
    //    System.out.println("11111")
    object MyColors {
      val background = "rgb(249,205,173,0.4)"
      val rankList = "#9933FA"
    }


    def drawTextLine(str: String, x: Int, lineNum: Int, lineBegin: Int = 0) = {
      ctx.fillText(str, x, (lineNum + lineBegin - 1) * textLineHeight)
    }
    //    println("22222")
    ctx.font = "12px Helvetica"
    ctx.fillStyle =MyColors.background
    ctx.fillRect(0,0,150,200)

    val currentRankBaseLine = 1
    var index = 0
    ctx.fillStyle = MyColors.rankList
    drawTextLine(s" --- Current Rank --- ", leftBegin, index, currentRankBaseLine)
    currentRank.foreach{ score =>
      index += 1
      drawTextLine(s"[$index]: ${score.n.+("   ").take(3)} kill=${score.k} damage=${score.d}", leftBegin, index, currentRankBaseLine)
    }
    //    println("33333")
    ctx.fillStyle =MyColors.background
    ctx.fillRect(canvasBoundary.x*canvasUnit-160,0,200,200)
    val historyRankBaseLine =1
    index = 0
    ctx.fillStyle = MyColors.rankList
    drawTextLine(s" --- History Rank --- ", rightBegin, index, historyRankBaseLine)
    historyRank.foreach { score =>
      index += 1
      drawTextLine(s"[$index]: ${score.n.+("   ").take(3)} kill=${score.k} damage=${score.d}", rightBegin, index, historyRankBaseLine)
    }
  }


  protected def drawMinimap(tank:Tank) = {
    def drawTankMinimap(position:Point,color:String) = {
      val offset = Point(position.x / boundary.x * LittleMap.w, position.y / boundary.y * LittleMap.h)
      ctx.beginPath()
      ctx.fillStyle = color
      ctx.arc((canvasBoundary.x - LittleMap.w + offset.x) * canvasUnit, (canvasBoundary.y  - LittleMap.h + offset.y) * canvasUnit, 0.5 * canvasUnit,0,2*Math.PI)
      ctx.fill()
      ctx.closePath()
    }

    val mapColor = "rgb(41,238,238,0.3)"
    val myself = "#000080"
    val otherTankColor = "#CD5C5C"
    ctx.fillStyle = mapColor
    ctx.fillRect((canvasBoundary.x - LittleMap.w) * canvasUnit,(canvasBoundary.y  - LittleMap.h) * canvasUnit ,LittleMap.w * canvasUnit ,LittleMap.h * canvasUnit )

    drawTankMinimap(tank.getPosition,myself)
    tankMap.filterNot(_._1 == tank.tankId).values.toList.foreach{ t =>
      drawTankMinimap(t.getPosition,otherTankColor)
    }

  }


  protected def drawKillInformation():Unit = {
    val killInfoList = getDisplayKillInfo()
    if(killInfoList.nonEmpty){
      var offsetY = canvasBoundary.y / 3 * 2
      ctx.beginPath()
      ctx.font="12px Arial"
      ctx.fillStyle = Color.Black.toString()
      ctx.lineWidth = 1
      println(killInfoList)
      killInfoList.foreach{
        case (killerName,killedName,_) =>
          ctx.fillText(s"$killerName 击杀了 $killedName.",15 * canvasUnit, offsetY * canvasUnit, 40 * canvasUnit)
          offsetY -= 2
      }
      ctx.closePath()
    }

  }


  def drawLevel(level:Byte,maxLevel:Byte,name:String,start:Point,length:Float,color:String) = {
    ctx.strokeStyle = "#4D4D4D"
    ctx.lineCap = "round"
    ctx.lineWidth = 30
    ctx.beginPath()
    ctx.moveTo(start.x,start.y)
    ctx.lineTo(start.x+length,start.y)
    ctx.stroke()
    ctx.closePath()

    ctx.lineWidth = 25
    ctx.strokeStyle = color
    if(level == maxLevel){
      ctx.beginPath()
      ctx.moveTo(start.x + length,start.y)
      ctx.lineTo(start.x+length,start.y)
      ctx.stroke()
      ctx.closePath()
    }

    if(level >= 1){
      ctx.beginPath()
      ctx.moveTo(start.x,start.y)
      ctx.lineTo(start.x,start.y)
      ctx.stroke()
      ctx.closePath()


      ctx.lineCap = "butt"
      (0 until level).foreach{ index =>
        ctx.beginPath()
        ctx.moveTo(start.x + index * (length / maxLevel) + 2,start.y)
        ctx.lineTo(start.x + (index + 1) * (length / maxLevel) - 2,start.y)
        ctx.stroke()
        ctx.closePath()
      }
    }




    ctx.font = "bold 20px Arial"
    ctx.textAlign = "center"
    ctx.textBaseline = "middle"
    ctx.fillStyle = "#FCFCFC"
    ctx.fillText(name, start.x + length / 2, start.y)
  }
}
