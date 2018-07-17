package com.neo.sk.tank.shared.ptcl.tank
import com.neo.sk.tank.shared.ptcl.model
import com.neo.sk.tank.shared.ptcl.model.Point

/**
  * Created by hongruying on 2018/7/9
  * 道具
  * 1.增加生命最大上限
  * 2.移动速度增加
  * 3.炮弹威力增加
  * 4.医疗包
  */
case class PropState(pId:Long,t:Int,p:Point)

trait Prop extends CircleObjectOfGame{

  val pId:Long

  val propType:Int

  //todo
  override def getObjectRect(): model.Rectangle = {
    model.Rectangle(Point(position.x - 2 * model.PropParameters.r/3, position.y - 2 * model.PropParameters.r/3),
      Point(position.x + 2 * model.PropParameters.r/3,position.y + 2 * model.PropParameters.r/3))
  }




  override var position: model.Point

  def getPropState:PropState = PropState(pId,propType,position)

  override val r: Double = model.PropParameters.r

}

object Prop{
  def apply(p:PropState): Prop = {
    p.t match {
      case 1 => AddBloodLevelProp(p.pId,position = p.p)
      case 2 => AddSpeedLevelProp(p.pId,position = p.p)
      case 3 => AddBulletLevelProp(p.pId,position = p.p)
      case 4 => AddBloodProp(p.pId,position = p.p)

    }
  }
}


case class AddBloodLevelProp(
                              override val pId: Long,
                              override val propType: Int = 1,
                              override var position: model.Point
                            ) extends Prop

case class AddSpeedLevelProp(
                              override val pId: Long,
                              override val propType: Int = 2,
                              override var position: model.Point
                            ) extends Prop

case class AddBulletLevelProp(
                              override val pId: Long,
                              override val propType: Int = 3,
                              override var position: model.Point
                            ) extends Prop

case class AddBloodProp(
                              override val pId: Long,
                              override val propType: Int = 4,
                              override var position: model.Point
                            ) extends Prop






