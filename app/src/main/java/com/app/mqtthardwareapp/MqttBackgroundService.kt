package com.app.mqtthardwareapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.app.mqtthardwareapp.Data.AppDatabase
import com.app.mqtthardwareapp.Data.Device
import com.app.mqtthardwareapp.Utils.AlarmHelper
import com.app.mqtthardwareapp.Utils.NotificationHelper
import com.app.mqtthardwareapp.Utils.PrefsHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.forEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/*class MqttBackgroundService : Service() {

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
}*/

class MqttBackgroundService : Service() {
    private lateinit var mqttManager: MqttManager

    // … existing fields
    private lateinit var connectivityManager: ConnectivityManager
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()

        mqttManager = MqttManager(this)
        startForegroundServiceNotification()
        NotificationHelper.createChannels(this)

        connectivityManager = getSystemService(ConnectivityManager::class.java)
        registerNetworkCallback()

        // try first connect (may fail silently if offline)
        tryConnect()
    }

    private fun registerNetworkCallback() {
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d("MQTT", "🌐 Network available, reconnecting…")
                tryConnect()
            }

            override fun onLost(network: Network) {
                Log.w("MQTT", "🚫 Network lost")
            }
        }
        connectivityManager.registerDefaultNetworkCallback(networkCallback!!)
    }

    private fun tryConnect() {
        serviceScope.launch {
            mqttManager.connect(
                onConnected = {
                    Log.d("MQTT", "✅ Connected")
                    collectEnabledDevices()
                },
                onMessage = { topic, payload -> handleIncoming(topic, payload) },
                onDisconnected = { Log.w("MQTT", "⚠ Disconnected") }
            )
        }
    }

    private fun collectEnabledDevices() = serviceScope.launch {
        AppDatabase.getDatabase(applicationContext)
            .deviceDao()
            .getEnabledDevices()
            .collect { enabledList ->
                mqttManager.clearPeriodicReads()
                enabledList.forEach { device ->
                    val read = "${device.deviceId}READ"
                    val write = "${device.deviceId}WRITE"
                    val payload = "SN${device.deviceId}E"
                    mqttManager.subscribe(read)
                    mqttManager.subscribe(write)
                    mqttManager.startPeriodicRead(read, payload, device.interval)
                }
            }
    }

    /*private fun handleIncoming(topic: String, payload: String) {
        Log.d("MQTT", "📥 $topic → $payload")
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(
            Intent("MQTT_DEVICE_DATA").apply {
                putExtra("deviceId", topic.removeSuffix("READ").removeSuffix("WRITE"))
                putExtra("payload", payload)
            }
        )
    }*/
    private fun handleIncoming(topic: String, payload: String) {
        Log.d("MQTT", "📥 $topic → $payload")

        val deviceId = topic.removeSuffix("READ").removeSuffix("WRITE")
        val values = payload.split(":") // adjust if payload format is different

        val finalFinish = values.getOrNull(8)?.toIntOrNull()
        val beforeFinish = values.getOrNull(9)?.toIntOrNull()
        val lowPressure = values.getOrNull(10)?.toIntOrNull()
        val lowSpeed = values.getOrNull(11)?.toIntOrNull()
        Log.d(
            "MQTT_SERVICE",
            "📢 Parsed values → device=$deviceId finalFinish=$finalFinish beforeFinish=$beforeFinish lowPressure=$lowPressure lowSpeed=$lowSpeed"
        )

        // Reports
        if (finalFinish == 1) {
            showNotification(
                deviceId,
                "✅ Process Finished",
                "Device $deviceId: The process has completed.",
                CHANNEL_REPORTS
            )
        }

        // Before finish notification
        if (beforeFinish == 1) {
            showNotification(
                deviceId,
                "⏳ Before Finish",
                "Device $deviceId: The process is about to finish.",
                CHANNEL_REPORTS
            )
        }

        // Low pressure alarm
        if (lowPressure == 1) {
            showAlarmNotification(
                deviceId,
                "Pressure dropped below safe level!"
            )
        }

// Low speed alarm
        if (lowSpeed == 1) {
            showAlarmNotification(
                deviceId,
                "Speed dropped below threshold!"
            )
        }

        // still broadcast for UI updates
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(
            Intent("MQTT_DEVICE_DATA").apply {
                putExtra("deviceId", deviceId)
                putExtra("payload", payload)
            }
        )
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
        networkCallback?.let { connectivityManager.unregisterNetworkCallback(it) }
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
        const val CHANNEL_REPORTS = "reports_channel"
        const val CHANNEL_ALARMS = "alarms_channel"
        fun startIfClientIdSet(context: Context) {
            val clientId = PrefsHelper.getClientId(context)
            if (clientId.isNotBlank()) {
                val intent = Intent(context, MqttBackgroundService::class.java)
                ContextCompat.startForegroundService(context, intent)
                Log.d("MQTT", "▶️ Service started with ClientId=$clientId")
            } else {
                Log.w("MQTT", "⚠️ Service not started, ClientId missing")
            }
        }
    }
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val reportChannel = NotificationChannel(
                CHANNEL_REPORTS,
                "Device Reports",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Notifications for device reports" }

            val alarmChannel = NotificationChannel(
                CHANNEL_ALARMS,
                "Device Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical alarms from devices"
                enableVibration(true)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(reportChannel)
            manager.createNotificationChannel(alarmChannel)
        }
    }
  /*  private fun showNotification(deviceId: String, title: String, message: String, channelId: String) {
        Log.d("MQTT_SERVICE", "🔔 Building notification → $title : $message")
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("deviceId", deviceId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            deviceId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(
                if (channelId == CHANNEL_ALARMS) NotificationCompat.PRIORITY_MAX
                else NotificationCompat.PRIORITY_DEFAULT
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify((System.currentTimeMillis() % 10000).toInt(), notification)
        Log.d("MQTT_SERVICE", "✅ Notification sent for device=$deviceId")
    }*/
  private fun showNotification(deviceId: String, title: String, message: String, channelId: String) {
      val intent = Intent(this, MainActivity::class.java).apply {
          flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
          putExtra("deviceId", deviceId)
      }

      val pendingIntent = PendingIntent.getActivity(
          this,
          deviceId.hashCode(),
          intent,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )

      val notification = NotificationCompat.Builder(this, channelId)
          .setSmallIcon(android.R.drawable.ic_dialog_info)
          .setContentTitle(title)
          .setContentText(message)
          .setPriority(
              if (channelId == NotificationHelper.CHANNEL_ALARMS)
                  NotificationCompat.PRIORITY_MAX
              else
                  NotificationCompat.PRIORITY_DEFAULT
          )
          .setContentIntent(pendingIntent)
          .setAutoCancel(true)
          .build()

      val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      manager.notify((System.currentTimeMillis() % 10000).toInt(), notification)
  }
    private fun showAlarmNotification(deviceId: String, message: String) {
        // Start looping alarm sound
        AlarmHelper.startAlarm(this)

        // Intent to stop alarm
        val stopIntent = Intent(this, AlarmStopReceiver::class.java).apply {
            putExtra("deviceId", deviceId)
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ALARMS)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠️ Alarm: Device $deviceId")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true) // keeps notification until user stops
            .addAction(android.R.drawable.ic_media_pause, "Stop Alarm", stopPendingIntent)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(deviceId.hashCode(), notification)
    }









    // … rest of your class
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
