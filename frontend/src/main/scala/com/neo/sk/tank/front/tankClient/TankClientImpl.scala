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
    * @param isMove 坦克是否移动
    * */
  def getPositionCurFrame(isMove:Boolean):Point = {
    if(isMove){
      val distance = TankParameters.baseSpeed * this.speedLevel * Frame.millsAServerFrame / 1000//每帧移动的距离
      val plus = Point(distance * Math.cos(this.direction),distance * Math.sin(this.direction))
      this.position = this.position + plus
    }
    this.position
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
    ctx.fillStyle = Color.Blue.toString()
    for(curFrame <- maxClientFrame){
      val position = tank.getPositionCurFrame(true)
      ctx.beginPath()
      ctx.arc(position.x,position.y,model.TankParameters.TankSize.w / 2,0,2 * Math.PI)
      ctx.fill()
      ctx.closePath()
    }
  }


}
