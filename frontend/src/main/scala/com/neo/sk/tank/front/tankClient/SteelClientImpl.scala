package com.neo.sk.tank.front.tankClient

import com.neo.sk.tank.shared.ptcl
import com.neo.sk.tank.shared.ptcl.model
import com.neo.sk.tank.shared.ptcl.model.Point
import com.neo.sk.tank.shared.ptcl.tank._
import org.scalajs.dom
import org.scalajs.dom.raw.HTMLElement

class SteelClientImpl(
                     override val oId:Int,
                     override var position: model.Point,
                     override val obstacleType: Byte,
                     override protected val width: Float,
                     override protected val height: Float
                     )extends Steel{
  def this(o:ObstacleState,width:Float,height:Float) = {
    this(o.oId,o.p,o.t,width,height)
  }

  override def attacked(bullet: Bullet, destroyCallBack: Obstacle => Unit): Unit = {}

  override def attackDamage(d: Int): Unit = {}

  override def getObjectRect(): model.Rectangle = {
    this.obstacleType match {
      case model.ObstacleParameters.ObstacleType.steel => model.Rectangle(position- model.Point(model.ObstacleParameters.SteelParameters.border / 2,model.ObstacleParameters.SteelParameters.border / 2),position + model.Point(model.ObstacleParameters.SteelParameters.border /2 ,model.ObstacleParameters.SteelParameters.border / 2))
      case model.ObstacleParameters.ObstacleType.river => model.Rectangle(position- model.Point(model.ObstacleParameters.RiverParameters.width / 2,model.ObstacleParameters.RiverParameters.height / 2),position + model.Point(model.ObstacleParameters.RiverParameters.width / 2,model.ObstacleParameters.RiverParameters.height / 2))
    }
  }
  def isIntersectsObject(o:Seq[ObjectOfGame]):Boolean = {
    val rec = this.getObjectRect()
    o.exists(t => t.getObjectRect().intersects(rec))
  }

}
object SteelClientImpl {
  def drawSteel(ctx:dom.CanvasRenderingContext2D,offset:Point,canvasUnit:Int,steel:Obstacle) = {
    val position = steel.getObstacleState().p
    val img = dom.document.createElement("img")
    val image = steel.obstacleType match{
      case model.ObstacleParameters.ObstacleType.steel => img.setAttribute("src","/tank/static/img/钢铁.png")
      case model.ObstacleParameters.ObstacleType.river => img.setAttribute("src","/tank/static/img/河流.png")
    }
    steel.obstacleType match {
      case model.ObstacleParameters.ObstacleType.steel  => ctx.drawImage(img.asInstanceOf[HTMLElement], (position.x - model.ObstacleParameters.SteelParameters.border / 2 + offset.x) * canvasUnit,
        (position.y - model.ObstacleParameters.SteelParameters.border / 2 + offset.y) * canvasUnit,
        model.ObstacleParameters.SteelParameters.border * canvasUnit,model.ObstacleParameters.SteelParameters.border * canvasUnit)
      case model.ObstacleParameters.ObstacleType.river  => ctx.drawImage(img.asInstanceOf[HTMLElement], (position.x - model.ObstacleParameters.RiverParameters.width / 2 + offset.x) * canvasUnit,
        (position.y - model.ObstacleParameters.RiverParameters.height / 2 + offset.y) * canvasUnit,
        model.ObstacleParameters.RiverParameters.width * canvasUnit,model.ObstacleParameters.RiverParameters.height * canvasUnit)
    }
    ctx.fill()
    ctx.stroke()
  }

}
