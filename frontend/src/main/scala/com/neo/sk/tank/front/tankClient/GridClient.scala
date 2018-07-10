package com.neo.sk.tank.front.tankClient

import com.neo.sk.tank.shared.ptcl.model
import com.neo.sk.tank.shared.ptcl.tank.{Bullet, Grid, Tank}
/**
  * Created by hongruying on 2018/7/9
  */
class GridClient(override val boundary: model.Point) extends Grid {


  override def debug(msg: String): Unit = {}

  override def info(msg: String): Unit = println(msg)

  override def tankExecuteLaunchBulletAction(tankId: Long, tank: Tank, launchBulletCallback: Bullet => Unit): Unit = {}

}
