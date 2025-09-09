package com.app.mqtthardwareapp

import android.content.Context
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

import javax.net.ssl.SSLSocketFactory




/*class MqttManager(context: Context) {

    private val serverUri = "tcp://mqtt.pndsn.com:1883"

    private val clientId =
        "pub-c-6a38f739-6472-425e-a522-a5ec55e3a06e/sub-c-2e445fc0-405d-11ec-b2c1-a25c7fcd9558/POSITRON"

    private val mqttClient = MqttAndroidClient(context, serverUri, clientId)

    fun connect(onConnected: () -> Unit, onMessage: (String, String) -> Unit) {
        val options = MqttConnectOptions().apply {
            isCleanSession = true
            connectionTimeout = 10
            keepAliveInterval = 20
        }

        mqttClient.connect(options, null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                println("✅ Connected to PubNub MQTT broker")

                mqttClient.setCallback(object : MqttCallback {
                    override fun messageArrived(topic: String?, message: MqttMessage?) {
                        val payload = message?.toString() ?: ""
                        println("📩 Message on $topic → $payload")
                        if (topic != null) {
                            onMessage(topic, payload)
                        }
                    }
                    override fun connectionLost(cause: Throwable?) {
                        println("⚠ Connection lost: ${cause?.message}")
                    }
                    override fun deliveryComplete(token: IMqttDeliveryToken?) {}
                })

                onConnected()
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                println("❌ Failed to connect: ${exception?.message}")
                exception?.printStackTrace()
            }
        })
    }

    fun subscribe(topic: String) {
        mqttClient.subscribe(topic, 0, null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                println("✅ Subscribed to $topic")
            }
            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                println("❌ Failed to subscribe: ${exception?.message}")
            }
        })
    }

    fun publish(topic: String, message: String) {
        try {
            val mqttMessage = MqttMessage(message.toByteArray())
            mqttClient.publish(topic, mqttMessage)
            println("📤 Published → Topic: $topic | Message: $message")
        } catch (e: Exception) {
            e.printStackTrace()
            println("❌ Publish error: ${e.message}")
        }
    }
}*/


/*class MqttManager(context: Context) {

    private val serverUri = "tcp://mqtt.pndsn.com:1883"

    private val clientId =
        "pub-c-6a38f739-6472-425e-a522-a5ec55e3a06e/sub-c-2e445fc0-405d-11ec-b2c1-a25c7fcd9558/POSITRON"

    private val mqttClient = MqttAndroidClient(context, serverUri, clientId)

    fun connect(onConnected: () -> Unit, onMessage: (String, String) -> Unit) {
        val options = MqttConnectOptions().apply {
            isCleanSession = false            // keep session/subscriptions after reconnect
            isAutomaticReconnect = true       // auto-reconnect if connection drops
            connectionTimeout = 20
            keepAliveInterval = 30            // send ping every 60s
        }

        mqttClient.setCallback(object : MqttCallback {
            override fun messageArrived(topic: String?, message: MqttMessage?) {
                val payload = message?.toString() ?: ""
                Log.d("MQTT", "📩 Message on $topic → $payload")
                if (topic != null) {
                    onMessage(topic, payload)
                }
            }

            override fun connectionLost(cause: Throwable?) {
                Log.w("MQTT", "⚠ Connection lost: ${cause?.message}")
                try {
                    mqttClient.reconnect()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                Log.d("MQTT", "✅ Delivery complete")
            }
        })

        mqttClient.connect(options, null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                Log.d("MQTT", "✅ Connected to PubNub MQTT broker")
                onConnected()
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                Log.e("MQTT", "❌ Failed to connect: ${exception?.message}")
                exception?.printStackTrace()
            }
        })
    }

    fun subscribe(topic: String) {
        mqttClient.subscribe(topic, 0, null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                Log.d("MQTT", "✅ Subscribed to $topic")
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                Log.e("MQTT", "❌ Failed to subscribe: ${exception?.message}")
            }
        })
    }

    fun publish(topic: String, message: String) {
        try {
            val mqttMessage = MqttMessage(message.toByteArray())
            mqttClient.publish(topic, mqttMessage)
            Log.d("MQTT", "📤 Published → Topic: $topic | Message: $message")
        } catch (e: Exception) {
            Log.e("MQTT", "❌ Publish error: ${e.message}")
            e.printStackTrace()
        }
    }

    fun disconnect() {
        try {
            mqttClient.disconnect()
            Log.d("MQTT", "🔌 Disconnected")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}*/


/*class MqttManager(context: Context) {


    private val serverUri =
        "ssl://72d72cbaf385433cb30548e8ad5a4f69.s1.eu.hivemq.cloud:8883"


    private val clientId = "George1"

    private val mqttClient = MqttAndroidClient(context, serverUri, clientId)

    fun connect(onConnected: () -> Unit, onMessage: (String, String) -> Unit) {
        val options = MqttConnectOptions().apply {
            userName = "Positron1"
            password = "Positron1".toCharArray()
            isCleanSession = false
            isAutomaticReconnect = true
            connectionTimeout = 30
            keepAliveInterval = 60
            socketFactory = SSLSocketFactory.getDefault() // TLS required
        }

        mqttClient.setCallback(object : MqttCallback {
            override fun messageArrived(topic: String?, message: MqttMessage?) {
                val payload = message?.toString() ?: ""
                Log.d("MQTT", "📩 Message on $topic → $payload")
                if (topic != null) {
                    onMessage(topic, payload)
                }
            }

            override fun connectionLost(cause: Throwable?) {
                Log.e("MQTT", "Connection lost (safely handled): ${cause?.message}")
                // Do NOT let it bubble to service (which tries Parcelable cast)
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                Log.d("MQTT", "✅ Delivery complete")
            }
        })

        mqttClient.connect(options, null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                Log.d("MQTT", "✅ Connected to HiveMQ Cloud broker")
                onConnected()
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                Log.e("MQTT", "❌ Failed to connect: ${exception?.message}")
                exception?.printStackTrace()
            }
        })
    }

    fun subscribe(topic: String) {
        mqttClient.subscribe(topic, 0, null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                Log.d("MQTT", "✅ Subscribed to $topic")
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                Log.e("MQTT", "❌ Failed to subscribe: ${exception?.message}")
            }
        })
    }

    fun publish(topic: String, message: String) {
        try {
            val mqttMessage = MqttMessage(message.toByteArray())
            mqttClient.publish(topic, mqttMessage)
            Log.d("MQTT", "📤 Published → Topic: $topic | Message: $message")
        } catch (e: Exception) {
            Log.e("MQTT", "❌ Publish error: ${e.message}")
            e.printStackTrace()
        }
    }

    fun disconnect() {
        try {
            mqttClient.disconnect()
            Log.d("MQTT", "🔌 Disconnected")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}*/

/*class MqttManager(context: Context) {

    // ✅ New EMQX Cloud broker details
    private val serverUri = "tcp://ce47707f.ala.dedicated.gcp.emqxcloud.com:1883"

    private val clientId = "App Developers"

    private val mqttClient = MqttAndroidClient(context, serverUri, clientId)

    fun connect(onConnected: () -> Unit, onMessage: (String, String) -> Unit) {
        val options = MqttConnectOptions().apply {
            userName = "positron"
            password = "positron".toCharArray()
            isCleanSession = false
            isAutomaticReconnect = true
            connectionTimeout = 20
            keepAliveInterval = 30
        }

        mqttClient.setCallback(object : MqttCallback {
            override fun messageArrived(topic: String?, message: MqttMessage?) {
                val payload = message?.toString() ?: ""
                Log.d("MQTT", "📩 Message on $topic → $payload")
                if (topic != null) {
                    onMessage(topic, payload)
                }
            }

            override fun connectionLost(cause: Throwable?) {
                Log.w("MQTT", "⚠ Connection lost: ${cause?.message}")
                try {
                    mqttClient.reconnect()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                Log.d("MQTT", "✅ Delivery complete")
            }
        })

        mqttClient.connect(options, null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                Log.d("MQTT", "✅ Connected to EMQX Cloud broker")
                onConnected()
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                Log.e("MQTT", "❌ Failed to connect: ${exception?.message}")
                exception?.printStackTrace()
            }
        })
    }

    fun subscribe(topic: String) {
        mqttClient.subscribe(topic, 1, null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                Log.d("MQTT", "✅ Subscribed to $topic")
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                Log.e("MQTT", "❌ Failed to subscribe: ${exception?.message}")
            }
        })
    }

    fun publish(topic: String, message: String) {
        try {
            val mqttMessage = MqttMessage(message.toByteArray()).apply {
                qos = 1
                isRetained = false
            }
            mqttClient.publish(topic, mqttMessage)
            Log.d("MQTT", "📤 Published → Topic: $topic | Message: $message")
        } catch (e: Exception) {
            Log.e("MQTT", "❌ Publish error: ${e.message}")
            e.printStackTrace()
        }
    }

    fun disconnect() {
        try {
            mqttClient.disconnect()
            Log.d("MQTT", "🔌 Disconnected")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}*/
/*class MqttManager(
    private val context: Context,
    private val serverUri: String = "tcp://ce47707f.ala.dedicated.gcp.emqxcloud.com:1883",
    private val username: String = "positron",
    private val password: String = "positron"
) {
    private val clientId = "App Developers" + UUID.randomUUID().toString()
    private val mqttClient: MqttClient =
        MqttClient(serverUri, clientId, null)

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var readJob: Job? = null

    fun connect(
        onConnected: () -> Unit,
        onMessage: (String, String) -> Unit,
        onDisconnected: () -> Unit
    ) {
        val options = MqttConnectOptions().apply {
            userName = username
            password = this@MqttManager.password.toCharArray()
            isCleanSession = false   // keep subscriptions
            isAutomaticReconnect = true
            connectionTimeout = 10
            keepAliveInterval = 20
        }

        mqttClient.setCallback(object : MqttCallback {
            override fun messageArrived(topic: String?, message: MqttMessage?) {
                val payload = message?.toString() ?: ""
                Log.d("MQTT", "📩 $topic → $payload")
                if (topic != null) onMessage(topic, payload)
            }

            override fun connectionLost(cause: Throwable?) {
                Log.w("MQTT", "⚠ Lost: ${cause?.message}")
                onDisconnected()
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                Log.d("MQTT", "✅ Delivery complete")
            }
        })

        scope.launch {
            try {
                mqttClient.connect(options)
                Log.d("MQTT", "✅ Connected as $clientId")
                withContext(Dispatchers.Main) { onConnected() }
            } catch (e: Exception) {
                Log.e("MQTT", "❌ Connect error: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun subscribe(topic: String) {
        scope.launch {
            try {
                mqttClient.subscribe(topic, 1)
                Log.d("MQTT", "✅ Subscribed to $topic")
            } catch (e: Exception) {
                Log.e("MQTT", "❌ Subscribe failed: ${e.message}")
            }
        }
    }

    fun publish(topic: String, message: String) {
        scope.launch {
            try {
                val mqttMessage = MqttMessage(message.toByteArray()).apply {
                    qos = 1
                    isRetained = false
                }
                mqttClient.publish(topic, mqttMessage)
                Log.d("MQTT", "📤 $topic → $message")
            } catch (e: Exception) {
                Log.e("MQTT", "❌ Publish error: ${e.message}")
            }
        }
    }


    fun startPeriodicRead(topic: String, payload: String, intervalSec: Long = 10) {
        stopPeriodicRead()
        readJob = scope.launch {
            while (isActive) {
                publish(topic, payload)
                delay(intervalSec * 1000)
            }
        }
        Log.d("MQTT", "⏱ Started periodic READ every $intervalSec sec")
    }

    fun stopPeriodicRead() {
        readJob?.cancel()
        readJob = null
        Log.d("MQTT", "⏹ Stopped periodic READ")
    }

    fun disconnect() {
        scope.launch {
            try {
                stopPeriodicRead()
                mqttClient.disconnect()
                Log.d("MQTT", "🔌 Disconnected")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}*/

class MqttManager(
    private val context: Context,
    private val serverUri: String = "tcp://ce47707f.ala.dedicated.gcp.emqxcloud.com:1883",
    private val username: String = "positron",
    private val password: String = "positron"
) {
   /* private val clientId = "AppDevelopers-" + UUID.randomUUID()*/
   private val clientId: String by lazy {
       PrefsHelper.getClientId(context).ifBlank {
           throw IllegalStateException("❌ Client ID is missing! User must set it before connecting.")
       }
   }
    private val mqttClient = MqttClient(serverUri, clientId, null)


    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /** one periodic job per topic */
    private val periodicJobs = mutableMapOf<String, Job>()

    // ------------------- connect / subscribe / publish ------------------- //

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

    /** start or restart a timer for exactly one topic */
    fun startPeriodicRead(topic: String, payload: String, intervalSec: Long) {
        // cancel any existing job for this topic
        periodicJobs[topic]?.cancel()

        periodicJobs[topic] = scope.launch {
            while (isActive) {
                publish(topic, payload)
                delay(intervalSec * 1000)
            }
        }
        Log.d("MQTT", "⏱ Started periodic read for $topic every $intervalSec sec")
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






