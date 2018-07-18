package com.neo.sk.tank.core.tank

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext
import com.neo.sk.tank.core.{RoomActor, UserActor}
import com.neo.sk.tank.shared.ptcl
import com.neo.sk.tank.shared.ptcl.model
import com.neo.sk.tank.shared.ptcl.model.{Point, Score}
import com.neo.sk.tank.shared.ptcl.protocol.WsProtocol
import com.neo.sk.tank.shared.ptcl.tank._
import org.slf4j.Logger

import scala.util.Random

/**
  * Created by hongruying on 2018/7/10
  */
class GridServerImpl(
                      ctx:ActorContext[RoomActor.Command],
                      log:Logger,
                      dispatch:WsProtocol.WsMsgServer => Unit,
                      dispatchTo:(Long,WsProtocol.WsMsgServer) => Unit,
                      override val boundary: model.Point) extends Grid{

  override def info(msg: String): Unit = {
    log.info(s"[${ctx.self.path} grid] $msg")
  }

  override def debug(msg: String): Unit = {
    log.debug(s"[${ctx.self.path} grid] $msg")
  }

  private var justJoinUser:List[(Long,String,ActorRef[UserActor.Command])] = Nil

  private val random = new Random(System.currentTimeMillis())


  override def tankExecuteLaunchBulletAction(tankId: Long, tank: Tank): Unit = {
    tank.launchBullet() match {
      case Some((bulletDirection,position,damage)) =>
        val m = ptcl.model.BulletParameters.bulletMomentum.rotate(bulletDirection)
        val bullet = new BulletServerImpl(bulletIdGenerator.getAndIncrement(),tankId,position,System.currentTimeMillis(),damage,m,position)
        waitGenBullet = (systemFrame,bullet) :: waitGenBullet
        dispatch(WsProtocol.TankLaunchBullet(systemFrame,bullet.getBulletState()))
        //dispatch() 分发数据子弹位置 以及子弹位置的帧数
      case None => debug(s"tankId=${tankId} has no bullet now")
    }
  }

  override protected def attackTankCallBack(bullet: Bullet)(o:Tank):Unit = {
    super.attackTankCallBack(bullet)(o)
    o.attackedBullet(bullet,dropTankCallBack)
    tankMap.get(bullet.tankId) match {
      case Some(tank) => tank.damageTank += bullet.damage
      case None =>
    }
    dispatch(WsProtocol.TankAttacked(systemFrame,bullet.bId,o.tankId,bullet.damage))
  }

  //子弹攻击到障碍物的回调函数
  override protected def attackObstacleCallBack(bullet: Bullet)(o:Obstacle):Unit = {
    super.attackObstacleCallBack(bullet)(o)
    o.attackDamage(bullet.damage)
    if(!o.isLived()){
      quadTree.remove(o)
      obstacleMap.remove(o.oId)
    }
    if(!o.isLived()){
      val box = if(o.obstacleType == model.ObstacleParameters.ObstacleType.airDropBox){
        val pId = propIdGenerator.getAndIncrement()
        val prop = Prop.apply(PropState(pId,random.nextInt(Int.MaxValue)%4+1,o.position))
        propMap.put(pId,prop)
        quadTree.insert(prop)
        dispatch(WsProtocol.AddProp(systemFrame,pId,propMap.get(pId).get.getPropState))
        genADrop()
      }else{
        genABrick()
      }
      quadTree.insert(box)
      obstacleMap.put(box.oId,box)
    }
    dispatch(WsProtocol.ObstacleAttacked(systemFrame,bullet.bId,o.oId,bullet.damage))
  }

  //生成坦克的
  private def genTank():Unit = {
    while (justJoinUser.nonEmpty){
      val u = justJoinUser.head
      justJoinUser = justJoinUser.tail
      val tank = genATank(u._1,u._2)
      dispatch(WsProtocol.UserEnterRoom(u._1,u._2,tank.getTankState()))
      u._3 ! UserActor.JoinRoomSuccess(tank)
      tankMap.put(tank.tankId,tank)
      quadTree.insert(tank)
    }
  }

  private def genATank(uId:Long,name:String):TankServerImpl = {
    val tId = tankIdGenerator.getAndIncrement()
    val position = genTankPositionRandom()
    var n = new TankServerImpl(ctx.self,uId,tId,name,position)
    val objects = quadTree.retrieveFilter(n).filter(t => t.isInstanceOf[Tank] || t.isInstanceOf[Obstacle])
    while (n.isIntersectsObject(objects)){
      val position = genTankPositionRandom()
      n = new TankServerImpl(ctx.self,uId,tId,name,position)
    }
    n
  }



  def tankFillABullet(tankId:Long):Unit = {
    tankMap.get(tankId) match {
      case Some(tank) => tank.fillABullet()
      case None =>
    }
  }

  private def genTankPositionRandom():Point = {
    Point(random.nextInt(boundary.x.toInt),random.nextInt(boundary.y.toInt))
  }

  private def genObstaclePositionRandom():Point = {
    Point(random.nextInt(boundary.x.toInt - model.ObstacleParameters.border) + model.ObstacleParameters.halfBorder
      ,random.nextInt(boundary.y.toInt - model.ObstacleParameters.border) + model.ObstacleParameters.halfBorder)
  }


  def joinGame(uid:Long,name:String,userActor:ActorRef[UserActor.Command]):Unit = {
    justJoinUser = (uid,name,userActor) :: justJoinUser
  }

  override protected def tankEatProp(tank:Tank)(prop: Prop):Unit = {
    propMap.remove(prop.pId)
    quadTree.remove(prop)
    dispatch(WsProtocol.TankEatProp(systemFrame,prop.pId,tank.tankId,prop.propType))
  }



  def getGridStateWithoutBullet():GridStateWithoutBullet = {
    GridStateWithoutBullet(
      systemFrame,
      tankMap.values.map(_.getTankState()).toList,
      propMap.values.map(_.getPropState).toList,
      obstacleMap.values.map(_.getObstacleState()).toList,
      tankMoveAction = tankMoveAction.toList.flatMap(t => t._2.map(x => (t._1,x)))
    )
  }

  def getGridState():GridState = {
    GridState(
      systemFrame,
      tankMap.values.map(_.getTankState()).toList,
      bulletMap.values.map(_.getBulletState()).toList,
      propMap.values.map(_.getPropState).toList,
      obstacleMap.values.map(_.getObstacleState()).toList,
      tankMoveAction = tankMoveAction.toList.flatMap(t => t._2.map(x => (t._1,x)))
    )
  }

  private def genADrop() = {
    val bId = obstacleIdGenerator.getAndIncrement()
    val position = genObstaclePositionRandom()
    var n = new AirDropBoxImpl(bId,position)
    val objects = quadTree.retrieveFilter(n).filter(t => t.isInstanceOf[Tank] || t.isInstanceOf[Obstacle] || t.isInstanceOf[Prop])
    while (n.isIntersectsObject(objects)){
      val position = genTankPositionRandom()
      n = new AirDropBoxImpl(bId,position)
    }
    n
  }

  private def genABrick() = {
    val bId = obstacleIdGenerator.getAndIncrement()
    val position = genObstaclePositionRandom()
    var n = new BrickServerImpl(bId,position)
    val objects = quadTree.retrieveFilter(n).filter(t => t.isInstanceOf[Tank] || t.isInstanceOf[Prop] || t.isInstanceOf[Obstacle])
    while (n.isIntersectsObject(objects)){
      val position = genTankPositionRandom()
      n = new BrickServerImpl(bId,position)
    }
    n
  }

  def obstaclesInit() = {
    for (i <- 0 until model.ObstacleParameters.AirDropBoxParameters.num) {
      val box = genADrop()
      quadTree.insert(box)
      obstacleMap.put(box.oId, box)
    }

    for (i <- 0 until model.ObstacleParameters.BrickDropBoxParameters.num) {
      val box = genABrick()
      quadTree.insert(box)
      obstacleMap.put(box.oId, box)
    }
  }


  private def dropTankCallBack(bullet:Bullet,tank:Tank):Unit = {
    val pId = propIdGenerator.getAndIncrement()
    val prop = Prop.apply(PropState(pId,random.nextInt(Int.MaxValue) % 4 + 1,tank.position))
    propMap.put(pId,prop)
    quadTree.insert(prop)
    dispatch(WsProtocol.AddProp(systemFrame,pId,propMap.get(pId).get.getPropState))
    quadTree.remove(tank)
    tankMap.remove(tank.tankId)
    tankMoveAction.remove(tank.tankId)
    tankMap.get(bullet.tankId) match {
      case Some(tank) => tank.killTankNum += 1
      case None =>
    }
    dispatchTo(tank.getTankState().userId,WsProtocol.YouAreKilled(bullet.tankId,0L))
  }

//  private[this] def updateRanksByKill(tankId:Long,attackTankId:Long)= {
//
//  }
  implicit val scoreOrdering = new Ordering[Score] {
    override def compare(x: Score, y: Score): Int = {
      var r = y.k - x.k
      if (r == 0) {
        r = y.d - x.d
      }
      if (r == 0) {
        r = (x.id - y.id).toInt
      }
      r
    }
  }

  private[this] def updateRanksByDamage()= {
    currentRank = tankMap.values.map(s => Score(s.tankId, s.name, s.killTankNum, s.damageTank)).toList.sorted
    var historyChange = false
    currentRank.foreach { cScore =>
      historyRankMap.get(cScore.id) match {
        case Some(oldScore) if cScore.d > oldScore.d =>
          historyRankMap += (cScore.id -> cScore)
          historyChange = true
        case None if cScore.d > historyRankThreshold =>
          historyRankMap += (cScore.id -> cScore)
          historyChange = true
        case _ =>

      }


    }

    if (historyChange) {
      historyRank = historyRankMap.values.toList.sorted.take(historyRankLength)
      historyRankThreshold = historyRank.lastOption.map(_.d).getOrElse(-1)
      historyRankMap = historyRank.map(s => s.id -> s).toMap


    }
  }



  override def update(): Unit = {
    super.update()
    genTank()
    updateRanksByDamage()
  }




}
