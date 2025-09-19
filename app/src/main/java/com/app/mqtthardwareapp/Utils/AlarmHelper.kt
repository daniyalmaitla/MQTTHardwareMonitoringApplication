package com.app.mqtthardwareapp.Utils



import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.provider.Settings

object AlarmHelper {
    private var mediaPlayer: MediaPlayer? = null

    fun startAlarm(context: Context) {
        if (mediaPlayer == null) {
            val alarmUri: Uri = Settings.System.DEFAULT_ALARM_ALERT_URI
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, alarmUri)
                isLooping = true
                setOnPreparedListener { start() }
                prepareAsync()
            }
        }
    }

    fun stopAlarm(context: Context) {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
