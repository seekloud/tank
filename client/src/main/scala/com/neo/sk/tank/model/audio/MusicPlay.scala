package com.neo.sk.tank.model.audio

import com.neo.sk.tank.App
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer

class MusicPlay(songName: String){

  private val gamePlay = new Media(App.getClass.getResource(songName).toString)
  private val gamePlayer = new MediaPlayer(gamePlay)

  def playAsBgm = {
    gamePlayer.setAutoPlay(true)
    gamePlayer.setCycleCount(MediaPlayer.INDEFINITE)
    gamePlayer.setVolume(0.5D)
    gamePlayer.play()
  }

  def playOnce = {
    gamePlayer.play()
  }

  def stop = {
    gamePlayer.stop()
  }
}








