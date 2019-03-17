/*
 * Copyright 2018 seekloud (https://github.com/seekloud)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.seekloud.tank.shared.game

import org.seekloud.tank.shared.`object`._
import org.seekloud.tank.shared.config.TankGameConfig
import org.seekloud.tank.shared.game.view._
import org.seekloud.tank.shared.model.Constants.{GameAnimation, PropGenerateType}
import org.seekloud.tank.shared.model.Point
import org.seekloud.tank.shared.protocol.TankGameEvent
import org.seekloud.tank.shared.protocol.TankGameEvent._
import org.seekloud.tank.shared.util.canvas.{MiddleCanvas, MiddleContext, MiddleFrame}

import scala.collection.mutable

/**
  * Created by hongruying on 2018/8/24
  * 终端
  *
  * @param canvasSize canvas实际大小
  * @param canvasBoundary canvas比例大小
  */
case class GameContainerClientImpl(
                                    drawFrame: MiddleFrame,
                                    viewCanvas: MiddleCanvas,
                                    override val config: TankGameConfig,
                                    myId: String,
                                    myTankId: Int,
                                    myName: String,
                                    var canvasSize: Point,
                                    var canvasUnit: Int,
                                    setKillCallback: Tank => Unit,
                                    versionInfo: Option[String] = None,
                                    isBot: Boolean = false,
                                    logInfo: String => Unit = println,
                                    layerCanvasSize:Point = Point(400,200),
                                    layerCanvasUnit: Int = 2
                                  ) extends GameContainer with EsRecover
  with BackgroundDrawUtil with BulletDrawUtil with FpsComponentsDrawUtil with ObstacleDrawUtil with PropDrawUtil with TankDrawUtil with InfoDrawUtil {

  import scala.language.implicitConversions

  val viewCtx = viewCanvas.getCtx

  /**
    * 目前 canvasBoundary遗留问题，在各绘制文件中的canvasBoundary可以使用canvasSize替换
    * layerCanvasUnit的使用
    * 分层图层重写
    * location: 视野范围
    * immutable: 钢铁、河流
    * mutable: 子弹、道具、砖块
    * bodies: 所有坦克，每个坦克颜色不同，开始可根据（tankId%255，三种等级对应（0，100，255），0）
    * ownership: 权视图，所有坦克操纵核心，所有子弹，对应相应颜色
    * self: 自身操控核心,自己的子弹
    * pointer: 鼠标位置
    * state: 自身坦克状态，柱状图：血量等级，速度等级，子弹等级，医疗包数量，血量剩余，子弹剩余
    **/

  val locationCanvas = if (isBot) drawFrame.createCanvas(layerCanvasSize.x, layerCanvasSize.y) else viewCanvas
  val locationCtx = locationCanvas.getCtx
  val immutableCanvas = if (isBot) drawFrame.createCanvas(layerCanvasSize.x, layerCanvasSize.y) else viewCanvas
  val immutableCtx = immutableCanvas.getCtx
  val mutableCanvas = if (isBot) drawFrame.createCanvas(layerCanvasSize.x, layerCanvasSize.y) else viewCanvas
  val mutableCtx = mutableCanvas.getCtx
  val bodiesCanvas = if (isBot) drawFrame.createCanvas(layerCanvasSize.x, layerCanvasSize.y) else viewCanvas
  val bodiesCtx = bodiesCanvas.getCtx
  val ownerShipCanvas = if (isBot) drawFrame.createCanvas(layerCanvasSize.x, layerCanvasSize.y) else viewCanvas
  val ownerShipCtx = ownerShipCanvas.getCtx
  val selfCanvas = if(isBot)drawFrame.createCanvas(layerCanvasSize.x,layerCanvasSize.y) else viewCanvas
  val selfCtx = selfCanvas.getCtx
  val statusCanvas = if (isBot) drawFrame.createCanvas(layerCanvasSize.x, layerCanvasSize.y) else viewCanvas
  val statusCtx = statusCanvas.getCtx

  protected val obstacleAttackedAnimationMap = mutable.HashMap[Int, Int]()
  protected val tankAttackedAnimationMap = mutable.HashMap[Int, Int]()
  protected val tankDestroyAnimationMap = mutable.HashMap[Int, Int]() //prop ->

  protected var killerList = List.empty[String]
  protected var killNum: Int = 0
  protected var damageNum: Int = 0
  protected var killerName: String = ""

  var tankId: Int = myTankId
  protected val myTankMoveAction = mutable.HashMap[Long, List[UserActionEvent]]()

  def changeTankId(id: Int) = tankId = id

  def updateDamageInfo(myKillNum: Int, name: String, myDamageNum: Int): Unit = {
    killerList = killerList :+ name
    killerName = name
    killNum = myKillNum
    damageNum = myDamageNum
  }

  def getCurTankId: Int = tankId

  def change2OtherTank: Int = {
    val keys = tankMap.keys.toArray
    val idx = (new util.Random).nextInt(keys.length)
    keys(idx)
  }

  def isKillerAlive(killerId: Int): Boolean = {
    if (tankMap.contains(killerId)) true else false
  }

  override def debug(msg: String): Unit = {}

  override def info(msg: String): Unit = logInfo(msg)

  private val esRecoverSupport: Boolean = true

  private val uncheckedActionMap = mutable.HashMap[Byte, Long]() //serinum -> frame

  private var gameContainerAllStateOpt: Option[GameContainerAllState] = None
  private var gameContainerStateOpt: Option[GameContainerState] = None

  protected var waitSyncData: Boolean = true

  private val preExecuteFrameOffset = org.seekloud.tank.shared.model.Constants.PreExecuteFrameOffset

  def updateClientSize(canvasS: Point, cUnit: Int) = {
    canvasUnit = cUnit
    canvasSize = canvasS*canvasUnit
    //fixme 其余canvasBoundary的修改
    updateBackSize(canvasS)
    updateBulletSize(canvasS)
    updateFpsSize(canvasS)
    updateObstacleSize(canvasS)
    updateTankSize(canvasS)
  }

  override protected def handleObstacleAttacked(e: TankGameEvent.ObstacleAttacked): Unit = {
    super.handleObstacleAttacked(e)
    if (obstacleMap.get(e.obstacleId).nonEmpty || environmentMap.get(e.obstacleId).nonEmpty) {
      obstacleAttackedAnimationMap.put(e.obstacleId, GameAnimation.bulletHitAnimationFrame)
    }
  }

  override protected def handleGenerateBullet(e: GenerateBullet) = {
    tankMap.get(e.bullet.tankId) match {
      case Some(tank) =>
        //todo
        if (e.s) {
          tank.setTankGunDirection(math.atan2(e.bullet.momentum.y, e.bullet.momentum.x).toFloat)
          tankExecuteLaunchBulletAction(tank.tankId, tank)
        }
      case None =>
        info(s"--------------------该子弹没有对应的tank")
    }
    super.handleGenerateBullet(e)
  }


  override protected def handleTankAttacked(e: TankGameEvent.TankAttacked): Unit = {
    super.handleTankAttacked(e)
    if (tankMap.get(e.tankId).nonEmpty) {
      tankAttackedAnimationMap.put(e.tankId, GameAnimation.bulletHitAnimationFrame)
    }
  }

  override protected def dropTankCallback(bulletTankId: Int, bulletTankName: String, tank: Tank) = {
    setKillCallback(tank)
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
      info(s"rollback-frame=${e.frame},curFrame=${this.systemFrame},e=${e}")
      rollback4GameEvent(e)
    }
  }

  //接受服务器的用户事件
  def receiveUserEvent(e: UserActionEvent) = {
    if (e.tankId == tankId) {
      uncheckedActionMap.get(e.serialNum) match {
        case Some(preFrame) =>
          if (e.frame != preFrame) {
            info(s"preFrame=${preFrame} eventFrame=${e.frame} curFrame=${systemFrame}")
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
    if (action.tankId == tankId) {
      myTankMoveAction.get(action.frame - preExecuteFrameOffset) match {
        case Some(actionEvents) => myTankMoveAction.put(action.frame - preExecuteFrameOffset, action :: actionEvents)
        case None => myTankMoveAction.put(action.frame - preExecuteFrameOffset, List(action))
      }
    }
  }


  //todo 处理出现错误动作的帧
  protected final def handleMyAction(actions: List[UserActionEvent]) = {
    if (tankMap.contains(tankId)) {
      val tank = tankMap(tankId).asInstanceOf[TankClientImpl]
      if (actions.nonEmpty) {
//        val tankMoveSet = mutable.Set[Byte]()
        var tankMoveState:Byte = 8
        actions.sortBy(t => t.serialNum).foreach {
//          case a: UserPressKeyDown =>
//            tankMoveSet.add(a.keyCodeDown)
//          case a: UserPressKeyUp =>
//            tankMoveSet.remove(a.keyCodeUp)
          case a: UserMoveState =>
            tankMoveState = a.moveState
          case _ =>
        }
//        if (tankMoveSet.nonEmpty && !tank.getTankIsMove()) {
//          tank.setFakeTankDirection(tankMoveSet.toSet, systemFrame)
//        }
        if (tankMoveState < 8 && !tank.getTankIsMove()) {
          tank.setFakeTankDirection(tankMoveState, systemFrame)
        }
      }
    }
  }

  protected def handleFakeActionNow(): Unit = {
    if (org.seekloud.tank.shared.model.Constants.fakeRender) {
      handleMyAction(myTankMoveAction.getOrElse(systemFrame, Nil).reverse)
      myTankMoveAction.remove(systemFrame - 10)
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
    tankMoveState.clear()
    bulletMap.clear()
    environmentMap.clear()

    gameContainerAllState.tanks.foreach { t =>
      val tank = new TankClientImpl(config, t, fillBulletCallBack, tankShotgunExpireCallBack)
      quadTree.insert(tank)
      tankMap.put(t.tankId, tank)
      tankHistoryMap.put(t.tankId, tank.name)
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
    gameContainerAllState.tankMoveState.foreach { t =>
      tankMoveState.put(t._1, t._2)
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
      info(s"handleGameContainerState update to now use Time=${endTime - startTime} and systemFrame=${systemFrame} sysFrame=${gameContainerState.f}")
    }

    if (!judge(gameContainerState) || systemFrame != gameContainerState.f) {
      systemFrame = gameContainerState.f
      quadTree.clear()
      tankMap.clear()
      tankMoveState.clear()
      gameContainerState.tanks match {
        case Some(tanks) =>
          tanks.foreach { t =>
            val tank = new TankClientImpl(config, t, fillBulletCallBack, tankShotgunExpireCallBack)
            quadTree.insert(tank)
            tankMap.put(t.tankId, tank)
            tankHistoryMap.put(t.tankId, tank.name)
          }
        case None =>
          info(s"handle game container client--no tanks")
      }
      gameContainerState.tankMoveState match {
        case Some(as) =>
          as.foreach { t =>
            tankMoveState.put(t._1, t._2)
          }
        case None =>
      }
      obstacleMap.values.foreach(o => quadTree.insert(o))
      propMap.values.foreach(o => quadTree.insert(o))

      environmentMap.values.foreach(quadTree.insert)
      bulletMap.values.foreach { bullet =>
        quadTree.insert(bullet)
      }
    }
  }

  private def judge(gameContainerState: GameContainerState) = {
    gameContainerState.tanks match {
      case Some(tanks) =>
        tanks.forall { tankState =>
          tankMap.get(tankState.tankId) match {
            case Some(t) =>
              //fixme 此处排除炮筒方向
              if (t.getTankState().copy(gunDirection = 0f) != tankState.copy(gunDirection = 0f)) {
                info(s"judge failed,because tank=${tankState.tankId} no same,tankMap=${t.getTankState()},gameContainer=${tankState}")
                false
              } else true
            case None => {
              info(s"judge failed,because tank=${tankState.tankId} not exists....")
              true
            }
          }
        }
      case None =>
        info(s"game container client judge function no tanks---")
        true
    }
  }

  def receiveGameContainerAllState(gameContainerAllState: GameContainerAllState) = {
    gameContainerAllStateOpt = Some(gameContainerAllState)
  }

  def receiveGameContainerState(gameContainerState: GameContainerState) = {
    if (gameContainerState.f > systemFrame) {
      gameContainerState.tanks match {
        case Some(tank) =>
          gameContainerStateOpt = Some(gameContainerState)
        case None =>
          gameContainerStateOpt match {
            case Some(state) =>
              gameContainerStateOpt = Some(TankGameEvent.GameContainerState(gameContainerState.f, state.tanks, state.tankMoveState))
            case None =>
          }
      }
    } else if (gameContainerState.f == systemFrame) {
      gameContainerState.tanks match {
        case Some(tanks) =>
          info(s"收到同步数据，立即同步，curSystemFrame=${systemFrame},sync game container state frame=${gameContainerState.f}")
          gameContainerStateOpt = None
          handleGameContainerState(gameContainerState)
        case None =>
          info(s"收到同步帧号的数据")
      }
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
    if (esRecoverSupport) {
      addEventHistory(systemFrame, gameEventMap.getOrElse(systemFrame, Nil), actionEventMap.getOrElse(systemFrame, Nil))
    }
    handleFakeActionNow()
    gameEventMap -= systemFrame
    actionEventMap -= systemFrame
    followEventMap -= systemFrame - maxFollowFrame
    systemFrame += 1
  }

  protected def rollbackUpdate(): Unit = {
    super.update()
    if (esRecoverSupport) addGameSnapShot(systemFrame, getGameContainerAllState())
  }

  //  def drawGame(time: Long, networkLatency: Long, dataSize:String): Unit = {
  def drawGame(time: Long, networkLatency: Long, dataSizeList: List[String], supportLiveLimit: Boolean = false): Unit = {
    val offsetTime = math.min(time, config.frameDuration)
    val h = canvasSize.y / canvasUnit
    val w = canvasSize.x / canvasUnit
    //    val startTime = System.currentTimeMillis()
    if (!waitSyncData) {
      viewCtx.setLineCap("round")
      viewCtx.setLineJoin("round")
      tankMap.get(tankId) match {
        case Some(tank) =>
          val offset = canvasSize / canvasUnit / 2 - tank.asInstanceOf[TankClientImpl].getPosition4Animation(boundary, quadTree, offsetTime, systemFrame)
          drawBackground(offset)
          drawLocationMap(tank)
          drawKernelMap(tank)
          drawObstacles(offset, Point(w, h))
          drawEnvironment(offset, Point(w, h))
          drawProps(offset, Point(w, h))
          drawBullet(offset, offsetTime, Point(w, h))
          drawTank(offset, offsetTime, Point(w, h))
          drawObstacleBloodSlider(offset)
          drawMyTankInfo(tank.asInstanceOf[TankClientImpl], supportLiveLimit)
          drawMinimap(tank)
          drawRank(supportLiveLimit, tank.tankId, tank.name)
          renderFps(networkLatency, dataSizeList)
          drawKillInformation()
          drawRoomNumber()
          drawCurMedicalNum(tank.asInstanceOf[TankClientImpl])
          val endTime = System.currentTimeMillis()
        //          renderTimes += 1
        //          renderTime += endTime - startTime


        case None =>
        //          info(s"tankid=${myTankId} has no in tankMap.....................................")
        //          setGameState(GameState.stop)
        //          if(isObserve) drawDeadImg()
      }
    }
  }

  def findAllTank(thisTank: Int) = {
    if (tankMap.contains(thisTank))
      Some(quadTree.retrieve(tankMap(thisTank)).filter(_.isInstanceOf[Tank]).map(_.asInstanceOf[Tank]))
    else None
  }

  def findOtherBullet(thisTank: Int) = {
    quadTree.retrieveFilter(tankMap(thisTank)).filter(_.isInstanceOf[Bullet]).map(_.asInstanceOf[Bullet])
  }

  def findOtherObstacle(thisTank: Tank) = {
    quadTree.retrieveFilter(thisTank).filter(_.isInstanceOf[Obstacle]).map(_.asInstanceOf[Obstacle])
  }


}
