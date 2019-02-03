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

package org.seekloud.tank.front.utils

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobalScope

/**
  * User: Taoz
  * Date: 11/30/2016
  * Time: 10:24 PM
  */
@js.native
@JSGlobalScope
object JsFunc extends js.Object{

  def decodeURI(str: String): String = js.native

  def decodeURIComponent(str: String): String = js.native

  def unecape(str: String): String = js.native

  def alert(msg: String): Unit = js.native






}
