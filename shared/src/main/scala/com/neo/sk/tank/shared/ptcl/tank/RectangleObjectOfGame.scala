package com.neo.sk.tank.shared.ptcl.tank

import com.neo.sk.tank.shared.ptcl.model
import com.neo.sk.tank.shared.ptcl.model.Point

/**
  * Created by hongruying on 2018/7/17
  */
trait RectangleObjectOfGame extends ObjectOfGame{

  override var position: model.Point

  protected val width : Double
  protected val height : Double


  override def getObjectRect(): model.Rectangle = {
    model.Rectangle(position - model.Point(width / 2, height / 2),position + model.Point(width / 2, height / 2))
  }

  override def getObjectShape(): model.Shape = {
    getObjectRect()
  }

  override def isIntersects(o: ObjectOfGame): Boolean = {
    o match {
      case t:CircleObjectOfGame => isIntersects(t)
      case t:RectangleObjectOfGame => isIntersects(t)
    }
  }

  private def isIntersects(o: CircleObjectOfGame): Boolean = {
    val topLeft = position - model.Point(width / 2, height / 2)
    val downRight = position + model.Point(width / 2, height / 2)
    if(o.position > topLeft && o.position < downRight){
      true
    }else{
      val relativeCircleCenter:Point = o.position - position
      val dx = math.min(relativeCircleCenter.x, width / 2)
      val dx1 = math.max(dx, - width / 2)
      val dy = math.min(relativeCircleCenter.y, height / 2)
      val dy1 = math.max(dy, - height / 2)
//      println("-----------------------")
//      println("circle",o.getObjectShape())
//      println("rectange",this.getObjectShape())
//
//      println(Point(dx1,dy1).distance(relativeCircleCenter))
//      println(o.r)
//
//      println("+++++++++++++++++++++++")

      Point(dx1,dy1).distance(relativeCircleCenter) < o.r
    }
  }

  private def isIntersects(o: RectangleObjectOfGame): Boolean = {
    getObjectRect().intersects(o.getObjectRect())
  }

}
