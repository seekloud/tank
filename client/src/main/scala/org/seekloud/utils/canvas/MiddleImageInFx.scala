package org.seekloud.utils.canvas

import org.seekloud.tank.shared.util.canvas.MiddleImage
import javafx.scene.image.Image

/**
  * Created by sky
  * Date on 2018/11/16
  * Time at 下午4:51
  */
object MiddleImageInFx {
  def apply(url: String): MiddleImageInFx = new MiddleImageInFx(url)
}

class MiddleImageInFx extends MiddleImage {
  private[this] var image: Image = _

  def this(url: String) = {
    this()
    image = new Image(url)
  }

  def getImage = image

  override def isComplete: Boolean = true
}
