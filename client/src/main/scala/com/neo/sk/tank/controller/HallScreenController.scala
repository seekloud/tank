package com.neo.sk.tank.controller

import com.neo.sk.tank.common.Context
import com.neo.sk.tank.view.{GameHallListener, GameHallScreen}
/**
  * created by benyafang on 2018/10/26
  * 获取房间列表，指定房间进入和随机进入房间
  * 实例化完成即向服务器请求房间列表
  * */
class HallScreenController(val context:Context,val gameHall:GameHallScreen){
  gameHall.setListener(new GameHallListener{
    override def randomBtnListener: Unit = super.randomBtnListener

    override def confirmBtnListener: Unit = super.confirmBtnListener
  })

}
