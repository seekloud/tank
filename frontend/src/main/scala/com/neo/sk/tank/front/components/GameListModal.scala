package com.neo.sk.tank.front.components

import com.neo.sk.tank.front.common.{Component, Routes}
import mhtml.Var
import com.neo.sk.tank.front.utils.{Http, JsFunc}
import io.circe.generic.auto._
import io.circe.syntax._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.xml.Elem
import com.neo.sk.tank.front.utils.Shortcut
import org.scalajs.dom
import org.scalajs.dom.KeyboardEvent
import org.scalajs.dom.ext.KeyCode
import org.scalajs.dom.html.Input
import com.neo.sk.tank.shared.ptcl.GameRecPtcl.{getGameRecReq, getGameRecByIdReq, getGameRecByRoomReq, getGameRecByPlayerReq, getGameRecRsp, gameRec}

/**
  * Created by hongruying on 2018/7/9
  */
object GameListModal extends Component{

  private val selectOpt = Var("用户ID")
  private var selectState = 0
  private val recordTable = Var(List.empty[gameRec])
  private var currentPage = Var(1)
  private var currentPageState = 1
  private var lastRecordId = 0L

  def getRecordById():Unit = {
    val id = dom.window.document.getElementById("inputContent").asInstanceOf[Input].value
    if(selectState == 0 && id != ""){
      lastRecordId = (currentPageState-1) * 10
      val data = getGameRecByPlayerReq(id, lastRecordId, 10).asJson.noSpaces
      Http.postJsonAndParse[getGameRecRsp](Routes.getRecordListByPlayerUrl, data).map{rsp =>
        if(rsp.errCode == 0){
          recordTable := rsp.data.get
        } else {
          JsFunc.alert(rsp.msg)
          println(rsp.msg)
        }
      }
    }else if(selectState == 1 && id != ""){
      lastRecordId = (currentPageState-1) * 10
      val data = getGameRecByIdReq(id.toLong).asJson.noSpaces
      Http.postJsonAndParse[getGameRecRsp](Routes.getRecordListByIdUrl, data).map{rsp =>
        if(rsp.errCode == 0){
          recordTable := rsp.data.get
        } else {
          JsFunc.alert(rsp.msg)
          println(rsp.msg)
        }
      }
    }else if(selectState==2 && id != ""){
      lastRecordId = (currentPageState-1) * 10
      val data = getGameRecByRoomReq(id.toLong, lastRecordId, 10).asJson.noSpaces
      Http.postJsonAndParse[getGameRecRsp](Routes.getRecordListByRoomUrl, data).map{rsp =>
        if(rsp.errCode == 0){
          recordTable := rsp.data.get
        } else {
          JsFunc.alert(rsp.msg)
          println(rsp.msg)
        }
      }
    }else{
      lastRecordId = (currentPageState-1) * 10
      val data = getGameRecReq(lastRecordId,10).asJson.noSpaces
      Http.postJsonAndParse[getGameRecRsp](Routes.getRecordListUrl, data).map{rsp =>
        if(rsp.errCode == 0){
          recordTable := rsp.data.get
        } else {
          JsFunc.alert(rsp.msg)
          println(rsp.msg)
        }
      }
    }
  }

  def longToTime(time:Long) = {
    Shortcut.dateFormatDefault(time)
  }

  def toOneRecord() ={
    dom.window.location.hash = "https://www.baidu.com"
  }

  def changeToUser = {
    selectOpt := "用户ID"
    selectState = 0
  }

  def changeToRec = {
    selectOpt := "记录ID"
    selectState = 1
  }

  def changeToRom = {
    selectOpt := "roomID"
    selectState = 2
  }

  def changePage(page:Int) = {
    currentPage := page
    currentPageState = page
    getRecordById()
  }

  def clickEnter(e:KeyboardEvent):Unit = {
    if(e.keyCode == KeyCode.Enter) {
      getRecordById()
      e.preventDefault()
    }
  }

    val gameRecordTable = recordTable.map {
    case Nil => <table class="table table-striped table-hover"></table>
    case lst =>
      <table class="table table-striped table-hover">
        <thead>
          <tr>
            <th>RecordID</th>
            <th>RoomID</th>
            <th>StartTime</th>
            <th>EndTime</th>
            <th>PepleCounts</th>
            <th>Members</th>
          </tr>
        </thead>
        <tbody>
          {lst.map{l =>
          <tr onclick={() => toOneRecord()}>
            <td>{l.recordId}</td>
            <td>{l.roomId}</td>
            <td>{longToTime(l.startTime)}</td>
            <td>{longToTime(l.endTime)}</td>
            <td>{l.userCounts}</td>
            <td>{l.userList.mkString(",")}</td>
          </tr>
          }}
        </tbody>
      </table>
    }

  val pageSwitch = currentPage.map{
    case 1 => <nav aria-label="Page navigation">
      <ul class="pagination">
        <li class="disabled">
          <a aria-label="Previous">
            <span aria-hidden="true">{"<<"}</span>
          </a>
        </li>
        <li class="disabled"><a>1</a></li>
        <li><a onclick={() => changePage(2)}>2</a></li>
        <li><a onclick={() => changePage(3)}>3</a></li>
        <li><a onclick={() => changePage(4)}>4</a></li>
        <li><a onclick={() => changePage(5)}>5</a></li>
        <li>
          <a onclick={() => changePage(2)} aria-label="Next">
            <span aria-hidden="true">{">>"}</span>
          </a>
        </li>
      </ul>
    </nav>

    case 2 => <nav aria-label="Page navigation">
      <ul class="pagination">
        <li>
          <a onclick={() => changePage(1)} aria-label="Previous">
            <span aria-hidden="true">{"<<"}</span>
          </a>
        </li>
        <li><a onclick={() => changePage(1)}>1</a></li>
        <li class="disabled"><a>2</a></li>
        <li><a onclick={() => changePage(3)}>3</a></li>
        <li><a onclick={() => changePage(4)}>4</a></li>
        <li><a onclick={() => changePage(5)}>5</a></li>
        <li>
          <a onclick={() => changePage(3)} aria-label="Next">
            <span aria-hidden="true">{">>"}</span>
          </a>
        </li>
      </ul>
    </nav>

    case x => <nav aria-label="Page navigation">
      <ul class="pagination">
        <li>
          <a onclick={() => changePage(x-1)} aria-label="Previous">
            <span aria-hidden="true">{"<<"}</span>
          </a>
        </li>
        <li><a onclick={() => changePage(x-2)}>{x-2}</a></li>
        <li><a onclick={() => changePage(x-1)}>{x-1}</a></li>
        <li class="disabled"><a>{x}</a></li>
        <li><a onclick={() => changePage(x+1)}>{x+1}</a></li>
        <li><a onclick={() => changePage(x+2)}>{x+2}</a></li>
        <li>
          <a onclick={() => changePage(x+1)} aria-label="Next">
            <span aria-hidden="true">{">>"}</span>
          </a>
        </li>
      </ul>
    </nav>

  }



  override def render: Elem = {
    Shortcut.scheduleOnce(() => getRecordById(),500)
    <div>
      <div id="searchGameRecord">
        <div class="input-group">
          <div class="input-group-btn">
            <button type="button" class="btn btn-default dropdown-toggle" data-toggle="dropdown">{selectOpt}
              <span class="caret"></span>
            </button>
            <ul class="dropdown-menu">
              <li onclick={() => changeToUser}><a>用户ID</a></li>
              <li onclick={() => changeToRec}><a>记录ID</a></li>
              <li onclick={() => changeToRom}><a>roomID</a></li>
            </ul>
          </div>
          <input type="text" id="inputContent" class="form-control" onkeydown = {e:KeyboardEvent => clickEnter(e)}></input>
          <div class="input-group-btn">
            <button type="button" class="btn btn-info" onclick={() => getRecordById()}>search</button>
          </div>
          </div>
      </div>
      <div id="gameRecordTable">
        {gameRecordTable}
      </div>
      <div id="pageSwitch">
        {pageSwitch}
      </div>
    </div>

  }
}

