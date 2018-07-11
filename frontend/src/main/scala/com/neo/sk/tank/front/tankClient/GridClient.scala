package com.neo.sk.tank.front.tankClient

import com.neo.sk.tank.shared.ptcl.model
import com.neo.sk.tank.shared.ptcl.tank.{Bullet, Grid, Prop, Tank}
import org.scalajs.dom
/**
  * Created by hongruying on 2018/7/9
  */
class GridClient(override val boundary: model.Point) extends Grid {


  override def debug(msg: String): Unit = {}

  override def info(msg: String): Unit = println(msg)

  override def tankExecuteLaunchBulletAction(tankId: Long, tank: Tank): Unit = {}


  override protected def tankEatProp(tank:Tank)(prop: Prop):Unit = {}



  def drawBullet(ctx:dom.CanvasRenderingContext2D,bullet:BulletClientImpl,curFrame:Int) = {
    val position = bullet.getPositionCurFrame(curFrame)
    
  }

}
