package com.neo.sk.tank.core.tank

import com.neo.sk.tank.shared.ptcl.model
import com.neo.sk.tank.shared.ptcl.tank.Bullet

/**
  * Created by hongruying on 2018/7/10
  */
class BulletServerImpl (
                         override val bId: Int,
                         override val tankId: Int,
                         override protected val startPosition: model.Point,
                         override protected val createTime: Long,
                         override val damage: Int,
                         override protected val momentum: model.Point,
                         override var position: model.Point
                       ) extends Bullet{

}
