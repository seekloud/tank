package com.neo.sk.tank.front.tankClient

import com.neo.sk.tank.shared.ptcl
import com.neo.sk.tank.shared.ptcl.model
import com.neo.sk.tank.shared.ptcl.model.{CanvasBoundary, Point}
import com.neo.sk.tank.shared.ptcl.protocol.{WsFrontProtocol, WsProtocol}
import com.neo.sk.tank.shared.ptcl.protocol.WsFrontProtocol.TankAction
import com.neo.sk.tank.shared.ptcl.tank._
import org.scalajs.dom
import org.scalajs.dom.ext.Color

import scala.collection.mutable
/**
  * Created by hongruying on 2018/7/9
  */
class GridClient(override val boundary: model.Point,canvasUnit:Int) extends Grid {


  override def debug(msg: String): Unit = {}

  override def info(msg: String): Unit = println(msg)

  override def tankExecuteLaunchBulletAction(tankId: Long, tank: Tank): Unit = {}


  override protected def tankEatProp(tank:Tank)(prop: Prop):Unit = {}

  private var recvTankAttackedList:List[WsProtocol.TankAttacked] = Nil
  private var recvObstacleAttackedList:List[WsProtocol.ObstacleAttacked] = Nil
  private var recvTankEatPropList:List[WsProtocol.TankEatProp] = Nil




  def playerJoin(tank:TankState) = {
    tankMap.put(tank.tankId,new TankClientImpl(tank))
  }

  def gridSyncStateWithoutBullet(d:GridStateWithoutBullet) = {
//    super.update()
    updateBullet()

    if(tankMap.values.nonEmpty && d.tanks.nonEmpty){
//      println(tankMap.values.head.getTankState().position)
//      println(d.tanks.head.position)
    }
//    println(d.tanks.head.position)
//    println("--------------------")


    systemFrame = d.f
    tankMap.clear()
    obstacleMap.clear()
    propMap.clear()
    tankMoveAction.clear()
    d.tanks.foreach(t => tankMap.put(t.tankId,new TankClientImpl(t)))
    d.obstacle.foreach{o =>
      o.t match {
        case ptcl.model.ObstacleParameters.ObstacleType.airDropBox => obstacleMap.put(o.oId,new AirDropBoxClientImpl(o))
        case ptcl.model.ObstacleParameters.ObstacleType.brick => obstacleMap.put(o.oId,new BrickClientImpl(o))
        case _ =>
      }
    }
    d.props.foreach(t => propMap.put(t.pId,Prop(t)))
    d.tankMoveAction.foreach{t =>
      val set = tankMoveAction.getOrElse(t._1,mutable.HashSet[Int]())
      set.add(t._2)
      tankMoveAction.put(t._1,set)
    }
  }

  def gridSyncState(d:GridState) = {
    updateBullet()
    systemFrame = d.f
    tankMap.clear()
    obstacleMap.clear()
    propMap.clear()
    tankMoveAction.clear()
    bulletMap.clear()
    d.tanks.foreach(t => tankMap.put(t.tankId,new TankClientImpl(t)))
    d.obstacle.foreach{o =>
      o.t match {
        case ptcl.model.ObstacleParameters.ObstacleType.airDropBox => obstacleMap.put(o.oId,new AirDropBoxClientImpl(o))
        case ptcl.model.ObstacleParameters.ObstacleType.brick => obstacleMap.put(o.oId,new BrickClientImpl(o))
        case _ =>
      }
    }
    d.props.foreach(t => propMap.put(t.pId,Prop(t)))
    d.tankMoveAction.foreach{t =>
      val set = tankMoveAction.getOrElse(t._1,mutable.HashSet[Int]())
      set.add(t._2)
      tankMoveAction.put(t._1,set)
    }
    d.bullet.foreach(t => bulletMap.put(t.bId,new BulletClientImpl(t)))
  }

  def recvTankAttacked(t:WsProtocol.TankAttacked) = {
    if(t.frame < systemFrame){
      updateTankAttacked(t.bId,t.tId,t.d)
    }else{
      recvTankAttackedList = t :: recvTankAttackedList
    }
  }

  def updateTankAttacked(bId:Long,tId:Long,d:Int) = {
    bulletMap.remove(bId)
    tankMap.get(tId) match {
      case Some(t) =>
        t.attackedDamage(d)
        if(!t.isLived()){
          tankMap.remove(tId)
          tankMoveAction.remove(tId)
        }
      case None =>
    }
  }

  def recvObstacleAttacked(t:WsProtocol.ObstacleAttacked) = {
    if(t.frame < systemFrame){
      updateObstacleAttacked(t.bId,t.oId,t.d)
    }else{
      recvObstacleAttackedList = t :: recvObstacleAttackedList
    }
  }

  def updateObstacleAttacked(bId:Long,oId:Long,d:Int) = {
    bulletMap.remove(bId)
    obstacleMap.get(oId) match {
      case Some(t) =>
        t.attackDamage(d)
        if(!t.isLived()){
          obstacleMap.remove(oId)
        }
      case None =>
    }
  }

  def recvTankEatProp(t:WsProtocol.TankEatProp) = {
    if(t.frame < systemFrame){
      updateTankEatProp(t.tId,t.pId,t.pType)
    }else{
      recvTankEatPropList = t :: recvTankEatPropList
    }
  }

  def updateTankEatProp(tId:Long,pId:Long,pType:Int) = {
    tankMap.get(tId) match {
      case Some(t) =>
        t.eatProp(propMap.get(pId).get)
      case None =>
    }
    propMap.remove(pId)
  }

  def draw(ctx:dom.CanvasRenderingContext2D,myName:String,myTankId:Long,curFrame:Int,maxClientFrame:Int,canvasBoundary:Point) = {
    var moveSet:Set[Int] = tankMoveAction.getOrElse(myTankId,mutable.HashSet[Int]()).toSet
    val action = tankActionQueueMap.getOrElse(systemFrame,mutable.Queue[(Long,TankAction)]()).filter(_._1 == myTankId).toList
    action.map(_._2).foreach{
      case WsFrontProtocol.PressKeyDown(k) => moveSet = moveSet + k
      case WsFrontProtocol.PressKeyUp(k) => moveSet = moveSet - k
      case WsFrontProtocol.MouseMove(k) =>
      case _ =>
    }
    val directionOpt = getDirection(moveSet)
    val offset = tankMap.get(myTankId).map((canvasBoundary / 2) - _.asInstanceOf[TankClientImpl].getPositionCurFrame(curFrame,maxClientFrame,directionOpt)).getOrElse(Point(0,0))
//    println(s"curFrame=${curFrame},offset=${offset}")
    drawBackground(ctx,offset,canvasBoundary)
    bulletMap.values.foreach{ b =>
      BulletClientImpl.drawBullet(ctx,canvasUnit,b.asInstanceOf[BulletClientImpl],curFrame,offset)
    }
    tankMap.values.foreach{ t =>
      var moveSet:Set[Int] = tankMoveAction.getOrElse(t.tankId,mutable.HashSet[Int]()).toSet
      val action = tankActionQueueMap.getOrElse(systemFrame,mutable.Queue[(Long,TankAction)]()).filter(_._1 == t.tankId).toList
      action.map(_._2).foreach{
        case WsFrontProtocol.PressKeyDown(k) => moveSet = moveSet + k
        case WsFrontProtocol.PressKeyUp(k) => moveSet = moveSet - k
        case WsFrontProtocol.MouseMove(k) =>
        case _ =>
      }
      val directionOpt = getDirection(moveSet)
      TankClientImpl.drawTank(ctx,t.asInstanceOf[TankClientImpl],curFrame,maxClientFrame,offset,directionOpt,canvasUnit)
    }
    TankClientImpl.drawTankInfo(ctx,myName,tankMap(myTankId).asInstanceOf[TankClientImpl],canvasUnit)
  }

  def tankIsLived(tankId:Long):Boolean = tankMap.contains(tankId)

  def drawProps(ctx:dom.CanvasRenderingContext2D,offset:Point) = {
    propMap.values.foreach{
      p=>
        val color = p.getPropState.t match {
          case 1 => Color.Red
          case 2 => Color.Yellow
          case 3 => Color.Green
          case 4 => Color.Blue
        }
        ctx.fillStyle = color.toString()
        ctx.strokeStyle = color.toString()
        ctx.beginPath()
        ctx.arc((p.getPropState.p.x + offset.x) * canvasUnit,(p.getPropState.p.y + offset.y) * canvasUnit,model.PropParameters.r * canvasUnit,0,360)
        ctx.fill()
        ctx.stroke()
    }
  }

  private def drawBackground(ctx:dom.CanvasRenderingContext2D,offset:Point,canvasBoundary:Point) = {
    //#F0F0F0   #CCCCCC
//    ctx.fillStyle = "#CCCCCC"
//    ctx.rect( (-(canvasBoundary.x / 2) + offset.x) * canvasUnit, (-(canvasBoundary.x / 2) + offset.y) * canvasUnit, (boundary.x + canvasBoundary.x / 2 + offset.x) * canvasUnit, (boundary.y + canvasBoundary.y / 2 + offset.y) * canvasUnit)
//    ctx.fill()
    ctx.lineWidth = 0.7
    ctx.strokeStyle = "#A3A3A3"
    ctx.fillStyle = "#FCFCFC"
    ctx.fillRect(0, 0, canvasBoundary.x * canvasUnit, canvasBoundary.y  * canvasUnit)
    obstacleMap.values.foreach {
      o =>
        if (o.obstacleType == model.ObstacleParameters.ObstacleType.airDropBox) {
          AirDropBoxClientImpl.drawAirDrop(ctx, offset, canvasUnit, o)
        } else {
          BrickClientImpl.drawBrick(ctx,offset,canvasUnit,o)
        }
    }
    drawProps(ctx,offset)
    ctx.fillStyle = Color.Black.toString()
    for(i <- 0 to(boundary.x.toInt,3)){
      ctx.beginPath()
      ctx.strokeStyle = Color.Black.toString()
      ctx.moveTo((i + offset.x) * canvasUnit, (0 + offset.y) * canvasUnit)
      ctx.lineTo((i + offset.x) * canvasUnit, (boundary.y + offset.y) * canvasUnit)
      ctx.stroke()
      ctx.closePath()
    }

    for(i <- 0 to(boundary.y.toInt,3)){
      ctx.beginPath()
      ctx.strokeStyle = Color.Black.toString()
      ctx.moveTo((0 + offset.x) * canvasUnit, (i + offset.y) * canvasUnit)
      ctx.lineTo((boundary.x + offset.x) * canvasUnit,(i + offset.y)  * canvasUnit)
      ctx.stroke()
      ctx.closePath()
    }

    //绘制当前排行榜
    val textLineHeight = 18
    val leftBegin =7 * canvasUnit
    val rightBegin = (canvasBoundary.x.toInt-8) * canvasUnit
    object MyColors {
      val rankList = "#FFE384"
      val background = "#9933FA"
//      val stripe = "rgba(181, 181, 181, 0.5)"
//      val myHeader = "#cccccc"
//      val myBody = "#FFFFFF"
//      val otherHeader = "rgba(78,69,69,0.82)"
//      val otherBody = "#696969"
    }


    def drawTextLine(str: String, x: Int, lineNum: Int, lineBegin: Int = 0) = {
      ctx.fillText(str, x, (lineNum + lineBegin - 1) * textLineHeight)
    }
    ctx.font = "12px Helvetica"
    ctx.fillStyle =MyColors.rankList
    ctx.fillRect(0,0,150,200)

    val currentRankBaseLine = 1
    var index = 0
    ctx.fillStyle = MyColors.background
    drawTextLine(s" --- Current Rank --- ", leftBegin, index, currentRankBaseLine)
    currentRank.foreach{ score =>
      index += 1
      drawTextLine(s"[$index]: ${score.n.+("   ").take(3)} kill=${score.k} damage=${score.d}", leftBegin, index, currentRankBaseLine)
    }
    ctx.fillStyle =MyColors.rankList
    ctx.fillRect(1050,0,200,200)
    val historyRankBaseLine =1
    index = 0
    ctx.fillStyle = MyColors.background
    drawTextLine(s" --- History Rank --- ", rightBegin, index, historyRankBaseLine)
    historyRank.foreach { score =>
      index += 1
      drawTextLine(s"[$index]: ${score.n.+("   ").take(3)} kill=${score.k} damage=${score.d}", rightBegin, index, historyRankBaseLine)
    }


  }





  override def update(): Unit = {
    recvTankAttackedList.filter(_.frame == systemFrame).foreach{t =>
      updateTankAttacked(t.bId,t.tId,t.d)
    }
    recvTankAttackedList = recvTankAttackedList.filter(_.frame > systemFrame)
    recvObstacleAttackedList.filter(_.frame == systemFrame).foreach{t =>
      updateObstacleAttacked(t.bId,t.oId,t.d)
    }
    recvObstacleAttackedList = recvObstacleAttackedList.filter(_.frame > systemFrame)
    recvTankEatPropList.filter(_.frame == systemFrame).foreach{t =>
      updateTankEatProp(t.tId,t.pId,t.pType)
    }
    recvTankEatPropList = recvTankEatPropList.filter(_.frame > systemFrame)

    super.update()
  }




//  def drawBullet(ctx:dom.CanvasRenderingContext2D,bullet:BulletClientImpl,curFrame:Int) = {
//    val position = bullet.getPositionCurFrame(curFrame)
//
//  }

}
