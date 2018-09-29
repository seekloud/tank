package com.neo.sk.tank.shared.game

import com.neo.sk.tank.shared.`object`._
import com.neo.sk.tank.shared.config.TankGameConfig
import com.neo.sk.tank.shared.model.Constants.ObstacleType
import com.neo.sk.tank.shared.model.{Point, Rectangle, Score}
import com.neo.sk.tank.shared.protocol.TankGameEvent._
import com.neo.sk.tank.shared.util.QuadTree

import scala.collection.mutable

/**
  * Created by hongruying on 2018/8/22
  * 游戏逻辑的基类
  *
  * 逻辑帧更新逻辑：
  * 先处理玩家离开的游戏事件
  * 坦克和子弹的运动逻辑，障碍检测，（坦克吃道具事件，子弹攻击目标的事件）
  * 更新用户操作所影响坦克的状态
  * 伤害计算事件
  * 坦克吃道具事件
  * 用户生成子弹事件
  * 用户加入游戏事件的处理
  */
final case class GameContainerAllState(
                                        f:Long,
                                        tanks:List[TankState],
                                        bullet:List[BulletState],
                                        props:List[PropState],
                                        obstacle:List[ObstacleState],
                                        environment:List[ObstacleState],
                                        tankMoveAction:List[(Int,List[Int])]
                                      )

case class GameContainerState(
                               f:Long,
                               tanks:List[TankState],
                               props:List[PropState],
                               obstacle:List[ObstacleState],
                               tankMoveAction:List[(Int,List[Int])]
                             )



trait GameContainer extends KillInformation{

  import scala.language.implicitConversions

  def debug(msg: String): Unit

  def info(msg: String): Unit

  implicit val config:TankGameConfig

  val boundary : Point = config.boundary

  var currentRank = List.empty[Score]

  var historyRankMap =Map.empty[Int,Score]
  var historyRank = historyRankMap.values.toList.sortBy(_.d).reverse
  var historyRankThreshold =if (historyRank.isEmpty)-1 else historyRank.map(_.d).min
  val historyRankLength = 5
  val tankLivesMap:mutable.HashMap[Int,TankState] = mutable.HashMap[Int,TankState]() // tankId -> lives
//  val tankEatPropMap = mutable.HashMap[Int,mutable.HashSet[Prop]]()//tankId -> Set(propId)

  var tankId = -1
  var systemFrame:Long = 0L //系统帧数

  val tankMap = mutable.HashMap[Int,Tank]() //tankId -> Tank
  val bulletMap = mutable.HashMap[Int,Bullet]() //bulletId -> Bullet
  val obstacleMap = mutable.HashMap[Int,Obstacle]() //obstacleId -> Obstacle
  val environmentMap = mutable.HashMap[Int,Obstacle]() //obstacleId -> steel and river
  val propMap = mutable.HashMap[Int,Prop]() //propId -> prop 道具信息

  val tankMoveAction = mutable.HashMap[Int,mutable.HashSet[Int]]() //tankId -> pressed direction key code

  protected val quadTree : QuadTree = new QuadTree(Rectangle(Point(0,0),boundary))


  protected val gameEventMap = mutable.HashMap[Long,List[GameEvent]]() //frame -> List[GameEvent] 待处理的事件 frame >= curFrame
  protected val actionEventMap = mutable.HashMap[Long,List[UserActionEvent]]() //frame -> List[UserActionEvent]
  protected val myTankAction = mutable.HashMap[Long,List[UserActionEvent]]()
  final protected def handleUserJoinRoomEvent(l:List[UserJoinRoom]) :Unit = {
    l foreach handleUserJoinRoomEvent
  }


  final protected def handleUserJoinRoomEvent(e:UserJoinRoom) :Unit = {
    val tank : Tank = e.tankState
    tankMap.put(e.tankState.tankId,tank)
    quadTree.insert(tank)
  }

  //服务器和客户端执行的逻辑不一样
  protected implicit def tankState2Impl(tank:TankState):Tank

  //服务器和客户端执行的逻辑不一致
  protected def handleUserJoinRoomEventNow() = {
    gameEventMap.get(systemFrame).foreach{ events =>
      handleUserJoinRoomEvent(events.filter(_.isInstanceOf[UserJoinRoom]).map(_.asInstanceOf[UserJoinRoom]).reverse)
    }
  }

  protected final def handleUserLeftRoom(e:UserLeftRoom) :Unit = {
    tankMoveAction.remove(e.tankId)
    tankMap.get(e.tankId).foreach(quadTree.remove)
    tankMap.remove(e.tankId)
  }

  final protected def handleUserLeftRoom(l:List[UserLeftRoom]) :Unit = {
    l foreach handleUserLeftRoom
  }

  final protected def handleUserLeftRoomNow() = {
    gameEventMap.get(systemFrame).foreach{ events =>
      handleUserLeftRoom(events.filter(_.isInstanceOf[UserLeftRoom]).map(_.asInstanceOf[UserLeftRoom]).reverse)
    }
  }

  protected final def handleUserActionEvent(actions:List[UserActionEvent]) = {
    /**
      * 用户行为事件
      * */
    actions.sortBy(t => (t.tankId,t.serialNum)).foreach{ action =>
      val tankMoveSet = tankMoveAction.getOrElse(action.tankId,mutable.HashSet[Int]())
      tankMap.get(action.tankId) match {
        case Some(tank) =>
          action match {
            case a:UserMouseMove => tank.setTankGunDirection(a.d)
            case a:UserMouseClick => tankExecuteLaunchBulletAction(a.tankId,tank)
            case a:UserPressKeyDown =>
              tankMoveSet.add(a.keyCodeDown)
              tankMoveAction.put(a.tankId,tankMoveSet)
              tank.setTankDirection(tankMoveSet.toSet)
            case a:UserPressKeyUp =>
              tankMoveSet.remove(a.keyCodeUp)
              tankMoveAction.put(a.tankId,tankMoveSet)
              tank.setTankDirection(tankMoveSet.toSet)
            case a:UserKeyboardMove => tank.setTankKeyBoardDirection(a.angle)
            case a:UserPressKeyMedical => tank.addBlood()
          }
        case None => info(s"tankId=${action.tankId} action=${action} is no valid,because the tank is not exist")
      }
    }
  }

  protected final def handleMyAction(actions:List[UserActionEvent]) = {

    def isHaveReal(id: Int) = {
      var isHave = false
      actionEventMap.get(systemFrame).foreach {
        list =>
          list.foreach {
            a =>
              if (a.tankId == id) isHave = true
          }
      }
      isHave
    }

    if (tankId != -1) {
      val tank = tankMap(tankId)
      if (!isHaveReal(tankId)) {
        if (!tank.getIsMove()) {
          tank.isFakeMove = true
          tank.fakePosition = tank.getPosition
          val tankMoveSet = mutable.Set[Int]()
          actions.sortBy(t => t.serialNum).foreach {

            case a: UserPressKeyDown =>
              tankMoveSet.add(a.keyCodeDown)
              tank.setTankDirection(tankMoveSet.toSet)
//            case a: UserPressKeyUp =>
//              tankMoveSet.remove(a.keyCodeUp)
//              tank.setTankDirection(tankMoveSet.toSet)
            case a: UserKeyboardMove => tank.setTankKeyBoardDirection(a.angle)
            case _ =>
          }
        }
      }else{
        if(tank.isFakeMove) tank.cavasFrame = 1
        tank.isFakeMove = false
      }
    }
  }

  final def getTankId(id:Int) = {
    tankId = id
  }

  final protected def handleUserActionEventNow() = {
    actionEventMap.get(systemFrame).foreach{ actionEvents =>
      handleUserActionEvent(actionEvents.reverse)
    }
  }
  final protected def handleMyActionNow() = {
    handleMyAction(myTankAction.getOrElse(systemFrame,Nil).reverse)
    myTankAction.remove(systemFrame - 10)
  }

  /**
    * 服务器和客户端执行的逻辑不一致
    * 服务器需要进行坦克子弹容量计算，子弹生成事件，
    * 客户端只需要进行子弹容量计算
    * */
  protected def tankExecuteLaunchBulletAction(tankId:Int,tank:Tank) : Unit


  protected def handleTankAttacked(e:TankAttacked) :Unit = {
    bulletMap.get(e.bulletId).foreach(quadTree.remove)
    bulletMap.remove(e.bulletId)
    val bulletTankOpt = tankMap.get(e.bulletTankId)
    tankMap.get(e.tankId).foreach{ tank =>
      tank.attackedDamage(e.damage)
      bulletTankOpt.foreach(_.damageStatistics += e.damage)
      if(!tank.isLived()){
        bulletTankOpt.foreach(_.killTankNum += 1)
        quadTree.remove(tank)
        tankMap.remove(e.tankId)
        tankMoveAction.remove(e.tankId)

        addKillInfo(e.bulletTankName,tank.name)
        dropTankCallback(e.bulletTankId, e.bulletTankName,tank)
      }
    }
  }

  protected def dropTankCallback(bulletTankId:Int, bulletTankName:String,tank:Tank):Unit = {}


  protected final def handleTankAttacked(es:List[TankAttacked]) :Unit = {
    es foreach handleTankAttacked
  }

  final protected def handleTankAttackedNow() = {
    gameEventMap.get(systemFrame).foreach{ events =>
      handleTankAttacked(events.filter(_.isInstanceOf[TankAttacked]).map(_.asInstanceOf[TankAttacked]).reverse)
    }
  }

  protected def handleObstacleAttacked(e:ObstacleAttacked) :Unit = {
    bulletMap.get(e.bulletId).foreach(quadTree.remove)
    bulletMap.remove(e.bulletId)
    obstacleMap.get(e.obstacleId).foreach{ obstacle =>
      obstacle.attackDamage(e.damage)
      if(!obstacle.isLived()){
        quadTree.remove(obstacle)
        obstacleMap.remove(e.obstacleId)
      }
    }
  }



  protected final def handleObstacleAttacked(es:List[ObstacleAttacked]) :Unit = {
    es foreach handleObstacleAttacked
  }

  final protected def handleObstacleAttackedNow() = {
    gameEventMap.get(systemFrame).foreach{ events =>
      handleObstacleAttacked(events.filter(_.isInstanceOf[ObstacleAttacked]).map(_.asInstanceOf[ObstacleAttacked]).reverse)
    }
  }

  protected def handleTankEatProp(e:TankEatProp) :Unit = {
    propMap.get(e.propId).foreach{ prop =>
      if(prop.propType == 4){
        quadTree.remove(prop)
        propMap.remove(e.propId)
      }
      else{
        quadTree.remove(prop)
        tankMap.get(e.tankId).foreach(_.eatProp(prop))
        propMap.remove(e.propId)
      }
    }
  }


  protected final def handleTankEatProp(es:List[TankEatProp]) :Unit = {
    es foreach handleTankEatProp
  }

  final protected def handleTankEatPropNow() = {
    /**
      * 坦克吃道具,propType==4
      * */
    gameEventMap.get(systemFrame).foreach{ events =>
      handleTankEatProp(events.filter(_.isInstanceOf[TankEatProp]).map(_.asInstanceOf[TankEatProp]).reverse)
    }
  }

  protected def handleGenerateBullet(e:GenerateBullet) :Unit = {
    val bullet = new Bullet(config,e.bullet)
    bulletMap.put(e.bullet.bId,bullet)
    quadTree.insert(bullet)
  }


  protected final def handleGenerateBullet(es:List[GenerateBullet]) :Unit = {
    es foreach handleGenerateBullet
  }

  final protected def handleGenerateBulletNow() = {
    gameEventMap.get(systemFrame).foreach{ events =>
      handleGenerateBullet(events.filter(_.isInstanceOf[GenerateBullet]).map(_.asInstanceOf[GenerateBullet]).reverse)
    }
  }

  protected def handleGenerateProp(e:GenerateProp) :Unit = {
    val prop = Prop(e.propState,config.propRadius)
    propMap.put(prop.pId,prop)
    quadTree.insert(prop)
  }

  protected final def handleGenerateProp(es:List[GenerateProp]) :Unit = {
    es foreach handleGenerateProp
  }

  final protected def handleGeneratePropNow() = {
    gameEventMap.get(systemFrame).foreach{ events =>
      handleGenerateProp(events.filter(_.isInstanceOf[GenerateProp]).map(_.asInstanceOf[GenerateProp]).reverse)
    }
  }

  protected def handleGenerateObstacle(e:GenerateObstacle) :Unit = {
    val obstacle = Obstacle(config,e.obstacleState)
    if (e.obstacleState.t <= ObstacleType.brick) obstacleMap.put(obstacle.oId,obstacle)
    else environmentMap.put(obstacle.oId,obstacle)
    quadTree.insert(obstacle)
  }

  protected final def handleGenerateObstacle(es:List[GenerateObstacle]) :Unit = {
    es foreach handleGenerateObstacle
  }

  final protected def handleGenerateObstacleNow() = {
    gameEventMap.get(systemFrame).foreach{ events =>
      handleGenerateObstacle(events.filter(_.isInstanceOf[GenerateObstacle]).map(_.asInstanceOf[GenerateObstacle]).reverse)
    }
  }

  protected def handleTankFillBullet(e:TankFillBullet) :Unit = {
    tankMap.get(e.tankId).foreach{ tank =>
      tank.fillABullet()
    }
  }

  protected final def handleTankFillBullet(es:List[TankFillBullet]) :Unit = {
    es foreach handleTankFillBullet
  }

  final protected def handleTankFillBulletNow() = {
    gameEventMap.get(systemFrame).foreach{ events =>
      handleTankFillBullet(events.filter(_.isInstanceOf[TankFillBullet]).map(_.asInstanceOf[TankFillBullet]).reverse)
    }
  }


  protected def handleTankInvincible(e:TankInvincible) :Unit = {
    tankMap.get(e.tankId).foreach{ tank =>
      tank.clearInvincibleState()
    }
  }

  protected final def handleTankInvincible(es:List[TankInvincible]) :Unit = {
    es foreach handleTankInvincible
  }

  final protected def handleTankInvincibleNow() :Unit = {
    gameEventMap.get(systemFrame).foreach{ events =>
      handleTankInvincible(events.filter(_.isInstanceOf[TankInvincible]).map(_.asInstanceOf[TankInvincible]).reverse)
    }
  }


  protected def handleTankShotgunExpire(e:TankShotgunExpire) :Unit = {
    tankMap.get(e.tankId).foreach{ tank =>
      tank.clearShotgunState()
    }
  }

  protected final def handleTankShotgunExpire(es:List[TankShotgunExpire]) :Unit = {
    es foreach handleTankShotgunExpire
  }

  final protected def handleTankShotgunExpireNow() = {
    gameEventMap.get(systemFrame).foreach{ events =>
      handleTankShotgunExpire(events.filter(_.isInstanceOf[TankShotgunExpire]).map(_.asInstanceOf[TankShotgunExpire]).reverse)
    }
  }



  protected def tankMove():Unit = {
    /**
      * 坦克移动过程中检测是否吃道具
      * */
    tankMap.toList.sortBy(_._1).map(_._2).foreach{ tank =>
      tank.move(boundary,quadTree)
      //tank 进行检测是否吃到道具
      val tankMaybeEatProps = quadTree.retrieveFilter(tank).filter(_.isInstanceOf[Prop]).map(_.asInstanceOf[Prop])
      tankMaybeEatProps.foreach(tank.checkEatProp(_,tankEatPropCallback(tank)))
    }
  }

  //后台需要重写，生成迟到道具事件，客户端不必重写
  protected def tankEatPropCallback(tank:Tank)(prop: Prop):Unit = {}

  protected def bulletMove():Unit = {
    bulletMap.toList.sortBy(_._1).map(_._2).foreach{ bullet =>
      val objects = quadTree.retrieveFilter(bullet)
      objects.filter(_.isInstanceOf[Tank]).map(_.asInstanceOf[Tank]).filter(_.tankId != bullet.tankId)
        .foreach(t => bullet.checkAttackObject(t,attackTankCallBack(bullet)))
      objects.filter(t => t.isInstanceOf[ObstacleBullet] && t.isInstanceOf[Obstacle]).map(_.asInstanceOf[Obstacle])
        .foreach(t => bullet.checkAttackObject(t,attackObstacleCallBack(bullet)))
      bullet.move(boundary,removeBullet)

    }
  }


  protected def bulletFlyEndCallback(bullet: Bullet):Unit = {
    bulletMap.remove(bullet.bId)
    quadTree.remove(bullet)
  }

  //游戏后端需要重写，生成伤害事件
  protected def attackTankCallBack(bullet: Bullet)(tank:Tank):Unit = {
    removeBullet(bullet)
  }


  //子弹攻击到障碍物的回调函数，游戏后端需要重写,生成伤害事件
  protected def attackObstacleCallBack(bullet: Bullet)(o:Obstacle):Unit = {
    removeBullet(bullet)
  }

  protected final def removeBullet(bullet: Bullet):Unit = {
    bulletMap.remove(bullet.bId)
    quadTree.remove(bullet)
  }

  protected final def objectMove():Unit = {
    tankMove()
    bulletMove()
  }

  protected final def addUserAction(action:UserActionEvent):Unit = {
    /**
      * 增加用户使用血包
      * */
//    info(s"frame=${action.frame},action=${action}")
    actionEventMap.get(action.frame) match {
      case Some(actionEvents) => actionEventMap.put(action.frame,action :: actionEvents)
      case None => actionEventMap.put(action.frame,List(action))
    }
  }


  protected final def addGameEvent(event:GameEvent):Unit = {
    gameEventMap.get(event.frame) match {
      case Some(events) => gameEventMap.put(event.frame, event :: events)
      case None => gameEventMap.put(event.frame,List(event))
    }
  }

  final protected def handlePropLifecycleNow() = {
    propMap.values.foreach{ prop =>
      if(!prop.updateLifecycle()){
        quadTree.remove(prop)
        propMap.remove(prop.pId)
      }
    }
  }

  //更新本桢的操作
  def update():Unit = {
    handleUserLeftRoomNow()
    objectMove()
    handleUserActionEventNow()
    handleMyActionNow()

    handleTankAttackedNow()
    handleObstacleAttackedNow()

    handleTankFillBulletNow()
    handleTankInvincibleNow()
    handleTankShotgunExpireNow()

    handleTankEatPropNow()

    handlePropLifecycleNow()

    handleGenerateObstacleNow()
    handleGeneratePropNow()
    handleGenerateBulletNow()
    handleUserJoinRoomEventNow()

    quadTree.refresh(quadTree)
    updateKillInformation()
    clearEventWhenUpdate()

  }

  protected def clearEventWhenUpdate():Unit = {
    gameEventMap -= systemFrame
    actionEventMap -= systemFrame
    systemFrame += 1
  }

  def getGameContainerAllState():GameContainerAllState = {
    GameContainerAllState(
      systemFrame,
      tankMap.values.map(_.getTankState()).toList,
      bulletMap.values.map(_.getBulletState()).toList,
      propMap.values.map(_.getPropState).toList,
      obstacleMap.values.map(_.getObstacleState()).toList,
      environmentMap.values.map(_.getObstacleState()).toList,
      tankMoveAction = tankMoveAction.toList.map(t => (t._1,t._2.toList))
    )
  }

  protected def addGameEvents(frame:Long,events:List[GameEvent],actionEvents:List[UserActionEvent]) = {
    gameEventMap.put(frame,events)
    actionEventMap.put(frame,actionEvents)
  }

  def removePreEvent(frame:Long, tankId:Int, serialNum:Int):Unit = {
    actionEventMap.get(frame).foreach{ actions =>
      actionEventMap.put(frame,actions.filterNot(t => t.tankId == tankId && t.serialNum == serialNum))
    }
  }
}