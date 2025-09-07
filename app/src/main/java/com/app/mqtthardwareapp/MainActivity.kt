package com.app.mqtthardwareapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.app.mqtthardwareapp.Data.AppDatabase
import com.app.mqtthardwareapp.Screens.DeviceRegScreen
import com.app.mqtthardwareapp.Screens.HomeScreen
import com.app.mqtthardwareapp.Theme.MqttHardwareAppTheme
import com.app.mqtthardwareapp.ViewModels.DeviceRepository
import com.app.mqtthardwareapp.ViewModels.DeviceViewModel
import com.app.mqtthardwareapp.ViewModels.DeviceViewModelFactory


/*class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start your background service
        val serviceIntent = Intent(this, MqttBackgroundService::class.java)
        startForegroundService(serviceIntent)
        val database = AppDatabase.getDatabase(this)
        val repo = DeviceRepository(database.deviceDao())
        setContent {
            MqttHardwareAppTheme {
                val navController = rememberNavController()
                AppNavigation(navController, repo)
            }
        }

    }
}*/
/*class MainActivity : ComponentActivity() {

    // hold onto the viewModel so the receiver can update it
    private lateinit var deviceViewModel: DeviceViewModel

    // --- broadcast receiver for MQTT messages ---
    private val mqttReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            val id   = intent.getStringExtra("deviceId") ?: return
            val raw  = intent.getStringExtra("payload") ?: return
            val parsed = parsePayload(raw)          // your parse logic
            deviceViewModel.updateDeviceData(id, parsed) // push into ViewModel
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // spin up service
        val serviceIntent = Intent(this, MqttBackgroundService::class.java)
        startForegroundService(serviceIntent)

        // build repository & ViewModel once so both UI and receiver share the same instance
        val database = AppDatabase.getDatabase(this)
        val repo = DeviceRepository(database.deviceDao())
        deviceViewModel = DeviceViewModelFactory(repo).create(DeviceViewModel::class.java)

        setContent {
            MqttHardwareAppTheme {
                val navController = rememberNavController()
                AppNavigation(navController, repo)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter("MQTT_DEVICE_DATA")
        LocalBroadcastManager.getInstance(this).registerReceiver(mqttReceiver, filter)
    }

    override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mqttReceiver)
    }
}*/
class MainActivity : ComponentActivity() {

    private lateinit var deviceViewModel: DeviceViewModel

    // receiver for MQTT broadcast
    private val mqttReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            val id  = intent.getStringExtra("deviceId")
            val raw = intent.getStringExtra("payload")

            Log.d("MQTT_UI", "Broadcast received → id=$id raw=$raw")

            if (id.isNullOrBlank() || raw.isNullOrBlank()) {
                Log.w("MQTT_UI", "Missing id or payload, ignoring…")
                return
            }

            val parsed = parsePayload(raw)
            Log.d("MQTT_UI", "Parsed data for $id → $parsed")

            deviceViewModel.updateDeviceData(id, parsed)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // spin up service
        val serviceIntent = Intent(this, MqttBackgroundService::class.java)
        startForegroundService(serviceIntent)
        Log.d("MQTT_UI", "Started foreground service")

        // build repository & shared ViewModel
        val database = AppDatabase.getDatabase(this)
        val repo = DeviceRepository(database.deviceDao())
        val mqttManager = MqttManager(applicationContext)
        deviceViewModel = ViewModelProvider(this, DeviceViewModelFactory(repo,mqttManager))
            .get(DeviceViewModel::class.java)

        setContent {
            MqttHardwareAppTheme {
                val navController = rememberNavController()
                AppNavigation(navController, repo,mqttManager = mqttManager)
            }
        }

        Log.d("MQTT_UI", "MainActivity created, waiting for broadcasts")
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter("MQTT_DEVICE_DATA")
        LocalBroadcastManager.getInstance(this).registerReceiver(mqttReceiver, filter)
        Log.d("MQTT_UI", "Registered mqttReceiver for MQTT_DEVICE_DATA")
    }

    override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mqttReceiver)
        Log.d("MQTT_UI", "Unregistered mqttReceiver")
    }
}

@Composable
fun AppNavigation(navController: NavHostController,repo: DeviceRepository,  mqttManager: MqttManager) {
    val viewModel: DeviceViewModel = viewModel(
        factory = DeviceViewModelFactory(repo,mqttManager) // custom factory if needed
    )
    NavHost(navController = navController, startDestination = "home") {
        composable("home") { HomeScreen(
            navController = navController,
            onMachineClick = {navController.navigate("device_reg")},
            viewModel = viewModel)}
        composable("device_reg") {
            DeviceRegScreen(
                navController = navController,
                repo = repo,
                viewModel = viewModel
            ) }
    }
}



