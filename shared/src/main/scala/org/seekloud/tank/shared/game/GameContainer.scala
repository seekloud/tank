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
import org.seekloud.tank.shared.model.Constants.ObstacleType
import org.seekloud.tank.shared.model.{Point, Rectangle, Score}
import org.seekloud.tank.shared.protocol.TankGameEvent
import org.seekloud.tank.shared.protocol.TankGameEvent._
import org.seekloud.tank.shared.util.QuadTree

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

trait GameContainer extends KillInformation{

  import scala.language.implicitConversions

  def debug(msg: String): Unit

  def info(msg: String): Unit

  implicit val config:TankGameConfig

  val boundary : Point = config.boundary

  var currentRank = List.empty[Score]

  var historyRankMap = Map.empty[Int,Score]
  var historyRank = historyRankMap.values.toList.sortBy(_.d).reverse
  var historyRankThreshold =if (historyRank.isEmpty)-1 else historyRank.map(_.d).min
  val historyRankLength = 5
  val tankLivesMap:mutable.HashMap[Int,TankState] = mutable.HashMap[Int,TankState]() // tankId -> lives
//  val tankEatPropMap = mutable.HashMap[Int,mutable.HashSet[Prop]]()//tankId -> Set(propId)

  val maxFollowFrame=math.max(math.max(config.shotgunDuration,config.initInvincibleFrame),config.fillBulletFrame)

//  var tankId = -1
  var systemFrame:Long = 0L //系统帧数

  val tankMap = mutable.HashMap[Int,Tank]() //tankId -> Tank
  val bulletMap = mutable.HashMap[Int,Bullet]() //bulletId -> Bullet
  val obstacleMap = mutable.HashMap[Int,Obstacle]() //obstacleId -> Obstacle  可打击的砖头
  val environmentMap = mutable.HashMap[Int,Obstacle]() //obstacleId -> steel and river  不可打击
  val propMap = mutable.HashMap[Int,Prop]() //propId -> prop 道具信息

  val tankMoveState = mutable.HashMap[Int,Byte]()

  val quadTree : QuadTree = new QuadTree(Rectangle(Point(0,0),boundary))

  protected val tankHistoryMap = mutable.HashMap[Int,String]()
  protected val removeTankHistoryMap=mutable.HashMap[Long,List[Int]]()

  protected val gameEventMap = mutable.HashMap[Long,List[GameEvent]]() //frame -> List[GameEvent] 待处理的事件 frame >= curFrame
  protected val actionEventMap = mutable.HashMap[Long,List[UserActionEvent]]() //frame -> List[UserActionEvent]
  protected val followEventMap = mutable.HashMap[Long,List[FollowEvent]]()  // 记录游戏逻辑中产生事件
  final protected def handleUserJoinRoomEvent(l:List[UserJoinRoom]) :Unit = {
    l foreach handleUserJoinRoomEvent
  }

  protected def handleRemoveHistoryMapNow():Unit={
    removeTankHistoryMap.get(systemFrame) match {
      case Some(l)=>
        l.foreach(t=>tankHistoryMap.remove(t))
        removeTankHistoryMap.remove(systemFrame)
      case None=>
    }
  }


  protected def handleUserJoinRoomEvent(e:UserJoinRoom) :Unit = {
//    println(s"-------------------处理用户加入房间事件")
    val tank : Tank = e.tankState
    tankMap.put(e.tankState.tankId,tank)
    tankHistoryMap.put(e.tankState.tankId,e.tankState.name)
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

  final protected def handleUserReliveEvent(l:List[UserRelive]):Unit = {
    l foreach handleUserReliveEvent
  }

  protected def handleUserReliveEvent(e:UserRelive):Unit = {
    val t = e.tankState
    if(!tankMap.exists(_._1 == t.tankId)){
      println(s"------------${e.tankState}")
      tankMap.put(t.tankId,t)
      quadTree.insert(t)
    }
  }

  protected def handleUserReliveNow() = {
    gameEventMap.get(systemFrame).foreach{events =>
      handleUserReliveEvent(events.filter(_.isInstanceOf[UserRelive]).map(_.asInstanceOf[UserRelive]).reverse)
    }
  }

  protected final def handleUserLeftRoom(e:UserLeftRoom) :Unit = {
    tankMoveState.remove(e.tankId)
    tankMap.get(e.tankId).foreach(quadTree.remove)
    tankMap.remove(e.tankId)
    removeTankHistoryMap.get(systemFrame+1000) match {
      case Some(l)=>removeTankHistoryMap.put(systemFrame+1000,e.tankId::l)
      case None=>removeTankHistoryMap.put(systemFrame+1000,List(e.tankId))
    }
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
//      val tankMoveSet = tankMoveAction.getOrElse(action.tankId,mutable.HashSet[Byte]())

//      var tankMove = tankMoveState.getOrElse(action.tankId,8)

      tankMap.get(action.tankId) match {
        case Some(tank) =>
          action match {
            case a:UserMouseMove => tank.setTankGunDirection(a.d)
            case a:UserMouseMoveByte => tank.setTankGunDirection(a.d)
            case a:UserMouseClick => {
              //remind 调整鼠标方向
              tank.setTankGunDirection(a.d)
              tankExecuteLaunchBulletAction(a.tankId,tank)
            }

            case a:UserMoveState =>
//              tankMove = a.moveState
              tankMoveState.put(a.tankId,a.moveState)
              tank.setTankDirection(a.moveState)

            case a:UserKeyboardMove => tank.setTankKeyBoardDirection(a.angle)
            case a:UserPressKeyMedical => tank.addBlood()
          }
        case None => info(s"tankId=${action.tankId} action=${action} is no valid,because the tank is not exist")
      }
    }
  }

  final protected def handleUserActionEventNow() = {
    actionEventMap.get(systemFrame).foreach{ actionEvents =>
      handleUserActionEvent(actionEvents.reverse)
    }
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
        tankMoveState.remove(e.tankId)
        addKillInfo(tankHistoryMap.getOrElse(e.bulletTankId,"未知"),tank.name)
        dropTankCallback(e.bulletTankId,tankHistoryMap.getOrElse(e.bulletTankId,"未知"),tank)
      }
    }
  }

  protected def dropTankCallback(bulletTankId:Int, bulletTankName:String,tank:Tank):Unit = {}


  protected final def handleTankAttacked(es:List[TankAttacked]) :Unit = {
    es foreach handleTankAttacked
  }

  final protected def handleTankAttackedNow() = {
    followEventMap.get(systemFrame).foreach{ events =>
      handleTankAttacked(events.filter(_.isInstanceOf[TankAttacked]).map(_.asInstanceOf[TankAttacked]).reverse)
    }
  }

  protected def handleObstacleAttacked(e:ObstacleAttacked) :Unit = {
    bulletMap.get(e.bulletId).foreach(quadTree.remove)
    bulletMap.remove(e.bulletId)
    obstacleMap.get(e.obstacleId).foreach{ obstacle =>
      if(obstacle.isLived()){
        obstacle.attackDamage(e.damage)
      }
    }
  }



  protected final def handleObstacleAttacked(es:List[ObstacleAttacked]) :Unit = {
    es foreach handleObstacleAttacked
  }

  final protected def handleObstacleAttackedNow() = {
    followEventMap.get(systemFrame).foreach{ events =>
      handleObstacleAttacked(events.filter(_.isInstanceOf[ObstacleAttacked]).map(_.asInstanceOf[ObstacleAttacked]).reverse)
    }
  }

  protected def handleTankEatProp(e:TankEatProp) :Unit = {
    propMap.get(e.propId).foreach{ prop =>
      quadTree.remove(prop)
      tankMap.get(e.tankId).foreach(_.eatProp(prop))
      propMap.remove(e.propId)
//      if(prop.propType == 4){
//        quadTree.remove(prop)
//        propMap.remove(e.propId)
//      } else{
//        quadTree.remove(prop)
//        tankMap.get(e.tankId).foreach(_.eatProp(prop))
//        propMap.remove(e.propId)
//      }
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
    //客户端和服务端重写
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

  protected def handleObstacleRemove(e:ObstacleRemove) :Unit = {
    obstacleMap.get(e.obstacleId).foreach { obstacle =>
      quadTree.remove(obstacle)
      obstacleMap.remove(e.obstacleId)
    }
  }

  protected final def handleObstacleRemove(es:List[ObstacleRemove]) :Unit = {
    es foreach handleObstacleRemove
  }

  protected def handleObstacleRemoveNow()={
    gameEventMap.get(systemFrame).foreach{events=>
      handleObstacleRemove(events.filter(_.isInstanceOf[ObstacleRemove]).map(_.asInstanceOf[ObstacleRemove]).reverse)
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
    followEventMap.get(systemFrame).foreach{ events =>
      handleTankFillBullet(events.filter(_.isInstanceOf[TankFillBullet]).map(_.asInstanceOf[TankFillBullet]).reverse)
    }
  }


  protected def handleTankInvincible(e:TankInvincible) :Unit = {
//    println(s"removeininEvent${e.tankId}")
    tankMap.get(e.tankId).foreach{ tank =>
      tank.clearInvincibleState()
    }
  }

  protected final def handleTankInvincible(es:List[TankInvincible]) :Unit = {
    es foreach handleTankInvincible
  }

  final protected def handleTankInvincibleNow() :Unit = {
//    println(s"---------------------------------------invicible")
    followEventMap.get(systemFrame).foreach{ events =>
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
    followEventMap.get(systemFrame).foreach{ events =>
      handleTankShotgunExpire(events.filter(_.isInstanceOf[TankShotgunExpire]).map(_.asInstanceOf[TankShotgunExpire]).reverse)
    }
  }



  protected def tankMove():Unit = {
    /**
      * 坦克移动过程中检测是否吃道具
      * */
    tankMap.toList.sortBy(_._1).map(_._2).foreach{ tank =>
      tank.move(boundary,quadTree,systemFrame)
      //tank 进行检测是否吃到道具
      val tankMaybeEatProps = quadTree.retrieveFilter(tank).filter(_.isInstanceOf[Prop]).map(_.asInstanceOf[Prop])
      tankMaybeEatProps.foreach(tank.checkEatProp(_,tankEatPropCallback(tank)))
    }
  }

  //后台需要重写，生成吃到道具事件，客户端不必重写
  protected def tankEatPropCallback(tank:Tank)(prop: Prop):Unit = {}

  protected def bulletMove():Unit = {
    bulletMap.toList.sortBy(_._1).map(_._2).foreach{ bullet =>
      val objects = quadTree.retrieveFilter(bullet)
      objects.filter(_.isInstanceOf[Tank]).map(_.asInstanceOf[Tank]).filter(_.tankId != bullet.tankId)
        .foreach{t =>
          bullet.checkAttackObject(t,attackTankCallBack(bullet))}
      objects.filter(t => t.isInstanceOf[ObstacleBullet] && t.isInstanceOf[Obstacle]).map(_.asInstanceOf[Obstacle])
        .foreach(t => bullet.checkAttackObject(t,attackObstacleCallBack(bullet)))
      bullet.move(boundary,systemFrame,removeBullet)

    }
  }


  protected def bulletFlyEndCallback(bullet: Bullet):Unit = {
    bulletMap.remove(bullet.bId)
    quadTree.remove(bullet)
  }

  //游戏后端需要重写，生成伤害事件
  protected def attackTankCallBack(bullet: Bullet)(tank:Tank):Unit = {
    removeBullet(bullet)
    val event = TankGameEvent.TankAttacked(tank.tankId,bullet.bId, bullet.tankId, bullet.damage,systemFrame)
    addFollowEvent(event)
  }


  //子弹攻击到障碍物的回调函数，游戏后端需要重写,生成伤害事件
  protected def attackObstacleCallBack(bullet: Bullet)(o:Obstacle):Unit = {
    removeBullet(bullet)
    val event = TankGameEvent.ObstacleAttacked(o.oId,bullet.bId,bullet.damage,systemFrame)
    addFollowEvent(event)
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

  protected final def addFollowEvent(event:FollowEvent):Unit = {
    followEventMap.get(event.frame) match {
      case Some(events) => followEventMap.put(event.frame, event :: events)
      case None => followEventMap.put(event.frame,List(event))
    }
  }

  final protected def fillBulletCallBack(tid:Int):Unit={
    addFollowEvent(TankGameEvent.TankFillBullet(tid,systemFrame+config.fillBulletFrame))
  }

  final protected def tankInvincibleCallBack(tid:Int):Unit={
    addFollowEvent(TankGameEvent.TankInvincible(tid,systemFrame+config.initInvincibleFrame))
  }

  final protected def tankShotgunExpireCallBack(tid:Int):Unit={
    //remind 删除之前的tank散弹失效事件
    followEventMap.foreach{r=>
      followEventMap.update(r._1,r._2.filterNot(e=>e.isInstanceOf[TankShotgunExpire]&&e.asInstanceOf[TankShotgunExpire].tankId==tid))
    }
    addFollowEvent(TankGameEvent.TankShotgunExpire(tid,systemFrame+config.shotgunDuration))
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
    handleTankAttackedNow()
    handleObstacleAttackedNow()

    handleTankFillBulletNow()
    handleTankInvincibleNow()
    handleTankShotgunExpireNow()

    handleTankEatPropNow()

    handlePropLifecycleNow()

    handleObstacleRemoveNow() //此处需要结合坦克攻击，在移动之后
    handleGenerateObstacleNow()
    handleGeneratePropNow()

    handleGenerateBulletNow()
    handleUserJoinRoomEventNow()
    handleUserReliveNow()

    quadTree.refresh(quadTree)
    updateKillInformation()

    handleRemoveHistoryMapNow()
    clearEventWhenUpdate()
  }

  implicit val scoreOrdering = new Ordering[Score] {
    override def compare(x: Score, y: Score): Int = {
      var r = y.k - x.k
      if (r == 0) {
        r = y.d - x.d
      }
      if (r == 0) {
        r = y.l - x.l
      }
      if (r == 0) {
        r = (x.id - y.id).toInt
      }
      r
    }
  }

  def updateRanks() = {
//    println(s"更新排行榜")
    currentRank = tankMap.values.map(s => Score(s.tankId, s.name, s.killTankNum, s.damageStatistics, s.lives)).toList.sorted
    var historyChange = false
    currentRank.foreach { cScore =>
      historyRankMap.get(cScore.id) match {
        case Some(oldScore) if cScore.d > oldScore.d || cScore.l < oldScore.l =>
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

  protected def clearEventWhenUpdate():Unit

  def getGameContainerAllState():GameContainerAllState = {
    GameContainerAllState(
      systemFrame,
      tankMap.values.map(_.getTankState()).toList,
      bulletMap.values.map(_.getBulletState()).toList,
      propMap.values.map(_.getPropState).toList,
      obstacleMap.values.map(_.getObstacleState()).toList,
      environmentMap.values.map(_.getObstacleState()).toList,
      tankMoveState.toList
    )
  }

  /**
    * @author sky
    * 重置followEventMap
    * 筛选回溯之前帧产生的事件,不包含本帧
    * */
  protected def reSetFollowEventMap(frame:Long)={
    followEventMap.foreach{l=>
      val eventList=l._2.filter(r=>
        (r.isInstanceOf[TankGameEvent.TankInvincible]&&(r.frame-config.initInvincibleFrame<frame))||(r.isInstanceOf[TankGameEvent.TankFillBullet]&&(r.frame-config.fillBulletFrame<frame))||
          (r.isInstanceOf[TankGameEvent.TankShotgunExpire]&&(r.frame-config.shotgunDuration<frame)))
      followEventMap.put(l._1,eventList)
    }
  }

  protected def addGameEvents(frame:Long,events:List[GameEvent],actionEvents:List[UserActionEvent]) = {
    gameEventMap.put(frame,events)
    actionEventMap.put(frame,actionEvents)
  }

  def removePreEvent(frame:Long, tankId:Int, serialNum:Byte):Unit = {
    actionEventMap.get(frame).foreach{ actions =>
      actionEventMap.put(frame,actions.filterNot(t => t.tankId == tankId && t.serialNum == serialNum))
    }
  }
}
