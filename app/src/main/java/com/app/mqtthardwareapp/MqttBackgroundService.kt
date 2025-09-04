package com.app.mqtthardwareapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.File

class MqttBackgroundService : Service() {

    private lateinit var mqttManager: MqttManager
    private val handler = Handler()
    private val interval: Long = 30_000 // 30 seconds

    private val publishRunnable = object : Runnable {
        override fun run() {
            // Send request every 30 sec
            mqttManager.publish("1303202510000READ", "SN1303202510000E")
            Log.d("MQTT-SERVICE", "📤 Sent periodic request")
            handler.postDelayed(this, interval)
        }
    }

    /*override fun onCreate() {
        super.onCreate()
        mqttManager = MqttManager(this)

        startForegroundServiceNotification()

        mqttManager.connect(
            onConnected = {
                mqttManager.subscribe("1303202510000READ")
                mqttManager.subscribe("1303202510000WRITE")

                // Start periodic task
                handler.post(publishRunnable)
            },
            /*onMessage = { topic, payload ->
                Log.d("MQTT-SERVICE", "📥 Message from $topic → $payload")
                val intent = Intent("MQTT_MESSAGE")
                intent.putExtra("payload", payload)
                sendBroadcast(intent)
            }*/
            onMessage = { topic, payload ->
                Log.d("MQTT-SERVICE", "📥 Raw → $payload")

                // Parse into object
                val deviceData = parsePayload(payload)

                // Log each field in a readable way
                Log.d("MQTT-SERVICE", "✅ Parsed Data:")
                Log.d("MQTT-SERVICE", "1. Speed Setting = ${deviceData.speedSetting}")
                Log.d("MQTT-SERVICE", "2. Current Speed = ${deviceData.currentSpeed}")
                Log.d("MQTT-SERVICE", "3. Remaining Distance = ${deviceData.remainingDistance}")
                Log.d("MQTT-SERVICE", "4. Remaining Time = ${deviceData.remainingTime}")
                Log.d("MQTT-SERVICE", "5. Battery Voltage = ${deviceData.batteryVoltage}")
                Log.d("MQTT-SERVICE", "6. Pressure = ${deviceData.pressure}")
                Log.d("MQTT-SERVICE", "7. Pressure Limit = ${deviceData.pressureLimit}")
                Log.d("MQTT-SERVICE", "8. Automatic Reports Enable = ${deviceData.automaticReportsEnable}")
                Log.d("MQTT-SERVICE", "9. Final Finish Notification = ${deviceData.finalFinishNotification}")
                Log.d("MQTT-SERVICE", "10. Before Finish Notification = ${deviceData.beforeFinishNotification}")
                Log.d("MQTT-SERVICE", "11. Low Pressure Alarm = ${deviceData.lowPressureAlarm}")
                Log.d("MQTT-SERVICE", "12. Low Speed Alarm = ${deviceData.lowSpeedAlarm}")
                Log.d("MQTT-SERVICE", "13. Main Valve Status = ${deviceData.mainValveStatus}")

                // Save to a file
                val logFile = File(applicationContext.filesDir, "mqtt_data.txt")
                logFile.appendText("$deviceData\n")

                // Still broadcast raw payload (for future UI updates)
                val intent = Intent("MQTT_MESSAGE")
                intent.putExtra("payload", payload)
                sendBroadcast(intent)
            }

        )
    }*/
    override fun onCreate() {
        super.onCreate()
        mqttManager = MqttManager(this)

        startForegroundServiceNotification()

        mqttManager.connect(
            onConnected = {
                mqttManager.subscribe("1303202510000READ")
                mqttManager.subscribe("1303202510000WRITE")

                // ✅ Start periodic reads
                mqttManager.startPeriodicRead(
                    topic = "1303202510000READ",
                    payload = "SN1303202510000E",
                    intervalSec = 30
                )
            },
            onMessage = { topic, payload ->
                Log.d("MQTT-SERVICE", "📥 Raw → $payload")

                val deviceData = parsePayload(payload)

                Log.d("MQTT-SERVICE", "✅ Parsed Data: $deviceData")

                val logFile = File(applicationContext.filesDir, "mqtt_data.txt")
                logFile.appendText("$deviceData\n")

                val intent = Intent("MQTT_MESSAGE")
                intent.putExtra("payload", payload)
                sendBroadcast(intent)
            },
            onDisconnected = {
                Log.w("MQTT-SERVICE", "⚠ Disconnected from broker")
            }
        )
    }



    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(publishRunnable)
        mqttManager.disconnect()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundServiceNotification() {
        val channelId = "mqtt_service_channel"
        val channelName = "MQTT Background Service"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(chan)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("MQTT Service Running")
            .setContentText("Fetching data every 30 seconds...")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .build()

        startForeground(1, notification)
    }
}

