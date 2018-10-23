package com.neo.sk.tank.game.draw

import com.neo.sk.tank.game.GameContainerClientImpl
import com.neo.sk.tank.shared.`object`.Tank
import com.neo.sk.tank.shared.model.Constants.LittleMap
import com.neo.sk.tank.shared.model.{Point, Score}
import org.scalajs.dom

import scala.collection.mutable

/**
  * Created by hongruying on 2018/8/29
  */
trait Background{ this:GameContainerClientImpl =>








  private def clearScreen(color:String, alpha:Double, width:Float = canvasBoundary.x, height:Float = canvasBoundary.y, context:dom.CanvasRenderingContext2D = ctx , start:Point = Point(0,0)):Unit = {

  }





  protected def drawBackground(offset:Point) = {
    clearScreen("#FCFCFC",1)

  }

  protected def drawRank():Unit = {

  }


  protected def drawMinimap(tank:Tank) = {


  }


  protected def drawKillInformation():Unit = {


  }

  protected def drawRoomNumber():Unit = {


  }


}
