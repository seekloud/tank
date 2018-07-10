package com.neo.sk.tank.shared.ptcl.tank

import java.awt.event.KeyEvent
import java.util.concurrent.atomic.AtomicLong

import com.neo.sk.tank.shared.ptcl.model._
import com.neo.sk.tank.shared.ptcl.protocol.WsProtocol.TankAction

import scala.collection.mutable

/**
  * Created by hongruying on 2018/7/8
  */
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
    bulletMap.put(bullet.bId,bullet)
  }



  //todo this code need to rebuild 碰撞检测，坦克移动 以及吃道具的判定
  def updateTank():Unit = {
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
    //todo 需要调整

  }

  //todo this code need to rebuild 更新子弹的位置 碰撞检测 如果碰撞到障碍物和坦克，扣血操作等。
  def updateBullet():Unit = {
  }
  //处理本桢的移动
  def update():Unit ={
    handleCurFrameTankAction()
    updateTank() //更新坦克的移动
    updateBullet() //更新坦克的子弹
    tankActionQueueMap -= systemFrame
    systemFrame += 1
  }


  //todo 将本桢接受的所有操作，进行处理，更新坦克的移动操作和坦克炮的方向和开炮操作
  private def handleCurFrameTankAction():Unit = {

  }





  // todo 子弹攻击到坦克的回调函数
  private def attackTankCallBack(o:Tank):Unit = {

  }

  //子弹攻击到障碍物的回调函数
  private def attackObstacleCallBack(o:Obstacle):Unit = {

  }

  //todo 坦克吃到道具的回调函数
  private def tankObtainProp(tank:Tank)(prop: Prop):Unit = {}



  //服务器端需要重写
  def tankExecuteLaunchBulletAction(tankId:Long,tank:Tank,launchBulletCallback:Bullet => Unit) : Unit







}
