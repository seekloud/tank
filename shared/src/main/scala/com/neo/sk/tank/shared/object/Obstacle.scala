package com.neo.sk.tank.shared.`object`

import com.neo.sk.tank.shared.config.TankGameConfig
import com.neo.sk.tank.shared.model.Constants.ObstacleType
import com.neo.sk.tank.shared.model.Point

/**
  * Created by hongruying on 2018/7/9
  * 游戏中的阻碍物
  * 空投（包含随机道具），
  * 砖头（可被子弹打碎）
  * 钢铁（）
  * 河流
  *
  */
case class ObstacleState(oId:Int,t:Byte,b:Option[Int],p:Point)

//阻碍坦克的移动
trait ObstacleTank

trait ObstacleBullet

trait Obstacle extends RectangleObjectOfGame{

  val oId:Int

  val obstacleType:Byte


  def getObstacleState():ObstacleState

  def attackDamage(d:Int):Unit

  def isLived():Boolean

  final def isIntersectsObject(o:Seq[ObjectOfGame]):Boolean = {
    o.exists(t => t.isIntersects(this))
  }

  def bloodPercent():Float

}

object Obstacle{
  def apply(config: TankGameConfig, obstacleState: ObstacleState): Obstacle = obstacleState.t match {
    case ObstacleType.airDropBox => new AirDropBox(config,obstacleState)
    case ObstacleType.brick => new Brick(config,obstacleState)
    case ObstacleType.steel => new Steel(config,obstacleState)
    case ObstacleType.river => new River(config,obstacleState)

  }
}
