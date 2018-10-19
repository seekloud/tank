package com.neo.sk.tank.protocol

import akka.actor.typed.ActorRef
import com.neo.sk.tank.core.{RoomManager, UserActor4WatchGame}

object WatchGameProtocol {
  //esheep请求房间号
  case class GetRoomIdReq(playerId:Long,replyTo:ActorRef[RoomIdRsp]) extends RoomManager.Command
  case class RoomInfo(roomId:Int)
  case class RoomIdRsp(
                        data:RoomInfo,
                        errCode:Int = 0,
                        msg:String = "ok")

  //根据房间Id获取该房间的用户列表
  case class UserInfo(
                     uId:Long,
                     name:String
                     )
  case class GetUserInfoListReq(roomId:Int,replyTo:ActorRef[UserInfoListByRoomIdRsp]) extends RoomManager.Command
  case class UserInfoListByRoomIdRsp(
                                    data:Option[List[UserInfo]],
                                    errCode:Int = 0,
                                    msg:String = "ok"
                                    )

  //获取进行中游戏房间列表
  case class GetRoomListReq(replyTo:ActorRef[RoomListRsp]) extends RoomManager.Command
  case class RoomList(roomList:List[Int])
  case class RoomListRsp(data:RoomList,
                         errCode:Int = 0,
                         msg:String = "ok")


}
