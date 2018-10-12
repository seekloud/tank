package com.neo.sk.tank.test




import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.scene.{Scene, SnapshotParameters}
import scalafx.scene.effect._
import scalafx.scene.layout.HBox
import scalafx.scene.paint.Color._
import scalafx.scene.paint.{LinearGradient, Stops}
import scalafx.scene.text.Text
import scalafx.scene.canvas.Canvas
import scalafx.scene.paint.Color
import scalafx.scene.shape.ArcType


object JavaFXCanvas extends JFXApp{
  val helloStage = new PrimaryStage{
    title = "test scalaFx"
    width = 640
    height = 600
    scene = new Scene {
      fill = White
      content = new HBox {
        children = Seq(
          new Text {
            text = "scala"
            style = "-fx-font-size:100pt"
            fill = new LinearGradient(
              endX = 0,
              stops = Stops(PaleGreen, SeaGreen)
            )
          }, new Text {
            text = "FX"
            style = "-fx-font-size: 100pt"
            fill = new LinearGradient(
              endX = 0,
              stops = Stops(Cyan, DodgerBlue))
            effect = new DropShadow {
              color = DodgerBlue
              radius = 25
              spread = 0.25
            }
          })
        effect = new Reflection {
          fraction = 0.5
          topOffset = -5.0
          bottomOpacity = 0.75
          input = new Lighting {
            light = new Light.Distant {
              elevation = 60
            }
          }
        }
      }
    }
  }


  val canvas = new Canvas(300, 300)
  val gc = canvas.graphicsContext2D

  gc.fill = Color.Green
  gc.stroke = Color.Blue
  gc.lineWidth = 5
  gc.strokeLine(40, 10, 10, 40)

  gc.fillOval(10, 60, 30, 30)
  gc.strokeOval(60, 60, 30, 30)
  gc.fillRoundRect(110, 60, 30, 30, 10, 10)
  gc.strokeRoundRect(160, 60, 30, 30, 10, 10)
  gc.fillArc(10, 110, 30, 30, 45, 240, ArcType.Open)
  gc.fillArc(60, 110, 30, 30, 45, 240, ArcType.Chord)
  gc.fillArc(110, 110, 30, 30, 45, 240, ArcType.Round)
  gc.strokeArc(10, 160, 30, 30, 45, 240, ArcType.Open)
  gc.strokeArc(60, 160, 30, 30, 45, 240, ArcType.Chord)
  gc.strokeArc(110, 160, 30, 30, 45, 240, ArcType.Round)
  gc.fillPolygon(Seq((10.0, 210), (40, 210), (10, 240), (40, 240)))
  gc.strokePolygon(Seq((60.0, 210), (90, 210), (60, 240), (90, 240)))
  gc.strokePolyline(Seq((110.0, 210), (140, 210), (110, 240), (140, 240)))
  val img = canvas.snapshot(new SnapshotParameters(), null)
  println(img.getPixelReader.getPixelFormat().getType)
  val canvasTestStage = new PrimaryStage {
    title = "Drawing Operations Test"
    scene = new Scene {
      content = canvas
    }
  }

  stage = canvasTestStage
}
