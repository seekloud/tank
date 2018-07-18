package com.neo.sk.tank.front.tankClient

import com.neo.sk.tank.front.common.Constants
import com.neo.sk.tank.front.components.StartGameModal
import com.neo.sk.tank.front.utils.{JsFunc, Shortcut}
import com.neo.sk.tank.shared.ptcl
import com.neo.sk.tank.shared.ptcl.model.{Boundary, Point}
import com.neo.sk.tank.shared.ptcl.protocol._
import com.neo.sk.tank.shared.ptcl.tank.{GridState, GridStateWithoutBullet}
import mhtml.Var
import com.neo.sk.tank.shared.ptcl.tank.Prop
import org.scalajs.dom
import org.scalajs.dom.ext.{Color, KeyCode}
import org.scalajs.dom.html.Canvas
import org.scalajs.dom.raw.{Event, MessageEvent, MouseEvent}

import scala.collection.mutable
import scala.xml.Elem

/**
  * Created by hongruying on 2018/7/9
  */
class GameHolder(canvasName:String) {

  import io.circe._, io.circe.generic.auto.exportDecoder, io.circe.parser._, io.circe.syntax._


  private[this] val canvas = dom.document.getElementById(canvasName).asInstanceOf[Canvas]
  private[this] val ctx = canvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]

  private[this] val bounds = Point(ptcl.model.Boundary.w,ptcl.model.Boundary.h)
  private[this] val canvasBounds = ptcl.model.CanvasBoundary.getBoundary
  private[this] val canvasUnit = 10
  private[this] val canvasBoundary = canvasBounds * canvasUnit

  private[this] var myId = -1L
  private[this] var myTankId = -1L
  private[this] var myName = ""

  private[this] val grid = new GridClient(bounds,canvasUnit:Int)

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
    KeyCode.Down,
  )

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



    decode[WsProtocol.WsMsgServer](e.data.toString).right.get match {
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

      case t:WsProtocol.TankEatProp =>
        grid.recvTankEatProp(t)

      case t:WsProtocol.AddProp =>
        grid.recvAddProp(t)

      case WsProtocol.TankLaunchBullet(frame,bullet) =>
        grid.addBullet(frame,new BulletClientImpl(bullet))
//
      case  _ => println(s"接收到无效消息ss")


    }
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
      val point = Point(e.clientX,e.clientY)
      val theta = point.getTheta(canvasBoundary / 2)
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

  def gameLoop():Unit = {
//    println(s"----${System.currentTimeMillis()}")
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
              val x = System.currentTimeMillis()
              grid.update()
//              if(grid.systemFrame % 10 == 0)
//                println(s"${grid.systemFrame} user ${System.currentTimeMillis() - x}")
            }

        }
        clientFrame += 1
        clientFrame = clientFrame % maxClientFrameDrawForSystemFrame
        val x = System.currentTimeMillis()
        drawGame(clientFrame,maxClientFrameDrawForSystemFrame)
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


    grid.draw(ctx,myTankId,curFrame,maxClientFrame,canvasBounds)

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











}
