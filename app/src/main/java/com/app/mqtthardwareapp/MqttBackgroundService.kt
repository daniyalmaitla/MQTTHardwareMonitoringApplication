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
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.app.mqtthardwareapp.Data.AppDatabase
import com.app.mqtthardwareapp.Data.Device
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.forEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MqttBackgroundService : Service() {

    private lateinit var mqttManager: MqttManager
    private val handler = Handler()
    private val interval: Long = 30_000


    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())


    override fun onCreate() {
        super.onCreate()

        mqttManager = MqttManager(this)
        startForegroundServiceNotification() // your existing notif builder


        serviceScope.launch {
            mqttManager.connect(
                onConnected = {
                    Log.d("MQTT", "✅ Connected")

                    // after first connect, start watching DB
                    serviceScope.launch {
                        AppDatabase.getDatabase(applicationContext)
                            .deviceDao()
                            .getEnabledDevices()      // Flow<List<Device>>
                            .collect { enabledList ->
                                // cancel & clear old timers/subs
                                mqttManager.clearPeriodicReads()

                                if (enabledList.isEmpty()) {
                                    Log.d("MQTT", "ℹ No enabled devices")
                                    return@collect
                                }

                                enabledList.forEach { device ->
                                    val readTopic  = "${device.deviceId}READ"
                                    val writeTopic = "${device.deviceId}WRITE"
                                    val payload    = "SN${device.deviceId}E"

                                    mqttManager.subscribe(readTopic)
                                    mqttManager.subscribe(writeTopic)
                                    mqttManager.startPeriodicRead(
                                        topic = readTopic,
                                        payload = payload,
                                        intervalSec = device.interval
                                    )
                                }
                            }
                    }
                },

                onMessage = { topic, payload ->
                    // raw log so you know what really arrived
                    Log.d("MQTT", "📥 topic=$topic  payload=$payload")

                    // optional parse
                    val parsed = parsePayload(payload)

                    // file debug (optional)
                    File(applicationContext.filesDir, "mqtt_data.txt")
                        .appendText("$parsed\n")

                    // broadcast to whoever cares
                    LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(
                        Intent("MQTT_DEVICE_DATA").apply {
                            putExtra("deviceId", topic.removeSuffix("READ").removeSuffix("WRITE"))
                            putExtra("payload", payload)
                        }
                    )
                },
                onDisconnected = {
                    Log.w("MQTT", "⚠ disconnected")
                }
            )
        }
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_MANUAL_PUBLISH) {
            val deviceId = intent.getStringExtra(EXTRA_DEVICE_ID)
            val payload  = intent.getStringExtra(EXTRA_PAYLOAD)
            if (!deviceId.isNullOrEmpty() && !payload.isNullOrEmpty()) {
                sendManualRead(deviceId, payload)
            }
        }
        return START_STICKY
    }

    /** Publish over the existing MQTT connection */
    fun sendManualRead(deviceId: String, command: String) {
        val topic = "${deviceId}READ"
        mqttManager.publish(topic, command)
        Log.d("MQTT", "📤 Manual publish → topic=$topic payload=$command")
    }


    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
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
    companion object {
        const val ACTION_MANUAL_PUBLISH = "ACTION_MANUAL_PUBLISH"
        const val EXTRA_DEVICE_ID = "deviceId"
        const val EXTRA_PAYLOAD = "payload"
    }
}
/*private val publishRunnable = object : Runnable {
        override fun run() {
            // Send request every 30 sec
            mqttManager.publish("1303202510000READ", "SN1303202510000E")
            Log.d("MQTT-SERVICE", "📤 Sent periodic request")
            handler.postDelayed(this, interval)
        }
    }*/

/*override fun onCreate() {
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
}*/
/*override fun onCreate() {
        super.onCreate()
        mqttManager = MqttManager(this)
        startForegroundServiceNotification()

        serviceScope.launch {
            val enabledDevices = AppDatabase.getDatabase(applicationContext)
                .deviceDao()
                .getEnabledDevices()

            mqttManager.connect(
                onConnected = {
                    enabledDevices.forEach { device ->
                        val readTopic  = "${device.deviceId}READ"
                        val writeTopic = "${device.deviceId}WRITE"
                        val payload    = "SN${device.deviceId}E"

                        mqttManager.subscribe(readTopic)
                        mqttManager.subscribe(writeTopic)

                        mqttManager.startPeriodicRead(
                            topic = readTopic,
                            payload = payload,
                            intervalSec = device.interval
                        )
                    }
                },
                onMessage = { topic, payload ->
                    Log.d("MQTT-SERVICE", "📥 Raw → $payload")
                    // TODO implement parsePayload()
                    File(applicationContext.filesDir, "mqtt_data.txt").appendText("$payload\n")
                    sendBroadcast(Intent("MQTT_MESSAGE").putExtra("payload", payload))
                },
                onDisconnected = { Log.w("MQTT-SERVICE", "⚠ Disconnected") }
            )
        }
    }*/
/*override fun onCreate() {
    super.onCreate()
    mqttManager = MqttManager(this)
    startForegroundServiceNotification()

    // Launch on IO dispatcher so Room query is off the main thread
    CoroutineScope(Dispatchers.IO).launch {
        // take one snapshot of all enabled devices
        val enabledDevices: List<Device> = AppDatabase
            .getDatabase(applicationContext)
            .deviceDao()
            .getEnabledDevices()      // returns Flow<List<Device>>
            .first()                  // take first emission, convert Flow -> List

        // switch back to main for MQTT connect
        withContext(Dispatchers.Main) {
            mqttManager.connect(
                onConnected = {
                    enabledDevices.forEach { device ->

                        val readTopic  = "${device.deviceId}READ"
                        val writeTopic = "${device.deviceId}WRITE"
                        val payload    = "SN${device.deviceId}E"

                        mqttManager.subscribe(readTopic)
                        mqttManager.subscribe(writeTopic)

                        mqttManager.startPeriodicRead(
                            topic = readTopic,
                            payload = payload,
                            intervalSec = device.interval  // interval from DB
                        )
                    }
                },
                onMessage = { topic, payload ->
                    Log.d("MQTT-SERVICE", "📥 Raw → $payload")
                    val parsed = parsePayload(payload)
                    File(applicationContext.filesDir, "mqtt_data.txt")
                        .appendText("$parsed\n")
                    sendBroadcast(Intent("MQTT_MESSAGE").putExtra("payload", payload))
                },
                onDisconnected = { Log.w("MQTT-SERVICE", "⚠ Disconnected") }
            )
        }
    }
}*/
/*onMessage = { topic, payload ->
                    Log.d("MQTT", "📥 Raw → $payload")
                    // parse + persist
                    val parsed = parsePayload(payload)
                    File(applicationContext.filesDir, "mqtt_data.txt")
                        .appendText("$parsed\n")

                    // optional UI broadcast
                    sendBroadcast(
                        Intent("MQTT_MESSAGE").putExtra("payload", payload)
                    )
                },*/
/* onMessage = { topic, payload ->
     // parse the payload
     val parsed = parsePayload(payload)

     // normalize the topic -> just the device id
     val deviceId = topic.removeSuffix("READ").removeSuffix("WRITE")

     // broadcast to whoever wants it (Activity/ViewModel)
     LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(
         Intent("MQTT_DEVICE_DATA").apply {
             putExtra("deviceId", deviceId)
             putExtra("payload", payload)
         }
     )

     // you could also update a repository singleton instead of broadcasting
 },*/
/*override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(publishRunnable)
        mqttManager.disconnect()
    }*/
