package com.neo.sk.tank.shared.game.canvas

/**
  * Created by sky
  * Date on 2018/11/16
  * Time at 下午1:56
  */
trait MiddleCanvas {
  def getCtx:Any

  def getWidth:Int

  def getHeight:Int

  def setWidth(h:Int):Unit  //设置宽

  def setHeight(h:Int):Unit  //设置高

  def setLineWidth(h:Int):Unit  //设置线

  def setStroke(s:Any):Unit


}
