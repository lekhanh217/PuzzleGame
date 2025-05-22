package com.example.puzzle_pieces.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.IBinder
import android.util.Log

class GameMusicService : Service() {
    companion object {
         var mediaPlayer: MediaPlayer? = null
         var currentSourceId: Int? = null;

        fun stopMusic(context: Context) {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            context.stopService(Intent(context, GameMusicService ::class.java))
        }
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (mediaPlayer == null) {
            Log.d("game service","");
            mediaPlayer = MediaPlayer.create(this,currentSourceId!!)
            mediaPlayer?.isLooping = true
            mediaPlayer?.setVolume(0.5f, 0.5f)
            mediaPlayer?.start()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
