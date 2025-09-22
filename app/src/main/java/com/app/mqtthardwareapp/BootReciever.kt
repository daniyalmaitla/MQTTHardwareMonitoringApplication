package com.app.mqtthardwareapp
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import kotlin.jvm.java
import android.provider.Settings

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


                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            System.currentTimeMillis() + interval,
                            alarmIntent
                        )
                    } else {
                        Log.w("BOOT", "❌ Exact alarms not allowed. Ask user to allow in settings.")
                        val settingsIntent = Intent(
                            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                            Uri.parse("package:${context.packageName}")
                        )
                        settingsIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(settingsIntent)
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        System.currentTimeMillis() + interval,
                        alarmIntent
                    )
                }
                Log.d("BOOT", "⏰ Alarms re-scheduled with interval: $interval ms")
            }
        }
    }
}
