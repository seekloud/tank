package com.neo.sk.tank.front.tankClient

import com.neo.sk.tank.shared.ptcl.model
import com.neo.sk.tank.shared.ptcl.tank.Brick

/**
  * Created by hongruying on 2018/7/10
  */
class BrickClientImpl(
                       override val oId: Long,
                       override protected var curBlood: Int,
                       override protected var position: model.Point
                     ) extends Brick{

}
