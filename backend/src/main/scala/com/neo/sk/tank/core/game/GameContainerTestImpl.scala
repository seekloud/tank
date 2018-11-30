package com.neo.sk.tank.core.game

import com.neo.sk.tank.shared.`object`._
import com.neo.sk.tank.shared.config.TankGameConfig
import com.neo.sk.tank.shared.game._
import com.neo.sk.tank.shared.model.Constants.{GameAnimation, GameState, PropGenerateType}
import com.neo.sk.tank.shared.protocol.TankGameEvent
import com.neo.sk.tank.shared.protocol.TankGameEvent.{GameEvent, TankFollowEventSnap, UserActionEvent}

import scala.collection.mutable

case class GameContainerTestImpl(
                                  override val config:TankGameConfig,
                                  myId:String,
                                  myTankId:Int,
                                  myName:String,
                                  setGameState:Int => Unit,
                                  isObserve: Boolean = false,
                                  setKillCallback: (String, Boolean, Int, Int) => Unit = {(_,_,_,_) =>}
                                ) extends GameContainer with EsRecoverForTest {


  import scala.language.implicitConversions

  protected val obstacleAttackedAnimationMap = mutable.HashMap[Int, Int]()
  protected val tankAttackedAnimationMap = mutable.HashMap[Int, Int]()
  protected val tankDestroyAnimationMap = mutable.HashMap[Int, Int]() //prop ->

  protected var tId: Int = myTankId

  def changeTankId(id: Int) = tId = id


  override def debug(msg: String): Unit = {}

  override def info(msg: String): Unit = println(msg)

  private val esRecoverSupport: Boolean = true

  private val uncheckedActionMap = mutable.HashMap[Int, Long]() //serinum -> frame

  private var gameContainerAllStateOpt: Option[GameContainerAllState] = None
  private var gameContainerStateOpt: Option[GameContainerState] = None

  protected var waitSyncData: Boolean = true

  private val preExecuteFrameOffset = com.neo.sk.tank.shared.model.Constants.PreExecuteFrameOffset


  override protected def handleObstacleAttacked(e: TankGameEvent.ObstacleAttacked): Unit = {
    super.handleObstacleAttacked(e)
    if (obstacleMap.get(e.obstacleId).nonEmpty || environmentMap.get(e.obstacleId).nonEmpty) {
      obstacleAttackedAnimationMap.put(e.obstacleId, GameAnimation.bulletHitAnimationFrame)
    }
  }

  override protected def handleTankAttacked(e: TankGameEvent.TankAttacked): Unit = {
    super.handleTankAttacked(e)
    if (tankMap.get(e.tankId).nonEmpty) {
      tankAttackedAnimationMap.put(e.tankId, GameAnimation.bulletHitAnimationFrame)
    }
  }

  override protected def dropTankCallback(bulletTankId: Int, bulletTankName: String, tank: Tank) = {
    if (tank.tankId == tId) {
      setKillCallback(bulletTankName, tank.lives > 1, tank.killTankNum, tank.damageStatistics)
      if (tank.lives <= 1) setGameState(GameState.stop)
    }
  }

  override protected def handleGenerateProp(e: TankGameEvent.GenerateProp): Unit = {
    super.handleGenerateProp(e)
    if (e.generateType == PropGenerateType.tank) {
      tankDestroyAnimationMap.put(e.propState.pId, GameAnimation.tankDestroyAnimationFrame)
    }
  }


  override def tankExecuteLaunchBulletAction(tankId: Int, tank: Tank): Unit = {
    tank.launchBullet()(config)
  }

  override protected implicit def tankState2Impl(tank: TankState): Tank = {
    new TankClientImpl(config, tank, fillBulletCallBack, tankShotgunExpireCallBack)
  }

  def receiveGameEvent(e: GameEvent) = {
    if (e.frame >= systemFrame) {
      addGameEvent(e)
    } else if (esRecoverSupport) {
      println(s"rollback-frame=${e.frame},curFrame=${this.systemFrame},e=${e}")
      rollback4GameEvent(e)
    }
  }

  //接受服务器的用户事件
  def receiveUserEvent(e: UserActionEvent) = {
    if (e.tankId == tId) {
      uncheckedActionMap.get(e.serialNum) match {
        case Some(preFrame) =>
          if (e.frame != preFrame) {
            println(s"preFrame=${preFrame} eventFrame=${e.frame} curFrame=${systemFrame}")
            //          require(preFrame <= e.frame)
            if (preFrame < e.frame && esRecoverSupport) {
              if (preFrame >= systemFrame) {
                removePreEvent(preFrame, e.tankId, e.serialNum)
                addUserAction(e)
              } else if (e.frame >= systemFrame) {
                removePreEventHistory(preFrame, e.tankId, e.serialNum)
                rollback(preFrame)
                addUserAction(e)
              } else {
                removePreEventHistory(preFrame, e.tankId, e.serialNum)
                addUserActionHistory(e)
                rollback(preFrame)
              }
            }
          }
        case None =>
          if (e.frame >= systemFrame) {
            addUserAction(e)
          } else if (esRecoverSupport) {
            rollback4UserActionEvent(e)
          }
      }
    } else {
      if (e.frame >= systemFrame) {
        addUserAction(e)
      } else if (esRecoverSupport) {
        rollback4UserActionEvent(e)
      }
    }
  }

  def preExecuteUserEvent(action: UserActionEvent) = {
    addUserAction(action)
    uncheckedActionMap.put(action.serialNum, action.frame)
  }

  final def addMyAction(action: UserActionEvent): Unit = {
    if (action.tankId == tId) {
      myTankAction.get(action.frame - preExecuteFrameOffset) match {
        case Some(actionEvents) => myTankAction.put(action.frame - preExecuteFrameOffset, action :: actionEvents)
        case None => myTankAction.put(action.frame - preExecuteFrameOffset, List(action))
      }
    }
  }

  override protected def handleUserJoinRoomEvent(e: TankGameEvent.UserJoinRoom): Unit = {
    super.handleUserJoinRoomEvent(e)
    tankInvincibleCallBack(e.tankState.tankId)
  }

  override protected def handleUserReliveEvent(e: TankGameEvent.UserRelive): Unit = {
    super.handleUserReliveEvent(e)
    tankInvincibleCallBack(e.tankState.tankId)
  }

  /** 同步游戏逻辑产生的延时事件 */
  def receiveTankFollowEventSnap(snap: TankFollowEventSnap) = {
    snap.invincibleList.foreach(addFollowEvent(_))
    snap.tankFillList.foreach(addFollowEvent(_))
    snap.shotExpireList.foreach(addFollowEvent(_))
  }

  protected def handleGameContainerAllState(gameContainerAllState: GameContainerAllState) = {
    systemFrame = gameContainerAllState.f
    quadTree.clear()
    tankMap.clear()
    obstacleMap.clear()
    propMap.clear()
    tankMoveAction.clear()
    bulletMap.clear()
    environmentMap.clear()

    //remind 重置followEventMap
    //    followEventMap.clear()
    //    gameContainerAllState.followEvent.foreach{t=>followEventMap.put(t._1,t._2)}

    gameContainerAllState.tanks.foreach { t =>
      val tank = new TankClientImpl(config, t, fillBulletCallBack, tankShotgunExpireCallBack)
      quadTree.insert(tank)
      tankMap.put(t.tankId, tank)
    }
    gameContainerAllState.obstacle.foreach { o =>
      val obstacle = Obstacle(config, o)
      quadTree.insert(obstacle)
      obstacleMap.put(o.oId, obstacle)
    }
    gameContainerAllState.props.foreach { t =>
      val prop = Prop(t, config.propRadius)
      quadTree.insert(prop)
      propMap.put(t.pId, prop)
    }
    gameContainerAllState.tankMoveAction.foreach { t =>
      val set = tankMoveAction.getOrElse(t._1, mutable.HashSet[Int]())
      t._2.foreach(set.add)
      tankMoveAction.put(t._1, set)
    }
    gameContainerAllState.bullet.foreach { t =>
      val bullet = new Bullet(config, t)
      quadTree.insert(bullet)
      bulletMap.put(t.bId, bullet)
    }
    gameContainerAllState.environment.foreach { t =>
      val obstacle = Obstacle(config, t)
      quadTree.insert(obstacle)
      environmentMap.put(obstacle.oId, obstacle)
    }
    waitSyncData = false

  }

  protected def handleGameContainerState(gameContainerState: GameContainerState) = {
    val curFrame = systemFrame
    val startTime = System.currentTimeMillis()
    (curFrame until gameContainerState.f).foreach { _ =>
      super.update()
      if (esRecoverSupport) addGameSnapShot(systemFrame, getGameContainerAllState())
    }
    val endTime = System.currentTimeMillis()
    if (curFrame < gameContainerState.f) {
      println(s"handleGameContainerState update to now use Time=${endTime - startTime} and systemFrame=${systemFrame} sysFrame=${gameContainerState.f}")
    }
    systemFrame = gameContainerState.f
    judge(gameContainerState)
    quadTree.clear()
    tankMap.clear()
    obstacleMap.clear()
    propMap.clear()
    tankMoveAction.clear()
    gameContainerState.tanks.foreach { t =>
      val tank = new TankClientImpl(config, t, fillBulletCallBack, tankShotgunExpireCallBack)
      quadTree.insert(tank)
      tankMap.put(t.tankId, tank)
    }
    gameContainerState.obstacle.foreach { o =>
      val obstacle = Obstacle(config, o)
      quadTree.insert(obstacle)
      obstacleMap.put(o.oId, obstacle)
    }
    gameContainerState.props.foreach { t =>
      val prop = Prop(t, config.propRadius)
      quadTree.insert(prop)
      propMap.put(t.pId, prop)
    }
    gameContainerState.tankMoveAction.foreach { t =>
      val set = tankMoveAction.getOrElse(t._1, mutable.HashSet[Int]())
      t._2.foreach(set.add)
      tankMoveAction.put(t._1, set)
    }
    environmentMap.values.foreach(quadTree.insert)
    bulletMap.values.foreach { bullet =>
      quadTree.insert(bullet)
      //      bullet.move(boundary,removeBullet)
    }
  }

  private def judge(gameContainerState: GameContainerState): Unit = {
    gameContainerState.tanks.foreach { tankState =>
      tankMap.get(tankState.tankId) match {
        case Some(t) =>
          if (t.getTankState() != tankState) {
            println(s"judge failed,because tank=${tankState.tankId} no same,tankMap=${t.getTankState()},gameContainer=${tankState}")
          }
        case None => println(s"judge failed,because tank=${tankState.tankId} not exists....")
      }
    }
  }

  def receiveGameContainerAllState(gameContainerAllState: GameContainerAllState) = {
    gameContainerAllStateOpt = Some(gameContainerAllState)
  }

  def receiveGameContainerState(gameContainerState: GameContainerState) = {
    if (gameContainerState.f > systemFrame) {
      gameContainerStateOpt = Some(gameContainerState)
    } else if (gameContainerState.f == systemFrame) {
      info(s"收到同步数据，立即同步，curSystemFrame=${systemFrame},sync game container state frame=${gameContainerState.f}")
      gameContainerStateOpt = None
      handleGameContainerState(gameContainerState)
    } else {
      info(s"收到同步数据，但未同步，curSystemFrame=${systemFrame},sync game container state frame=${gameContainerState.f}")
    }
  }

  override def update(): Unit = {
    //    val startTime = System.currentTimeMillis()
    if (gameContainerAllStateOpt.nonEmpty) {
      val gameContainerAllState = gameContainerAllStateOpt.get
      info(s"立即同步所有数据，curSystemFrame=${systemFrame},sync game container state frame=${gameContainerAllState.f}")
      handleGameContainerAllState(gameContainerAllState)
      gameContainerAllStateOpt = None
      if (esRecoverSupport) {
        clearEsRecoverData()
        addGameSnapShot(systemFrame, this.getGameContainerAllState())
      }
    } else if (gameContainerStateOpt.nonEmpty && (gameContainerStateOpt.get.f - 1 == systemFrame || gameContainerStateOpt.get.f - 2 > systemFrame)) {
      info(s"同步数据，curSystemFrame=${systemFrame},sync game container state frame=${gameContainerStateOpt.get.f}")
      handleGameContainerState(gameContainerStateOpt.get)
      gameContainerStateOpt = None
      if (esRecoverSupport) {
        clearEsRecoverData()
        addGameSnapShot(systemFrame, this.getGameContainerAllState())
      }
    } else {
      super.update()
      if (esRecoverSupport) addGameSnapShot(systemFrame, getGameContainerAllState())
    }
  }

  override protected def clearEventWhenUpdate(): Unit = {
    super.clearEventWhenUpdate()
    if (esRecoverSupport) {
      addEventHistory(systemFrame, gameEventMap.getOrElse(systemFrame, Nil), actionEventMap.getOrElse(systemFrame, Nil))
    }
    gameEventMap -= systemFrame
    actionEventMap -= systemFrame
    followEventMap -= systemFrame-maxFollowFrame
    systemFrame += 1
  }

  protected def rollbackUpdate(): Unit = {
    super.update()
    if (esRecoverSupport) addGameSnapShot(systemFrame, getGameContainerAllState())
  }

  def findAllTank(thisTank:Int) = {
    if(tankMap.contains(thisTank))
      Some(quadTree.retrieve(tankMap(thisTank)).filter(_.isInstanceOf[Tank]).map(_.asInstanceOf[Tank]))
    else None
  }

  def findOtherProp(thisTank:Tank) = {
    quadTree.retrieveFilter(thisTank).filter(_.isInstanceOf[Prop]).map(_.asInstanceOf[Prop])
  }

  def findOtherObstacle(thisTank:Tank) = {
    quadTree.retrieveFilter(thisTank).filter(_.isInstanceOf[Obstacle]).map(_.asInstanceOf[Obstacle])
  }

}

