package com.app.mqtthardwareapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
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


class MainActivity : ComponentActivity() {

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
}
@Composable
fun AppNavigation(navController: NavHostController,repo: DeviceRepository) {
    val viewModel: DeviceViewModel = viewModel(
        factory = DeviceViewModelFactory(repo) // custom factory if needed
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



