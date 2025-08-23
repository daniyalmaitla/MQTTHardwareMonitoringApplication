package com.app.mqtthardwareapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi




class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start your background service
        val serviceIntent = Intent(this, MqttBackgroundService::class.java)
        startForegroundService(serviceIntent)

    }
}

/*@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MqttMessageScreen() {
    var mqttMessage by remember { mutableStateOf("Waiting for MQTT data...") }
    val context = LocalContext.current

    // Register a broadcast receiver
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val payload = intent?.getStringExtra("payload") ?: ""
                mqttMessage = payload
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter("MQTT_MESSAGE"),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("MQTT Data Viewer") })
        }
    ) { padding ->
        Text(
            text = "Latest Payload:\n$mqttMessage",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(padding).padding(16.dp)
        )
    }
}*/
