package com.app.mqtthardwareapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi

class WakeReceiver : BroadcastReceiver() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context, intent: Intent) {
        val serviceIntent = Intent(context, MqttBackgroundService::class.java)
        serviceIntent.action = "PERFORM_PERIODIC_READ"
        context.startForegroundService(serviceIntent)
    }
}
