/*
 * Copyright 2018 seekloud (https://github.com/seekloud)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.seekloud.tank.shared.`object`

import org.seekloud.tank.shared.model
import org.seekloud.tank.shared.model.Point

/**
  * Created by hongruying on 2018/8/22
  * 道具元素
  */
final case class PropState(pId:Int,t:Byte,position:Point, disappearTime:Short)

trait Prop extends CircleObjectOfGame{

  val pId:Int

  val propType:Byte

  protected var disappearTime:Short // 帧数


  def getDisappearTime:Short = disappearTime


  override def getObjectRect(): model.Rectangle = {
    model.Rectangle(model.Point(position.x - radius, position.y - radius),
      model.Point(position.x + radius,position.y + radius))
  }

  //if prop is live return true, else return false
  def updateLifecycle(): Boolean = {
    disappearTime = (disappearTime - 1).toShort
    if(disappearTime > 0) true
    else false
  }

  final def isIntersectsObject(o:Seq[ObjectOfGame]):Boolean = {
    o.exists(t => t.isIntersects(this))
  }

  override var position: model.Point

  final def getPropState:PropState = PropState(pId,propType,position, disappearTime)

  override val radius: Float

}

object Prop{
  def apply(propState : PropState, propRadius:Float): Prop = {
    propState.t match {
      case 1 => AddBloodLevelProp(propState.pId,propRadius,propState.position, propState.disappearTime)
      case 2 => AddSpeedLevelProp(propState.pId,propRadius,propState.position, propState.disappearTime)
      case 3 => AddBulletLevelProp(propState.pId,propRadius,propState.position, propState.disappearTime)
      case 4 => AddBloodProp(propState.pId,propRadius,propState.position, propState.disappearTime)
      case 5 => ShotgunDurationDataProp(propState.pId,propRadius,propState.position, propState.disappearTime)
    }
  }
}


case class AddBloodLevelProp(
                              override val pId: Int,
                              override val radius: Float,
                              override var position: model.Point,
                              override protected var disappearTime:Short,
                              override val propType: Byte = 1
                            ) extends Prop

case class AddSpeedLevelProp(
                              override val pId: Int,
                              override val radius: Float,
                              override var position: model.Point,
                              override protected var disappearTime:Short,
                              override val propType: Byte = 2
                            ) extends Prop

case class AddBulletLevelProp(
                               override val pId: Int,
                               override val radius: Float,
                               override var position: model.Point,
                               override protected var disappearTime:Short,
                               override val propType: Byte = 3
                             ) extends Prop

case class AddBloodProp(
                         override val pId: Int,
                         override val radius: Float,
                         override var position: model.Point,
                         override protected var disappearTime:Short,
                         override val propType: Byte = 4
                       ) extends Prop

/**
  * 散弹道具
  * */
case class ShotgunDurationDataProp(
                                    override val pId: Int,
                                    override val radius: Float,
                                    override var position: model.Point,
                                    override protected var disappearTime:Short,
                                    override val propType: Byte = 5
                                  ) extends  Prop
