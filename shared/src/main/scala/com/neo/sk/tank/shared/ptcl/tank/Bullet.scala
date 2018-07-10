package com.neo.sk.tank.shared.ptcl.tank
import com.neo.sk.tank.shared.ptcl.model
import com.neo.sk.tank.shared.ptcl.model.Point

/**
  * Created by hongruying on 2018/7/8
  * 子弹
  */
case class BulletState(bId:Long,tankId:Long,position:Point,damage:Int,momentum:Point,startPosition:Point,createTime:Long)

trait Bullet extends ObjectOfGame{

  override protected var position: model.Point

  protected val damage:Int //威力

  protected val momentum: Point //动量

  protected val startPosition: model.Point //起始位置

  protected val createTime:Long

  val bId:Long

  val tankId:Long

  private val maxFlyDistance = model.BulletParameters.maxFlyDistance


  // TODO: 获取子弹外形
  override def getObjectRect(): model.Rectangle = {
    null
  }


  def getBulletState(): BulletState = {
    BulletState(bId,tankId,position,damage,momentum,startPosition,createTime)
  }


  // TODO: 子弹碰撞检测
  def isIntersectsObject(o: ObjectOfGame):Boolean = {
    false
  }

  // todo: 生命周期是否截至或者打到目标
  def isFlyEnd(boundary: Point,other:Seq[ObjectOfGame]):Boolean = {
    false
  }

  // todo
  def move(boundary: Point,other:Seq[ObjectOfGame]):Unit = {

  }

  // todo 检测是否子弹有攻击到，攻击到，执行回调函数
  def checkAttackObject[T <: ObjectOfGame](o:T,attackCallBack:T => Unit):Unit = {

  }


}
