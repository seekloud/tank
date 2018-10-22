package com.neo.sk.tank.protocol

import akka.actor.typed.ActorRef
import com.neo.sk.tank.core.RoomManager

object WatchGameProtocol {
  //esheep请求房间号
  case class GetRoomIdReq(playerId:Long)
  case class GetRoomId(playerId:Long,replyTo:ActorRef[RoomIdRsp]) extends RoomManager.Command
  case class RoomInfo(roomId:Long)
  case class RoomIdRsp(
                        data:RoomInfo,
                        errCode:Int = 0,
                        msg:String = "ok")

  //根据房间Id获取该房间的用户列表
  case class GetUserInfoListReq(roomId:Long)
  case class GetUserInfoList(roomId:Long,replyTo:ActorRef[UserInfoListByRoomIdRsp]) extends RoomManager.Command
  case class UserInfo(
                       playerId:String,
                       nickname:String
                     )
  case class UserInfoList(playerList:List[UserInfo])
  case class UserInfoListByRoomIdRsp(
                                    data:UserInfoList,
                                    errCode:Int = 0,
                                    msg:String = "ok"
                                    )

  //获取进行中游戏房间列表
  case class GetRoomListReq(replyTo:ActorRef[RoomListRsp]) extends RoomManager.Command
  case class RoomList(roomList:List[Long])
  case class RoomListRsp(data:RoomList,
                         errCode:Int = 0,
                         msg:String = "ok")


}
