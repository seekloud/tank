package com.neo.sk.tank.shared.ptcl.tank
import com.neo.sk.tank.shared.ptcl.model
import com.neo.sk.tank.shared.ptcl.model.Point

/**
  * Created by hongruying on 2018/7/8
  * 子弹
  */
case class BulletState(bId:Int,tankId:Int,position:Point,damage:Int,momentum:Point,startPosition:Point,createTime:Long)

trait Bullet extends CircleObjectOfGame{

  override var position: model.Point

  val damage:Int //威力

  protected val momentum: Point //动量

  protected val startPosition: model.Point //起始位置

  protected val createTime:Long

  val bId:Int

  val tankId:Int

  private val maxFlyDistance = model.BulletParameters.maxFlyDistance

  override val r: Float = model.BulletSize.r


//  // 获取子弹外形
//  override def getObjectRect(): model.Rectangle = {
//    model.Rectangle(this.position - Point(model.BulletSize.r,model.BulletSize.r),this.position + Point(model.BulletSize.r,model.BulletSize.r))
//  }


  def getBulletState(): BulletState = {
    BulletState(bId,tankId,position,damage,momentum,startPosition,createTime)
  }


  // TODO: 子弹碰撞检测
  def isIntersectsObject(o: ObjectOfGame):Boolean = {
    getObjectRect().intersects(o.getObjectRect())
  }

  // todo: 生命周期是否截至或者打到地图边界
  def isFlyEnd(boundary: Point):Boolean = {
   if( this.position.distance(startPosition) > maxFlyDistance || position.x <= 0 || position.y <= 0 || position.x >= boundary.x || position.y >= boundary.y)
     true
    else
     false
  }

  // todo 先检测是否生命周期结束，如果没结束继续移动
  def move(boundary: Point,flyEndCallBack:Bullet => Unit):Unit = {
    if(isFlyEnd(boundary)){
      flyEndCallBack(this)
    } else{
      this.position = this.position + momentum / 1000 * model.Frame.millsAServerFrame
    }


  }

  // todo 检测是否子弹有攻击到，攻击到，执行回调函数
  def checkAttackObject[T <: ObjectOfGame](o:T,attackCallBack:T => Unit):Unit = {
    if(this.isIntersects(o)){
      attackCallBack(o)
    }

  }


}
