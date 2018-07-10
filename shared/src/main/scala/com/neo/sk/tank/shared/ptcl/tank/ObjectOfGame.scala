package com.neo.sk.tank.shared.ptcl.tank

import com.neo.sk.tank.shared.ptcl.model.{Point, Rectangle}

/**
  * Created by hongruying on 2018/7/9
  * 游戏中的所有物体
  */
trait ObjectOfGame {

  protected var position:Point

  //获取物体的外形形状box
  def getObjectRect():Rectangle


}
