package com.neo.sk.tank.front.tankClient

import com.neo.sk.tank.shared.ptcl.model
import com.neo.sk.tank.shared.ptcl.model.{CanvasBoundary, Point, TankParameters}
import com.neo.sk.tank.shared.ptcl.tank.{Bullet, Tank, TankState}
import org.scalajs.dom
import org.scalajs.dom.ext.Color
import org.scalajs.dom.raw.HTMLElement

import scala.util.Random

/**
  * Created by hongruying on 2018/7/10
  * * 道具
  * * 1.增加生命最大上限
  * * 2.移动速度增加
  * * 3.炮弹威力增加
  * * 4.医疗包

  */
class TankClientImpl(
                      override protected val userId:Long,
                      override val tankId: Int,
                      override protected var blood: Int,
                      override protected var bloodLevel: Byte,
                      override protected var bulletPowerLevel: Byte,
                      override protected var curBulletNum: Int,
                      override protected var direction: Float,
                      override protected var gunDirection: Float,
                      override var position: model.Point,
                      override protected var speedLevel: Byte,
                      override protected val tankColorType: Byte,
                      override val name: String,
                      override var killTankNum: Int,
                      override var damageTank: Int,
                      override var invincible:Boolean,
                      override protected var bulletStrengthen: Int
                    ) extends Tank{

  def this(tankState:TankState) = {
    this(tankState.userId,tankState.tankId,tankState.blood,tankState.bloodLevel,tankState.bulletPowerLevel,tankState.curBulletNum,tankState.direction,tankState.gunDirection,tankState.position,tankState.speedLevel,tankState.tankColorType,tankState.name,tankState.killTankNum,tankState.damageTank,tankState.invincible,tankState.bulletStrengthen)
  }

  override protected def startFillBullet(): Unit = {}

  def getColor():String = model.TankParameters.TankColor.tankColorList(tankColorType)

  /**
    * tank知道当前systemFrame的初始位置（position），如果isMove，根据(curFrame/maxClientFrame)计算当前动画桢的位置
    * @param curFrame 当前动画帧的帧数
    * @param maxClientFrame 一个systemFrame下动画帧的渲染最大帧数
    * @param directionOpt 坦克是否移动
    * */
  def getPositionCurFrame(curFrame:Int,maxClientFrame:Int,directionOpt:Option[Double],canMove:Boolean):Point = {
   if(directionOpt.nonEmpty && canMove){
      val distance = model.TankParameters.SpeedType.getMoveByFrame(speedLevel) / maxClientFrame * curFrame //每帧移动的距离
      this.position + distance.rotate(directionOpt.get.toFloat)
    }else position
  }

  def getPositionByOffsetTime(offSetTime:Long,directionOpt:Option[Double],canMove:Boolean):Point = {
    if(directionOpt.nonEmpty && canMove){
      val distance = model.TankParameters.SpeedType.getMoveByMs(speedLevel) * offSetTime //移动的距离
      this.position + distance.rotate(directionOpt.get.toFloat)
    }else position
  }

  //4个点
  def getGunPosition(gunH:Float):List[Point] = {
    List(
      Point(0,- gunH / 2).rotate(this.gunDirection),
      Point(0, gunH / 2).rotate(this.gunDirection),
      Point(model.TankParameters.TankSize.gunLen,gunH / 2).rotate(this.gunDirection),
      Point(model.TankParameters.TankSize.gunLen,- gunH / 2).rotate(this.gunDirection)

  def getGunPosition4San():List[Point] = {
    List(
      Point(0,- model.TankParameters.TankSize.gunH / 2).rotate(this.gunDirection),
      Point(0, model.TankParameters.TankSize.gunH / 2).rotate(this.gunDirection),
      Point(model.TankParameters.TankSize.gunLen,model.TankParameters.TankSize.gunH ).rotate(this.gunDirection),
      Point(model.TankParameters.TankSize.gunLen,- model.TankParameters.TankSize.gunH ).rotate(this.gunDirection)
    )
  }

  //三个点
  def getSliderPosition(offset:Int,bloodPercent:Float):List[Point] = {
    List(
       Point(- 0.8f * TankParameters.TankSize.r,- (offset + TankParameters.TankSize.r)),
       Point(- (1 - bloodPercent * 2) * 0.8f * TankParameters.TankSize.r,- (offset + TankParameters.TankSize.r)),
       Point(0.8f * TankParameters.TankSize.r,- offset - TankParameters.TankSize.r)
    )
  }



}

object TankClientImpl{

  /**
    * 在画布上绘制坦克图像
    * @param ctx canvas绘制
    * @param tank tank代码
    * @param curFrame 动画渲染桢数 （0，1，2，3）
    * @param maxClientFrame 每个systemFrame 动画渲染的帧数
    * */
  def drawTank(ctx:dom.CanvasRenderingContext2D,tank: TankClientImpl,curFrame:Int,maxClientFrame:Int,offset:Point,directionOpt:Option[Double],canMove:Boolean,canvasUnit:Int = 10): Unit ={
    val position = tank.getPositionCurFrame(curFrame,maxClientFrame,directionOpt,canMove)
    val gunH = tank.bulletPowerLevel match{
        case TankParameters.TankBulletBulletPowerLevel.first => 1f * TankParameters.TankSize.gunH
        case TankParameters.TankBulletBulletPowerLevel.second => 1.4f * TankParameters.TankSize.gunH
        case TankParameters.TankBulletBulletPowerLevel.third => 1.8f * TankParameters.TankSize.gunH
    }
    val gunPositionList = tank.getGunPosition(gunH:Float).map(_ + position).map(t => (t + offset) * canvasUnit)
//    println(s"curFrame=${curFrame} tankId=${tank.tankId},position = ${position}")
    val gunPositionList = if(tank.getTankState().bulletStrengthen <=0) {
      tank.getGunPosition().map(_ + position).map(t => (t + offset) * canvasUnit)
    }else{
      tank.getGunPosition4San().map(_ + position).map(t => (t + offset) * canvasUnit)
    }
    val bloodSliderList = tank.getSliderPosition(3,1.0f * tank.blood / TankParameters.TankBloodLevel.getTankBlood(tank.bloodLevel)).map(_ + position).map(t => (t + offset) * canvasUnit)
    ctx.beginPath()
    ctx.moveTo(gunPositionList.last.x,gunPositionList.last.y)
    gunPositionList.foreach(t => ctx.lineTo(t.x,t.y))
    ctx.fillStyle = tank.bulletPowerLevel match{
      case TankParameters.TankBulletBulletPowerLevel.first =>
        "#CD6600"
      case TankParameters.TankBulletBulletPowerLevel.second =>
        "#FF4500"
      case TankParameters.TankBulletBulletPowerLevel.third =>
        "#8B2323"
    }
    ctx.strokeStyle = "#383838"
    ctx.fill()
    ctx.lineWidth = 4
    ctx.stroke()
    ctx.closePath()
    val angel = tank.bloodLevel match {
      case TankParameters.TankBloodLevel.first => 2 * math.Pi / 3 * 1
      case TankParameters.TankBloodLevel.second => 2 * math.Pi / 3 * 2
      case TankParameters.TankBloodLevel.third => 2 * math.Pi / 3 * 3
    }
    if(tank.invincible == true) {

      ctx.beginPath()
      ctx.fillStyle = "rgba(128, 100, 162, 0.2)"
      ctx.arc((position.x + offset.x) * canvasUnit, (position.y + offset.y) * canvasUnit, model.TankParameters.invincibleSize.r * canvasUnit, 0, 2 * math.Pi)
      ctx.fill()
      ctx.closePath()
    }
    ctx.beginPath()
    ctx.lineWidth = 4
    ctx.strokeStyle = "black"
    ctx.arc((position.x + offset.x) * canvasUnit, (position.y + offset.y) * canvasUnit, model.TankParameters.TankSize.r * canvasUnit, 0, angel)
    ctx.fillStyle = tank.getColor()
    ctx.fill()
    ctx.stroke()
    ctx.closePath()
    ctx.beginPath()
    ctx.lineWidth = 4
    ctx.strokeStyle = "#8B008B"
    ctx.arc((position.x + offset.x) * canvasUnit, (position.y + offset.y) * canvasUnit, model.TankParameters.TankSize.r * canvasUnit, angel, 2 * math.Pi)
    ctx.stroke()
    ctx.fillStyle = tank.getColor()
    ctx.fill()
    ctx.closePath()

    for(i <- 0 to bloodSliderList.length - 2){
      ctx.beginPath()
      ctx.lineWidth = 5
      if(i == 1){
        ctx.strokeStyle = "#8B8682"
      }else{
        ctx.strokeStyle = "#DC143C"
      }
      ctx.moveTo(bloodSliderList(i).x,bloodSliderList(i).y)
      ctx.lineTo(bloodSliderList(i + 1).x,bloodSliderList(i + 1).y)
      ctx.stroke()
      ctx.closePath()
    }
    ctx.beginPath()
    val namePosition = (position + Point(0,5) + offset) * canvasUnit
    ctx.fillStyle = "#006699"
    ctx.textAlign = "center"
    ctx.font = "normal normal 20px 楷体"
    ctx.lineWidth = 2
    ctx.fillText(s"${tank.name}",namePosition.x,namePosition.y,20 * canvasUnit)
    ctx.closePath()


  }

  def drawTankByOffsetTime(ctx:dom.CanvasRenderingContext2D,tank: TankClientImpl,offsetTime:Long,offset:Point,directionOpt:Option[Double],canMove:Boolean,canvasUnit:Int = 10): Unit ={
    val position = tank.getPositionByOffsetTime(offsetTime,directionOpt,canMove)
    //    println(s"curFrame=${curFrame} tankId=${tank.tankId},position = ${position}")
    val gunH = tank.bulletPowerLevel match{
      case TankParameters.TankBulletBulletPowerLevel.first => 1f* TankParameters.TankSize.gunH
      case TankParameters.TankBulletBulletPowerLevel.second => 1.4f* TankParameters.TankSize.gunH
      case TankParameters.TankBulletBulletPowerLevel.third => 1.8f* TankParameters.TankSize.gunH
    }
    val gunPositionList = tank.getGunPosition(gunH:Float).map(_ + position).map(t => (t + offset) * canvasUnit)
    val bloodSliderPosition = tank.generateSliderPosition(3).map(_ + position).map(t => (t + offset) * canvasUnit)
    val gunPositionList = if(tank.getTankState().bulletStrengthen <=0) {
      tank.getGunPosition().map(_ + position).map(t => (t + offset) * canvasUnit)
    }else{
      tank.getGunPosition4San().map(_ + position).map(t => (t + offset) * canvasUnit)
    }
    val bloodSliderList = tank.getSliderPosition(3,1.0f * tank.blood / TankParameters.TankBloodLevel.getTankBlood(tank.bloodLevel)).map(_ + position).map(t => (t + offset) * canvasUnit)
    //------------------------绘制炮筒--------------------------#
    ctx.beginPath()
    ctx.moveTo(gunPositionList.last.x,gunPositionList.last.y)
    gunPositionList.foreach(t => ctx.lineTo(t.x,t.y))
    ctx.fillStyle = tank.bulletPowerLevel match{
      case TankParameters.TankBulletBulletPowerLevel.first =>
        "#CD6600"
      case TankParameters.TankBulletBulletPowerLevel.second =>
        "#FF4500"
      case TankParameters.TankBulletBulletPowerLevel.third =>
        "#8B2323"
    }
    ctx.strokeStyle = "#383838"
    ctx.fill()
    ctx.lineWidth = 4
    ctx.stroke()
    ctx.closePath()
    //----------------------------绘制坦克---------------------#
    val angel = tank.bloodLevel match {
      case TankParameters.TankBloodLevel.first => 2 * math.Pi / 3 * 1
      case TankParameters.TankBloodLevel.second => 2 * math.Pi / 3 * 2
      case TankParameters.TankBloodLevel.third => 2 * math.Pi / 3 * 3
    }
    if(tank.invincible) {
      ctx.beginPath()
      ctx.fillStyle = "rgba(128, 100, 162, 0.2)"
      ctx.arc((position.x + offset.x) * canvasUnit, (position.y + offset.y) * canvasUnit, model.TankParameters.invincibleSize.r * canvasUnit, 0, 360)
      ctx.fill()
      ctx.closePath()
    }
    ctx.beginPath()
    ctx.lineWidth = 4
    ctx.strokeStyle = "black"
    ctx.arc((position.x + offset.x) * canvasUnit, (position.y + offset.y) * canvasUnit, model.TankParameters.TankSize.r * canvasUnit, 0, angel)
    ctx.fillStyle = tank.getColor()
    ctx.strokeStyle = "#383838"
    ctx.fill()
    ctx.stroke()
    ctx.closePath()
    ctx.beginPath()
    ctx.lineWidth = 4
    ctx.strokeStyle = "#8B008B"
    ctx.arc((position.x + offset.x) * canvasUnit, (position.y + offset.y) * canvasUnit, model.TankParameters.TankSize.r * canvasUnit, angel, 2 * math.Pi)
    ctx.stroke()
    ctx.fillStyle = tank.getColor()
    ctx.fill()
    ctx.closePath()

    for(i <- 0 to bloodSliderList.length - 2){
      ctx.beginPath()
      ctx.lineWidth = 5
      if(i == 1){
        ctx.strokeStyle = "#8B8682"
      }else{
        ctx.strokeStyle = "#DC143C"
      }
      ctx.moveTo(bloodSliderList(i).x,bloodSliderList(i).y)
      ctx.lineTo(bloodSliderList(i + 1).x,bloodSliderList(i + 1).y)
      ctx.stroke()
      ctx.closePath()
    }
    ctx.beginPath()
    val namePosition = (position + Point(0,5) + offset) * canvasUnit
    ctx.fillStyle = "#006699"
    ctx.textAlign = "center"
    ctx.font = "normal normal 20px 楷体"
    ctx.lineWidth = 2
    ctx.fillText(s"${tank.name}",namePosition.x,namePosition.y,20 * canvasUnit)
    ctx.closePath()


    var left = TankParameters.tankBulletMaxCapacity * model.smallBullet.width / 2 * -1

    (1 to tank.curBulletNum).foreach{ indedx =>

      ctx.beginPath()
      val smallBulletPosition = (position + Point(left, -9) + offset)
      val img = dom.document.createElement("img")
      val image =  img.setAttribute("src", "/tank/static/img/子弹初始重构.png")
//        case model.bulletType.vanish => img.setAttribute("src","/tank/static/img/子弹消失.png")

      ctx.drawImage(img.asInstanceOf[HTMLElement], (smallBulletPosition.x - model.smallBullet.width / 2) * canvasUnit,
        (smallBulletPosition.y - model.smallBullet.height / 2) * canvasUnit,
        model.smallBullet.width * canvasUnit, model.smallBullet.height * canvasUnit)
      ctx.fill()
      ctx.stroke()
      ctx.closePath()
      left =left +  model.smallBullet.width

    }
    (tank.curBulletNum + 1 to TankParameters.tankBulletMaxCapacity).foreach{ indedx =>

      ctx.beginPath()
      val smallBulletPosition = (position + Point(left, -9) + offset)
      val img = dom.document.createElement("img")
      val image =  img.setAttribute("src", "/tank/static/img/子弹消失重构.png")
      //        case model.bulletType.vanish => img.setAttribute("src","/tank/static/img/子弹消失.png")

      ctx.drawImage(img.asInstanceOf[HTMLElement], (smallBulletPosition.x - model.smallBullet.width / 2) * canvasUnit,
        (smallBulletPosition.y - model.smallBullet.height / 2) * canvasUnit,
        model.smallBullet.width * canvasUnit, model.smallBullet.height * canvasUnit)
      ctx.fill()
      ctx.stroke()
      ctx.closePath()
      left =left +  model.smallBullet.width

    }




  }

  def drawTankInfo(ctx:dom.CanvasRenderingContext2D,myName:String,tank: TankClientImpl,canvasBoundary:model.Point,canvasUnit:Int = 10) = {
    val basePoint = Point(13 * canvasUnit,canvasBoundary.y * canvasUnit / 1.2f)
    val length = 20 * canvasUnit
    val bloodList = List(
      basePoint,
      basePoint + Point(length * 1.0f * tank.bloodLevel / TankParameters.TankBloodLevel.third,0),
      basePoint + Point(length * 1.0f,0)
    )
    val speedList = List(
      basePoint + Point(0,2.5f * canvasUnit),
      basePoint + Point(length * 1.0f * tank.speedLevel / TankParameters.SpeedType.high,2.5f * canvasUnit),
      basePoint + Point(length * 1.0f,2.5f * canvasUnit)
    )
    val bulletPowerList = List(
      basePoint + Point(0,5.5f * canvasUnit),
      basePoint + Point(length * 1.0f * tank.bulletPowerLevel / TankParameters.TankBloodLevel.third,5.5f * canvasUnit),
      basePoint + Point(length * 1.0f,5.5f * canvasUnit)
    )
    drawLine(ctx,bloodList)
    drawLine(ctx,speedList)
    drawLine(ctx,bulletPowerList)
    val breakPointList:List[Point] = breakPointPosition(basePoint,canvasUnit,length,1.0f / 45)
    for(i <- Range(0,breakPointList.length - 1,2)){
      ctx.beginPath()
      ctx.strokeStyle = "#8B8682"
      ctx.lineWidth = 8
      ctx.moveTo(breakPointList(i).x,breakPointList(i).y)
      ctx.lineTo(breakPointList(i + 1).x,breakPointList(i + 1).y)
      ctx.stroke()
      ctx.closePath()
    }
    ctx.beginPath()
    ctx.font = "normal normal 20px 楷体"
    ctx.fillStyle = Color.Black.toString()
    ctx.lineWidth = 1
    ctx.fillText(s"${myName}",15 * canvasUnit,canvasBoundary.y * canvasUnit / 1.2 - 4 * canvasUnit, 40 * canvasUnit)
    ctx.fillText(s"血量等级",5 * canvasUnit,canvasBoundary.y * canvasUnit / 1.2 - canvasUnit,20 * canvasUnit)
    ctx.fillText(s"速度等级",5 * canvasUnit,canvasBoundary.y * canvasUnit / 1.2 + 1.5 * canvasUnit,20 * canvasUnit)
    ctx.fillText(s"炮弹等级",5 * canvasUnit,canvasBoundary.y * canvasUnit / 1.2 + 4.5 * canvasUnit,20 * canvasUnit)
    ctx.closePath()

  }
  def drawLine(ctx:dom.CanvasRenderingContext2D,ls:List[Point]) = {
    for(i <- 0 to ls.length - 2){
      ctx.beginPath()
      ctx.lineWidth = 8
      if(i == 1){
        ctx.strokeStyle = "#8B8682"
      }else{
        ctx.strokeStyle = "#DC143C"
      }
      ctx.moveTo(ls(i).x,ls(i).y)
      ctx.lineTo(ls(i + 1).x,ls(i + 1).y)
      ctx.stroke()
      ctx.closePath()
    }
  }

  def breakPointPosition(basePoint:Point,canvasUnit:Int,length:Int,ratio:Float) = {
    List(
      basePoint + Point(length * 1.0f / 3 - length * ratio,0),
      basePoint + Point(length * 1.0f / 3 + length * ratio,0),
      basePoint + Point(length * 2.0f / 3 - length * ratio,0),
      basePoint + Point(length * 2.0f / 3 + length * ratio,0),
      basePoint + Point(length * 1.0f / 3 - length * ratio,2.5f  * canvasUnit),
      basePoint + Point(length * 1.0f / 3 + length * ratio,2.5f  * canvasUnit),
      basePoint + Point(length * 2.0f / 3 - length * ratio,2.5f * canvasUnit),
      basePoint + Point(length * 2.0f / 3 + length * ratio,2.5f  * canvasUnit),
      basePoint + Point(length * 1.0f / 3 - length * ratio,5.5f * canvasUnit),
      basePoint + Point(length * 1.0f / 3 + length * ratio,5.5f * canvasUnit),
      basePoint + Point(length * 2.0f / 3 - length * ratio,5.5f * canvasUnit),
      basePoint + Point(length * 2.0f / 3 + length * ratio,5.5f * canvasUnit)
    )
  }

  def getBulletPowerLevel(tank:TankClientImpl) = {
    tank.bulletPowerLevel
  }


}
