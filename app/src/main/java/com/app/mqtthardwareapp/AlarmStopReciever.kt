package com.app.mqtthardwareapp


import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.app.mqtthardwareapp.Utils.AlarmHelper
object AlarmState {
    val acknowledgedAlarms = mutableMapOf<String, MutableMap<String, Boolean>>()

    fun acknowledgeAlarm(deviceId: String, field: String) {
        val ackMap = acknowledgedAlarms.getOrPut(deviceId) { mutableMapOf() }
        ackMap[field] = true
    }

    fun isAcknowledged(deviceId: String, field: String): Boolean {
        return acknowledgedAlarms[deviceId]?.get(field) ?: false
    }
    fun clearAcknowledgement(deviceId: String, field: String) {
        acknowledgedAlarms[deviceId]?.remove(field)
        android.util.Log.d("MQTT_SERVICE", "Alarm acknowledgement cleared: device=$deviceId field=$field")
    }
}
class AlarmStopReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val deviceId = intent.getStringExtra("deviceId") ?: return
        val field = intent.getStringExtra("field") ?: return

        android.util.Log.d("MQTT_SERVICE", "AlarmStopReceiver -> device=$deviceId field=$field")

        AlarmState.acknowledgeAlarm(deviceId, field)
        AlarmHelper.stopAlarm(context)

        // cancel the notification for that device+field
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel((deviceId + field).hashCode())
    }
}