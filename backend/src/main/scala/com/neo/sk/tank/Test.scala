package com.neo.sk.tank

import com.neo.sk.tank.shared.ptcl.tank.QuadTree
import com.neo.sk.tank.shared.ptcl.model
import com.neo.sk.tank.core.tank.{BrickServerImpl, TankServerImpl}

/**
  * Created by hongruying on 2018/7/17
  */
object Test {

  val boundary:model.Rectangle = model.Rectangle(model.Point(0,0),model.Point(100,100))
  val quadTree = new QuadTree(boundary)

  val tank1 = new TankServerImpl(null,1,1,100,1,1,1,0,0,model.Point(20,20),1,1,"ss",0,0)
  val tank2 = new TankServerImpl(null,2,2,100,1,1,1,0,0,model.Point(25,25),1,1,"ss",0,0)
  val tank3 = new TankServerImpl(null,3,3,100,1,1,1,0,0,model.Point(80,20),1,1,"ss",0,0)
  val tank4 = new TankServerImpl(null,4,4,100,1,1,1,0,0,model.Point(40,90),1,1,"ss",0,0)
  val tank5 = new TankServerImpl(null,5,5,100,1,1,1,0,0,model.Point(60,90),1,1,"ss",0,0)
  val tank6 = new TankServerImpl(null,6,6,100,1,1,1,0,0,model.Point(12,12),1,1,"ss",0,0)
  val tank7 = new TankServerImpl(null,7,7,100,1,1,1,0,0,model.Point(30,30),1,1,"ss",0,0)
  val tank8 = new TankServerImpl(null,8,8,100,1,1,1,0,0,model.Point(50,50),1,1,"ss",0,0)
  val brick1 = new BrickServerImpl(1,model.Point(70,39))
  val brick2 = new BrickServerImpl(2,model.Point(71.5,35.5))


  quadTree.insert(tank1)
  quadTree.insert(tank2)
  quadTree.insert(tank3)
  quadTree.insert(tank4)
  quadTree.insert(tank5)
  quadTree.insert(tank6)
  quadTree.insert(tank7)
  quadTree.insert(tank8)
  quadTree.insert(brick1)
  quadTree.insert(brick2)

  def main(args: Array[String]): Unit = {
    val x = quadTree.retrieveFilter(brick2).filter(_.isInstanceOf[Obstacle])
    println(x.filter(_.isInstanceOf[Obstacle]).map(_.asInstanceOf[BrickServerImpl].getObstacleState()))
    println(brick2.isIntersectsObject(x))
//    println(tank7.getObjectRect(),quadTree.children(1).children.size)
  }





}
