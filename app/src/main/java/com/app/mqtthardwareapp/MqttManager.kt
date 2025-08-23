package com.app.mqtthardwareapp

import android.content.Context
import android.util.Log
import info.mqtt.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*

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


class MqttManager(context: Context) {

    private val serverUri = "tcp://mqtt.pndsn.com:1883"

    private val clientId =
        "pub-c-6a38f739-6472-425e-a522-a5ec55e3a06e/sub-c-2e445fc0-405d-11ec-b2c1-a25c7fcd9558/POSITRON"

    private val mqttClient = MqttAndroidClient(context, serverUri, clientId)

    fun connect(onConnected: () -> Unit, onMessage: (String, String) -> Unit) {
        val options = MqttConnectOptions().apply {
            isCleanSession = false            // keep session/subscriptions after reconnect
            isAutomaticReconnect = true       // auto-reconnect if connection drops
            connectionTimeout = 10
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
}

