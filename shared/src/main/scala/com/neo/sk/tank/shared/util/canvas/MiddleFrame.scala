package com.neo.sk.tank.shared.util.canvas

/**
  * Created by sky
  * Date on 2018/11/17
  * Time at 上午11:22
  * 合并两个框架
  */
trait MiddleFrame {
  def createCanvas(width: Double, height: Double): MiddleCanvas

  def createImage(url: String): MiddleImage
}
