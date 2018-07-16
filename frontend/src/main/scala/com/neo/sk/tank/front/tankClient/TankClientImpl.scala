package com.neo.sk.tank.front.tankClient

import com.neo.sk.tank.shared.ptcl.model
import com.neo.sk.tank.shared.ptcl.model.Point
import com.neo.sk.tank.shared.ptcl.tank.{Tank, TankState}
import org.scalajs.dom
import org.scalajs.dom.ext.Color

/**
  * Created by hongruying on 2018/7/10
  * * 道具
  * * 1.增加生命最大上限
  * * 2.移动速度增加
  * * 3.炮弹威力增加
  * * 4.医疗包

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
    * @param directionOpt 坦克是否移动
    * */
  def getPositionCurFrame(curFrame:Int,maxClientFrame:Int,directionOpt:Option[Double]):Point = {
//    this.position + momentum * curFrame / 1000 * model.Frame.millsAServerFrame / model.Frame.clientFrameAServerFrame
    if(directionOpt.nonEmpty){
      val distance = model.TankParameters.SpeedType.getMoveByFrame(speedLevel) / maxClientFrame * curFrame //每帧移动的距离
      this.position + distance.rotate(directionOpt.get)
    }else position
//    this.position

  }

  //4个点
  def getGunPosition():List[Point] = {
    List(
      Point(0,- model.TankParameters.TankSize.gunH / 2).rotate(this.gunDirection),
      Point(0, model.TankParameters.TankSize.gunH / 2).rotate(this.gunDirection),
      Point(model.TankParameters.TankSize.gunLen,model.TankParameters.TankSize.gunH / 2).rotate(this.gunDirection),
      Point(model.TankParameters.TankSize.gunLen,- model.TankParameters.TankSize.gunH / 2).rotate(this.gunDirection)

    )
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
  def drawTank(ctx:dom.CanvasRenderingContext2D,tank: TankClientImpl,curFrame:Int,maxClientFrame:Int,offset:Point,directionOpt:Option[Double],canvasUnit:Int = 10): Unit ={
    val position = tank.getPositionCurFrame(curFrame,maxClientFrame,directionOpt)

    val gunPositionList = tank.getGunPosition().map(_ + position).map(t => (t + offset) * canvasUnit)
    ctx.beginPath()
    ctx.moveTo(gunPositionList.last.x,gunPositionList.last.y)
    gunPositionList.foreach(t => ctx.lineTo(t.x,t.y))
    ctx.fillStyle = Color.Yellow.toString()
    ctx.fill()
    ctx.closePath()
    ctx.beginPath()
    ctx.arc((position.x + offset.x) * canvasUnit,(position.y + offset.y)*canvasUnit,model.TankParameters.TankSize.r * canvasUnit ,0, 360)
    ctx.fillStyle = Color.Yellow.toString()
    ctx.fill()
    ctx.closePath()

  }


}
