package com.neo.sk.tank.core.tank

import com.neo.sk.tank.shared.ptcl.model
import com.neo.sk.tank.shared.ptcl.tank.AirDropBox

/**
  * Created by hongruying on 2018/7/10
  */
class AirDropBoxImpl(
                      override protected val oId: Long,
                      override protected var position: model.Point
                    ) extends AirDropBox{
}
