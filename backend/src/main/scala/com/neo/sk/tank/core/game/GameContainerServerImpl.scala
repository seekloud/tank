package com.neo.sk.tank.core.game

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.TimerScheduler
import com.neo.sk.tank.core.{RoomActor, UserActor}
import com.neo.sk.tank.shared.`object`._
import com.neo.sk.tank.shared.config.TankGameConfig
import com.neo.sk.tank.shared.game.{GameContainer, GameContainerState}
import com.neo.sk.tank.shared.model.Constants.{ObstacleType, PropGenerateType, TankColor}
import com.neo.sk.tank.shared.model.{Point, Score}
import com.neo.sk.tank.shared.protocol.TankGameEvent
import org.slf4j.Logger
import concurrent.duration._
import scala.util.Random
import com.neo.sk.tank.Boot.roomManager

/**
  * Created by hongruying on 2018/8/29
  */
case class GameContainerServerImpl(
                                    config: TankGameConfig,
                                    roomActorRef:ActorRef[RoomActor.Command],
                                    timer:TimerScheduler[RoomActor.Command],
                                    log:Logger,
                                    dispatch:TankGameEvent.WsMsgServer => Unit,
                                    dispatchTo:(Long,TankGameEvent.WsMsgServer) => Unit
                                  ) extends GameContainer{

  import scala.language.implicitConversions

  private val bulletIdGenerator = new AtomicInteger(100)
  private val tankIdGenerator = new AtomicInteger(100)
  private val obstacleIdGenerator = new AtomicInteger(100)
  private val propIdGenerator = new AtomicInteger(100)

  private var justJoinUser:List[(Long,String,ActorRef[UserActor.Command],ActorRef[RoomActor.Command])] = Nil
  private val random = new Random(System.currentTimeMillis())

  init()


  override def debug(msg: String): Unit = log.debug(msg)

  override def info(msg: String): Unit = log.info(msg)

  override protected implicit def tankState2Impl(tank:TankState):Tank = {
    new TankServerImpl(roomActorRef,timer,config,tank)
  }

  override def tankExecuteLaunchBulletAction(tankId: Int, tank: Tank): Unit = {

    def transformGenerateBulletEvent(bulletState: BulletState) = {
      val event = TankGameEvent.GenerateBullet(systemFrame,bulletState)
      dispatch(event)
      addGameEvent(event)
    }

    tank.launchBullet()(config) match {
      case Some((bulletDirection,position,damage)) =>
        val bulletState = BulletState(bulletIdGenerator.getAndIncrement(),tankId,position,damage,position,System.currentTimeMillis(),tank.name,bulletDirection)
        transformGenerateBulletEvent(bulletState)
        if(tank.getShotGunState()){
          List(Math.PI / 8, - Math.PI / 8).foreach{ bulletOffsetDirection =>
            val bulletPos = tank.getOtherLaunchBulletPosition(bulletOffsetDirection.toFloat)(config)
            val bulletDir = bulletDirection + bulletOffsetDirection.toFloat
            val bulletState = BulletState(bulletIdGenerator.getAndIncrement(),tankId,bulletPos,damage,bulletPos,System.currentTimeMillis(),tank.name,bulletDir)
            transformGenerateBulletEvent(bulletState)
          }
        }
      case None => debug(s"tankId=${tankId} has no bullet now")
    }
  }

  override protected def tankEatPropCallback(tank:Tank)(prop: Prop):Unit = {
    val event = TankGameEvent.TankEatProp(tank.tankId,prop.pId,prop.propType,systemFrame)
    dispatch(event)
    addGameEvent(event)
  }

  override protected def attackTankCallBack(bullet: Bullet)(tank:Tank):Unit = {
    super.attackTankCallBack(bullet)(tank)
    val event = TankGameEvent.TankAttacked(tank.tankId,bullet.bId, bullet.tankId, bullet.tankName,bullet.damage,systemFrame)
    dispatch(event)
    addGameEvent(event)
  }



  override protected def dropTankCallback(bulletTankId:Int, bulletTankName:String,tank:Tank) = {
    dispatchTo(tank.userId,TankGameEvent.YouAreKilled(bulletTankId,bulletTankName))
    val tankState = tank.getTankState()
    val totalScore = tankState.bulletPowerLevel.toInt + tankState.bloodLevel.toInt + tankState.speedLevel.toInt - 2
    val propType:Byte = random.nextInt(4 + totalScore) + 1 match {
      case 1 => 1
      case 2 => 2
      case 3 => 3
      case 4 => 4
      case _ => 5
    }
    val event = TankGameEvent.GenerateProp(systemFrame,PropState(propIdGenerator.getAndIncrement(), propType, tank.getPosition, config.getPropDisappearFrame),PropGenerateType.tank)
    dispatch(event)
    addGameEvent(event)
  }

  private def genObstaclePositionRandom():Point = {
    Point(random.nextInt(boundary.x.toInt - config.obstacleWidth.toInt) + config.obstacleWidth.toInt / 2
      ,random.nextInt(boundary.y.toInt - config.obstacleWidth.toInt) + config.obstacleWidth.toInt / 2)
  }

  private def generateAirDrop() = {
    val oId = obstacleIdGenerator.getAndIncrement()
    val position = genObstaclePositionRandom()
    var n = AirDropBox(config,oId,position,config.airDropBlood)
    var objects = quadTree.retrieveFilter(n).filter(t => t.isInstanceOf[Tank] || t.isInstanceOf[Obstacle] || t.isInstanceOf[Prop])
    while (n.isIntersectsObject(objects)){
      val position = genObstaclePositionRandom()
      n = AirDropBox(config,oId,position,config.airDropBlood)
      objects = quadTree.retrieveFilter(n).filter(t => t.isInstanceOf[Tank] || t.isInstanceOf[Prop] || t.isInstanceOf[Obstacle])
    }
    n
  }

  private def generateBrick() = {
    val oId = obstacleIdGenerator.getAndIncrement()
    val position = genObstaclePositionRandom()
    var n = Brick(config, oId, position, config.brickBlood)
    var objects = quadTree.retrieveFilter(n).filter(t => t.isInstanceOf[Tank] || t.isInstanceOf[Prop] || t.isInstanceOf[Obstacle])
    while (n.isIntersectsObject(objects)){
      val position = genObstaclePositionRandom()
      n = Brick(config, oId, position, config.brickBlood)
      objects = quadTree.retrieveFilter(n).filter(t => t.isInstanceOf[Tank] || t.isInstanceOf[Prop] || t.isInstanceOf[Obstacle])
    }
    n
  }

  override protected def attackObstacleCallBack(bullet: Bullet)(o:Obstacle):Unit = {

    super.attackObstacleCallBack(bullet)(o)
    val event = TankGameEvent.ObstacleAttacked(o.oId,bullet.bId,bullet.damage,systemFrame)
    dispatch(event)
    addGameEvent(event)
    //    if(!o.isLived()){
    //      val obstacleOpt = o.obstacleType match {
    //        case ObstacleType.airDropBox =>
    //          generatePropEvent((random.nextInt(4) + 1).toByte, o.getPosition,PropGenerateType.airDrop)
    //          Some(generateAirDrop())
    //        case ObstacleType.brick => Some(generateBrick())
    //        case _ => None
    //      }
    //      obstacleOpt.foreach{ obstacle =>
    //        val event = TankGameEvent.GenerateObstacle(systemFrame,obstacle.getObstacleState())
    //        dispatch(event)
    //        addGameEvent(event)
    //      }
    //    }
  }

  override protected def handleObstacleAttacked(e: TankGameEvent.ObstacleAttacked): Unit = {
    bulletMap.get(e.bulletId).foreach(quadTree.remove)
    bulletMap.remove(e.bulletId)
    obstacleMap.get(e.obstacleId).foreach{ obstacle =>
      obstacle.attackDamage(e.damage)
      if(!obstacle.isLived()){
        quadTree.remove(obstacle)
        obstacleMap.remove(e.obstacleId)

        val obstacleOpt = obstacle.obstacleType match {
          case ObstacleType.airDropBox =>
            generatePropEvent((random.nextInt(4) + 1).toByte, obstacle.getPosition,PropGenerateType.airDrop)
            Some(generateAirDrop())
          case ObstacleType.brick => Some(generateBrick())
          case _ => None
        }
        obstacleOpt.foreach{ obstacle =>
          val event = TankGameEvent.GenerateObstacle(systemFrame,obstacle.getObstacleState())
          dispatch(event)
          addGameEvent(event)
        }
      }
    }
  }


  private def generatePropEvent(pType:Byte, position:Point, propGenerateType: Byte) = {
    val propState = PropState(propIdGenerator.getAndIncrement(),pType,position, config.getPropDisappearFrame)
    val event = TankGameEvent.GenerateProp(systemFrame,propState,propGenerateType)
    dispatch(event)
    addGameEvent(event)
  }

  override protected def handleUserJoinRoomEventNow() = {

    def genATank(userId:Long,name:String):TankServerImpl = {
      def genTankPositionRandom():Point = {
        Point(random.nextInt(boundary.x.toInt - (2 * config.tankRadius.toInt)) + config.tankRadius.toInt,
          random.nextInt(boundary.y.toInt - (2 * config.tankRadius.toInt)) + config.tankRadius.toInt)
      }

      val tankId = tankIdGenerator.getAndIncrement()
      val position = genTankPositionRandom()
      var tank = TankServerImpl(roomActorRef, timer, config, userId, tankId, name, config.getTankBloodByLevel(1), TankColor.getRandomColorType(random), position, config.maxBulletCapacity)
      var objects = quadTree.retrieveFilter(tank).filter(t => t.isInstanceOf[Tank] || t.isInstanceOf[Obstacle])
      while (tank.isIntersectsObject(objects)){
        val position = genTankPositionRandom()
        tank = TankServerImpl(roomActorRef, timer, config, userId, tankId, name, config.getTankBloodByLevel(1), TankColor.getRandomColorType(random), position, config.maxBulletCapacity)
        objects = quadTree.retrieveFilter(tank).filter(t => t.isInstanceOf[Tank] || t.isInstanceOf[Obstacle])
      }
      tank
    }

    justJoinUser.foreach{
      case (userId,name,ref,roomActor) =>
        val tank = genATank(userId,name)
        val event = TankGameEvent.UserJoinRoom(userId,name,tank.getTankState(),systemFrame)
        dispatch(event)
        addGameEvent(event)
//        roomManager ! UserActor.JoinRoomSuccess(tank,config.getTankGameConfigImpl(),ref,userId,roomId)
        ref ! UserActor.JoinRoomSuccess(tank, config.getTankGameConfigImpl(),userId,roomActor)
        tankMap.put(tank.tankId,tank)
        quadTree.insert(tank)
        //无敌时间消除
        timer.startSingleTimer(s"TankInvincible_${tank.tankId}",RoomActor.TankInvincible(tank.tankId),config.initInvincibleDuration.second)
    }
    justJoinUser = Nil
  }


  def leftGame(userId:Long,name:String,tankId:Int) = {
    val event = TankGameEvent.UserLeftRoom(userId,name,tankId,systemFrame)
    addGameEvent(event)
    dispatch(event)
  }


  def joinGame(userId:Long,name:String,userActor:ActorRef[UserActor.Command],roomActor:ActorRef[RoomActor.Command]):Unit = {
    justJoinUser = (userId,name,userActor,roomActor) :: justJoinUser
  }


  def receiveUserAction(preExecuteUserAction:TankGameEvent.UserActionEvent):Unit = {
    val f = math.max(preExecuteUserAction.frame,systemFrame)
    if(preExecuteUserAction.frame != f){
      log.debug(s"preExecuteUserAction fame=${preExecuteUserAction.frame}, systemFrame=${systemFrame}")
    }
    val action = preExecuteUserAction match {
      case a:TankGameEvent.UserMouseMove => a.copy(frame = f)
      case a:TankGameEvent.UserMouseClick => a.copy(frame = f)
      case a:TankGameEvent.UserPressKeyDown => a.copy(frame = f)
      case a:TankGameEvent.UserPressKeyUp => a.copy(frame = f)
    }

    addUserAction(action)
    dispatch(action)
  }

  //定时器发的定时事件
  def receiveGameEvent(event:TankGameEvent.GameEvent with TankGameEvent.WsMsgServer) = {
    dispatch(event)
    addGameEvent(event)
  }

  private def generateEnvironment(pType:Byte) = {
    def isSuitable(position:Point, environmentTypePosition : List[(Int, Int)]) = {
      !environmentTypePosition.exists{
        case (x,y) =>
          val p = position + Point(x * config.obstacleWidth, y * config.obstacleWidth)
          val obstacle = Steel(config,-1,p)
          val others = quadTree.retrieveFilter(obstacle)
          !(p + Point(config.obstacleWidth / 2, config.obstacleWidth / 2) < boundary) || obstacle.isIntersectsObject(others)
      }
    }

    val environmentTypePositions = if(pType == ObstacleType.river) config.riverPosType else config.steelPosType

    environmentTypePositions.foreach{ t =>
      var p = genObstaclePositionRandom()
      while(!isSuitable(p, t)) p = genObstaclePositionRandom()
      t.foreach{ case (offsetX,offsetY) =>
        val position = p + Point(offsetX * config.obstacleWidth,offsetY * config.obstacleWidth)
        val obstacleState = ObstacleState(obstacleIdGenerator.getAndIncrement(), pType, None, position)
        val obstacle = Obstacle(config, obstacleState)
        val event = TankGameEvent.GenerateObstacle(systemFrame,obstacleState)
        addGameEvent(event)
        environmentMap.put(obstacle.oId, obstacle)
        quadTree.insert(obstacle)
      }

    }
  }

  private def initObstacle() = {
    (1 to config.airDropNum).foreach{ _ =>
      val obstacle = generateAirDrop()
      val event = TankGameEvent.GenerateObstacle(systemFrame, obstacle.getObstacleState())
      addGameEvent(event)
      obstacleMap.put(obstacle.oId, obstacle)
      quadTree.insert(obstacle)
    }
    (1 to config.airDropNum).foreach{ _ =>
      val obstacle = generateBrick()
      val event = TankGameEvent.GenerateObstacle(systemFrame, obstacle.getObstacleState())
      addGameEvent(event)
      obstacleMap.put(obstacle.oId, obstacle)
      quadTree.insert(obstacle)
    }
  }


  private def init():Unit = {
    generateEnvironment(ObstacleType.steel)
    generateEnvironment(ObstacleType.river)
    initObstacle()
    clearEventWhenUpdate()
  }

  override protected def clearEventWhenUpdate():Unit = {
    //记录数据
    gameEventMap -= systemFrame
    actionEventMap -= systemFrame
    systemFrame += 1
  }

  override def update(): Unit = {
    super.update()
    updateRanks()
  }

  def getGameContainerState():GameContainerState = {
    GameContainerState(
      systemFrame,
      tankMap.values.map(_.getTankState()).toList,
      propMap.values.map(_.getPropState).toList,
      obstacleMap.values.map(_.getObstacleState()).toList,
      tankMoveAction = tankMoveAction.toList.map(t => (t._1,t._2.toList))
    )
  }

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

  private[this] def updateRanks()= {
    currentRank = tankMap.values.map(s => Score(s.tankId, s.name, s.killTankNum, s.damageStatistics)).toList.sorted
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

  def getCurGameSnapshot():TankGameEvent.TankGameSnapshot = {
    TankGameEvent.TankGameSnapshot(getGameContainerAllState())
  }


  def getGameEventAndSnapshot():(List[TankGameEvent.WsMsgServer],Option[TankGameEvent.GameSnapshot]) = {
    ((gameEventMap.getOrElse(this.systemFrame, Nil) ::: actionEventMap.getOrElse(this.systemFrame, Nil))
      .filter(_.isInstanceOf[TankGameEvent.WsMsgServer]).map(_.asInstanceOf[TankGameEvent.WsMsgServer]),
      Some(getCurGameSnapshot())
    )
  }




}
