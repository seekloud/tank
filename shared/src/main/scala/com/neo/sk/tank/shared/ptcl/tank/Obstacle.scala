package com.neo.sk.tank.shared.ptcl.tank
import com.neo.sk.tank.shared.ptcl.model
import com.neo.sk.tank.shared.ptcl.model.Point

/**
  * Created by hongruying on 2018/7/9
  * 游戏中的阻碍物
  * 空投（包含随机道具），
  * 砖头（可被子弹打碎）
  * --todo钢铁（）
  *
  */
case class ObstacleState(oId:Long,t:Int,b:Option[Int],p:Point)

trait Obstacle extends RectangleObjectOfGame{

  val oId:Long

  val obstacleType:Int

  override var position: model.Point

  override def getObjectRect(): model.Rectangle

  def getObstacleType:Int = obstacleType

  def attacked(bullet: Bullet,destroyCallBack:Obstacle => Unit)

  def getObstacleState():ObstacleState

  def attackDamage(d:Int):Unit

  def isLived():Boolean



}


