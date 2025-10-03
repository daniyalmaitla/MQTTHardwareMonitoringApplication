package com.app.mqtthardwareapp

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import com.app.mqtthardwareapp.Utils.PrefsHelper
import info.mqtt.android.service.MqttAndroidClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import org.eclipse.paho.client.mqttv3.*
import java.util.UUID
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.isActive
import javax.net.ssl.SSLContext
import android.app.AlarmManager
import android.os.Build
import android.os.SystemClock
import org.json.JSONObject

import javax.net.ssl.SSLSocketFactory









class MqttManager(

    private val context: Context,
    /*private val serverUri: String = "tcp://ce47707f.ala.dedicated.gcp.emqxcloud.com:1883",
    private val username: String = "positron",
    private val password: String = "positron",*/


) {

    private val config: JSONObject by lazy {
        try {
            JSONObject(PrefsHelper.getServerDetails(context))
        } catch (e: Exception) {
            JSONObject(PrefsHelper.DEFAULT_SERVER_JSON)
        }
    }

    private var serverUri: String = config.optString("serverUri")
    private var username: String = config.optString("username")
    private var password: String = config.optString("password")


    init {

        val maskedPassword = if (password.isNotEmpty()) "*".repeat(password.length) else ""
        Log.d("MqttManager", "Loaded from Prefs -> serverUri: $serverUri, username: $username, password: $maskedPassword")
    }


   private val clientId: String by lazy {
       PrefsHelper.getClientId(context).ifBlank {
           throw IllegalStateException("❌ Client ID is missing! User must set it before connecting.")
       }
   }
    private var mqttClient = MqttClient(serverUri, clientId, null)
    fun reloadConfig(
        onConnected: () -> Unit,
        onMessage: (String, String) -> Unit,
        onDisconnected: () -> Unit
    ) {
        try {
            val newConfig = JSONObject(PrefsHelper.getServerDetails(context))
            val newServerUri = newConfig.optString("serverUri")
            val newUsername = newConfig.optString("username")
            val newPassword = newConfig.optString("password")

            if (newServerUri != serverUri || newUsername != username || newPassword != password) {
                Log.d("MQTT", "🔄 Config changed → reconnecting...")

                // close old client
                try {
                    mqttClient.setCallback(null)
                    if (mqttClient.isConnected) mqttClient.disconnectForcibly(1000, 1000)
                    mqttClient.close()
                } catch (e: Exception) { }

                // rebuild client
                val newClientId = PrefsHelper.getClientId(context)
                mqttClient = MqttClient(newServerUri, newClientId, null)

                serverUri = newServerUri
                username = newUsername
                password = newPassword

                // reconnect with service callbacks
                connect(onConnected, onMessage, onDisconnected)
            }
        } catch (e: Exception) {
            Log.e("MQTT", "❌ Failed to reload config: ${e.message}", e)
        }
    }





    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /** one periodic job per topic */
    private val periodicJobs = mutableMapOf<String, Job>()



    fun connect(
        onConnected: () -> Unit,
        onMessage: (String, String) -> Unit,
        onDisconnected: () -> Unit
    ) {
        val options = MqttConnectOptions().apply {
            userName = username
            password = this@MqttManager.password.toCharArray()
            isCleanSession = false
            isAutomaticReconnect = true
            connectionTimeout = 10
            keepAliveInterval = 20
        }

        mqttClient.setCallback(object : MqttCallback {
            override fun messageArrived(topic: String?, message: MqttMessage?) {
                val payload = message?.toString() ?: ""
                if (topic != null) onMessage(topic, payload)
            }
            override fun connectionLost(cause: Throwable?) { onDisconnected() }
            override fun deliveryComplete(token: IMqttDeliveryToken?) {}
        })

        scope.launch {
            try {
                mqttClient.connect(options)
                withContext(Dispatchers.Main) { onConnected() }
            } catch (e: Exception) {
                Log.e("MQTT", "❌ Connect error ${e.message}", e)
            }
        }
    }

    fun subscribe(topic: String) = scope.launch {
        try {
            mqttClient.subscribe(topic, 1)
            Log.d("MQTT", "✅ Subscribed to $topic")
        } catch (e: Exception) {
            Log.e("MQTT", "❌ Subscribe failed ${e.message}")
        }
    }

    fun unsubscribe(topic: String) = scope.launch {
        try {
            mqttClient.unsubscribe(topic)
            Log.d("MQTT", "🚫 Unsubscribed $topic")
        } catch (e: Exception) {
            Log.e("MQTT", "❌ Unsubscribe failed ${e.message}")
        }
    }

    fun publish(topic: String, message: String) = scope.launch {
        try {
            mqttClient.publish(topic, MqttMessage(message.toByteArray()).apply {
                qos = 1; isRetained = false
            })
            Log.d("MQTT", "📤 $topic → $message")
        } catch (e: Exception) {
            Log.e("MQTT", "❌ Publish error ${e.message}")
        }
    }

    // ------------------- periodic read control ------------------- //


    fun startPeriodicRead(topic: String, payload: String, intervalMs: Long) {
        periodicJobs[topic]?.cancel()

        periodicJobs[topic] = scope.launch(Dispatchers.IO) {
            try {
                while (isActive) {
                    publish(topic, payload)
                    delay(intervalMs)
                }
            } finally {
                Log.d("MQTT", "⏹ Periodic job for $topic stopped")
            }
        }

        Log.d("MQTT", "⏱ Started periodic read for $topic every ${intervalMs} ms")
    }





    fun stopPeriodicRead(topic: String) {
        periodicJobs[topic]?.cancel()
        periodicJobs.remove(topic)
        Log.d("MQTT", "⏹ Stopped periodic read for $topic")
    }

    /** stop & forget everything (use when DB snapshot changes) */
    fun clearPeriodicReads() {
        periodicJobs.values.forEach { it.cancel() }
        periodicJobs.clear()
        Log.d("MQTT", "🧹 Cleared all periodic reads")
    }

    fun disconnect() {
        scope.launch {
            clearPeriodicReads()
            try {
                mqttClient.disconnect()
                Log.d("MQTT", "🔌 Disconnected")
            } catch (e: Exception) {
                Log.e("MQTT", "❌ Disconnect error ${e.message}")
            }
        }
    }
}




