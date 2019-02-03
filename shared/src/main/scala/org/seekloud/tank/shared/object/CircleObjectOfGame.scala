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

/**
  * Created by hongruying on 2018/8/22
  * 圆形的游戏物体元素
  */
trait CircleObjectOfGame extends ObjectOfGame{



  val radius:Float //半径


  final def getRadius = radius

  /**
    * 获取当前元素的包围盒
    * @return  rectangle
    */
  override def getObjectRect(): model.Rectangle = {
    model.Rectangle(position - model.Point(radius, radius),position + model.Point(radius, radius))
  }

  /**
    * 获取当前元素的外形
    * @return  shape
    */
  override def getObjectShape(): model.Shape = {
    model.Circle(position,radius)
  }

  /**
    * 判断元素是否和其他元素有碰撞
    * @param o 其他物体
    * @return  如果碰撞，返回true；否则返回false
    */
  override def isIntersects(o: ObjectOfGame): Boolean = {
    o match {
      case t:CircleObjectOfGame => isIntersects(t)
      case t:RectangleObjectOfGame => isIntersects(t)
    }
  }


  private def isIntersects(o: CircleObjectOfGame): Boolean = {
    this.position.distance(o.position) < o.radius + this.radius
  }

  private def isIntersects(o: RectangleObjectOfGame): Boolean = {
    o.isIntersects(this)
  }

}
