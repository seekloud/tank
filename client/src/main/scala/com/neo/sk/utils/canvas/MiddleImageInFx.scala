package com.neo.sk.utils.canvas

import com.neo.sk.tank.shared.util.canvas.MiddleImage
import javafx.scene.image.Image
/**
  * Created by sky
  * Date on 2018/11/16
  * Time at 下午4:51
  */
object MiddleImageInFx{
  def apply(url: String): MiddleImageInFx = new MiddleImageInFx()(url)
}
class MiddleImageInFx extends MiddleImage {
  private[this] var canvas: Image = _
  def this(url:String)={
    this()
    canvas=new Image(url)
  }
}
