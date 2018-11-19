package com.neo.sk.tank.shared.util.canvas

/**
  * Created by sky
  * Date on 2018/11/18
  * Time at 下午10:41
  */
trait MiddleContext {
  def setGlobalAlpha(alpha: Double): Unit

  def setLineWidth(h: Double): Unit //设置线

  def setStrokeStyle(s: String): Unit

  def arc(x: Double, y: Double, radius: Double, startAngle: Double,
          endAngle: Double): Unit

  def fill(): Unit

  def closePath(): Unit

  def setFill(color: String): Unit

  def drawImage(image: Any, offsetX: Double, offsetY: Double): Unit

  def moveTo(x: Double, y: Double): Unit

  def fillRec(x: Double, y: Double, w: Double, h: Double):Unit

  def clearRect(x: Double, y: Double, w: Double, h: Double):Unit

  def beginPath():Unit

  def lineTo(x1: Double, y1: Double):Unit

  def stroke():Unit

  def fillText(text: String, x: Double, y: Double):Unit

  def setFont(f:String):Unit

  def setTextAlign(s:String)

  def setTextBaseline(s:String)

  def setLineCap(s:String)

  def rect(x: Double, y: Double, w: Double, h: Double)

  def strokeText(text: String, x: Double, y: Double, maxWidth: Double):Unit
}
