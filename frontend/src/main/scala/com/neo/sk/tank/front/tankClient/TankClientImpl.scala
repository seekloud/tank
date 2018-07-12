package com.neo.sk.tank.front.tankClient

import com.neo.sk.tank.shared.ptcl.model
import com.neo.sk.tank.shared.ptcl.model.{Frame, Point, TankParameters,Boundary}
import com.neo.sk.tank.shared.ptcl.tank.{Tank, TankState}
import org.scalajs.dom
import org.scalajs.dom.ext.Color
import com.neo.sk.tank.front.tankClient.GameHolder

/**
  * Created by hongruying on 2018/7/10
  */
class TankClientImpl(
                      override protected val userId:Long,
                      override val tankId: Long,
                      override protected var blood: Int,
                      override protected var bloodLevel: Int,
                      override protected var bulletPowerLevel: Int,
                      override protected var curBulletNum: Int,
                      override protected var direction: Double,
                      override protected var gunDirection: Double,
                      override protected var position: model.Point,
                      override protected var speedLevel: Int
                    ) extends Tank{

  def this(tankState:TankState) = {
    this(tankState.userId,tankState.tankId,tankState.blood,tankState.bloodLevel,tankState.bulletPowerLevel,tankState.curBulletNum,tankState.direction,tankState.gunDirection,tankState.position,tankState.speedLevel)
  }

  override protected def startFillBullet(): Unit = {}

  /**
    * tank知道当前systemFrame的初始位置（position），如果isMove，根据(curFrame/maxClientFrame)计算当前动画桢的位置
    * @param curFrame 当前动画帧的帧数
    * @param maxClientFrame 一个systemFrame下动画帧的渲染最大帧数
    * @param isMove 坦克是否移动
    * */
  def getPositionCurFrame(curFrame:Int,maxClientFrame:Int,isMove:Boolean):Point = {
    var position:Point = this.position
    if(isMove){
      val distance = (curFrame + 1) * TankParameters.baseSpeed * this.speedLevel * Frame.millsAServerFrame / 1000//每帧移动的距离
      val plus = Point(distance * Math.cos(this.direction),distance * Math.sin(this.direction))
      position = this.position + plus
    }
    position
  }



}

object TankClientImpl{

  /**
    * 在画布上绘制坦克图像
    * @param ctx canvas绘制
    * @param tank tank代码
    * @param curFrame 动画渲染桢数 （0，1，2，3）
    * @param maxClientFrame 每个systemFrame 动画渲染的帧数
    * */
  def drawGame(ctx:dom.CanvasRenderingContext2D,tank: TankClientImpl,curFrame:Int,maxClientFrame:Int): Unit ={
    ctx.fillStyle = Color.Green.toString()
    ctx.fillRect(0,0,Boundary.w,Boundary.h)
//    tank.position
//    tank.getPositionCurFrame(curFrame,maxClientFrame,true)
  }


}
