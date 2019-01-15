package com.neo.sk.tank.core.game

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.TimerScheduler
import com.neo.sk.tank.core.{RoomActor, UserActor}
import com.neo.sk.tank.shared.`object`._
import com.neo.sk.tank.shared.config.{TankGameConfig, TankGameConfigImpl}
import com.neo.sk.tank.shared.game.GameContainer
import com.neo.sk.tank.shared.model.Constants.{ObstacleType, PropGenerateType, TankColor}
import com.neo.sk.tank.shared.model.{Point, Score}
import com.neo.sk.tank.shared.protocol.TankGameEvent
import com.neo.sk.tank.shared.protocol.TankGameEvent._
import org.slf4j.Logger

import concurrent.duration._
import scala.util.Random
import collection.mutable
import com.neo.sk.tank.shared.`object`.TankState
import com.neo.sk.tank.Boot.{botManager, userManager}
import com.neo.sk.tank.common.AppSettings
import com.neo.sk.tank.core.UserActor.TankRelive4UserActor
import com.neo.sk.tank.core.bot.{BotActor, BotManager}
import com.neo.sk.tank.shared.`object`

/**
  * Created by hongruying on 2018/8/29
  */
case class GameContainerServerImpl(
                                    config: TankGameConfig,
                                    roomActorRef: ActorRef[RoomActor.Command],
                                    timer: TimerScheduler[RoomActor.Command],
                                    log: Logger,
                                    dispatch: TankGameEvent.WsMsgServer => Unit,
                                    dispatchTo: (String, TankGameEvent.WsMsgServer, Option[mutable.HashMap[String, ActorRef[UserActor.Command]]]) => Unit
                                  ) extends GameContainer {

  import scala.language.implicitConversions

  private val bulletIdGenerator = new AtomicInteger(100)
  private val tankIdGenerator = new AtomicInteger(100)
  private val obstacleIdGenerator = new AtomicInteger(100)
  private val propIdGenerator = new AtomicInteger(100)

  private var justJoinUser: List[(String, Option[Int], String, ActorRef[UserActor.Command])] = Nil // tankIdOpt
  private var justJoinBot: List[(String, Option[Int], String, ActorRef[BotActor.Command],ActorRef[RoomActor.Command])] = Nil // tankIdOpt
  private val userMapObserver: mutable.HashMap[String, mutable.HashMap[String, ActorRef[UserActor.Command]]] = mutable.HashMap.empty
  private val random = new Random(System.currentTimeMillis())

  init()


  override def debug(msg: String): Unit = log.debug(msg)

  override def info(msg: String): Unit = log.info(msg)

  override protected implicit def tankState2Impl(tank: TankState): Tank = {
    new TankServerImpl(config, tank, fillBulletCallBack, tankShotgunExpireCallBack)
  }

  def getUserActor4WatchGameList(uId: String) = userMapObserver.get(uId)

  override def tankExecuteLaunchBulletAction(tankId: Int, tank: Tank): Unit = {

    def transformGenerateBulletEvent(bulletState: BulletState) = {
      val event = TankGameEvent.GenerateBullet(systemFrame, bulletState)
      dispatch(event)
      addGameEvent(event)
    }

    tank.launchBullet()(config) match {
      case Some((bulletDirection, position, damage)) =>
        val momentum = if (tank.getTankIsMove()) config.bulletSpeed.rotate(bulletDirection) * config.frameDuration / 1000 +
          config.getMoveDistanceByFrame(tank.getTankSpeedLevel()).rotate(tank.getTankDirection()).*(0.2f)
        else config.bulletSpeed.rotate(bulletDirection) * config.frameDuration / 1000

        val bulletState = BulletState(bulletIdGenerator.getAndIncrement(), tankId, systemFrame, position, damage.toByte, momentum, tank.name)
        transformGenerateBulletEvent(bulletState)
        if (tank.getShotGunState()) {
          List(Math.PI / 8, -Math.PI / 8).foreach { bulletOffsetDirection =>
            val bulletPos = tank.getOtherLaunchBulletPosition(bulletOffsetDirection.toFloat)(config)
            val bulletDir = bulletDirection + bulletOffsetDirection.toFloat
            val momentum = if (tank.getTankIsMove()) config.bulletSpeed.rotate(bulletDir) * config.frameDuration / 1000 +
              config.getMoveDistanceByFrame(tank.getTankSpeedLevel()).rotate(tank.getTankDirection()).*(0.2f)
            else config.bulletSpeed.rotate(bulletDir) * config.frameDuration / 1000
            val bulletState = `object`.BulletState(bulletIdGenerator.getAndIncrement(), tankId, systemFrame, bulletPos, damage.toByte, momentum, tank.name)
            transformGenerateBulletEvent(bulletState)
          }
        }
      case None => debug(s"tankId=${tankId} has no bullet now")
    }
  }

  override protected def tankEatPropCallback(tank: Tank)(prop: Prop): Unit = {
    /**
      * tank吃道具,当前存储道具数量等于限制值，即吃即用
      **/
    val event = TankGameEvent.TankEatProp(tank.tankId, prop.pId, prop.propType, systemFrame)
    dispatch(event)
    addGameEvent(event)
  }

  override protected def dropTankCallback(bulletTankId: Int, bulletTankName: String, tank: Tank) = {
    val tankState = tank.getTankState()
    val killEvent = TankGameEvent.YouAreKilled(bulletTankId, bulletTankName, tankState.lives > 1, tank.killTankNum, tank.lives, tank.damageStatistics)
    if (tank.lives > 1 && AppSettings.supportLiveLimit) {
      log.debug(s"${roomActorRef.path} timer for relive is starting...")
      timer.startSingleTimer(s"TankRelive_${tank.tankId}", RoomActor.TankRelive(tank.userId, Some(tank.tankId), tank.name), config.getTankReliveDuration.millis)
    }
    //todo 此处需要判断bot
    if(tank.userId.contains("BotActor-")){
      botManager ! BotManager.StopBot(tank.userId,if(tank.lives>1 && AppSettings.supportLiveLimit) BotManager.StopMap.stop else BotManager.StopMap.delete)
    }else{
      dispatchTo(tank.userId, killEvent, getUserActor4WatchGameList(tank.userId))
    }
    //后台增加一个玩家离开消息（不传给前端）,以便录像的时候记录玩家死亡和websocket断开的情况。
    //fixme 此处是否存在BUG
    if (tankState.lives <= 1 || (!AppSettings.supportLiveLimit)) {
      val event = TankGameEvent.UserLeftRoomByKill(tank.userId, tank.name, tank.tankId, systemFrame)
      addGameEvent(event)
    }
    if (AppSettings.supportLiveLimit) {
      val curTankState = TankState(tankState.userId, tankState.tankId, tankState.direction, tankState.gunDirection, tankState.blood, tankState.bloodLevel, tankState.speedLevel, tankState.curBulletNum,
        tankState.position, tankState.bulletPowerLevel, tankState.tankColorType, tankState.name, tankState.lives - 1, None, tankState.killTankNum, tankState.damageTank, tankState.invincible,
        tankState.shotgunState, tankState.speed, tankState.isMove)
      tankLivesMap.get(tankState.tankId) match {
        case Some(tankStateOld) =>
          log.debug(s"${roomActorRef.path}更新生命值信息")
          tankLivesMap.update(tankState.tankId, curTankState)
        case None =>
          tankLivesMap += (tankState.tankId -> tankState)
      }
    }
    val totalScore = tankState.bulletPowerLevel.toInt + tankState.bloodLevel.toInt + tankState.speedLevel.toInt - 2
    val propType: Byte = random.nextInt(4 + totalScore) + 1 match {
      case 1 => 1
      case 2 => 2
      case 3 => 3
      case 4 => 4
      case _ => 5
    }
    val event = TankGameEvent.GenerateProp(systemFrame, PropState(propIdGenerator.getAndIncrement(), propType, tank.getPosition, config.getPropDisappearFrame), PropGenerateType.tank)
    dispatch(event)
    addGameEvent(event)
  }

  private def genObstaclePositionRandom(): Point = {
    Point(random.nextInt(boundary.x.toInt - config.obstacleWidth.toInt) + config.obstacleWidth.toInt / 2
      , random.nextInt(boundary.y.toInt - config.obstacleWidth.toInt) + config.obstacleWidth.toInt / 2)
  }

  private def genEnvironmentPositionRandom(): Point = {
    Point(random.nextInt(boundary.x.toInt - 10 * config.obstacleWidth.toInt) + 5 * config.obstacleWidth.toInt,
      random.nextInt(boundary.y.toInt - 10 * config.obstacleWidth.toInt) + 5 * config.obstacleWidth.toInt)
  }

  private def generateAirDrop() = {
    val oId = obstacleIdGenerator.getAndIncrement()
    val position = genObstaclePositionRandom()
    var n = AirDropBox(config, oId, position, config.airDropBlood)
    var objects = quadTree.retrieveFilter(n).filter(t => t.isInstanceOf[Tank] || t.isInstanceOf[Obstacle] || t.isInstanceOf[Prop])
    while (n.isIntersectsObject(objects)) {
      val position = genObstaclePositionRandom()
      n = AirDropBox(config, oId, position, config.airDropBlood)
      objects = quadTree.retrieveFilter(n).filter(t => t.isInstanceOf[Tank] || t.isInstanceOf[Prop] || t.isInstanceOf[Obstacle])
    }
    n
  }

  private def generateBrick() = {
    val oId = obstacleIdGenerator.getAndIncrement()
    val position = genObstaclePositionRandom()
    var n = Brick(config, oId, position, config.brickBlood)
    var objects = quadTree.retrieveFilter(n).filter(t => t.isInstanceOf[Tank] || t.isInstanceOf[Prop] || t.isInstanceOf[Obstacle])
    while (n.isIntersectsObject(objects)) {
      val position = genObstaclePositionRandom()
      n = Brick(config, oId, position, config.brickBlood)
      objects = quadTree.retrieveFilter(n).filter(t => t.isInstanceOf[Tank] || t.isInstanceOf[Prop] || t.isInstanceOf[Obstacle])
    }
    n
  }

  override protected def handleObstacleRemoveNow() = {}

  override protected def handleObstacleAttacked(e: TankGameEvent.ObstacleAttacked): Unit = {
    bulletMap.get(e.bulletId).foreach(quadTree.remove)
    bulletMap.remove(e.bulletId)
    obstacleMap.get(e.obstacleId).foreach { obstacle =>
      obstacle.attackDamage(e.damage)
      if (!obstacle.isLived()) {
        quadTree.remove(obstacle)
        obstacleMap.remove(e.obstacleId)
        //砖块消失
        val event = TankGameEvent.ObstacleRemove(e.obstacleId, systemFrame)
        dispatch(event)
        addGameEvent(event)

        val obstacleOpt = obstacle.obstacleType match {
          case ObstacleType.airDropBox =>
            generatePropEvent((random.nextInt(4) + 1).toByte, obstacle.getPosition, PropGenerateType.airDrop)
            Some(generateAirDrop())
          case ObstacleType.brick => Some(generateBrick())
          case _ => None
        }
        obstacleOpt.foreach { obstacle =>
          val event = TankGameEvent.GenerateObstacle(systemFrame, obstacle.getObstacleState())
          dispatch(event)
          addGameEvent(event)
          obstacleMap.put(obstacle.oId, obstacle)
          quadTree.insert(obstacle)
        }
      }
    }
  }


  private def generatePropEvent(pType: Byte, position: Point, propGenerateType: Byte) = {
    val propState = PropState(propIdGenerator.getAndIncrement(), pType, position, config.getPropDisappearFrame)
    val event = TankGameEvent.GenerateProp(systemFrame, propState, propGenerateType)
    dispatch(event)
    addGameEvent(event)
  }

  private def genATank(userId: String, tankIdOpt: Option[Int], name: String) = {
    def genTankPositionRandom(): Point = {
      Point(random.nextInt(boundary.x.toInt - (2 * config.tankRadius.toInt)) + config.tankRadius.toInt,
        random.nextInt(boundary.y.toInt - (2 * config.tankRadius.toInt)) + config.tankRadius.toInt)
    }

    def genTankServeImpl(tankId: Int, killTankNum: Int, damageStatistics: Int, lives: Int) = {
      val position = genTankPositionRandom()
      var tank = TankServerImpl(fillBulletCallBack, tankShotgunExpireCallBack, config, userId, tankId, name,
        config.getTankBloodByLevel(1), TankColor.getRandomColorType(random), position,
        config.maxBulletCapacity, lives = lives, None,
        killTankNum = killTankNum, damageStatistics = damageStatistics)
      var objects = quadTree.retrieveFilter(tank).filter(t => t.isInstanceOf[Tank] || t.isInstanceOf[Obstacle])
      while (tank.isIntersectsObject(objects)) {
        val position = genTankPositionRandom()
        tank = TankServerImpl(fillBulletCallBack, tankShotgunExpireCallBack, config, userId, tankId, name,
          config.getTankBloodByLevel(1), TankColor.getRandomColorType(random), position,
          config.maxBulletCapacity, lives = lives, None,
          killTankNum = killTankNum, damageStatistics = damageStatistics)
        objects = quadTree.retrieveFilter(tank).filter(t => t.isInstanceOf[Tank] || t.isInstanceOf[Obstacle])
      }
      tank
    }

    tankIdOpt match {
      case Some(id) =>
        val tankStateOld = tankLivesMap.get(id)
        tankStateOld match {
          case Some(tankState) =>
            if (tankState.lives > 0) {
              //tank复活还有生命
              log.debug(s"${roomActorRef.path}坦克${tankState.tankId}的当前生命值${tankState.lives}")
              genTankServeImpl(id, tankState.killTankNum, tankState.damageStatistics, tankState.lives)
            } else {
              //tank复活没有生命,更新tankLivesMap
              tankLivesMap.remove(id)
              val tankId = tankIdGenerator.getAndIncrement()
              genTankServeImpl(tankId, 0, 0, config.getTankLivesLimit)
            }
          case None =>
            log.debug(s"${roomActorRef.path}没有该坦克信息")
            genTankServeImpl(id, 0, 0, config.getTankLivesLimit)
        }
      case None =>
        log.debug(s"${roomActorRef.path}没有携带坦克id重新生成")
        val tankId = tankIdGenerator.getAndIncrement()
        genTankServeImpl(tankId, 0, 0, config.getTankLivesLimit)
    }
  }

  override protected def handleUserJoinRoomEventNow() = {
    justJoinUser.foreach {
      case (userId, tankIdOpt, name, ref) =>
        val tank = genATank(userId, tankIdOpt, name)
        //        tankEatPropMap.update(tank.tankId,mutable.HashSet())
        if (AppSettings.supportLiveLimit) {
          tankLivesMap.update(tank.tankId, tank.getTankState())
        }
        val event = TankGameEvent.UserJoinRoom(userId, name, tank.getTankState(), systemFrame)
        dispatch(event)
        addGameEvent(event)
        println(s"the path is $ref")
        ref ! UserActor.JoinRoomSuccess(tank, config.getTankGameConfigImpl(), userId, roomActor = roomActorRef)
        //ref ! UserActor.JoinRoom()
        userMapObserver.get(userId) match {
          case Some(maps) =>
            maps.foreach { p =>
              val event1 = UserActor.JoinRoomSuccess4Watch(tank, config.getTankGameConfigImpl(), roomActorRef, TankGameEvent.SyncGameAllState(getGameContainerAllState()))
              p._2 ! event1
            }

          case None =>
            userMapObserver.update(userId, mutable.HashMap.empty)
        }
        tankMap.put(tank.tankId, tank)
        quadTree.insert(tank)
        //无敌时间消除
        tankInvincibleCallBack(tank.tankId)
    }
    justJoinUser = Nil
    /**加入bot*/
    justJoinBot.foreach {
      case (userId, tankIdOpt, name, botActor,roomActor) =>
        val tank = genATank(userId, tankIdOpt, name)
        //        tankEatPropMap.update(tank.tankId,mutable.HashSet())
        if (AppSettings.supportLiveLimit) {
          tankLivesMap.update(tank.tankId, tank.getTankState())
        }
        val event = TankGameEvent.UserJoinRoom(userId, name, tank.getTankState(), systemFrame)
        dispatch(event)
        addGameEvent(event)
        botActor ! BotActor.JoinRoomSuccess(tank,roomActor)

        tankMap.put(tank.tankId, tank)
        quadTree.insert(tank)
        //无敌时间消除
        tankInvincibleCallBack(tank.tankId)
    }
    justJoinBot = Nil
  }

  def handleTankRelive(userId: String, tankIdOpt: Option[Int], name: String) = {
    val tank = genATank(userId, tankIdOpt, name)
    tankLivesMap.update(tank.tankId, tank.getTankState())
    //remind 区分bot和user复活
    if(userId.contains("BotActor-")){
      botManager ! BotManager.ReliveBot(userId)
    }else{
      val event = TankRelive4UserActor(tank, userId, name, roomActorRef, config.getTankGameConfigImpl())
      userMapObserver.get(userId) match {
        case Some(maps) =>
          maps.foreach { p =>
            p._2 ! event
          }
        case None =>
      }
      log.debug(s"${roomActorRef.path} is processing to generate tank for ${userId} ")
      userManager ! event
    }
    val userReliveEvent = TankGameEvent.UserRelive(userId, name, tank.getTankState(), systemFrame)
    dispatch(userReliveEvent)
    addGameEvent(userReliveEvent)
    tankMap.put(tank.tankId, tank)
    quadTree.insert(tank)
    //无敌时间消除
    tankInvincibleCallBack(tank.tankId)

  }

  def handleJoinRoom4Watch(userActor4WatchGame: ActorRef[UserActor.Command], uid: String, playerId: String) = {
    tankMap.find(_._2.userId == playerId) match {
      case Some((_, tank)) =>
        userMapObserver.values.foreach(t => t.remove(uid))
        val playerObserversMap = userMapObserver.getOrElse(playerId, mutable.HashMap[String, ActorRef[UserActor.Command]]())
        playerObserversMap.put(uid, userActor4WatchGame)
        userMapObserver.put(playerId, playerObserversMap)
        log.debug(s"当前的userMapObservers是${userMapObserver}")
        userActor4WatchGame ! UserActor.JoinRoomSuccess4Watch(tank.asInstanceOf[TankServerImpl], config.getTankGameConfigImpl(), roomActorRef, TankGameEvent.SyncGameAllState(getGameContainerAllState()))

      case None =>
        userActor4WatchGame ! UserActor.JoinRoomFail4Watch(s"观察的用户id=${playerId}不存在")
    }
  }


  def leftGame(userId: String, name: String, tankId: Int) = {
    log.info(s"bot/user leave $userId")
    val event = TankGameEvent.UserLeftRoom(userId, name, tankId, systemFrame)
    addGameEvent(event)
    dispatch(event)
    dispatchTo(userId, TankGameEvent.PlayerLeftRoom(userId, name, tankId, systemFrame), getUserActor4WatchGameList(userId))
    userMapObserver.remove(userId)
  }

  def leftWatchGame(uId: String, playerId: String) = {
    userMapObserver.get(playerId) match {
      case Some(maps) =>
        maps.remove(uId)
        userMapObserver.update(playerId, maps)
      case None =>
    }

  }


  def joinGame(userId: String, tankIdOpt: Option[Int], name: String, userActor: ActorRef[UserActor.Command]): Unit = {
    justJoinUser = (userId, tankIdOpt, name, userActor) :: justJoinUser
  }

  def botJoinGame(bId: String, tankIdOpt: Option[Int], name: String, botActor: ActorRef[BotActor.Command],roomActor:ActorRef[RoomActor.Command]): Unit = {
    justJoinBot = (bId, tankIdOpt, name, botActor,roomActor) :: justJoinBot
  }


  def receiveUserAction(preExecuteUserAction: TankGameEvent.UserActionEvent): Unit = {
    //    println(s"receive user action preExecuteUserAction frame=${preExecuteUserAction.frame}----system fram=${systemFrame}")
    val f = math.max(preExecuteUserAction.frame, systemFrame)
    if (preExecuteUserAction.frame != f) {
      log.debug(s"preExecuteUserAction fame=${preExecuteUserAction.frame}, systemFrame=${systemFrame}")
    }
    /**
      * gameAction,userAction
      * 新增按键操作，补充血量，
      **/
    val action = preExecuteUserAction match {
      case a: TankGameEvent.UserMouseMove => a.copy(frame = f)
      case a: TankGameEvent.UserMouseClick => a.copy(frame = f)
      case a: TankGameEvent.UserPressKeyDown => a.copy(frame = f)
      case a: TankGameEvent.UserPressKeyUp => a.copy(frame = f)
      case a: TankGameEvent.UserKeyboardMove => a.copy(frame = f)
      case a: TankGameEvent.UserPressKeyMedical => a.copy(frame = f)
    }

    addUserAction(action)
    dispatch(action)
  }

  private def generateEnvironment(pType: Byte, barrierPosList: List[RectangleObjectOfGame], barrier: List[List[(Int, Int)]]) = {
    var barrierPosListCopy = barrierPosList

    def isSuitable(position: Point, environmentTypePosition: List[(Int, Int)]) = {
      !environmentTypePosition.exists {
        case (x, y) =>
          val p = position + Point(x * config.obstacleWidth, y * config.obstacleWidth)
          val obstacle = Steel(config, -1, p)
          val others = quadTree.retrieveFilter(obstacle)
          !(p + Point(config.obstacleWidth / 2, config.obstacleWidth / 2) < (boundary - Point(5 * config.obstacleWidth, 5 * config.obstacleWidth))) || obstacle.isIntersectsObject(others) || obstacle.isIntersectsObject(barrierPosListCopy)
      }

    }

    val environmentTypePositions = if (pType == ObstacleType.river) config.riverPosType else config.steelPosType

    environmentTypePositions.foreach { t =>
      var p = genEnvironmentPositionRandom()
      while (!isSuitable(p, t)) p = genEnvironmentPositionRandom()
      val i = environmentTypePositions.indexOf(t)
      val topLeft = p + Point(barrier(i)(0)._1 * config.obstacleWidth, barrier(i)(0)._2 * config.obstacleWidth) - Point(config.obstacleWidth / 2, config.obstacleWidth / 2)
      val downRight = p + Point(barrier(i)(1)._1 * config.obstacleWidth, barrier(i)(1)._2 * config.obstacleWidth) + Point(config.obstacleWidth / 2, config.obstacleWidth / 2)
      val rectBarrier = new RectangleObjectOfGame {
        override protected val width: Float = downRight.x - topLeft.x
        override protected val height: Float = downRight.y - topLeft.y
        override protected val collisionOffset: Float = 0
        override protected var position: Point = Point((topLeft.x + downRight.x) / 2, (topLeft.y + downRight.y) / 2)
      }
      barrierPosListCopy = rectBarrier :: barrierPosListCopy
      t.foreach { case (offsetX, offsetY) =>
        val position = p + Point(offsetX * config.obstacleWidth, offsetY * config.obstacleWidth)
        val obstacleState = ObstacleState(obstacleIdGenerator.getAndIncrement(), pType, None, position)
        val obstacle = Obstacle(config, obstacleState)
        val event = TankGameEvent.GenerateObstacle(systemFrame, obstacleState)
        addGameEvent(event)
        environmentMap.put(obstacle.oId, obstacle)
        quadTree.insert(obstacle)
      }

    }
    barrierPosListCopy
  }

  private def initObstacle() = {
    (1 to config.airDropNum).foreach { _ =>
      val obstacle = generateAirDrop()
      val event = TankGameEvent.GenerateObstacle(systemFrame, obstacle.getObstacleState())
      addGameEvent(event)
      obstacleMap.put(obstacle.oId, obstacle)
      quadTree.insert(obstacle)
    }
    (1 to config.airDropNum).foreach { _ =>
      val obstacle = generateBrick()
      val event = TankGameEvent.GenerateObstacle(systemFrame, obstacle.getObstacleState())
      addGameEvent(event)
      obstacleMap.put(obstacle.oId, obstacle)
      quadTree.insert(obstacle)
    }
  }


  private def init(): Unit = {
    var barrierPosList: List[RectangleObjectOfGame] = Nil
    barrierPosList = generateEnvironment(ObstacleType.steel, barrierPosList, config.barrierPos4River)
    generateEnvironment(ObstacleType.river, barrierPosList, config.barrierPos4Steel)
    barrierPosList = Nil
    initObstacle()
    clearEventWhenUpdate()
  }

  override protected def clearEventWhenUpdate(): Unit = {
    super.clearEventWhenUpdate()
    gameEventMap -= systemFrame - 1
    actionEventMap -= systemFrame - 1
    followEventMap -= systemFrame - maxFollowFrame - 1
    systemFrame += 1
  }

  override def update(): Unit = {
    super.update()
//    log.info(s"userSize ${tankMap.size} list=${tankMap.values.map(r=>(r.tankId,r.name))}")
    super.updateRanks()
  }

  def getGameContainerState(): GameContainerState = {
    GameContainerState(
      systemFrame,
      tankMap.values.map(_.getTankState()).toList
    )
  }

  def getFollowEventSnap(): TankFollowEventSnap = {
    val l = followEventMap.filter(_._1 >= systemFrame).toList
    TankFollowEventSnap(
      systemFrame,
      l.flatMap(_._2).filter(_.isInstanceOf[TankGameEvent.TankFillBullet]).map(_.asInstanceOf[TankGameEvent.TankFillBullet]),
      l.flatMap(_._2).filter(_.isInstanceOf[TankGameEvent.TankInvincible]).map(_.asInstanceOf[TankInvincible]),
      l.flatMap(_._2).filter(_.isInstanceOf[TankGameEvent.TankShotgunExpire]).map(_.asInstanceOf[TankGameEvent.TankShotgunExpire])
    )
  }

//  implicit val scoreOrdering = new Ordering[Score] {
//    override def compare(x: Score, y: Score): Int = {
//      var r = y.k - x.k
//      if (r == 0) {
//        r = y.d - x.d
//      }
//      if (r == 0) {
//        r = y.l - x.l
//      }
//      if (r == 0) {
//        r = (x.id - y.id).toInt
//      }
//      r
//    }
//  }

//  private[this] def updateRanks() = {
//    currentRank = tankMap.values.map(s => Score(s.tankId, s.name, s.killTankNum, s.damageStatistics, s.lives)).toList.sorted
//    var historyChange = false
//    currentRank.foreach { cScore =>
//      historyRankMap.get(cScore.id) match {
//        case Some(oldScore) if cScore.d > oldScore.d || cScore.l < oldScore.l =>
//          historyRankMap += (cScore.id -> cScore)
//          historyChange = true
//        case None if cScore.d > historyRankThreshold =>
//          historyRankMap += (cScore.id -> cScore)
//          historyChange = true
//        case _ =>
//
//      }
//    }
//
//    if (historyChange) {
//      historyRank = historyRankMap.values.toList.sorted.take(historyRankLength)
//      historyRankThreshold = historyRank.lastOption.map(_.d).getOrElse(-1)
//      historyRankMap = historyRank.map(s => s.id -> s).toMap
//    }
//  }

  def getCurGameSnapshot(): TankGameEvent.TankGameSnapshot = {
    TankGameEvent.TankGameSnapshot(getGameContainerAllState())
  }


  def getLastGameEvent(): List[TankGameEvent.WsMsgServer] = {
    (gameEventMap.getOrElse(this.systemFrame - 1, Nil) ::: actionEventMap.getOrElse(this.systemFrame - 1, Nil))
      .filter(_.isInstanceOf[TankGameEvent.WsMsgServer]).map(_.asInstanceOf[TankGameEvent.WsMsgServer])
  }


  def getCurSnapshot(): Option[TankGameEvent.GameSnapshot] = {
    Some(getCurGameSnapshot())
  }

  //  def getGameEventAndSnapshot():(List[TankGameEvent.WsMsgServer],Option[TankGameEvent.GameSnapshot]) = {
  //    ((gameEventMap.getOrElse(this.systemFrame, Nil) ::: actionEventMap.getOrElse(this.systemFrame, Nil))
  //      .filter(_.isInstanceOf[TankGameEvent.WsMsgServer]).map(_.asInstanceOf[TankGameEvent.WsMsgServer]),
  //      Some(getCurGameSnapshot())
  //    )
  //  }


  override protected def handleGenerateObstacle(e: TankGameEvent.GenerateObstacle): Unit = {
    val obstacle = Obstacle(config, e.obstacleState)
    val originObstacleOpt = if (e.obstacleState.t <= ObstacleType.brick) obstacleMap.put(obstacle.oId, obstacle)
    else environmentMap.put(obstacle.oId, obstacle)
    if (originObstacleOpt.isEmpty) {
      quadTree.insert(obstacle)
    } else {
      quadTree.remove(originObstacleOpt.get)
      quadTree.insert(obstacle)
    }
  }


}
