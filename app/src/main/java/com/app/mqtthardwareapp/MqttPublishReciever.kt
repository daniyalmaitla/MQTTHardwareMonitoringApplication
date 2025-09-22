package com.app.mqtthardwareapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.annotation.RequiresApi
import com.app.mqtthardwareapp.Data.AppDatabase
import kotlinx.coroutines.runBlocking

class MqttPublishReceiver : BroadcastReceiver() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == "MQTT_PERIODIC_PUBLISH") {
            val topic = intent.getStringExtra("topic") ?: return
            val payload = intent.getStringExtra("payload") ?: return

            Log.d("MQTT_ALARM", "⏰ Periodic alarm triggered for $topic")

            // Publish via service
            val serviceIntent = Intent(context, MqttBackgroundService::class.java).apply {
                action = MqttBackgroundService.ACTION_MANUAL_PUBLISH
                putExtra(MqttBackgroundService.EXTRA_DEVICE_ID, topic.removeSuffix("READ"))
                putExtra(MqttBackgroundService.EXTRA_PAYLOAD, payload)
            }

            try {
                context.startForegroundService(serviceIntent)

                // Reschedule next alarm (for exact alarms on API 31+)
                rescheduleNextAlarm(context, intent)

            } catch (e: Exception) {
                Log.e("MQTT_ALARM", "❌ Failed to start service from alarm: ${e.message}")
            }
        }
    }

    private fun rescheduleNextAlarm(context: Context, originalIntent: Intent) {
        // Get the interval from SharedPreferences or database
        // For now, we'll use a default interval - you should store this properly
        val topic = originalIntent.getStringExtra("topic") ?: return
        val payload = originalIntent.getStringExtra("payload") ?: return

        // You'll need to store intervals somewhere accessible
        // For now, using a default of 30 seconds
        val intervalMs = getStoredInterval(context, topic) ?: 30000L

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            topic.hashCode(),
            originalIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        SystemClock.elapsedRealtime() + intervalMs,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + intervalMs,
                    pendingIntent
                )
            }
            Log.d("MQTT_ALARM", "⏰ Rescheduled next alarm for $topic in ${intervalMs}ms")
        } catch (e: Exception) {
            Log.e("MQTT_ALARM", "❌ Failed to reschedule alarm: ${e.message}")
        }
    }

    private fun getStoredInterval(context: Context, topic: String): Long? {
        // Extract device ID from topic
        val deviceId = topic.removeSuffix("READ").removeSuffix("WRITE")

        // Query database for device interval
        return try {
            val database = AppDatabase.getDatabase(context)
            // Since we can't use suspend functions in BroadcastReceiver,
            // we'll use runBlocking (not ideal, but necessary here)
            runBlocking {
                database.deviceDao().getDeviceByDeviceId(deviceId)?.interval
            }
        } catch (e: Exception) {
            Log.e("MQTT_ALARM", "❌ Failed to get interval for $deviceId: ${e.message}")
            30000L // Default 30 seconds
        }
    }
}