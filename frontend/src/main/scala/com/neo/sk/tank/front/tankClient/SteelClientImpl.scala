package com.neo.sk.tank.front.tankClient

import com.neo.sk.tank.shared.ptcl
import com.neo.sk.tank.shared.ptcl.model
import com.neo.sk.tank.shared.ptcl.model.Point
import com.neo.sk.tank.shared.ptcl.tank._
import org.scalajs.dom
import org.scalajs.dom.raw.HTMLElement

import scala.collection.mutable

class SteelClientImpl(
                     override val oId:Int,
                     override var position: model.Point,
                     override val obstacleType: Byte
                     )extends Steel{
  def this(o:ObstacleState) = {
    this(o.oId,o.p,o.t)
  }

  override def attacked(bullet: Bullet, destroyCallBack: Obstacle => Unit): Unit = {}

  override def attackDamage(d: Int): Unit = {}

  override def getObjectRect(): model.Rectangle = {
      model.Rectangle(position- model.Point(model.ObstacleParameters.SteelParameters.border / 2,model.ObstacleParameters.SteelParameters.border / 2),position + model.Point(model.ObstacleParameters.SteelParameters.border /2 ,model.ObstacleParameters.SteelParameters.border / 2))
  }
  def isIntersectsObject(o:Seq[ObjectOfGame]):Boolean = {
    val rec = this.getObjectRect()
    o.exists(t => t.getObjectRect().intersects(rec))
  }

}
object SteelClientImpl {
  def drawSteel(ctx:dom.CanvasRenderingContext2D,offset:Point,canvasUnit:Int,steel:Obstacle, justAttackedSet:mutable.HashMap[Long,Int]) = {
    val position = steel.getObstacleState().p
    val img = dom.document.createElement("img")

    ctx.beginPath()
    val image = steel.obstacleType match{
      case model.ObstacleParameters.ObstacleType.steel =>
        img.setAttribute("src","/tank/static/img/钢铁.png")
      case model.ObstacleParameters.ObstacleType.river => img.setAttribute("src","/tank/static/img/river.png")
    }
    ctx.drawImage(img.asInstanceOf[HTMLElement], (position.x - model.ObstacleParameters.SteelParameters.border / 2 + offset.x) * canvasUnit,
      (position.y - model.ObstacleParameters.SteelParameters.border / 2 + offset.y) * canvasUnit,
      model.ObstacleParameters.SteelParameters.border * canvasUnit,model.ObstacleParameters.SteelParameters.border * canvasUnit)
    ctx.fill()
    ctx.stroke()
    ctx.closePath()
    if(steel.obstacleType == model.ObstacleParameters.ObstacleType.steel && justAttackedSet.keySet.contains(steel.oId)) {
      val imgData = ctx.getImageData((position.x - model.ObstacleParameters.SteelParameters.border / 2 + offset.x) * canvasUnit,
        (position.y - model.ObstacleParameters.SteelParameters.border / 2 + offset.y) * canvasUnit,
        model.ObstacleParameters.SteelParameters.border * canvasUnit, model.ObstacleParameters.SteelParameters.border * canvasUnit)
      var i = 0
      val len = imgData.data.length
      while ( {
        i < len
      }) { // 改变每个像素的透明度
        imgData.data(i + 3) =  math.ceil(imgData.data(i + 3) * 0.5).toInt
        i += 4
      }
      // 将获取的图片数据放回去。
      ctx.putImageData(imgData, (position.x - model.ObstacleParameters.SteelParameters.border / 2 + offset.x) * canvasUnit,
        (position.y - model.ObstacleParameters.SteelParameters.border / 2 + offset.y) * canvasUnit)
      if(justAttackedSet(steel.oId )< 7){
        justAttackedSet(steel.oId) +=1
      }else justAttackedSet.remove(steel.oId)
    }
  }

}
