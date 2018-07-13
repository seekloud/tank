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

  case class Score(id:Long,n:String,k:Int,d:Int,t:Option[Long])

  case class Point(x: Double, y: Double){
    def +(other: Point) = Point(x + other.x, y + other.y)
    def -(other: Point) = Point(x - other.x, y - other.y)
    def %(other: Point) = Point(x % other.x, y % other.y)
    def <(other: Point) = x < other.x && y < other.y
    def >(other: Point) = x > other.x && y > other.y
    def /(value: Double) = Point(x / value, y / value)
    def *(value: Double) = Point(x * value, y * value)
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
    def rotate(theta: Double) = {
      val (cos, sin) = (Math.cos(theta), math.sin(theta))
      Point(cos * x - sin * y, sin * x + cos * y)
    }

    def getTheta(center:Point):Double = {
      math.atan2(y - center.y,x - center.x)
    }
  }

  case class Rectangle(topLeft: Point, downRight: Point){
    def intersects(r: Rectangle):Boolean = {
      val (rx,rw,ry,rh) = (r.topLeft.x,r.downRight.x,r.topLeft.y,r.downRight.y)
      val (tx,tw,ty,th) = (topLeft.x,downRight.x,topLeft.y,downRight.y)


      (rw < rx || rw > tx) &&
        (rh < ry || rh > ty) &&
        (tw < tx || tw > rx) &&
        (th < ty || th > ry)
    }
  }

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
    final val right = 0
    final val upRight = math.Pi / 4 * 7
    final val up = math.Pi / 2 * 3
    final val upLeft = math.Pi / 4 * 5
    final val left = math.Pi
    final val downLeft = math.Pi / 4 * 3
    final val down = math.Pi / 2
    final val downRight = math.Pi / 4
  }



  object Boundary{
    val w = 1200
    val h = 600

    def getBoundary:Point = Point(w,h)
  }

  object CanvasBoundary{
    val w = 120
    val h = 60

    def getBoundary:Point = Point(w,h)
  }

  object Frame{
    val millsAServerFrame = 60

    val clientFrameAServerFrame = 4


  }





  object BulletSize{
    val r = 1
  }



  object TankParameters{

    object TankColor{
      val blue = "#1E90FF"
      val green = "#4EEE94"
      val red = "#EE4000"

      val tankColorList = List(blue,green,red)

      val gun = "#7A7A7A"

      def getRandomColorType():Int = random.nextInt(tankColorList.size)
    }




    object SpeedType {
      val low = 1
      val intermediate = 2
      val high = 3


      def tankSpeedByType(t:Int):Point = { //每桢移动多少
        t match {
          case SpeedType.low => Point(10,0)
          case SpeedType.intermediate => Point(20,0)
          case SpeedType.high => Point(25,0)
        }
      }

      def getMoveByFrame(t:Int):Point = tankSpeedByType(t) * Frame.millsAServerFrame / 1000
    }

    final val baseSpeed = 5 //每秒移动距离

    object TankBloodLevel{
      val first = 1
      val second = 2
      val third = 3

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

    object TankBulletBulletPowerLevel{

      val first = 1
      val second = 2
      val third = 3



      def getBulletDamage(l:Int):Int = {
        l match {
          case 1 => 20
          case 2 => 30
          case 3 => 40

        }
      }
    }
  }

  object BulletParameters{

    final val maxFlyDistance = 70

    final val bulletMomentum = Point(20,0)
  }

  object ObstacleParameters{
    object ObstacleType{
      val airDropBox = 1
      val brick = 2
    }

    object AirDropBoxParameters{
      val blood = 100

      object Size {
        val w = 10
        val h = 10
      }
    }


    object BrickDropBoxParameters{
      val blood = 100

      object Size {
        val w = 10
        val h = 10
      }
    }



  }





}
