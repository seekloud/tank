package com.neo.sk.tank.front.utils.canvas

import com.neo.sk.tank.shared.util.canvas.MiddleContext
import org.scalajs.dom
import org.scalajs.dom.html
import org.scalajs.dom.html.Canvas

/**
  * Created by sky
  * Date on 2018/11/19
  * Time at 下午1:41
  */
object MiddleContextInJs {
  def apply(canvas:MiddleCanvasInJs): MiddleContextInJs = new MiddleContextInJs(canvas)
}

class MiddleContextInJs extends MiddleContext{
  private[this] var context: dom.CanvasRenderingContext2D = _

  def this(canvas:MiddleCanvasInJs) = {
    this()
    context = canvas.returnSelf.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
  }

  def returnContext=context

  override def setGlobalAlpha(alpha: Double): Unit = context.globalAlpha = alpha

  override def setLineWidth(h: Double) = context.lineWidth = h

  override def setStrokeStyle(color:String): Unit = context.strokeStyle= color

  override def arc(x: Double, y: Double, radius: Double, startAngle: Double,
                   endAngle: Double) = context.arc(x, y, radius, startAngle, endAngle)

  override def fill = context.fill()

  override def closePath = context.closePath()

  override def setFill(color: String) = context.fillStyle = color

  override def moveTo(x: Double, y: Double): Unit = context.moveTo(x, y)

  override def drawImage(image: Any, offsetX: Double, offsetY: Double, size: Option[(Double,Double)]): Unit = {
    image match {
      case js: MiddleImageInJs =>
        if (size.isEmpty) {
          context.drawImage(js.returnSelf, offsetX, offsetY)
        } else {
          context.drawImage(js.returnSelf, offsetX, offsetY, size.get._1, size.get._2)
        }
      case js:Canvas =>
        if (size.isEmpty) {
          context.drawImage(js, offsetX, offsetY)
        } else {
          context.drawImage(js, offsetX, offsetY, size.get._1, size.get._2)
        }
    }
  }

  override def fillRec(x: Double, y: Double, w: Double, h: Double)=context.fillRect(x,y,w,h)

  override def clearRect(x: Double, y: Double, w: Double, h: Double) = context.clearRect(x,y,w,h)

  override def beginPath() = context.beginPath()

  override def lineTo(x1: Double, y1: Double) = context.lineTo(x1,y1)

  override def stroke() = context.stroke()

  override def fillText(text: String, x: Double, y: Double) = context.fillText(text,x,y)

  override def setFont(f:String,fw:String,s:Double) = context.font = s"$fw $f ${s}px"

  override def setTextAlign(s:String) = context.textAlign = s

  override def setTextBaseline(s:String)= context.textBaseline = s

  override def setLineCap(s:String) = context.lineCap = s

  override def setLineJoin(s:String) = context.lineJoin = s

  override def rect(x: Double, y: Double, w: Double, h: Double) = context.rect(x,y,w,h)

  override def strokeText(text: String, x: Double, y: Double, maxWidth: Double) = context.strokeText(text,x,y,maxWidth)
}
