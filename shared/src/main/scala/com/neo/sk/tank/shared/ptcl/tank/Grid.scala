package com.neo.sk.tank.shared.ptcl.tank

import java.awt.event.KeyEvent
import java.util.concurrent.atomic.AtomicLong

import com.neo.sk.tank.shared.ptcl.model._
import com.neo.sk.tank.shared.ptcl.protocol.{WsFrontProtocol, WsProtocol}
import com.neo.sk.tank.shared.ptcl.protocol.WsFrontProtocol.TankAction

import scala.collection.mutable

/**
  * Created by hongruying on 2018/7/8
  */

case class GridStateWithoutBullet(
                                 f:Long,
                                 tanks:List[TankState],
                                 props:List[PropState],
                                 obstacle:List[ObstacleState],
                                 tankMoveAction:List[(Long,Int)]
                                 )

case class GridState(
                                   f:Long,
                                   tanks:List[TankState],
                                   bullet:List[BulletState],
                                   props:List[PropState],
                                   obstacle:List[ObstacleState],
                                   tankMoveAction:List[(Long,Int)]
                                 )

trait Grid {

  val boundary : Point

  def debug(msg: String): Unit

  def info(msg: String): Unit

  var currentRank = List.empty[Score]
  var historyRank = List.empty[Score]


  val bulletIdGenerator = new AtomicLong(100L)
  val tankIdGenerator = new AtomicLong(100L)
  val obstacleIdGenerator = new AtomicLong(100L)
  val propIdGenerator = new AtomicLong(100L)

  var systemFrame:Long = 0L //系统帧数

  val tankMap = mutable.HashMap[Long,Tank]() //tankId -> Tank
  val bulletMap = mutable.HashMap[Long,Bullet]() //bulletId -> Bullet
  val obstacleMap = mutable.HashMap[Long,Obstacle]() //obstacleId -> Obstacle
  val propMap = mutable.HashMap[Long,Prop]() //propId -> prop 道具信息

  protected var waitGenBullet:List[(Long,Bullet)] = Nil


  val tankMoveAction = mutable.HashMap[Long,mutable.HashSet[Int]]() //tankId -> pressed direction key code

  val tankActionQueueMap = mutable.HashMap[Long,mutable.Queue[(Long,TankAction)]]() //frame -> (tankId,TankAction)




  def addAction(id:Long,tankAction:TankAction) = {
    addActionWithFrame(id,tankAction,systemFrame)
  }

  def addActionWithFrame(id: Long, tankAction: TankAction, frame: Long) = {
    val queue = tankActionQueueMap.getOrElse(frame,mutable.Queue[(Long,TankAction)]())
    queue.enqueue((id,tankAction))
    tankActionQueueMap.put(frame,queue)
  }



  def addBullet(frame:Long,bullet: Bullet) = {
    waitGenBullet = (frame,bullet) :: waitGenBullet
//    bulletMap.put(bullet.bId,bullet)
  }



  def getDirection(actionSet:mutable.HashSet[Int]):Option[Double] = {
    if(actionSet.contains(KeyEvent.VK_LEFT) && actionSet.contains(KeyEvent.VK_UP)){
      Some(DirectionType.upLeft)
    }else if(actionSet.contains(KeyEvent.VK_RIGHT) && actionSet.contains(KeyEvent.VK_UP)){
      Some(DirectionType.upRight)
    }else if(actionSet.contains(KeyEvent.VK_LEFT) && actionSet.contains(KeyEvent.VK_DOWN)){
      Some(DirectionType.downLeft)
    }else if(actionSet.contains(KeyEvent.VK_RIGHT) && actionSet.contains(KeyEvent.VK_DOWN)){
      Some(DirectionType.downRight)
    }else if(actionSet.contains(KeyEvent.VK_RIGHT)){
      Some(DirectionType.right)
    }else if(actionSet.contains(KeyEvent.VK_LEFT)){
      Some(DirectionType.left)
    }else if(actionSet.contains(KeyEvent.VK_UP) ){
      Some(DirectionType.up)
    }else if(actionSet.contains(KeyEvent.VK_DOWN)){
      Some(DirectionType.down)
    }else None
  }

  def getDirection(actionSet:Set[Int]):Option[Double] = {
    if(actionSet.contains(KeyEvent.VK_LEFT) && actionSet.contains(KeyEvent.VK_UP)){
      Some(DirectionType.upLeft)
    }else if(actionSet.contains(KeyEvent.VK_RIGHT) && actionSet.contains(KeyEvent.VK_UP)){
      Some(DirectionType.upRight)
    }else if(actionSet.contains(KeyEvent.VK_LEFT) && actionSet.contains(KeyEvent.VK_DOWN)){
      Some(DirectionType.downLeft)
    }else if(actionSet.contains(KeyEvent.VK_RIGHT) && actionSet.contains(KeyEvent.VK_DOWN)){
      Some(DirectionType.downRight)
    }else if(actionSet.contains(KeyEvent.VK_RIGHT)){
      Some(DirectionType.right)
    }else if(actionSet.contains(KeyEvent.VK_LEFT)){
      Some(DirectionType.left)
    }else if(actionSet.contains(KeyEvent.VK_UP) ){
      Some(DirectionType.up)
    }else if(actionSet.contains(KeyEvent.VK_DOWN)){
      Some(DirectionType.down)
    }else None
  }

  //todo this code need to rebuild 碰撞检测，坦克移动 以及吃道具的判定
  def updateTank():Unit = {


    val tankList = tankMap.values.toList
    val obstacleList = obstacleMap.values.toList
    //坦克移动
    tankMoveAction.foreach{
      case (tankId,actionSet) =>
        tankMap.get(tankId) match {
          case Some(tank) =>
            if(tankMap.contains(tankId)){
              getDirection(actionSet) match {
                case Some(d) =>
                  tank.move(d,boundary,tankList.filter(_.tankId != tankId) ::: obstacleList)
                case None =>
              }
            }
          case None =>
        }
      case _ =>
    }

    tankMap.foreach{
      case (_,tank) =>
        propMap.values.foreach(tank.checkEatProp(_,tankEatProp(tank)))
    }



  }

  //todo 更新子弹的位置 碰撞检测 如果碰撞到障碍物和坦克，扣血操作等。 并且将waitBullet添加到bulletMap
  def updateBullet():Unit = {
    val tankList = tankMap.values.toList
    val obstacleList = obstacleMap.values.toList

    bulletMap.foreach{
      case (bId,bullet) =>
        bullet.move(boundary,bulletFlyEndCallback)
        tankList.filter(_.tankId != bullet.tankId).foreach(t => bullet.checkAttackObject(t,attackTankCallBack(bullet)))
        obstacleList.foreach(t => bullet.checkAttackObject(t,attackObstacleCallBack(bullet)))
    }
  }

  def updateGenBullet():Unit = {
    waitGenBullet.foreach{
      case (frame,bullet) =>
        if(frame <= systemFrame){
          bulletMap.put(bullet.bId,bullet)
        }else{
          for(_ <- 1 to (frame - systemFrame).toInt) bullet.move(boundary,bulletFlyEndCallback)
          bulletMap.put(bullet.bId,bullet)
        }
    }
    waitGenBullet = Nil
  }

  //处理本桢的移动
  def update():Unit ={
    handleCurFrameTankAction()
    updateTank() //更新坦克的移动
    updateBullet() //更新坦克的子弹
    updateGenBullet() //更新刚发的子弹
    tankActionQueueMap -= systemFrame
    systemFrame += 1
  }


  // 将本桢接受的所有操作，进行处理，更新坦克的移动操作和坦克炮的方向和开炮操作
  def handleCurFrameTankAction():Unit = {
    val curActionQueue = tankActionQueueMap.getOrElse(systemFrame,mutable.Queue[(Long,TankAction)]())
    while (curActionQueue.nonEmpty){
      val (tankId,action) = curActionQueue.dequeue()
      val tankMoveSet = tankMoveAction.getOrElse(tankId,mutable.HashSet[Int]())
      tankMap.get(tankId) match {
        case Some(tank) =>
          action match {
            case WsFrontProtocol.MouseMove(d) => tank.setTankGunDirection(d)
            case WsFrontProtocol.PressKeyDown(k) =>
              tankMoveSet.add(k)
              tankMoveAction.put(tankId,tankMoveSet)
            case WsFrontProtocol.PressKeyUp(k) =>
              tankMoveSet.remove(k)
              tankMoveAction.put(tankId,tankMoveSet)
            case WsFrontProtocol.MouseClick(_) =>
              tankExecuteLaunchBulletAction(tankId,tank)

            case _ => debug(s"tankId=${tankId} action=${action} is no valid")
          }
        case None => debug(s"tankId=${tankId} action=${action} is no valid")
      }
    }

  }





  // todo 子弹攻击到坦克的回调函数
  protected def attackTankCallBack(bullet: Bullet)(o:Tank):Unit = {
    bulletMap.remove(bullet.bId)
  }



  //子弹攻击到障碍物的回调函数
  protected def attackObstacleCallBack(bullet: Bullet)(o:Obstacle):Unit = {
    bulletMap.remove(bullet.bId)
  }

  //todo 坦克吃到道具的回调函数
  protected def tankEatProp(tank:Tank)(prop: Prop):Unit

  protected def bulletFlyEndCallback(bullet: Bullet):Unit = {
    bulletMap.remove(bullet.bId)
  }






  protected def tankExecuteLaunchBulletAction(tankId:Long,tank:Tank) : Unit

  def leftGame(tankId:Long):Unit = {
    tankMoveAction.remove(tankId)
    tankMap.remove(tankId)
  }







}
