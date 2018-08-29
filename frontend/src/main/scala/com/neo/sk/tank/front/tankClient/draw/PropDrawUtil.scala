package com.neo.sk.tank.front.tankClient.draw

import com.neo.sk.tank.front.common.Routes
import com.neo.sk.tank.front.tankClient.GameContainerClientImpl
import com.neo.sk.tank.shared.model.Constants.GameAnimation
import com.neo.sk.tank.shared.model.Point
import org.scalajs.dom
import org.scalajs.dom.raw.HTMLElement

/**
  * Created by hongruying on 2018/8/29
  */
trait PropDrawUtil{ this:GameContainerClientImpl =>

  protected def drawProps(offset:Point) = {
    propMap.values.foreach{ prop =>
      val img = dom.document.createElement("img")
      prop.propType match {
        case 1 => img.setAttribute("src", s"${Routes.base}/static/img/xueliang.png")
        case 2 => img.setAttribute("src", s"${Routes.base}/static/img/sudu.png")
        case 3 => img.setAttribute("src", s"${Routes.base}/static/img/qiang.png")
        case 4 => img.setAttribute("src", s"${Routes.base}/static/img/yiliao.png")
        case 5 => img.setAttribute("src", s"${Routes.base}/static/img/sandan.png")
      }
      val p = prop.getPosition + offset
      ctx.drawImage(img.asInstanceOf[HTMLElement], (p.x - prop.getRadius) * canvasUnit, (p.y - prop.getRadius) * canvasUnit,
        prop.getRadius * 2 * canvasUnit, prop.getRadius * 2 * canvasUnit)
      ctx.fill()
      ctx.stroke()
      if(tankDestroyAnimationMap.contains(prop.pId)){
        img.setAttribute("src", s"${Routes.base}/static/img/boom.png")
        if (tankDestroyAnimationMap(prop.pId) > GameAnimation.tankDestroyAnimationFrame * 2 / 3) {
          ctx.drawImage(img.asInstanceOf[HTMLElement], (p.x - prop.getRadius) * canvasUnit, (p.y - prop.getRadius) * canvasUnit, prop.getRadius * 2 * canvasUnit, prop.getRadius * 2 * canvasUnit)
          ctx.fill()
          ctx.stroke()
        } else if (tankDestroyAnimationMap(prop.pId) > GameAnimation.tankDestroyAnimationFrame / 3) {
          ctx.drawImage(img.asInstanceOf[HTMLElement], (p.x - prop.getRadius * 2.5f) * canvasUnit,(p.y - prop.getRadius * 2.5f) * canvasUnit, prop.getRadius * 2 * 2.5 * canvasUnit, prop.getRadius * 2 * 2.5 * canvasUnit)
          ctx.fill()
          ctx.stroke()
        } else if (tankDestroyAnimationMap(prop.pId) > 0) {
          ctx.drawImage(img.asInstanceOf[HTMLElement], (p.x - prop.getRadius * 2.5f) * canvasUnit,(p.y - prop.getRadius * 2.5f) * canvasUnit, prop.getRadius * 2 * 2.5 * canvasUnit, prop.getRadius * 2 * 2.5 * canvasUnit)
          ctx.fill()
          ctx.stroke()
          val imgData = ctx.getImageData((p.x - prop.getRadius * 2.5f) * canvasUnit, (p.y - prop.getRadius * 2.5f) * canvasUnit, prop.getRadius * 2 * 2.5 * canvasUnit, prop.getRadius * 2 * 2.5 * canvasUnit)
          var i = 0
          val len = imgData.data.length
          while ( {
            i < len
          }) { // 改变每个像素的透明度
            imgData.data(i + 3) = math.ceil(imgData.data(i + 3) * 0.5).toInt
            i += 4
          }
          // 将获取的图片数据放回去。
          ctx.putImageData(imgData, (p.x - prop.getRadius * 2.5f) * canvasUnit, (p.y - prop.getRadius * 2.5f) * canvasUnit)
        }
        if(tankDestroyAnimationMap(prop.pId) <= 0) tankDestroyAnimationMap.remove(prop.pId)
        else tankDestroyAnimationMap.put(prop.pId, tankDestroyAnimationMap(prop.pId) - 1)
      }
    }
  }
}
