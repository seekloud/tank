package com.neo.sk.tank.shared.game

import com.neo.sk.tank.shared.`object`._
import com.neo.sk.tank.shared.config.TankGameConfig
import com.neo.sk.tank.shared.model.Constants.ObstacleType
import com.neo.sk.tank.shared.model.{Point, Rectangle, Score}
import com.neo.sk.tank.shared.protocol.TankGameEvent._
import com.neo.sk.tank.shared.util.QuadTree

import scala.collection.mutable


/**
  * Created by hongruying on 2018/8/24
  * 终端
  */
class GameContainerImpl(
                         override val config: TankGameConfig,
                         myId:Long,
                         myTankId:Int,
                         myName:String
                       ) extends GameContainer with EsRecover {

  import scala.language.implicitConversions



  override def debug(msg: String): Unit = {}

  override def info(msg: String): Unit = println(msg)

  private val esRecoverSupport:Boolean = true

  private val uncheckedActionMap = mutable.HashMap[Int,Long]() //serinum -> frame

  private var gameContainerAllStateOpt:Option[GameContainerAllState] = None
  private var gameContainerStateOpt:Option[GameContainerState] = None

  protected var waitSyncData:Boolean = true


  override def tankExecuteLaunchBulletAction(tankId: Int, tank: Tank): Unit = {
    tank.launchBullet()(config)
  }

  override protected implicit def tankState2Impl(tank:TankState):Tank = {
    new TankImpl(config,tank)
  }

  def receiveGameEvent(e:GameEvent) = {
    if(e.frame >= systemFrame){
      addGameEvent(e)
    }else if(esRecoverSupport){
      println(s"rollback-frame=${e.frame},curFrame=${this.systemFrame},e=${e}")
      rollback4GameEvent(e)
    }
  }


  //接受服务器的用户事件
  def receiveUserEvent(e:UserActionEvent) = {
    if(e.tankId == myTankId){
      uncheckedActionMap.get(e.serialNum) match {
        case Some(preFrame) =>
          if(e.frame != preFrame) {
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
          if(e.frame >= systemFrame){
            addUserAction(e)
          }else if(esRecoverSupport){
            rollback4UserActionEvent(e)
          }
      }
    }else{
      if(e.frame >= systemFrame){
        addUserAction(e)
      }else if(esRecoverSupport){
        println(s"rollback-frame=${e.frame},curFrame=${this.systemFrame},e=${e}")
        rollback4GameEvent(e)
      }
    }
  }

  def preExecuteUserEvent(action:UserActionEvent) ={
    addUserAction(action)
    uncheckedActionMap.put(action.serialNum,action.frame)
  }

  //  def setUserInfo(name:String,userId:Long,tankId:Int) = {
  //    myName = name
  //    myId = userId
  //    myTankId = tankId
  //  }

  protected def handleGameContainerAllState(gameContainerAllState: GameContainerAllState) = {
    systemFrame = gameContainerAllState.f
    quadTree.clear()
    tankMap.clear()
    obstacleMap.clear()
    propMap.clear()
    tankMoveAction.clear()
    bulletMap.clear()
    environmentMap.clear()
    gameContainerAllState.tanks.foreach{t =>
      val tank = new TankImpl(config,t)
      quadTree.insert(tank)
      tankMap.put(t.tankId,tank)
    }
    gameContainerAllState.obstacle.foreach{o =>
      val obstacle = Obstacle(config,o)
      quadTree.insert(obstacle)
      obstacleMap.put(o.oId,obstacle)
    }
    gameContainerAllState.props.foreach{t =>
      val prop = Prop(t,config.propRadius)
      quadTree.insert(prop)
      propMap.put(t.pId,prop)
    }
    gameContainerAllState.tankMoveAction.foreach{t =>
      val set = tankMoveAction.getOrElse(t._1,mutable.HashSet[Int]())
      t._2.foreach(set.add)
      tankMoveAction.put(t._1,set)
    }
    gameContainerAllState.bullet.foreach{t =>
      val bullet = new Bullet(config,t)
      quadTree.insert(bullet)
      bulletMap.put(t.bId,bullet)
    }
    gameContainerAllState.environment.foreach{t =>
      val obstacle = Obstacle(config,t)
      quadTree.insert(obstacle)
      environmentMap.put(obstacle.oId,obstacle)
    }
    waitSyncData = false

  }

  protected def handleGameContainerState(gameContainerState: GameContainerState) = {
    val curFrame = systemFrame
    val startTime = System.currentTimeMillis()
    (curFrame until gameContainerState.f).foreach{_ =>
      super.update()
      if(esRecoverSupport) addGameSnapShot(systemFrame,getGameContainerAllState())
    }
    val endTime = System.currentTimeMillis()
    if(curFrame < gameContainerState.f){
      println(s"handleGameContainerState update to now use Time=${endTime-startTime}")
    }
    systemFrame = gameContainerState.f
    judge(gameContainerState)
    quadTree.clear()
    tankMap.clear()
    obstacleMap.clear()
    propMap.clear()
    tankMoveAction.clear()
    gameContainerState.tanks.foreach{t =>
      val tank = new TankImpl(config,t)
      quadTree.insert(tank)
      tankMap.put(t.tankId,tank)
    }
    gameContainerState.obstacle.foreach{o =>
      val obstacle = Obstacle(config,o)
      quadTree.insert(obstacle)
      obstacleMap.put(o.oId,obstacle)
    }
    gameContainerState.props.foreach{t =>
      val prop = Prop(t,config.propRadius)
      quadTree.insert(prop)
      propMap.put(t.pId,prop)
    }
    gameContainerState.tankMoveAction.foreach{t =>
      val set = tankMoveAction.getOrElse(t._1,mutable.HashSet[Int]())
      t._2.foreach(set.add)
      tankMoveAction.put(t._1,set)
    }
    environmentMap.values.foreach(quadTree.insert)
    bulletMap.values.foreach{ bullet =>
      quadTree.insert(bullet)
      //      bullet.move(boundary,removeBullet)
    }
  }

  private def judge(gameContainerState: GameContainerState):Unit = {
    gameContainerState.tanks.foreach{ tankState =>
      tankMap.get(tankState.tankId) match {
        case Some(t) =>
          if(t.getTankState() != tankState){
            println(s"judge failed,because tank=${tankState.tankId} no same,tankMap=${t.getPosition},gameContainer=${tankState.position}")
          }
        case None => println(s"judge failed,because tank=${tankState.tankId} not exists....")
      }

    }
  }

  def receiveGameContainerAllState(gameContainerAllState: GameContainerAllState) = {
    gameContainerAllStateOpt = Some(gameContainerAllState)
  }

  def receiveGameContainerState(gameContainerState: GameContainerState) = {
    if(gameContainerState.f > systemFrame){
      gameContainerStateOpt = Some(gameContainerState)
    }else if(gameContainerState.f == systemFrame){
      info(s"收到同步数据，但未同步，curSystemFrame=${systemFrame},sync game container state frame=${gameContainerState.f}")
      gameContainerStateOpt = None
      handleGameContainerState(gameContainerState)
    }else{
      info(s"收到同步数据，但未同步，curSystemFrame=${systemFrame},sync game container state frame=${gameContainerState.f}")
    }

  }


  override def update(): Unit = {
    //    val startTime = System.currentTimeMillis()
    if(gameContainerAllStateOpt.nonEmpty){
      val gameContainerAllState = gameContainerAllStateOpt.get
      info(s"立即同步所有数据，curSystemFrame=${systemFrame},sync game container state frame=${gameContainerAllState.f}")
      handleGameContainerAllState(gameContainerAllState)
      gameContainerAllStateOpt = None
      if(esRecoverSupport){
        clearEsRecoverData()
        addGameSnapShot(systemFrame,this.getGameContainerAllState())
      }
    }else if(gameContainerStateOpt.nonEmpty && (gameContainerStateOpt.get.f - 1  == systemFrame || gameContainerStateOpt.get.f - 2 > systemFrame)){
      info(s"同步数据，curSystemFrame=${systemFrame},sync game container state frame=${gameContainerStateOpt.get.f}")
      handleGameContainerState(gameContainerStateOpt.get)
      gameContainerStateOpt = None
      if(esRecoverSupport){
        clearEsRecoverData()
        addGameSnapShot(systemFrame,this.getGameContainerAllState())
      }
    }else{
      super.update()
      if(esRecoverSupport) addGameSnapShot(systemFrame,getGameContainerAllState())
    }
  }

  override protected def clearEventWhenUpdate(): Unit = {
    if(esRecoverSupport){
      addEventHistory(systemFrame,gameEventMap.getOrElse(systemFrame,Nil), actionEventMap.getOrElse(systemFrame, Nil))
    }
    gameEventMap -= systemFrame
    actionEventMap -= systemFrame
    systemFrame += 1
  }


  protected def rollbackUpdate():Unit = {
    super.update()
    if(esRecoverSupport) addGameSnapShot(systemFrame,getGameContainerAllState())
  }
}
