package com.neo.sk.tank.front.tankClient

import com.neo.sk.tank.shared.ptcl
import com.neo.sk.tank.shared.ptcl.model
import com.neo.sk.tank.shared.ptcl.model.Point
import com.neo.sk.tank.shared.ptcl.tank._
import org.scalajs.dom

import scala.collection.mutable
/**
  * Created by hongruying on 2018/7/9
  */
class GridClient(override val boundary: model.Point) extends Grid {


  override def debug(msg: String): Unit = {}

  override def info(msg: String): Unit = println(msg)

  override def tankExecuteLaunchBulletAction(tankId: Long, tank: Tank): Unit = {}


  override protected def tankEatProp(tank:Tank)(prop: Prop):Unit = {}

  def playerJoin(tank:TankState) = {
    tankMap.put(tank.tankId,new TankClientImpl(tank))
  }

  def gridSyncStateWithoutBullet(d:GridStateWithoutBullet) = {
    systemFrame = d.f
    tankMap.clear()
    obstacleMap.clear()
    propMap.clear()
    tankMoveAction.clear()
    d.tanks.foreach(t => tankMap.put(t.tankId,new TankClientImpl(t)))
    d.obstacle.foreach{o =>
      o.t match {
        case ptcl.model.ObstacleParameters.ObstacleType.airDropBox => obstacleMap.put(o.oId,new AirDropBoxClientImpl(o))
        case ptcl.model.ObstacleParameters.ObstacleType.brick => obstacleMap.put(o.oId,new BrickClientImpl(o))
        case _ =>
      }
    }
    d.props.foreach(t => propMap.put(t.pId,Prop(t)))
    d.tankMoveAction.foreach{t =>
      val set = tankMoveAction.getOrElse(t._1,mutable.HashSet[Int]())
      set.add(t._2)
      tankMoveAction.put(t._1,set)
    }
  }

  def gridSyncState(d:GridState) = {
    systemFrame = d.f
    tankMap.clear()
    obstacleMap.clear()
    propMap.clear()
    tankMoveAction.clear()
    bulletMap.clear()
    d.tanks.foreach(t => tankMap.put(t.tankId,new TankClientImpl(t)))
    d.obstacle.foreach{o =>
      o.t match {
        case ptcl.model.ObstacleParameters.ObstacleType.airDropBox => obstacleMap.put(o.oId,new AirDropBoxClientImpl(o))
        case ptcl.model.ObstacleParameters.ObstacleType.brick => obstacleMap.put(o.oId,new BrickClientImpl(o))
        case _ =>
      }
    }
    d.props.foreach(t => propMap.put(t.pId,Prop(t)))
    d.tankMoveAction.foreach{t =>
      val set = tankMoveAction.getOrElse(t._1,mutable.HashSet[Int]())
      set.add(t._2)
      tankMoveAction.put(t._1,set)
    }
    d.bullet.foreach(t => bulletMap.put(t.bId,new BulletClientImpl(t)))
  }

  def recvTankAttacked(bId:Long,tId:Long,d:Int) = {
    bulletMap.remove(bId)
    tankMap.get(tId) match {
      case Some(t) =>
        t.attackedDamage(d)
        if(!t.isLived()){
          tankMap.remove(tId)
          tankMoveAction.remove(tId)
        }

      case None =>
    }
  }

  def recvObstacleAttacked(bId:Long,oId:Long,d:Int) = {
    bulletMap.remove(bId)
    obstacleMap.get(oId) match {
      case Some(t) =>
        t.attackDamage(d)
        if(!t.isLived()){
          obstacleMap.remove(oId)
        }
      case None =>
    }
  }

  def recvTankEatProp(tId:Long,pId:Long,pType:Int) = {
    propMap.remove(pId)
    tankMap.get(tId) match {
      case Some(t) =>
        t.eatProp(Prop(PropState(pId,pType,Point(0,0))))
      case None =>
    }
  }




//  def drawBullet(ctx:dom.CanvasRenderingContext2D,bullet:BulletClientImpl,curFrame:Int) = {
//    val position = bullet.getPositionCurFrame(curFrame)
//
//  }

}
