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

import org.seekloud.tank.shared.model.{Point, Rectangle, Shape}

/**
  * Created by hongruying on 2018/8/22
  * 游戏中的所有物体元素基类
  */
trait ObjectOfGame {

  protected var position:Point

  /**
   * 获取当前元素的位置
   * @return  point(x,y)
   */
  final def getPosition:Point = position

  /**
   * 获取当前元素的包围盒
   * @return  rectangle
   */
  def getObjectRect():Rectangle

  /**
   * 获取当前元素的外形
   * @return  shape
   */
  def getObjectShape():Shape

  /**
   * 判断元素是否和其他元素有碰撞
   * @param o 其他物体
   * @return  如果碰撞，返回true；否则返回false
   */
  def isIntersects(o: ObjectOfGame): Boolean


}
