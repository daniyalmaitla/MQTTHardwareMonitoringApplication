package com.app.mqtthardwareapp

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
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
import android.provider.Settings
import androidx.core.content.ContextCompat.getSystemService
import kotlinx.coroutines.delay


class MqttBackgroundService : Service() {
    private lateinit var mqttManager: MqttManager
    private val lastStates = mutableMapOf<String, MutableMap<String, Int>>()
    private val acknowledgedAlarms = mutableMapOf<String, MutableMap<String, Boolean>>()


    // … existing fields
    private lateinit var connectivityManager: ConnectivityManager
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private fun safeLog(tag: String, msg: String) {
        try { Log.d(tag, msg) } catch (_: Exception) {}
    }
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
    private var wakeLock: PowerManager.WakeLock? = null

     fun acquireWakeLock(timeoutMs: Long = 30_000L) {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (wakeLock == null) {
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MqttApp::SharedWakeLock")
            wakeLock?.setReferenceCounted(false)
        }
        if (wakeLock?.isHeld == false) {
            wakeLock?.acquire(timeoutMs)
        }
    }

    fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        } catch (e: Exception) { }
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

    /*private fun tryConnect() {
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
    }*/
    private fun tryConnect() {
        serviceScope.launch {
            try {
                mqttManager.connect(
                    onConnected = {
                        safeLog("MQTT", "✅ Connected (onConnected called)")
                        // start collector once connected
                        collectEnabledDevices()
                    },
                    onMessage = { topic, payload ->
                        safeLog("MQTT_RX", "onMessage → $topic : $payload")
                        handleIncoming(topic, payload)
                    },
                    onDisconnected = {
                        safeLog("MQTT", "⚠ Disconnected (callback)")
                    }
                )
                safeLog("MQTT", "connect() returned (called)")
            } catch (ex: Exception) {
                Log.e("MQTT", "connect() failed: ${ex.message}", ex)
            }
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
    /*private fun collectEnabledDevices() = serviceScope.launch {
        safeLog("MQTT", "collectEnabledDevices() started, subscribing DB flow...")
        AppDatabase.getDatabase(applicationContext)
            .deviceDao()
            .getEnabledDevices()
            .collect { enabledList ->
                safeLog("MQTT", "DB emitted enabledList size=${enabledList.size}")
                // DON'T clear periodic reads until we confirm scheduling (but keep behavior as-is for now)
                mqttManager.clearPeriodicReads()
                safeLog("MQTT", "Cleared previous periodic reads")
                enabledList.forEach { device ->
                    val read = "${device.deviceId}READ"
                    val write = "${device.deviceId}WRITE"
                    val payload = "SN${device.deviceId}E"
                    safeLog("MQTT", "Scheduling device=${device.deviceId} interval=${device.interval}")
                    mqttManager.subscribe(read)
                    mqttManager.subscribe(write)
                    // Log inside mqttManager.startPeriodicRead should exist; if not, we log here
                    mqttManager.startPeriodicRead(read, payload, device.interval)
                    safeLog("MQTT", "Started periodic read for $read (payload=$payload)")
                }
            }
    }*/

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
        /*if (lowPressure == 1) {
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
        }*/
        val deviceStates = lastStates.getOrPut(deviceId) { mutableMapOf() }
        checkAndTriggerAlarm(deviceId, "lowPressure", lowPressure, "Pressure dropped below safe level!", deviceStates)
        checkAndTriggerAlarm(deviceId, "lowSpeed", lowSpeed, "Speed dropped below threshold!", deviceStates)



        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(
            Intent("MQTT_DEVICE_DATA").apply {
                putExtra("deviceId", deviceId)
                putExtra("payload", payload)
            }
        )
    }
    /*private fun checkAndTriggerAlarm(
        deviceId: String,
        field: String,
        currentValue: Int?,
        message: String,
        deviceStates: MutableMap<String, Int>
    ) {
        if (currentValue == null) return

        val lastValue = deviceStates[field] ?: 0
        if (lastValue == 0 && currentValue == 1) {
            // transition 0 -> 1 → trigger alarm
            showAlarmNotification(deviceId, message)
        }

        // update stored state
        deviceStates[field] = currentValue
    }*/
    private fun checkAndTriggerAlarm(
        deviceId: String,
        field: String,
        currentValue: Int?,
        message: String,
        deviceStates: MutableMap<String, Int>
    ) {
        if (currentValue == null) return

        val lastValue = deviceStates[field] ?: 0
        val acknowledged = AlarmState.isAcknowledged(deviceId, field)

        if (currentValue == 1) {
            if (!acknowledged) {

                Log.d("MQTT_SERVICE", "🔥 Alarm active device=$deviceId field=$field")
                showAlarmNotification(deviceId, field, message)
            } else {
               Log.d("MQTT_SERVICE", "🚫 Alarm suppressed (user stopped) device=$deviceId field=$field")
            }
        }


        if (lastValue == 1 && currentValue == 0) {
           Log.d("MQTT_SERVICE", "🔄 Reset ack device=$deviceId field=$field")
            AlarmState.clearAcknowledgement(deviceId, field)
        }

        // update stored state
        deviceStates[field] = currentValue
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
    override fun onTaskRemoved(rootIntent: Intent?) {
        val restartServiceIntent = Intent(applicationContext, MqttBackgroundService::class.java)
        restartServiceIntent.setPackage(packageName)

        val restartPendingIntent = PendingIntent.getService(
            applicationContext,
            1,
            restartServiceIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                try {
                    alarmManager.setExact(
                        AlarmManager.ELAPSED_REALTIME,
                        SystemClock.elapsedRealtime() + 1000, // restart after 1s
                        restartPendingIntent
                    )
                } catch (se: SecurityException) {
                    Log.e("MQTT_SERVICE", "Exact alarm not permitted: ${se.message}")
                }
            } else {
                Log.w("MQTT_SERVICE", "App not allowed to schedule exact alarms. Consider fallback.")
            }
        } else {
            // Older Android versions don’t require permission
            alarmManager.setExact(
                AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + 1000,
                restartPendingIntent
            )
        }

        super.onTaskRemoved(rootIntent)
    }


    /** Publish over the existing MQTT connection */
    fun sendManualRead(deviceId: String, command: String) {
        val topic = "${deviceId}READ"
        mqttManager.publish(topic, command)
        Log.d("MQTT", "📤 Manual publish → topic=$topic payload=$command")
    }



    override fun onDestroy() {
        super.onDestroy()
        releaseWakeLock()
        serviceScope.cancel()
        mqttManager.disconnect()
        networkCallback?.let { connectivityManager.unregisterNetworkCallback(it) }
    }
    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundServiceNotification() {
        val channelId = "mqtt_service_channel"
        val channelName = "MQTT Background Service"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(chan)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Cloud Service Running")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentText("Fetching data...")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .build()

        startForeground(1, notification)
    }
    /*private fun startForegroundServiceNotification() {
        val channelId = "mqtt_service_channel"
        val channelName = "MQTT Background Service"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(
                channelId, channelName, NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "MQTT background service (keeps device polling)"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(chan)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("MQTT Service Running")
            .setContentText("Fetching data every configured interval")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setOngoing(true) // make it sticky
            .setOnlyAlertOnce(true)
            .build()

        startForeground(1, notification)
        safeLog("MQTT", "startForeground called with high importance notification")
    }*/


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
    /*private fun showAlarmNotification(deviceId: String, message: String) {
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
    }*/

    private fun showAlarmNotification(deviceId: String, field: String, message: String) {
        android.util.Log.d("MQTT_SERVICE", "🔔 showAlarmNotification -> device=$deviceId field=$field message=$message")

        // Start looping alarm sound
        AlarmHelper.startAlarm(this)

        // Intent to stop alarm (with deviceId + field)
        val stopIntent = Intent(this, AlarmStopReceiver::class.java).apply {
            putExtra("deviceId", deviceId)
            putExtra("field", field)
        }

        val requestCode = (deviceId + field).hashCode()
        val stopPendingIntent = PendingIntent.getBroadcast(
            this,
            requestCode,
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
        // use device+field unique id so stopping cancels the correct notification
        manager.notify(requestCode, notification)
    }










}




