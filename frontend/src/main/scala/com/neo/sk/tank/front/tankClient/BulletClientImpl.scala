package com.neo.sk.tank.front.tankClient

import com.neo.sk.tank.shared.ptcl.model
import com.neo.sk.tank.shared.ptcl.model.Point
import com.neo.sk.tank.shared.ptcl.tank.Bullet

/**
  * Created by hongruying on 2018/7/10
  */
class BulletClientImpl(
                        override val bId: Long,
                        override val tankId: Long,
                        override protected val startPosition: model.Point,
                        override protected val createTime: Long,
                        override val damage: Int,
                        override protected val momentum: model.Point,
                        override protected var position: model.Point
                      ) extends Bullet{

  //todo
  def getPositionCurFrame(curFrame:Int):Point = {
    //计算当前子弹动画渲染的位置
    null
  }





}
