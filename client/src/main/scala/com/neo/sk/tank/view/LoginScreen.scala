package com.neo.sk.tank.view

import com.neo.sk.tank.common.Context
import com.neo.sk.tank.shared.model.Point
import javafx.scene.{Group, Scene}
import javafx.scene.canvas.Canvas
/**
  * Created by hongruying on 2018/10/23
  */
class LoginScreen(context: Context) {

  val group = new Group()
  val canvas = new Canvas(1000, 800)
  val gc = canvas.getGraphicsContext2D

  group.getChildren.add(canvas)
  val sence = new Scene(group)

  var startTime = System.currentTimeMillis()
  var lastRenders = 0
  var renders = 0

  def draw():Unit = {
    renders += 1
    val curTime = System.currentTimeMillis()
    if(curTime - startTime > 1000){
      lastRenders = renders
      renders = 0
      startTime = curTime
      println(s"rendessssssssssr=${lastRenders}")
    }
    gc.clearRect(0,0, 1000, 800)
    gc.fillText(s"render=${lastRenders}", 100, 100)
  }


  def showScanUrl(scanUrl:String) = {

  }






}

