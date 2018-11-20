package com.neo.sk.utils.canvas

import com.neo.sk.tank.shared.util.canvas.MiddleContext
import javafx.geometry.VPos
import javafx.scene.image.{Image, WritableImage}
import javafx.scene.paint.Color
import javafx.scene.canvas.GraphicsContext
import javafx.scene.shape.{StrokeLineCap, StrokeLineJoin}
import javafx.scene.text.{Font, FontWeight, TextAlignment}

/**
  * Created by sky
  * Date on 2018/11/19
  * Time at 下午12:53
  */
object MiddleContextInFx {
  def apply(canvas:MiddleCanvasInFx): MiddleContextInFx = new MiddleContextInFx(canvas)

  //todo 匹配所有情况
  def string2FontWeight(s:String):FontWeight={
    s match {
      case "blod" => FontWeight.BOLD
      case "normal" => FontWeight.NORMAL
      case _ => FontWeight.NORMAL
    }
  }

  implicit def string2TextAlignment(s:String):TextAlignment={
    s match {
      case "center" => TextAlignment.CENTER
      case "left" => TextAlignment.LEFT
      case "right" => TextAlignment.RIGHT
      case _ => TextAlignment.CENTER
    }
  }

  implicit def string2TextBaseline(s:String):VPos={
    s match {
      case "middle"=>VPos.CENTER
      case "top"=>VPos.TOP
      case "center"=>VPos.CENTER
      case "bottom"=>VPos.BOTTOM
      case _ => VPos.CENTER //设置默认值
    }
  }

  implicit def string2StrokeLineCap(s:String):StrokeLineCap={
    s match {
      case "round"=>StrokeLineCap.ROUND
      case "butt" =>StrokeLineCap.BUTT
      case _=>StrokeLineCap.ROUND //设置默认值
    }
  }

  implicit def string2StrokeLineJoin(s:String):StrokeLineJoin={
    s match {
      case "round"=> StrokeLineJoin.ROUND
      case "miter"=> StrokeLineJoin.MITER
      case _ => StrokeLineJoin.ROUND
    }
  }
}

class MiddleContextInFx extends MiddleContext{
  import MiddleContextInFx._

  private[this] var context: GraphicsContext = _

  def this(canvas:MiddleCanvasInFx) = {
    this()
    context = canvas.returnSelf.getGraphicsContext2D
  }

  def returnContext=context

  override def setGlobalAlpha(alpha: Double): Unit = context.setGlobalAlpha(alpha)

  override def setLineWidth(h: Double) = context.setLineWidth(h)

  override def setStrokeStyle(color:String) = {
    context.setStroke(Color.web(color))
  }

  override def arc(x: Double, y: Double, radius: Double, startAngle: Double,
                   endAngle: Double) = context.arc(x, y, radius,radius, startAngle, endAngle)

  override def fill = context.fill()

  override def closePath = context.closePath()

  override def setFill(color: String) = context.setFill(Color.web(color))

  override def moveTo(x: Double, y: Double): Unit = context.moveTo(x, y)

  override def drawImage(image: Any, offsetX: Double, offsetY: Double, size: Option[(Double,Double)]): Unit = {
    image match {
      case js: MiddleImageInFx =>
        if (size.isEmpty) {
          context.drawImage(js.returnSelf, offsetX, offsetY)
        } else {
          context.drawImage(js.returnSelf, offsetX, offsetY, size.get._1, size.get._2)
        }
      case js:WritableImage =>
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

  override def fillText(text: String, x: Double, y: Double, z:Double=500) = context.fillText(text,x,y,z)

  override def setFont(f:String,fw:String,s:Double) = context.setFont(Font.font(f,string2FontWeight(fw),s))

  override def setTextAlign(s:String) = context.setTextAlign(s)

  override def setTextBaseline(s:String)= context.setTextBaseline(s)

  override def setLineCap(s:String) = context.setLineCap(s)

  override def setLineJoin(s:String) = context.setLineJoin(s)

  override def rect(x: Double, y: Double, w: Double, h: Double) = context.rect(x,y,w,h)

  override def strokeText(text: String, x: Double, y: Double, maxWidth: Double) = context.strokeText(text,x,y,maxWidth)

  override def save(): Unit = context.save()

  override def restore(): Unit = context.restore()
}
