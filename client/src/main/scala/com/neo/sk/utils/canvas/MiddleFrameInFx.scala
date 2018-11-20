package com.neo.sk.utils.canvas

import com.neo.sk.tank.shared.util.canvas.{MiddleCanvas, MiddleFrame, MiddleImage}

/**
  * Created by sky
  * Date on 2018/11/17
  * Time at 上午11:29
  */
class MiddleFrameInFx extends MiddleFrame {
  override def createCanvas(width: Double, height: Double): MiddleCanvasInFx = MiddleCanvasInFx(width.toFloat, height.toFloat)

  override def createImage(url: String): MiddleImage = MiddleImageInFx(url)
}
