package com.neo.sk.tank.shared.ptcl.tank
import com.neo.sk.tank.shared.ptcl.model

/**
  * Created by hongruying on 2018/7/17
  */
trait CircleObjectOfGame extends ObjectOfGame{

  override var position: model.Point

  val r:Double

  override def getObjectRect(): model.Rectangle = {
    model.Rectangle(position - model.Point(r,r),position + model.Point(r,r))
  }

  override def getObjectShape(): model.Shape = {
    model.Circle(position,r)
  }

  override def isIntersects(o: ObjectOfGame): Boolean = {
    o match {
      case t:CircleObjectOfGame => isIntersects(t)
      case t:RectangleObjectOfGame => isIntersects(t)
    }
  }


  private def isIntersects(o: CircleObjectOfGame): Boolean = {
    this.position.distance(o.position) < o.r + this.r
  }

  private def isIntersects(o: RectangleObjectOfGame): Boolean = {
    o.isIntersects(this)
  }







}
