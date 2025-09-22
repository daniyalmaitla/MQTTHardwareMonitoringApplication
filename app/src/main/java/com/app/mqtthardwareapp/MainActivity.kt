package com.app.mqtthardwareapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.app.mqtthardwareapp.Utils.PrefsHelper
import com.app.mqtthardwareapp.ViewModels.DeviceRepository
import com.app.mqtthardwareapp.ViewModels.DeviceViewModel
import com.app.mqtthardwareapp.ViewModels.DeviceViewModelFactory
import java.util.Locale
import android.provider.Settings


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
    companion object {
        val LocalAppLocale = compositionLocalOf { Locale("en") }
    }

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

            /*deviceViewModel.updateDeviceData(id, parsed)*/
            deviceViewModel.updateDeviceData(applicationContext, id, parsed)

            PrefsHelper.saveDeviceData(applicationContext, id, parsed)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ignoreBatteryOptimizations(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
        val deviceId = intent.getStringExtra("deviceId")
        if (!deviceId.isNullOrEmpty()) {
            Log.d("MQTT", "🔔 Notification tapped for device $deviceId")

        }



        MqttBackgroundService.startIfClientIdSet(this)

        Log.d("MQTT_UI", "Started foreground service")

        // build repository & shared ViewModel
        val database = AppDatabase.getDatabase(this)
        val repo = DeviceRepository(database.deviceDao())
        val savedId = PrefsHelper.getClientId(this)
        if (savedId.isNotBlank()) {

            deviceViewModel = ViewModelProvider(this, DeviceViewModelFactory(repo))
                .get(DeviceViewModel::class.java)
        } else {

            Log.w("MQTT_UI", "⚠ No ClientId set yet, skipping mqttManager setup")
        }


        deviceViewModel = ViewModelProvider(this, DeviceViewModelFactory(repo))
            .get(DeviceViewModel::class.java)

        setContent {
            var locale by remember { mutableStateOf(Locale("en")) }

            CompositionLocalProvider(LocalAppLocale provides locale) {
                MqttHardwareAppTheme {
                    val navController = rememberNavController()
                    val startDeviceId = intent.getStringExtra("deviceId")
                    AppNavigation(
                        navController,
                        repo,
                        startDeviceId,
                        onChangeLanguage = { langCode ->
                            locale = Locale(langCode)
                        }
                    )
                }
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
private fun ignoreBatteryOptimizations(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val packageName = context.packageName
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:$packageName")
            )
            context.startActivity(intent)
        }
    }
}


@Composable
fun AppNavigation(navController: NavHostController,repo: DeviceRepository,startDeviceId: String?, onChangeLanguage: (String) -> Unit) {
    val viewModel: DeviceViewModel = viewModel(
        factory = DeviceViewModelFactory(repo) // custom factory if needed
    )
    NavHost(navController = navController, startDestination = "home") {
        composable("home") { HomeScreen(
            navController = navController,
            onMachineClick = {navController.navigate("device_reg")},
            viewModel = viewModel,
            startDeviceId = startDeviceId)}
        composable("device_reg") {
            DeviceRegScreen(
                navController = navController,
                repo = repo,
                viewModel = viewModel,
                onChangeLanguage = onChangeLanguage

            ) }
    }
}



