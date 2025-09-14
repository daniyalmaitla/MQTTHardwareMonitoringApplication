package com.app.mqtthardwareapp.Utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationHelper {
    const val CHANNEL_REPORTS = "reports_channel"
    const val CHANNEL_ALARMS = "alarms_channel"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val reportChannel = NotificationChannel(
                CHANNEL_REPORTS,
                "Device Reports",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for device reports"
            }

            val alarmChannel = NotificationChannel(
                CHANNEL_ALARMS,
                "Device Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical alarms from devices"
                enableVibration(true)
            }

            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(reportChannel)
            manager.createNotificationChannel(alarmChannel)
        }
    }
}
