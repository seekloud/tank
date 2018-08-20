package com.neo.sk.tank.shared.ptcl

import java.awt.Rectangle
import java.awt.event.KeyEvent
import java.util.concurrent.atomic.AtomicLong


import scala.collection.mutable
import scala.util.Random

/**
  * Created by hongruying on 2018/7/7
  */
package object model {


  val random = new Random(System.currentTimeMillis())

  case class Score(id:Int,n:String,k:Int,d:Int,t:Option[Long] =None)

  case class Point(x: Float, y: Float){
    def +(other: Point) = Point(x + other.x, y + other.y)
    def -(other: Point) = Point(x - other.x, y - other.y)
    def %(other: Point) = Point(x % other.x, y % other.y)
    def <(other: Point) = x < other.x && y < other.y
    def >(other: Point) = x > other.x && y > other.y
    def /(value: Float) = Point(x / value, y / value)
    def *(value: Float) = Point(x * value, y * value)
    def *(other: Point) = x * other.x + y * other.y
    def length = Math.sqrt(lengthSquared)
    def lengthSquared = x * x + y * y
    def distance(other: Point) = Math.sqrt((x - other.x) * (x - other.x) + (y - other.y) * (y - other.y))
    def within(a: Point, b: Point, extra: Point = Point(0, 0)) = {
      import math.{min, max}
      x >= min(a.x, b.x) - extra.x &&
        x < max(a.x, b.x) + extra.y &&
        y >= min(a.y, b.y) - extra.x &&
        y < max(a.y, b.y) + extra.y
    }
    def rotate(theta: Float) = {
      val (cos, sin) = (Math.cos(theta), math.sin(theta))
      Point((cos * x - sin * y).toFloat, (sin * x + cos * y).toFloat)
    }

    def getTheta(center:Point):Double = {
      math.atan2(y - center.y,x - center.x)
    }


  }

  trait Shape{
    protected var position: Point

    def isIntersects(o:Shape):Boolean
  }

  case class Rectangle(topLeft: Point, downRight: Point) extends Shape {

    override protected var position: Point = (topLeft + downRight) / 2
    private val width:Float = math.abs(downRight.x - topLeft.x)
    private val height:Float = math.abs(downRight.y - topLeft.y)


    override def isIntersects(o: Shape): Boolean = {
      o match {
        case t:Rectangle =>
          intersects(t)

        case _ =>
          false
      }
    }

    def intersects(r: Rectangle):Boolean = {
      val (rx,rw,ry,rh) = (r.topLeft.x,r.downRight.x,r.topLeft.y,r.downRight.y)
      val (tx,tw,ty,th) = (topLeft.x,downRight.x,topLeft.y,downRight.y)


      (rw < rx || rw > tx) &&
        (rh < ry || rh > ty) &&
        (tw < tx || tw > rx) &&
        (th < ty || th > ry)
    }

    def intersects(r:Circle):Boolean ={
      if(r.center > topLeft && r.center < downRight){
        true
      }else{
        val relativeCircleCenter:Point = r.center - position
        val dx = math.min(relativeCircleCenter.x, width / 2)
        val dx1 = math.max(dx, - width / 2)
        val dy = math.min(relativeCircleCenter.y, height / 2)
        val dy1 = math.max(dy, - height / 2)
        Point(dx1,dy1).distance(relativeCircleCenter) < r.r
      }
    }
  }

  case class Circle(center:Point,r:Float) extends Shape{

    override protected var position: Point = center


    override def isIntersects(o: Shape): Boolean = {
      o match {
        case t:Rectangle => intersects(t)
        case t:Circle => intersects(t)
      }
    }

    def intersects(r: Rectangle):Boolean = {
      r.intersects(this)
    }

    def intersects(r: Circle):Boolean = {
      r.center.distance(this.center) <= (r.r + this.r)
    }
  }
//  def DistanceBetweenTwoPoints(x1:Double,y1:Double,x2:Double,y2:Double)={
//    Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1))
//  }
//  def DistanceFromPointToLine(x:Double,y:Double,x1:Double,y1:Double,x2:Double,y2:Double)={
//    val a = y2 - y1
//    val b= x1 - x2
//    val c= x2 * y1-x1 * y2
//    math.abs(a * x + b * y +c)/ math.sqrt(a * a + b * b)
//  }
//  def IsCircleIntersectRectangle(x:Double,y:Double,r:Double,x0:Double,y0:Double,x1:Double,y1:Double,x2:Double,y2:Double)={
//    val w1 = DistanceBetweenTwoPoints(x0, y0, x2, y2)
//    val h1 = DistanceBetweenTwoPoints(x0, y0, x1, y1)
//    val w2 = DistanceFromPointToLine(x, y, x0, y0, x1, y1)
//    val h2 = DistanceFromPointToLine(x, y, x0, y0, x2, y2)
//    if(w2 > w1 + r)
//      false
//    if(h2 > h1 + r)
//      false
//    if(w2 <= w1)
//      true
//    if(h2 <= h1)
//      true
//
//    (w2 - w1) * (w2 - w1) + (h2 - h1) * (h2 - h1) <= r * r
//  }

//  def ComputerCollision(w:Double,h:Double,r:Double,rx:Double,ry:Double)={
//    val dx = math.min(rx ,w * 0.5)
//    val dx1 =math.max(dx ,-w * 0.5)
//    val dy =math.min(ry,h * 0.5)
//    val dy1 =math.max(dy,-h * 0.5)
//
//    (dx1 - rx) * ( dx1 - rx) + (dy1 - ry) * (dy - ry) <= r * r
//}
//  def getNewRx_Ry(x1:Double,y1:Double,x2:Double,y2:Double)={
//    val newRotation =math.rot(x1,y1,x2,y2) - rotation
//  }




//  trait Bullet {
//    val startPosition:Point
//    val momentum: Point
//    val startTime:Long
//    val damage:Int
//    val maxDistance:Double = 20
//
//    var position:Point =startPosition
//
//    def isIntersectsTank(tank: Tank) = {
////      println(getBulletRect(),tank.getTankRect())
//      getBulletRect().intersects(tank.getTankRect())
//    }
//
//
//    def getBulletRect():Rectangle = {
//      val min = position - Point(5, 5)
//      val max = position + Point(5, 5)
//      Rectangle(min,max)
//    }
//
//
//    def isFlyEnd(boundary: Point):Boolean = {
//      position.distance(startPosition) > maxDistance || !position.within(Point(0,0),boundary*10)
//    }
//
//    def fly(boundary: Point):Unit = {
//      if(!isFlyEnd(boundary))
//        position += momentum
//    }
//
//
//  }
//
//
//
//  trait Tank{
//
//
//    var position: Point
//    var speed: Point
//    var direction:Double
//    var health : Int
//
//    private val maxBullet = 10
//    protected var bullet = maxBullet
//    protected val genBulletSecond = 1
//
//    protected var isGenBullet = false
//
//
//    def fire():Boolean = {
//      if(bullet > 0){
//        bullet = bullet -1
//        if(!isGenBullet){
//          isGenBullet = true
//          setGenBullet()
//        }
//        true
//      }else false
//    }
//
//    def isLived() : Boolean = health > 0
//
//    def attackedByBulletAndIsLived(bullet: Bullet):Boolean = {
//      health -= bullet.damage
//      isLived()
//    }
//
//    protected def genBullet():Unit = {
//      if(bullet < maxBullet) bullet += 1
//      if(bullet == maxBullet) isGenBullet = false
//      if(bullet < maxBullet) setGenBullet()
//    }
//
//    protected def setGenBullet() :Unit
//
//    def contain(other: Point):Boolean = {
//      val min = position - Point(TankSize.w / 2, TankSize.h / 2)
//      val max = position + Point(TankSize.w / 2, TankSize.h / 2)
//      other.within(min,max)
//    }
//
//
//    def getTankRect() = {
//      val min = position - Point(TankSize.w / 2, TankSize.h / 2)
//      val max = position + Point(TankSize.w / 2, TankSize.h / 2)
//      Rectangle(min,max)
//    }
//
//
//    def move(direction:Double,boundary: Point) = {
//      this.direction = direction
//      if((position + speed.rotate(direction)).within(Point(0,0),boundary* 10)){
//        position += speed.rotate(direction)
//      }
//    }
//
//  }
//
//
//  trait Grid{
//
//    val boundary: Point
//
//    def debug(msg: String): Unit
//
//    def info(msg: String): Unit
//
//    val bulletIdGenerator = new AtomicLong(1L)
//    val tankIdGenerator = new AtomicLong(100L)
//
//
//    var frameCount = 0L
//    val tankSize = 6L
//    val tankMap = mutable.HashMap[Long,Tank]()
//    val bulletMap = mutable.HashMap[Long,(Long,Bullet)]()
//    val actionMap = mutable.HashMap[Long,mutable.HashMap[Long,mutable.HashSet[Int]]]()
//
//    def addAction(id:Long,keyCode:Int) = {
//      addActionWithFrame(id,keyCode,frameCount)
//    }
//
//    def addActionWithFrame(id: Long, keyCode: Int, frame: Long) = {
//      val map = actionMap.getOrElse(frameCount,mutable.HashMap[Long,mutable.HashSet[Int]]())
//      val cur = map.getOrElse(id,mutable.HashSet.empty[Int])
//      cur.add(keyCode)
//      map.put(id,cur)
//      actionMap.put(frame,map)
//    }
//
//    def genBullet(tankId:Long,tank: Tank) : Unit
//
//    def updateTank():Unit = {
//      def isFire(tankId:Long,tank:Tank,actionSet:mutable.HashSet[Int]) = {
//        if(actionSet.contains(KeyEvent.VK_SPACE)){
//          genBullet(tankId,tank)
//        }
//      }
//
//      def getDirection(actionSet:mutable.HashSet[Int]):Option[Double] = {
//        if(actionSet.contains(KeyEvent.VK_LEFT) && actionSet.contains(KeyEvent.VK_UP)){
//          Some(DirectionType.upLeft)
//        }else if(actionSet.contains(KeyEvent.VK_RIGHT) && actionSet.contains(KeyEvent.VK_UP)){
//          Some(DirectionType.upRight)
//        }else if(actionSet.contains(KeyEvent.VK_LEFT) && actionSet.contains(KeyEvent.VK_DOWN)){
//          Some(DirectionType.downLeft)
//        }else if(actionSet.contains(KeyEvent.VK_RIGHT) && actionSet.contains(KeyEvent.VK_DOWN)){
//          Some(DirectionType.downRight)
//        }else if(actionSet.contains(KeyEvent.VK_RIGHT)){
//          Some(DirectionType.right)
//        }else if(actionSet.contains(KeyEvent.VK_LEFT)){
//          Some(DirectionType.left)
//        }else if(actionSet.contains(KeyEvent.VK_UP) ){
//          Some(DirectionType.up)
//        }else if(actionSet.contains(KeyEvent.VK_DOWN)){
//          Some(DirectionType.down)
//        }else None
//      }
//
//      val act = actionMap.getOrElse(frameCount,mutable.HashMap[Long,mutable.HashSet[Int]]())
//      tankMap.foreach{
//        case (id,tank) =>
//          if(!tank.isLived()) tankMap.remove(id)
//      }
//      tankMap.foreach{
//        case (id,tank) =>
//
//          val curActOpt = act.get(id)
//          curActOpt match {
//            case Some(actionSet) =>
//              isFire(id,tank,actionSet)
//              val directionOpt = getDirection(actionSet)
////              debug(s"id=${id} dirction = ${actionSet}")
//              directionOpt match {
//                case Some(d) => tank.move(d,boundary)
//                case None =>
//              }
//
//            case None => //debug(s"tank id=${id} has not action")
//          }
//      }
//    }
//
//
//    def updateBullet():Unit = {
//      bulletMap.foreach{ case (id,(tId,bullet)) =>
//        var isAttacked = false
//        tankMap.filterNot(_._1 == tId).foreach{ case (tankId,tank) =>
//          if(bullet.isIntersectsTank(tank) && !isAttacked){
//            isAttacked = true
//            tank.attackedByBulletAndIsLived(bullet)
//            debug(s"tankId=${tId} attack tankId=${tankId}, and health=${tank.health}")
//            bulletMap.remove(id)
//          }
//        }
//
//
//      }
//      bulletMap.foreach{ case (id,(_,bullet)) =>
//          if(bullet.isFlyEnd(boundary))
//            bulletMap.remove(id)
//      }
////      debug(s"--${bulletMap.map(_._2).map(_.position)}")
//      bulletMap.foreach(_._2._2.fly(boundary))
//    }
//
//    def update():Unit ={
////      debug(s"sssssssss=${actionMap.flatMap(_._2).map(_._2.toList)}")
//      updateBullet()
//      updateTank()
//      actionMap -= frameCount
//      frameCount += 1
//    }
//  }

  object DirectionType {
    final val right:Float = 0
    final val upRight = math.Pi / 4 * 7
    final val up = math.Pi / 2 * 3
    final val upLeft = math.Pi / 4 * 5
    final val left = math.Pi
    final val downLeft = math.Pi / 4 * 3
    final val down = math.Pi / 2
    final val downRight = math.Pi / 4
  }



  object Boundary{
    val w = 360
    val h = 180

    def getBoundary:Point = Point(w,h)
  }

  object LittleMap {
    val w = 25
    val h = 20
  }

  object CanvasBoundary{
    val w = 120
    val h = 60

    def getBoundary:Point = Point(w,h)
  }

  object Frame{
    val millsAServerFrame = 120

    val clientFrameAServerFrame = 1


  }





  object BulletSize{
    val r = 1
  }

//  object invincibleSize{
//    val r = 8
//  }



  object TankParameters{


    object invincibleSize{
      val r = 5.5
    }

    object TankColor{
      val blue = "#1E90FF"
      val green = "#4EEE94"
      val red = "#EE4000"

      val tankColorList = List(blue,green,red)

      val gun = "#7A7A7A"

      def getRandomColorType():Byte = random.nextInt(tankColorList.size).toByte
    }




    val addBlood = 40
    val bulletStrengthenTime = 60

    object SpeedType {
      val low:Byte = 1
      val intermediate:Byte = 2
      val high:Byte = 3


      def tankSpeedByType(t:Int):Point = { //每桢移动多少
        t match {
          case SpeedType.low => Point(20,0)
          case SpeedType.intermediate => Point(25,0)
          case SpeedType.high => Point(30,0)
        }
      }

      def getMoveByFrame(t:Int):Point = tankSpeedByType(t) * Frame.millsAServerFrame / 1000

      def getMoveByMs(t:Int):Point = tankSpeedByType(t) / 1000
    }

    final val baseSpeed = 5 //每秒移动距离

    object TankBloodLevel{
      val first:Byte = 1
      val second:Byte = 2
      val third:Byte = 3

      def getTankBlood(level:Int) :Int  = {
        level match {
          case TankBloodLevel.first => 120
          case TankBloodLevel.second => 200
          case TankBloodLevel.third => 300

        }

      }

    }


    object TankSize{
      val w = 6
      val h = 6
      val r = 3


      val gunLen = 5
      val gunH = 2
    }

    object GunSize{
      val w = 5
      val h = 5
    }

    final val tankBulletMaxCapacity = 4

    final val tankFillBulletSpeed = 1 // 1发/1s

    final val tankInvincibleTime = 5

    object TankBulletBulletPowerLevel{

      val first:Byte = 1
      val second:Byte = 2
      val third:Byte = 3



      def getBulletDamage(l:Int):Int = {
        l match {
          case 1 => 20
          case 2 => 30
          case 3 => 40

        }
      }

      def getBulletLevelByDamage(d:Int) ={
        d match {
          case 20 => 1
          case 30 => 2
          case 40 => 3
          case _ => 1
        }
      }
    }
  }

  object BulletParameters{

    final val maxFlyDistance = 70

    final val bulletMomentum = Point(33,0)
  }

  object ObstacleParameters{
    object ObstacleType{
      val airDropBox:Byte = 1
      val brick:Byte = 2
      val steel:Byte = 3
      val river:Byte = 4
    }

    object AirDropBoxParameters{
      val blood = 100
      val num = 5
    }

    object RiverParameters{
      val width = 9
      val height = 64
      val num = 5
    }
    object SteelParameters{
      val border = 6
      val num = 20
    }

    val border:Float = 5f
    val halfBorder:Float = 2.5f

    object BrickDropBoxParameters{
      val blood = 100
      val num = 30
    }
  }

  object PropParameters{
    val r = 2.5f
    val l = 5
    val half = 2.5f
  }

  object bulletType{

    val storage:Byte = 1
    val vanish:Byte = 2
  }

  object smallBullet{
    val num = 4
    val height = 5
    val width = 1



  }





}
