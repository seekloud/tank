package com.neo.sk.tank.shared.ptcl.tank

import java.awt.event.KeyEvent
import java.util.concurrent.atomic.{AtomicInteger, AtomicLong}

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
                                 tankMoveAction:List[(Int,List[Int])]
                                 )

case class GridState(
                                   f:Long,
                                   tanks:List[TankState],
                                   bullet:List[BulletState],
                                   props:List[PropState],
                                   obstacle:List[ObstacleState],
                                   tankMoveAction:List[(Int,List[Int])]
                                 )

trait Grid {


  val boundary : Point

  def debug(msg: String): Unit

  def info(msg: String): Unit

  var currentRank = List.empty[Score]

  var historyRankMap =Map.empty[Int,Score]
  var historyRank = historyRankMap.values.toList.sortBy(_.d).reverse
  var historyRankThreshold =if (historyRank.isEmpty)-1 else historyRank.map(_.d).min
  val historyRankLength =5

  val bulletIdGenerator = new AtomicInteger(100)
  val tankIdGenerator = new AtomicInteger(100)
  val obstacleIdGenerator = new AtomicInteger(100)
  val propIdGenerator = new AtomicInteger(100)

  var systemFrame:Long = 0L //系统帧数

  val tankMap = mutable.HashMap[Int,Tank]() //tankId -> Tank
  val bulletMap = mutable.HashMap[Int,Bullet]() //bulletId -> Bullet
  val obstacleMap = mutable.HashMap[Int,Obstacle]() //obstacleId -> Obstacle
  val propMap = mutable.HashMap[Int,Prop]() //propId -> prop 道具信息

  protected var waitGenBullet:List[(Long,Bullet)] = Nil


  val tankMoveAction = mutable.HashMap[Int,mutable.HashSet[Int]]() //tankId -> pressed direction key code

  val tankActionQueueMap = mutable.HashMap[Long,mutable.Queue[(Int,TankAction)]]() //frame -> (tankId,TankAction)

  protected val quadTree : QuadTree = new QuadTree(Rectangle(Point(0,0),boundary)) //四叉树的引用




  def addAction(id:Int,tankAction:TankAction) = {
    addActionWithFrame(id,tankAction,systemFrame)
  }

  def addActionWithFrame(id: Int, tankAction: TankAction, frame: Long) = {
    val queue = tankActionQueueMap.getOrElse(frame,mutable.Queue[(Int,TankAction)]())
    queue.enqueue((id,tankAction))
    tankActionQueueMap.put(frame,queue)
  }

  def removeActionWithFrame(id: Int, tankAction: TankAction, frame: Long) = {
    val queue = tankActionQueueMap.getOrElse(frame,mutable.Queue[(Int,TankAction)]())
    val actionQueue = queue.filterNot(t => t._1 == id && tankAction.serialNum == t._2.serialNum)
    tankActionQueueMap.put(frame,actionQueue)
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


//    val tankList = tankMap.values.toList
//    val obstacleList = obstacleMap.values.toList
    //坦克移动
    tankMoveAction.foreach{
      case (tankId,actionSet) =>
        tankMap.get(tankId) match {
          case Some(tank) =>
            if(tankMap.contains(tankId)){
              getDirection(actionSet) match {
                case Some(d) =>
                  tank.move(d.toFloat,boundary, quadTree)
                case None =>
              }
            }
          case None =>
        }
      case _ =>
    }

    tankMap.foreach{
      case (_,tank) =>
        val tankMayEatProps = quadTree.retrieveFilter(tank).filter(_.isInstanceOf[Prop]).map(_.asInstanceOf[Prop])
        tankMayEatProps.foreach(tank.checkEatProp(_,tankEatProp(tank)))
//        propMap.values.foreach(tank.checkEatProp(_,tankEatProp(tank)))
    }



  }

  //todo 更新子弹的位置 碰撞检测 如果碰撞到障碍物和坦克，扣血操作等。 并且将waitBullet添加到bulletMap
  def updateBullet():Unit = {
//    val tankList = tankMap.values.toList
//    val obstacleList = obstacleMap.values.toList

    bulletMap.foreach{
      case (bId,bullet) =>
        bullet.move(boundary,bulletFlyEndCallback)
        quadTree.retrieveFilter(bullet).filter(_.isInstanceOf[Tank]).map(_.asInstanceOf[Tank]).filter(_.tankId != bullet.tankId)
          .foreach(t => bullet.checkAttackObject(t,attackTankCallBack(bullet)))
        quadTree.retrieveFilter(bullet).filter(_.isInstanceOf[Obstacle]).map(_.asInstanceOf[Obstacle])
          .foreach(t => bullet.checkAttackObject(t,attackObstacleCallBack(bullet)))


//        tankList.filter(_.tankId != bullet.tankId).foreach(t => bullet.checkAttackObject(t,attackTankCallBack(bullet)))
//        obstacleList.foreach(t => bullet.checkAttackObject(t,attackObstacleCallBack(bullet)))
    }
  }

  def updateGenBullet():Unit = {
    waitGenBullet.filter(_._1 <= systemFrame).foreach{
      case (frame,bullet) =>
        if(frame == systemFrame){
          bulletMap.put(bullet.bId,bullet)
        }else{
          for(_ <- 1 to (systemFrame - frame).toInt) bullet.move(boundary,bulletFlyEndCallback)
          bulletMap.put(bullet.bId,bullet)
        }
        quadTree.insert(bullet)
    }
    waitGenBullet = waitGenBullet.filter(_._1 > systemFrame)
  }

  //处理本桢的移动
  def update():Unit ={
    handleCurFrameTankAction()
    updateTank() //更新坦克的移动
    updateBullet() //更新坦克的子弹
    updateGenBullet() //更新刚发的子弹
    quadTree.refresh(quadTree)
    tankActionQueueMap -= systemFrame
    systemFrame += 1
  }


  // 将本桢接受的所有操作，进行处理，更新坦克的移动操作和坦克炮的方向和开炮操作
  def handleCurFrameTankAction():Unit = {
    val curActionQueue = tankActionQueueMap.getOrElse(systemFrame,mutable.Queue[(Int,TankAction)]())
    while (curActionQueue.nonEmpty){
      val (tankId,action) = curActionQueue.dequeue()
      val tankMoveSet = tankMoveAction.getOrElse(tankId,mutable.HashSet[Int]())
      tankMap.get(tankId) match {
        case Some(tank) =>
          action match {
            case WsFrontProtocol.MouseMove(d,serialNum) => tank.setTankGunDirection(d)
            case WsFrontProtocol.PressKeyDown(k,serialNum) =>
              tankMoveSet.add(k)
              tankMoveAction.put(tankId,tankMoveSet)
            case WsFrontProtocol.PressKeyUp(k,serialNum) =>
              tankMoveSet.remove(k)
              tankMoveAction.put(tankId,tankMoveSet)
            case WsFrontProtocol.MouseClick(_,serialNum) =>
              tankExecuteLaunchBulletAction(tankId,tank)

            case WsFrontProtocol.GunDirectionOffset(offset,serialNum) => tank.setTankGunDirectionByOffset(offset)


            case _ => debug(s"tankId=${tankId} action=${action} is no valid")
          }
        case None => debug(s"tankId=${tankId} action=${action} is no valid")
      }
    }

  }





  // todo 子弹攻击到坦克的回调函数
  protected def attackTankCallBack(bullet: Bullet)(o:Tank):Unit = {
    bulletMap.remove(bullet.bId)
    quadTree.remove(bullet)
  }



  //子弹攻击到障碍物的回调函数
  protected def attackObstacleCallBack(bullet: Bullet)(o:Obstacle):Unit = {
    bulletMap.remove(bullet.bId)
    quadTree.remove(bullet)
  }



  //todo 坦克吃到道具的回调函数
  protected def tankEatProp(tank:Tank)(prop: Prop):Unit

  protected def bulletFlyEndCallback(bullet: Bullet):Unit = {
    bulletMap.remove(bullet.bId)
    quadTree.remove(bullet)
  }






  protected def tankExecuteLaunchBulletAction(tankId:Int,tank:Tank) : Unit

  def leftGame(tankId:Int):Unit = {
    tankMoveAction.remove(tankId)
    tankMap.get(tankId).foreach(t => quadTree.remove(t))
    tankMap.remove(tankId)

  }

  def getGridState():GridState = {
    GridState(
      systemFrame,
      tankMap.values.map(_.getTankState()).toList,
      bulletMap.values.map(_.getBulletState()).toList,
      propMap.values.map(_.getPropState).toList,
      obstacleMap.values.map(_.getObstacleState()).toList,
      tankMoveAction = tankMoveAction.toList.map(t => (t._1,t._2.toList))
    )
  }







}
