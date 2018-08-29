package com.neo.sk.tank.shared.`object`

import com.neo.sk.tank.shared.model
import com.neo.sk.tank.shared.model.Point

/**
  * Created by hongruying on 2018/8/22
  * 道具元素
  */
final case class PropState(pId:Int,t:Byte,position:Point)

trait Prop extends CircleObjectOfGame{

  val pId:Int

  val propType:Byte


  override def getObjectRect(): model.Rectangle = {
    model.Rectangle(model.Point(position.x - radius, position.y - radius),
      model.Point(position.x + radius,position.y + radius))
  }




  override var position: model.Point

  final def getPropState:PropState = PropState(pId,propType,position)

  override val radius: Float

}

object Prop{
  def apply(propState : PropState, propRadius:Float): Prop = {
    propState.t match {
      case 1 => AddBloodLevelProp(propState.pId,propRadius,propState.position)
      case 2 => AddSpeedLevelProp(propState.pId,propRadius,propState.position)
      case 3 => AddBulletLevelProp(propState.pId,propRadius,propState.position)
      case 4 => AddBloodProp(propState.pId,propRadius,propState.position)
      case 5 => ShotgunDurationDataProp(propState.pId,propRadius,propState.position)
    }
  }
}


case class AddBloodLevelProp(
                              override val pId: Int,
                              override val radius: Float,
                              override var position: model.Point,
                              override val propType: Byte = 1
                            ) extends Prop

case class AddSpeedLevelProp(
                              override val pId: Int,
                              override val radius: Float,
                              override var position: model.Point,
                              override val propType: Byte = 2
                            ) extends Prop

case class AddBulletLevelProp(
                               override val pId: Int,
                               override val radius: Float,
                               override var position: model.Point,
                               override val propType: Byte = 3
                             ) extends Prop

case class AddBloodProp(
                         override val pId: Int,
                         override val radius: Float,
                         override var position: model.Point,
                         override val propType: Byte = 4
                       ) extends Prop

/**
  * 散弹道具
  * */
case class ShotgunDurationDataProp(
                                    override val pId: Int,
                                    override val radius: Float,
                                    override var position: model.Point,
                                    override val propType: Byte = 5
                                  ) extends  Prop
