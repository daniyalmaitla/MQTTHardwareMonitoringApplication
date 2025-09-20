package com.app.mqtthardwareapp
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlin.jvm.java

/*class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED)
        { Log.d("BOOT", "✅ Device booted, starting MQTT service")
            val serviceIntent = Intent(context, MqttBackgroundService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
            { context.startForegroundService(serviceIntent) }
            else { context.startService(serviceIntent)
            }
        }
    }
}*/
/*class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BOOT", "✅ Device booted, checking ClientId...")
            MqttBackgroundService.startIfClientIdSet(context)
        }
    }
}*/
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BOOT", "✅ Device booted, starting MQTT service")

            // Start your service
            val serviceIntent = Intent(context, MqttBackgroundService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }

            // Re-schedule alarms
            val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val interval = prefs.getLong("alarm_interval", 0L)
            if (interval > 0L) {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val alarmIntent = PendingIntent.getBroadcast(
                    context,
                    0,
                    Intent(context, AlarmReceiver::class.java),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )

                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + interval,
                    interval,
                    alarmIntent
                )
                Log.d("BOOT", "⏰ Alarms re-scheduled with interval: $interval ms")
            }
        }
    }
}
