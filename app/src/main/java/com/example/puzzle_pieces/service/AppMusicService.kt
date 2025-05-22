package com.example.puzzle_pieces.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.IBinder
import com.example.puzzle_pieces.R

class AppMusicService : Service() {
    companion object {
        var mediaPlayer: MediaPlayer? = null

        fun stopMusic(context: Context) {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            context.stopService(Intent(context, AppMusicService ::class.java))
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (mediaPlayer == null){
            mediaPlayer = MediaPlayer.create(this, R.raw.music)
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

