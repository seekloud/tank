package com.neo.sk.tank.shared.ptcl.tank
import com.neo.sk.tank.shared.ptcl.model

/**
  * Created by hongruying on 2018/7/9
  * 道具
  * 1.增加生命最大上限
  * 2.移动速度增加
  * 3.炮弹威力增加
  * 4.医疗包
  */
trait Prop extends ObjectOfGame{

  val pId:Long

  val propType:Int

  //todo
  override def getObjectRect(): model.Rectangle = null

  override protected var position: model.Point

}

case class AddBloodLevelProp(
                              override val pId: Long,
                              override val propType: Int = 1,
                              override protected var position: model.Point
                            ) extends Prop

case class AddSpeedLevelProp(
                              override val pId: Long,
                              override val propType: Int = 2,
                              override protected var position: model.Point
                            ) extends Prop

case class AddBulletLevelProp(
                              override val pId: Long,
                              override val propType: Int = 3,
                              override protected var position: model.Point
                            ) extends Prop

case class AddBloodProp(
                              override val pId: Long,
                              override val propType: Int = 4,
                              override protected var position: model.Point
                            ) extends Prop






