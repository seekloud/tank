package com.neo.sk.tank.controller

import com.neo.sk.tank.common.Context
import com.neo.sk.tank.game.NetworkInfo
import com.neo.sk.tank.model.{GameServerInfo, PlayerInfo}
import com.neo.sk.tank.view.PlayGameScreen

/**
  * Created by hongruying on 2018/10/23
  * 1.PlayScreenController 一实例化后启动PlayScreenController 连接gameServer的websocket
  * 然后使用AnimalTime来绘制屏幕，使用TimeLine来做gameLoop的更新
  *
  */
class PlayScreenController(
                            playerInfo: PlayerInfo,
                            gameServerInfo: GameServerInfo,
                            context: Context,
                            playGameScreen: PlayGameScreen
                          ) extends NetworkInfo {

}
