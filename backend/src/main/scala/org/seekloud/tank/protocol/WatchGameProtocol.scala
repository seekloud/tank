package org.seekloud.tank.protocol

import akka.actor.typed.ActorRef
import org.seekloud.tank.core.RoomManager

import scala.collection.mutable

/**
  * Created by hongruying on 2019/2/3
  */
object WatchGameProtocol {
  //esheep请求房间号
  case class GetRoomIdReq(playerId:String)
  case class GetRoomId(playerId:String,replyTo:ActorRef[RoomIdRsp]) extends RoomManager.Command
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
  case class RoomList(roomList:mutable.HashMap[Long,Boolean])
  case class RoomListRsp(data:RoomList,
                         errCode:Int = 0,
                         msg:String = "ok")




}
