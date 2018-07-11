package com.neo.sk.tank.front.tankClient

import com.neo.sk.tank.shared.ptcl.model
import com.neo.sk.tank.shared.ptcl.tank.{AirDropBox, Prop}

/**
  * Created by hongruying on 2018/7/10
  */
class AirDropBoxClientImpl(
                            override val oId: Long,
                            override protected var position: model.Point
                          ) extends AirDropBox{

}
