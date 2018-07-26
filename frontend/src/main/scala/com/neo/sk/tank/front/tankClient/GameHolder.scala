package com.neo.sk.tank.front.tankClient

import com.neo.sk.tank.front.common.Constants
import com.neo.sk.tank.front.components.StartGameModal
import com.neo.sk.tank.front.utils.byteObject.MiddleBufferInJs
import com.neo.sk.tank.front.utils.{JsFunc, Shortcut}
import com.neo.sk.tank.shared.ptcl
import com.neo.sk.tank.shared.ptcl.model.{Boundary, ObstacleParameters, Point}
import com.neo.sk.tank.shared.ptcl.protocol._
import com.neo.sk.tank.shared.ptcl.tank.{GridState, GridStateWithoutBullet, Prop, TankState}
import mhtml.Var
import org.scalajs.dom
import org.scalajs.dom.Blob
import org.scalajs.dom.ext.{Color, KeyCode}
import org.scalajs.dom.html.{Canvas, Image}
import org.scalajs.dom.raw.{Event, FileReader, MessageEvent, MouseEvent}

import scala.collection.mutable
import scala.scalajs.js.typedarray.ArrayBuffer
import scala.xml.Elem
import org.scalajs.dom

/**
  * Created by hongruying on 2018/7/9
  */
class GameHolder(canvasName:String) {

  import io.circe._, io.circe.generic.auto.exportDecoder, io.circe.parser._, io.circe.syntax._


  private[this] val canvas = dom.document.getElementById(canvasName).asInstanceOf[Canvas]
  private[this] val ctx = canvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]

  private[this] val bounds = Point(ptcl.model.Boundary.w,ptcl.model.Boundary.h)

  private[this] val SmallMap = Point(ptcl.model.LittleMap.w,ptcl.model.LittleMap.h)

//  var lastHeader = Point(bounds.x / 2, bounds.y / 2)
  private[this] val canvasUnit = 10
  private[this] val canvasBoundary = ptcl.model.Point(dom.window.innerWidth.toFloat,dom.window.innerHeight.toFloat)

  private[this] val canvasBounds = canvasBoundary / canvasUnit

  private[this] var myId = -1L
  private[this] var myTankId:Int = -1
  private[this] var myName = ""

  private[this] val grid = new GridClient(bounds,canvasUnit,canvasBounds)

  private[this] var firstCome = true
  private[this] var justSyncedFromServer = false
  private[this] val websocketClient = new WebSocketClient(wsConnectSuccess,wsConnectError,wsMessageHandler,wsConnectClose)


  canvas.width = canvasBoundary.x.toInt
  canvas.height = canvasBoundary.y.toInt

  private val gameStateVar:Var[Int] = Var(Constants.GameState.firstCome)
  private var gameState:Int = Constants.GameState.firstCome


  private val startGameModal = new StartGameModal(gameStateVar,start)

  private var timer:Int = 0

  private var justSynced:Boolean = false
  private var gridAllState:Option[GridState] = None
  private var gridStateWithoutBullet:Option[GridStateWithoutBullet] = None

  private val maxClientFrameDrawForSystemFrame:Int = 4 //比系统桢多渲染3桢
  private var clientFrame:Int = 0

  val watchKeys = Set(
    KeyCode.Left,
    KeyCode.Up,
    KeyCode.Right,
    KeyCode.Down
  )

  val watchBakKeys = Set(
    KeyCode.W,
    KeyCode.S,
    KeyCode.A,
    KeyCode.D
  )

  private var spaceKeyUpState = true

  private def getWatchBakKeys(k:Int):Int = k match {
    case KeyCode.W => KeyCode.Up
    case KeyCode.S => KeyCode.Down
    case KeyCode.A => KeyCode.Left
    case KeyCode.D => KeyCode.Right
    case _ => KeyCode.Escape
  }

  def getStartGameModal():Elem = {
    startGameModal.render
  }




  //todo
  private def wsConnectSuccess(e:Event) = {
    println(s"连接服务器成功")
    e
  }

  //todo
  private def wsConnectError(e:Event) = {
    JsFunc.alert("网络连接失败，请重新刷新")
    e
  }

  //todo
  private def wsConnectClose(e:Event) = {
    JsFunc.alert("网络连接失败，请重新刷新")
    e
  }

  //todo
  private def wsMessageHandler(e:MessageEvent) = {
    import com.neo.sk.tank.front.utils.byteObject.ByteObject._
    e.data match {
      case blobMsg:Blob =>
        val fr = new FileReader()
        fr.readAsArrayBuffer(blobMsg)
        fr.onloadend = { _: Event =>
          val buf = fr.result.asInstanceOf[ArrayBuffer]
          val middleDataInJs = new MiddleBufferInJs(buf)
          bytesDecode[WsProtocol.WsMsgServer](middleDataInJs) match {
            case Right(data) =>
              data match {
                case WsProtocol.YourInfo(uId,tankId) =>
                  myId = uId
                  myTankId = tankId


                case WsProtocol.UserEnterRoom(userId,name,tank) =>
                  if(myId != userId){
                    grid.playerJoin(tank)
                  }


                case WsProtocol.UserLeftRoom(tankId,name) =>
                  println(s"玩家=${name} left tankId=${tankId}")
                  grid.leftGame(tankId)


                case WsProtocol.YouAreKilled(tankId,userId) =>
                  println(s"you are killed")
                  setGameState(Constants.GameState.stop)

                case WsProtocol.TankActionFrameMouse(tankId,frame,action) => grid.addActionWithFrame(tankId,action,frame)

                case WsProtocol.TankActionFrameKeyDown(tankId,frame,action) => grid.addActionWithFrame(tankId,action,frame)
                case WsProtocol.TankActionFrameKeyUp(tankId,frame,action) => grid.addActionWithFrame(tankId,action,frame)


                case WsProtocol.Ranks(currentRank,historyRank) =>
                  grid.currentRank = currentRank
                  grid.historyRank = historyRank

                case WsProtocol.GridSyncState(d) =>
                  justSynced = true
                  gridStateWithoutBullet = Some(d)



                case WsProtocol.GridSyncAllState(gridState) =>
                  println(s"已同步游戏中所有数据，进行渲染，${gridState}")
                  justSynced = true
                  gridAllState = Some(gridState)
                  setGameState(Constants.GameState.play)

                case t:WsProtocol.TankAttacked =>
                  //        grid.attackTankCallBack(bId,null)
                  //移除子弹并且进行血量计算
                  grid.recvTankAttacked(t)


                case t:WsProtocol.ObstacleAttacked =>
                  //移除子弹并且进行血量计算
                  grid.recvObstacleAttacked(t)

                case t:WsProtocol.AddObstacle =>
                  grid.recvAddObstacle(t)



                case t:WsProtocol.TankEatProp =>
                  grid.recvTankEatProp(t)

                case t:WsProtocol.AddProp =>
                  grid.recvAddProp(t)

                case WsProtocol.TankLaunchBullet(frame,bullet) =>
                  //        println(s"recv msg:${e.data.toString}")
                  grid.addBullet(frame,new BulletClientImpl(bullet))
                //
                case  _ => println(s"接收到无效消息ss")
              }
            case Left(error) =>
              println(s"decode msg failed,error:${error.message}")
          }
        }
      case unknow =>
        println(s"recv unknow msg:${unknow}")
    }
//    decode[WsProtocol.WsMsgServer](e.data.toString).right.get match {
//      case WsProtocol.YourInfo(uId,tankId) =>
//        myId = uId
//        myTankId = tankId
//
//
//      case WsProtocol.UserEnterRoom(userId,name,tank) =>
//        if(myId != userId){
//          grid.playerJoin(tank)
//        }
//
//
//      case WsProtocol.UserLeftRoom(tankId,name) =>
//        println(s"玩家=${name} left tankId=${tankId}")
//        grid.leftGame(tankId)
//
//
//      case WsProtocol.YouAreKilled(tankId,userId) =>
//        println(s"you are killed")
//        setGameState(Constants.GameState.stop)
//
//      case WsProtocol.TankActionFrameMouse(tankId,frame,action) => grid.addActionWithFrame(tankId,action,frame)
//
//      case WsProtocol.TankActionFrameKeyDown(tankId,frame,action) => grid.addActionWithFrame(tankId,action,frame)
//      case WsProtocol.TankActionFrameKeyUp(tankId,frame,action) => grid.addActionWithFrame(tankId,action,frame)
//
//
//      case WsProtocol.Ranks(currentRank,historyRank) =>
//        grid.currentRank = currentRank
//        grid.historyRank = historyRank
//
//      case WsProtocol.GridSyncState(d) =>
//        justSynced = true
//        gridStateWithoutBullet = Some(d)
//
//
//
//      case WsProtocol.GridSyncAllState(gridState) =>
//        println(s"已同步游戏中所有数据，进行渲染，${gridState}")
//        justSynced = true
//        gridAllState = Some(gridState)
//        setGameState(Constants.GameState.play)
//
//      case t:WsProtocol.TankAttacked =>
////        grid.attackTankCallBack(bId,null)
//        //移除子弹并且进行血量计算
//        grid.recvTankAttacked(t)
//
//
//      case t:WsProtocol.ObstacleAttacked =>
//        //移除子弹并且进行血量计算
//        grid.recvObstacleAttacked(t)
//
//      case t:WsProtocol.AddObstacle =>
//        grid.recvAddObstacle(t)
//
//
//
//      case t:WsProtocol.TankEatProp =>
//        grid.recvTankEatProp(t)
//
//      case t:WsProtocol.AddProp =>
//        grid.recvAddProp(t)
//
//      case WsProtocol.TankLaunchBullet(frame,bullet) =>
////        println(s"recv msg:${e.data.toString}")
//        grid.addBullet(frame,new BulletClientImpl(bullet))
////
//      case  _ => println(s"接收到无效消息ss")
//
//
//    }
    e
  }


  private def setGameState(s:Int):Unit = {
    gameStateVar := s
    gameState = s
  }

  def sendMsg2Server(msg:WsFrontProtocol.WsMsgFront):Unit ={
    if(gameState == Constants.GameState.play)
      websocketClient.sendMsg(msg)

  }

  private val keySet = mutable.HashSet[Int]()

  def addActionListenEvent():Unit = {
    canvas.focus()
    canvas.onmousemove = { (e:dom.MouseEvent) =>
      val point = Point(e.clientX.toFloat,e.clientY.toFloat)
      val theta = point.getTheta(canvasBoundary / 2).toFloat

      sendMsg2Server(WsFrontProtocol.MouseMove(theta))//发送鼠标位置
      e.preventDefault()
    }
    canvas.onclick = {(e:MouseEvent) =>
      sendMsg2Server(WsFrontProtocol.MouseClick(System.currentTimeMillis()))//发送炮弹数据
      e.preventDefault()
    }

    canvas.onkeydown = {
      (e: dom.KeyboardEvent) => {
        if (watchKeys.contains(e.keyCode) && !keySet.contains(e.keyCode)) {
          keySet.add(e.keyCode)
          println(s"key down: [${e.keyCode}]")
          sendMsg2Server(WsFrontProtocol.PressKeyDown(e.keyCode))//发送操作指令
          e.preventDefault()
        } else if(watchBakKeys.contains(e.keyCode)){
          val directionKey = getWatchBakKeys(e.keyCode)
          if(!keySet.contains(directionKey)){
            keySet.add(directionKey)
            println(s"key down: [${e.keyCode}]")
            sendMsg2Server(WsFrontProtocol.PressKeyDown(directionKey))//发送操作指令
          }
          e.preventDefault()
        } else if(e.keyCode == KeyCode.Space && spaceKeyUpState){
          spaceKeyUpState = false
          sendMsg2Server(WsFrontProtocol.MouseClick(System.currentTimeMillis()))//发送炮弹数据
          e.preventDefault()
        }
      }
    }

    canvas.onkeyup = {
      (e: dom.KeyboardEvent) => {
        if (watchKeys.contains(e.keyCode)) {
          keySet.remove(e.keyCode)
          println(s"key up: [${e.keyCode}]")
          sendMsg2Server(WsFrontProtocol.PressKeyUp(e.keyCode))//发送操作指令
          e.preventDefault()
        } else if(watchBakKeys.contains(e.keyCode)){
          val directionKey = getWatchBakKeys(e.keyCode)
          keySet.remove(directionKey)
          println(s"key up: [${e.keyCode}]")
          sendMsg2Server(WsFrontProtocol.PressKeyUp(directionKey))//发送操作指令
          e.preventDefault()
        } else if(e.keyCode == KeyCode.Space){
          spaceKeyUpState = true
          e.preventDefault()
        }
      }
    }
  }

  //游戏启动
  def start(name:String):Unit = {
    myName = name
    if(firstCome){
      firstCome = false
      addActionListenEvent()
      setGameState(Constants.GameState.loadingPlay)
      websocketClient.setup(name)
      gameLoop()
      timer = Shortcut.schedule(gameLoop,ptcl.model.Frame.millsAServerFrame / ptcl.model.Frame.clientFrameAServerFrame)
    } else if(websocketClient.getWsState){
      websocketClient.sendMsg(WsFrontProtocol.RestartGame(name))
      setGameState(Constants.GameState.loadingPlay)
      timer = Shortcut.schedule(gameLoop,ptcl.model.Frame.millsAServerFrame / ptcl.model.Frame.clientFrameAServerFrame)
    }else{
      JsFunc.alert("网络连接失败，请重新刷新")
    }
  }

//  var tickCount = 0L
//  var testStartTime = System.currentTimeMillis()
//  var testEndTime = System.currentTimeMillis()
//  var startTime = System.currentTimeMillis()

  def gameLoop():Unit = {

//    val t = System.currentTimeMillis()
//    if(math.abs(t - startTime) < 15 || math.abs(t - startTime) > 45)
//    println(s"use Time = ${t - startTime}")
//    startTime = t
//    var flag =  true
//    while (flag){
//      if(System.currentTimeMillis() - t > 15){
//        flag = false
//      }
//    }
//    tickCount += 1
//
//    if(tickCount % 100 == 0){
//      testEndTime = System.currentTimeMillis()
//      println(s"user Time = ${testEndTime - testStartTime}")
//      testStartTime = testEndTime
//    }

    gameState match {
      case Constants.GameState.loadingPlay =>
        println(s"等待同步数据")
        drawGameLoading()
      case Constants.GameState.play =>
        justSynced match {
          case true =>
            if(clientFrame == maxClientFrameDrawForSystemFrame - 1){
              gridStateWithoutBullet.foreach(t =>grid.gridSyncStateWithoutBullet(t))
              gridStateWithoutBullet = None
              gridAllState.foreach(t => grid.gridSyncState(t))
              gridAllState = None
              justSynced = false
            }
          case false =>
            if(clientFrame == maxClientFrameDrawForSystemFrame - 1){
              grid.update()
//              if(grid.systemFrame % 10 == 0)
//                println(s"${grid.systemFrame} user ${System.currentTimeMillis() - x}")
            }

        }
        clientFrame += 1
        clientFrame = clientFrame % maxClientFrameDrawForSystemFrame
        val x = System.currentTimeMillis()
        drawGame(clientFrame,maxClientFrameDrawForSystemFrame)
//        if(tickCount % 100 == 0){
//          val end = System.currentTimeMillis()
//          println(s"cur Frame=${grid.systemFrame} use time=${end-startTime}")
//        }
//        if(grid.systemFrame % 10 == 0)
//          println(s"${grid.systemFrame} 动画 ${System.currentTimeMillis() - x}")






      case Constants.GameState.stop =>
        drawGameStop()
        Shortcut.cancelSchedule(timer)

    }
  }


  def drawGameLoading():Unit = {
    ctx.fillStyle = Color.Black.toString()
    ctx.fillRect(0, 0, canvasBounds.x * canvasUnit, canvasBounds.y * canvasUnit)
    ctx.fillStyle = "rgb(250, 250, 250)"
    ctx.textAlign = "left"
    ctx.textBaseline = "top"
    ctx.font = "36px Helvetica"
    ctx.fillText("请稍等，正在连接服务器", 150, 180)
    println()
  }

  def drawGame(curFrame:Int,maxClientFrame:Int): Unit ={
//
    // rintln("111111111111111111111")


    grid.draw(ctx,myName,myTankId,curFrame,maxClientFrame,canvasBounds)
    val tankList =grid.tankMap.values.map(_.getTankState())
    val otherTank = tankList.filterNot(_.tankId == myTankId)

    val lastHeader = tankList.find(_.tankId == myTankId).map(_.position)
    drawSmallMap(lastHeader,otherTank.toList)

  }

  def drawGameStop():Unit = {
    ctx.fillStyle = Color.Black.toString()
    ctx.fillRect(0, 0, canvasBounds.x * canvasUnit, canvasBounds.y * canvasUnit)
    ctx.fillStyle = "rgb(250, 250, 250)"
    ctx.textAlign = "left"
    ctx.textBaseline = "top"
    ctx.font = "36px Helvetica"
    ctx.fillText("您已经死亡", 150, 180)
    println()
  }
  object color{
    val mapColor = "rgb(41,238,238,0.4)"
    val myself = "#000080"
    val otherTankColor = "#CD5C5C"
  }

//  private val myHeaderImg = dom.document.createElement("img").asInstanceOf[Image]

  def drawSmallMap(myHeader:Option[Point],otherTank:List[TankState]):Unit = {
//    val offX = myHeader.x / bounds.x * SmallMap.x
//    val offY = myHeader.y / bounds.y * SmallMap.y
    ctx.fillStyle = color.mapColor
    ctx.fillRect((canvasBounds.x - ptcl.model.LittleMap.w) * canvasUnit,(canvasBounds.y  - ptcl.model.LittleMap.h) * canvasUnit ,ptcl.model.LittleMap.w * canvasUnit ,ptcl.model.LittleMap.h * canvasUnit )

    ctx.beginPath()
    ctx.fillStyle = color.myself
    myHeader.foreach{ point=>
      val offX = point.x / bounds.x * SmallMap.x
      val offY = point.y / bounds.y * SmallMap.y
      ctx.arc((canvasBounds.x - ptcl.model.LittleMap.w + offX) * canvasUnit, (canvasBounds.y  - ptcl.model.LittleMap.h + offY) * canvasUnit, 0.5 * canvasUnit,0,2*Math.PI)

    }
    ctx.fill()
    ctx.closePath()

    ctx.beginPath()
    ctx.fillStyle =color.otherTankColor
    otherTank.foreach{ i =>
      val x = i.position.x / bounds.x * SmallMap.x
      val y = i.position.y / bounds.y * SmallMap.y

//      ctx.fillStyle = i.tankColorType
      ctx.arc((canvasBounds.x - ptcl.model.LittleMap.w + x)*canvasUnit,(canvasBounds.y  - ptcl.model.LittleMap.h + y)*canvasUnit ,0.5*canvasUnit,0,2*Math.PI)

    }
    ctx.fill()
    ctx.closePath()


  }

//  val tank =
//  val otherTank = tank.fil
//  drawSmallMap(lastHeader,otherTank)











}
