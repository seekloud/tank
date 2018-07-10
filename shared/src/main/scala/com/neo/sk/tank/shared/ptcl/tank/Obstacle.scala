package com.neo.sk.tank.shared.ptcl.tank
import com.neo.sk.tank.shared.ptcl.model

/**
  * Created by hongruying on 2018/7/9
  * 游戏中的阻碍物
  * 空投（包含随机道具），
  * 砖头（可被子弹打碎）
  * --todo钢铁（）
  *
  */
trait Obstacle extends ObjectOfGame{

  protected val oId:Long

  protected val obstacleType:Int

  override protected var position: model.Point

  override def getObjectRect(): model.Rectangle

  def getObstacleType:Int = obstacleType

  def attacked(bullet: Bullet,destroyCallBack:Obstacle => Unit)

}
