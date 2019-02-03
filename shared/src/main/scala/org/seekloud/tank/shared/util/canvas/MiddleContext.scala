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

package org.seekloud.tank.shared.util.canvas

/**
  * Created by sky
  * Date on 2018/11/18
  * Time at 下午10:41
  * 合并框架中的ctx
  */

/**
  * 本文件为了统一JvavFx和Js，请注意以下:
  * color：设置rgb或者rgba或者16进制
  *
  **/
trait MiddleContext {
  def setGlobalAlpha(alpha: Double): Unit

  def setLineWidth(h: Double): Unit //设置线

  def setStrokeStyle(c: String): Unit

  def arc(x: Double, y: Double, radius: Double, startAngle: Double,
          endAngle: Double): Unit

  def fill(): Unit

  def closePath(): Unit

  def setFill(color: String): Unit

  def drawImage(image: Any, offsetX: Double, offsetY: Double, size: Option[(Double, Double)] = None): Unit

  def moveTo(x: Double, y: Double): Unit

  def fillRec(x: Double, y: Double, w: Double, h: Double): Unit

  def clearRect(x: Double, y: Double, w: Double, h: Double): Unit

  def beginPath(): Unit

  def lineTo(x1: Double, y1: Double): Unit

  def stroke(): Unit

  def fillText(text: String, x: Double, y: Double, z: Double = 500): Unit

  def setFont(f: String, fw: String, s: Double): Unit

  def setTextAlign(s: String)

  def setTextBaseline(s: String)

  def setLineCap(s: String)

  def setLineJoin(s: String)

  def rect(x: Double, y: Double, w: Double, h: Double)

  def strokeText(text: String, x: Double, y: Double, maxWidth: Double): Unit

  def save(): Unit

  def restore(): Unit
}
