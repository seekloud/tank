package com.neo.sk.tank.shared.util.canvas

/**
  * Created by sky
  * Date on 2018/11/16
  * Time at 下午1:56
  * 本文件中定义画图中使用的函数并在实例中重写
  */
trait MiddleCanvas {

  def getCtx:MiddleContext

  def getWidth(): Double

  def getHeight(): Double

  def setWidth(h: Int): Unit //设置宽

  def setHeight(h: Int): Unit //设置高

  def change2Image(): Any  //转换快照
}
