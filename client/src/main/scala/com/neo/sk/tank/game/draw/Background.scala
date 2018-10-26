package com.neo.sk.tank.game.draw

import com.neo.sk.tank.game.GameContainerClientImpl
import com.neo.sk.tank.shared.`object`.Tank
import com.neo.sk.tank.shared.model.Constants.LittleMap
import com.neo.sk.tank.shared.model.{Point, Score}
import scala.collection.mutable
import javafx.scene.canvas.GraphicsContext

/**
  * Created by hongruying on 2018/8/29
  */
trait Background{ this:GameContainerClientImpl =>








  private def clearScreen(color:String, alpha:Double, width:Float = canvasBoundary.x, height:Float = canvasBoundary.y, context:GraphicsContext=ctx , start:Point = Point(0,0)):Unit = {

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
