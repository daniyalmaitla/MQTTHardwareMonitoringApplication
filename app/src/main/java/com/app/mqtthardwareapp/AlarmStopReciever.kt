package com.app.mqtthardwareapp


import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.app.mqtthardwareapp.Utils.AlarmHelper

class AlarmStopReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AlarmHelper.stopAlarm()

        val deviceId = intent.getStringExtra("deviceId")
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(deviceId.hashCode()) // dismiss the alarm notification
    }
}