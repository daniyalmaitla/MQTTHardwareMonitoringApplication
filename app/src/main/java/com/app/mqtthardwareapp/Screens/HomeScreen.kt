package com.app.mqtthardwareapp.Screens

import android.R.id.bold
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.modifier.modifierLocalConsumer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.app.mqtthardwareapp.MqttBackgroundService
import com.app.mqtthardwareapp.R
import com.app.mqtthardwareapp.Theme.MqttHardwareAppTheme
import com.app.mqtthardwareapp.ViewModels.DeviceViewModel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.text.input.KeyboardType


@Composable
fun HomeScreen(
    navController: NavController,
    onMachineClick : () ->Unit,
    viewModel: DeviceViewModel

){
    val state by viewModel.state.collectAsState()
    var showDuplicateDialog by remember { mutableStateOf(false) }
    var duplicateDeviceId by remember { mutableStateOf("") }
    val enabledDevices by viewModel.enabledDevices.collectAsState()
    val selected by viewModel.selectedDevice.collectAsState()
    val device by viewModel.selectedDevice.collectAsState()
    val deviceDataMap by viewModel.deviceDataMap.collectAsState()

    val currentData = selected?.let { deviceDataMap[it.deviceId] }

    val context = LocalContext.current
    val isConnected by rememberConnectivityState()
    var showNoInternetDialog by remember { mutableStateOf(!isConnected) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(isConnected) {
        showNoInternetDialog = !isConnected
    }


    val barcodeLauncher = rememberLauncherForActivityResult(
        contract = ScanContract(),
        onResult = { result ->
            if (result.contents != null) {
                val scannedValue = result.contents.trim()


                val exists = state.devices.any { it.deviceId == scannedValue }

                if (exists) {
                    duplicateDeviceId = scannedValue
                    showDuplicateDialog = true
                } else {
                    viewModel.addDeviceFromQr(scannedValue)
                }
            }
        }
    )
    Scaffold(topBar = {HomeTopbar(onMachineClick = onMachineClick, onQrClick = {

        val options = ScanOptions()
        options.setPrompt("Scan Device QR")
        options.setBeepEnabled(true)
        options.setOrientationLocked(false)
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)

        barcodeLauncher.launch(options)
    },)},
        bottomBar = {bottomBar(
            onDelete = { showDeleteDialog = true }
        )}){ paddingValues->
        Column (modifier = Modifier.padding(paddingValues).padding(12.dp).verticalScroll(
            rememberScrollState()
        )){

            DeviceSelectorField(
                placeholder = "Select Device",
                suggestions = enabledDevices.map { it.name },
                selectedDevice = selected?.name ?: "Select Device",
                onDeviceSelected = { name ->

                    enabledDevices.firstOrNull { it.name == name }?.let {
                        viewModel.selectDevice(it)
                    }
                }
            )
            Spacer(modifier = Modifier.height(10.dp))

           SpeedSettingRow( speed = String.format("%.1f", currentData?.speedSetting ?: 0.0),
               onBtnClick = { entered ->
                   val dev = selected ?: return@SpeedSettingRow
                   val command = "CS$entered"

                   // build an intent for the service
                   val intent = Intent(context, MqttBackgroundService::class.java).apply {
                       action = MqttBackgroundService.ACTION_MANUAL_PUBLISH
                       putExtra(MqttBackgroundService.EXTRA_DEVICE_ID, dev.deviceId)
                       putExtra(MqttBackgroundService.EXTRA_PAYLOAD, command)
                   }
                   context.startService(intent)
               }
               )
            Spacer(modifier = Modifier.height(10.dp))
            Fields(
               icon = painterResource(R.drawable.download_speed),
                label = "CURRENT SPEED",
                value = String.format("%.2f", currentData?.currentSpeed ?: 0.00),
                unit = "m/h",
                iconRight = painterResource(R.drawable.download_speed)
            )
            Spacer(modifier = Modifier.height(10.dp))
            DistanceRow(
                distance = String.format("%.2f", currentData?.remainingDistance ?: 0.00),
                onReset = {
                    selected?.let { dev ->
                        val intent = Intent(context, MqttBackgroundService::class.java).apply {
                            action = MqttBackgroundService.ACTION_MANUAL_PUBLISH
                            putExtra(MqttBackgroundService.EXTRA_DEVICE_ID, dev.deviceId)
                            putExtra(MqttBackgroundService.EXTRA_PAYLOAD, "D!")
                        }
                        context.startService(intent)
                    }
                }
            )
            Spacer(modifier = Modifier.height(10.dp))
            Fields(
                icon = painterResource(R.drawable.download_speed),
                label = "REMAINING TIME",
                value = String.format("%.2f", currentData?.remainingTime ?: 0.00),
                unit = "h",
                iconRight = painterResource(R.drawable.download_speed)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Fields(
                icon = painterResource(R.drawable.battery_3713201),
                label = "BATTERY VOLTAGE",
                value = String.format("%.1f", currentData?.batteryVoltage ?: 0.0),
                unit = "V",
                iconRight = painterResource(R.drawable.download_speed)
            )
            Spacer(modifier = Modifier.height(10.dp))
            PressureSettingRow(value = String.format("%.1f", currentData?.pressure ?: 0.0),
                limit = String.format("%d", currentData?.pressureLimit ?: 0),
                onBtnClick = { entered ->
                    val dev = selected ?: return@PressureSettingRow
                    val command = "PL$entered"


                    val intent = Intent(context, MqttBackgroundService::class.java).apply {
                        action = MqttBackgroundService.ACTION_MANUAL_PUBLISH
                        putExtra(MqttBackgroundService.EXTRA_DEVICE_ID, dev.deviceId)
                        putExtra(MqttBackgroundService.EXTRA_PAYLOAD, command)
                    }
                    context.startService(intent)
                })


            Spacer(modifier = Modifier.height(10.dp))


            AutomaticReports(
                value = currentData?.automaticReportsEnable ?: 0,
                onSwitchChange = { newState ->
                    val dev = selected ?: return@AutomaticReports
                    val command = if (newState) "EN1" else "EN0"

                    val intent = Intent(context, MqttBackgroundService::class.java).apply {
                        action = MqttBackgroundService.ACTION_MANUAL_PUBLISH
                        putExtra(MqttBackgroundService.EXTRA_DEVICE_ID, dev.deviceId)
                        putExtra(MqttBackgroundService.EXTRA_PAYLOAD, command)
                    }
                    context.startService(intent)
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            AdvanceControlRow(
                value =  currentData?.mainValveStatus ?: 0,
                onSwitchChange = { newState ->
                    val dev = selected ?: return@AdvanceControlRow
                    val command = if (newState) "V1" else "V0"

                    val intent = Intent(context, MqttBackgroundService::class.java).apply {
                        action = MqttBackgroundService.ACTION_MANUAL_PUBLISH
                        putExtra(MqttBackgroundService.EXTRA_DEVICE_ID, dev.deviceId)
                        putExtra(MqttBackgroundService.EXTRA_PAYLOAD, command)
                    }
                    context.startService(intent)
                }
            )

            Spacer(modifier = Modifier.height(10.dp))
            UUIDField(uuid = device?.deviceId ?: "No Device")
            if (showDuplicateDialog) {
                AlertDialog(
                    onDismissRequest = { showDuplicateDialog = false },
                    confirmButton = {
                        TextButton(onClick = { showDuplicateDialog = false }) {
                            Text("OK")
                        }
                    },
                    title = { Text("Duplicate Device") },
                    text = { Text("Device with ID $duplicateDeviceId already exists.") }
                )
            }
            if (showNoInternetDialog) {
                AlertDialog(
                    onDismissRequest = { showNoInternetDialog = false },  // allow closing
                    title = { Text("No Internet") },
                    text = { Text("You are offline. Data cannot be fetched until a network is available.") },
                    confirmButton = {
                        TextButton(onClick = { showNoInternetDialog = false }) {
                            Text("OK")
                        }
                    }
                )
            }
            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text("Delete Device") },
                    text = { Text("Are you sure you want to delete this device?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.deleteSelectedDevice()
                                showDeleteDialog = false
                            }
                        ) {
                            Text("OK")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showDeleteDialog = false }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }


    }
}

@Composable
fun HomeTopbar(
    onQrClick: () -> Unit = {},
    onMachineClick : ()-> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.colorScheme.primary)
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // LEFT ICON
        Image(
            painter = painterResource(id = R.drawable.positron),
            contentDescription = "Positron Logo",
            modifier = Modifier
                .size(50.dp)
                .clickable { onMachineClick() },
            colorFilter = null
        )



        // CENTER TITLE
        Column(
            modifier = Modifier
                .weight(1f) // push center to middle
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "POSITRON",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "IRRIGATION  MASTER",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Blue,
                letterSpacing = 2.sp
            )
        }

        // RIGHT QR ACTION
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable { onQrClick() } // whole area clickable
                .padding(start = 8.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_qr_code), // your QR code icon
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = Color.Unspecified
            )
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceSelectorField(

    placeholder: String,
    suggestions: List<String>,
    selectedDevice: String,
    onDeviceSelected: (String) -> Unit
) {
    val displayText = if (selectedDevice.isBlank()) placeholder else selectedDevice
    var dialogOpen by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(selectedDevice) }
    Row(
        modifier = Modifier
            .fillMaxWidth() ,// Whole row clickable
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Cloud Icon
        Icon(
            painter = painterResource(R.drawable.cloud_platform),
            contentDescription = "Device Icon",
            modifier = Modifier.size(40.dp)
        )
   Spacer(modifier = Modifier.width(5.dp))
    Box(
        modifier = Modifier
            .weight(1f)
            .clickable { dialogOpen = true } // Whole box is clickable
    ) {
        OutlinedTextField(
            value = displayText,
            onValueChange = { }, // not editable
            placeholder = { Text(placeholder) },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Dropdown"
                )
            },
            readOnly = true,
            enabled = false, // disables input focus
            modifier = Modifier
                .fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                disabledTextColor = LocalContentColor.current,
                disabledPlaceholderColor = LocalContentColor.current.copy(alpha = 0.6f)
            )
        )
    }
        Spacer(modifier = Modifier.width(5.dp))

        Icon(
            painter = painterResource(R.drawable.down_arrow),
            contentDescription = "Dropdown",
            modifier = Modifier.size(40.dp)
        )
    }

    // Dialog with radio buttons
    if (dialogOpen) {
        AlertDialog(
            onDismissRequest = { dialogOpen = false },
            confirmButton = {
                TextButton(onClick = { dialogOpen = false }) {
                    Text("OK", fontSize = 20.sp)
                }
            },
            title = { Text("Choose Device") },
            text = {
                Column {
                    suggestions.forEach { device ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selected = device
                                    onDeviceSelected(device)
                                }
                                .padding(8.dp)
                        ) {
                            RadioButton(
                                selected = (selected == device),
                                onClick = {
                                    selected = device
                                    onDeviceSelected(device)
                                }
                            )
                            Text(
                                text = device,
                                fontSize = 18.sp,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        )
    }
}
@Composable
fun DeviceField(
    modifier: Modifier = Modifier,
    icon: Painter,
    label: String,
    value: String,
    unit: String? = null,
    backgroundColor: Color = MaterialTheme.colorScheme.primary,
    valueColor: Color = Color.Blue,
    unitColor: Color = Color.Black,
    iconRight: Painter,
    onRightIconClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Icon
        Icon(
            painter = icon,
            contentDescription = label,
            modifier = Modifier
                .size(40.dp)
                .padding(end = 8.dp),
            tint = Color.Unspecified
        )

        // Label + Value
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black
            )
        }

        // Value + Unit
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = valueColor
            )
            Spacer(modifier = Modifier.width(10.dp))

            if (unit != null) {
                Text(
                    text = " $unit",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,

                    style = MaterialTheme.typography.bodyMedium,
                    color = unitColor
                )
            }
            Spacer(modifier = Modifier.width(25.dp))
            Icon(
                painter = iconRight,
                contentDescription = label,
                modifier = Modifier
                    .size(40.dp)
                    .padding(end = 8.dp) .clickable { onRightIconClick() },
                tint = Color.Unspecified
            )

        }
    }
}
/*@Composable
fun SpeedSettingRow(speed : String, onBtnClick: (String) -> Unit  ) {
    var showSpeedField by remember { mutableStateOf(false) }
    var speedInput by remember { mutableStateOf("") }
    val isError = speedInput.isBlank()


    Column {
        // Current Speed Row
        DeviceField(
            icon = painterResource(R.drawable.optimization),
            label = "SPEED SETTING",
            value = speed,
            unit = "m/h",
            iconRight = painterResource(R.drawable.down_arrow),
            onRightIconClick = {
                showSpeedField = !showSpeedField
            }
        )

        // Appears only when icon is clicked
        if (showSpeedField) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth().background(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Icon
                Icon(
                    painter = painterResource(R.drawable.download_speed), // replace with your icon
                    contentDescription = "Speed",
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Label
                Text(
                    text = "CHANGE SPEED",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.width(80.dp),
                    style = MaterialTheme.typography.bodyMedium
                )

                // Input field
                OutlinedTextField(
                    value = speedInput,
                    onValueChange = { speedInput = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("ENTER SPEED") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = if (isError) Color.Red else MaterialTheme.colorScheme.surfaceContainer,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceContainer,
                        cursorColor = Color.Black
                    )
                )

                Spacer(modifier = Modifier.width(25.dp))


                Button(
                    onClick = {
                        if (!isError) {
                            onBtnClick(speedInput)
                            showSpeedField = false
                        }
                    },
                    enabled = !isError,
                    modifier = Modifier
                        .height(50.dp)
                        .width(50.dp)
                        .border(1.dp, Color.Gray, RoundedCornerShape(2.dp)),
                    shape = RoundedCornerShape(2.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isError) Color.LightGray
                        else MaterialTheme.colorScheme.surfaceContainer
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("OK", color = Color.Black, fontSize = 12.sp)
                }
            }
        }
    }
}*/
@Composable
fun SpeedSettingRow(
    speed: String,
    onBtnClick: (String) -> Unit
) {
    var showSpeedField by remember { mutableStateOf(false) }
    var speedInput by remember { mutableStateOf("") }
    var showErrorDialog by remember { mutableStateOf(false) }

    Column {
        // Current Speed Row
        DeviceField(
            icon = painterResource(R.drawable.optimization),
            label = "SPEED SETTING",
            value = speed,
            unit = "m/h",
            iconRight = painterResource(R.drawable.down_arrow),
            onRightIconClick = {
                showSpeedField = !showSpeedField
            }
        )

        // Appears only when icon is clicked
        if (showSpeedField) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Icon
                Icon(
                    painter = painterResource(R.drawable.download_speed),
                    contentDescription = "Speed",
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Label
                Text(
                    text = "CHANGE SPEED",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.width(80.dp),
                    style = MaterialTheme.typography.bodyMedium
                )

                // Input field
                OutlinedTextField(
                    value = speedInput,
                    onValueChange = { input ->
                        // Allow only numbers
                        if (input.all { it.isDigit() } && input.length <= 2) {
                            speedInput = input
                        }
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("ENTER SPEED") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = MaterialTheme.colorScheme.surfaceContainer,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceContainer,
                        cursorColor = Color.Black
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(modifier = Modifier.width(25.dp))

                Button(
                    onClick = {
                        val inputInt = speedInput.toIntOrNull()
                        if (inputInt == null || inputInt !in 1..99) {
                            showErrorDialog = true
                        } else {
                            // Pad with zero if single digit (e.g., 6 -> 06)
                            val formattedSpeed = inputInt.toString().padStart(2, '0')
                            onBtnClick(formattedSpeed)
                            showSpeedField = false
                        }
                    },
                    modifier = Modifier
                        .height(50.dp)
                        .width(50.dp)
                        .border(1.dp, Color.Gray, RoundedCornerShape(2.dp)),
                    shape = RoundedCornerShape(2.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("OK", color = Color.Black, fontSize = 12.sp)
                }
            }
        }

        // Error dialog
        if (showErrorDialog) {
            AlertDialog(
                onDismissRequest = { showErrorDialog = false },
                confirmButton = {
                    TextButton(onClick = { showErrorDialog = false }) {
                        Text("OK")
                    }
                },
                title = { Text("Invalid Speed") },
                text = { Text("Speed must be between 1 and 99.") }
            )
        }
    }
}

/*@Composable
fun PressureSettingRow(value:String,limit:String,onBtnClick: (String) -> Unit) {
    var showField by remember { mutableStateOf(false) }
    var newValue by remember { mutableStateOf("") }
    val isError = newValue.isBlank()
    val isValid = newValue.length == 2 && newValue.all { it.isDigit() }

    Column {
        // Current Pressure Row (collapsible trigger)
        DeviceField(
            icon = painterResource(R.drawable.pressure_meter),
            label = "PRESSURE",
            value = value,
            unit = "bars",
            iconRight = painterResource(R.drawable.down_arrow),
            onRightIconClick = {
                showField = !showField
            }
        )

        if (showField) {
            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    .padding(8.dp)
            ) {
                // Icon + Label above read-only field
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.pressure_meter),
                        contentDescription = "Pressure Icon",
                        modifier = Modifier.size(24.dp),
                        tint = Color.Unspecified
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PRESSURE UPPER LIMIT",
                        fontSize = 15.sp,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black
                    )
                }

                // Read-only field with current value
                OutlinedTextField(
                    value = limit,
                    onValueChange = { }, // Read-only
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    readOnly = true,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledContainerColor = Color.White,
                        disabledTextColor = Color.Black,
                        disabledBorderColor = Color.Gray
                    ),
                    enabled = false
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Editable input field
                    OutlinedTextField(
                        value = newValue,
                        onValueChange = { input ->
                            // strip out anything that isn't a digit and keep at most 2
                            newValue = input.filter { it.isDigit() }.take(2)
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text("Enter new value", color = Color.White,
                            modifier = Modifier.background(color = Color.Unspecified)) },
                        placeholder = { Text("00") },
                        isError = newValue.isNotEmpty() && !isValid,   // red only when text exists but not exactly 2
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = if (isValid || newValue.isEmpty()) Color.Gray else Color.Red,
                            unfocusedBorderColor = if (isValid || newValue.isEmpty()) Color.Gray else Color.Red,
                            cursorColor = Color.Black
                        )
                    )

                    Spacer(Modifier.width(16.dp))

                    Button(
                        onClick = {
                            if (!isError) {

                                newValue = ""      // clear
                                onBtnClick(newValue)
                                showField = false  // collapse
                            }

                        },
                        enabled = isValid || !isError,
                        modifier = Modifier
                            .height(50.dp)
                            .width(50.dp)
                            .border(1.dp, Color.Gray, RoundedCornerShape(2.dp)),
                        shape = RoundedCornerShape(2.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isValid) MaterialTheme.colorScheme.surfaceContainer else Color.LightGray
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("OK", color = Color.Black, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}*/
@Composable
fun PressureSettingRow(
    value: String,
    limit: String,
    onBtnClick: (String) -> Unit
) {
    var showField by remember { mutableStateOf(false) }
    var newValue by remember { mutableStateOf("") }
    var showErrorDialog by remember { mutableStateOf(false) }

    Column {
        // Current Pressure Row (collapsible trigger)
        DeviceField(
            icon = painterResource(R.drawable.pressure_meter),
            label = "PRESSURE",
            value = value,
            unit = "bars",
            iconRight = painterResource(R.drawable.down_arrow),
            onRightIconClick = {
                showField = !showField
            }
        )

        if (showField) {
            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    .padding(8.dp)
            ) {
                // Label
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.pressure_meter),
                        contentDescription = "Pressure Icon",
                        modifier = Modifier.size(24.dp),
                        tint = Color.Unspecified
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PRESSURE UPPER LIMIT",
                        fontSize = 15.sp,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black
                    )
                }

                // Read-only field with current value
                OutlinedTextField(
                    value = limit,
                    onValueChange = { }, // Read-only
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    readOnly = true,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledContainerColor = Color.White,
                        disabledTextColor = Color.Black,
                        disabledBorderColor = Color.Gray
                    ),
                    enabled = false
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Editable input field
                    OutlinedTextField(
                        value = newValue,
                        onValueChange = { input ->
                            newValue = input.filter { it.isDigit() }.take(2)
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = {
                            Text(
                                "Enter new value",
                                color = Color.White,
                                modifier = Modifier.background(color = Color.Unspecified)
                            )
                        },
                        placeholder = { Text("00") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            cursorColor = Color.Black
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    Spacer(Modifier.width(16.dp))

                    Button(
                        onClick = {
                            val inputInt = newValue.toIntOrNull()
                            if (inputInt == null || inputInt !in 1..15) {
                                showErrorDialog = true
                            } else {
                                // Format: pad with leading zero if < 10
                                val formatted = inputInt.toString().padStart(2, '0')
                                onBtnClick(formatted)   // send "01".."15"
                                newValue = ""           // clear after sending
                                showField = false       // collapse
                            }
                        },
                        modifier = Modifier
                            .height(50.dp)
                            .width(50.dp)
                            .border(1.dp, Color.Gray, RoundedCornerShape(2.dp)),
                        shape = RoundedCornerShape(2.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("OK", color = Color.Black, fontSize = 12.sp)
                    }
                }
            }
        }

        // Error dialog
        if (showErrorDialog) {
            AlertDialog(
                onDismissRequest = { showErrorDialog = false },
                confirmButton = {
                    TextButton(onClick = { showErrorDialog = false }) {
                        Text("OK")
                    }
                },
                title = { Text("Invalid Pressure") },
                text = { Text("Pressure must be between 1 and 15.") }
            )
        }
    }
}
@Composable
fun AutomaticReports(
    value: Int, // 0 or 1 directly from payload
    onSwitchChange: (Boolean) -> Unit
) {
    // Local state for immediate UI response
    var switchState by remember { mutableStateOf(value == 1) }

    // Update state whenever payload changes
    LaunchedEffect(value) {
        switchState = (value == 1)
    }

    val displayText = if (switchState) "ON" else "OFF"

    Column {
        DeviceFieldWithSwitch(
            modifier = Modifier.background(MaterialTheme.colorScheme.primary),
            icon = painterResource(id = R.drawable.notification),
            label = "AUTOMATIC REPORTS",
            value = displayText,
            switchState = switchState,
            onSwitchChange = { newState ->
                switchState = newState // update immediately for smooth UI
                onSwitchChange(newState) // send command to device
            }
        )
    }
}




@Composable
fun AdvanceControlRow(
    value: Int, // 0 or 1 from device
    onSwitchChange: (Boolean) -> Unit
) {
    var showField by remember { mutableStateOf(false) }

    val switchState = value == 1
    val displayText = if (switchState) "OPEN" else "CLOSED"

    Column {
        DeviceField(
            icon = painterResource(R.drawable.advance_control),
            label = "ADVANCE CONTROL",
            value = "",
            unit = "",
            iconRight = painterResource(R.drawable.down_arrow),
            onRightIconClick = { showField = !showField }
        )

        if (showField) {
            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                    .padding(8.dp)
            ) {
                DeviceFieldWithSwitch(
                    icon = painterResource(R.drawable.pneumatic),
                    label = "MAIN VALVE CONTROL STATUS",
                    value = displayText,
                    switchState = switchState,
                    onSwitchChange = { newState ->
                        onSwitchChange(newState) // just send command
                    }
                )
            }
        }
    }
}





@Composable
fun DistanceRow(distance:String,
                onReset: () -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    var selectedOption by remember { mutableStateOf("Option 1") }

    Column {
        // Current Speed Row
        DeviceField(
            icon = painterResource(R.drawable.travel),
            label = "REMAINING DISTANCE",
            value = distance,
            unit = "m",
            iconRight = painterResource(R.drawable.arrows_13170375),
            onRightIconClick = {
                showDialog = !showDialog
            }
        )

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Reset Remaining Distance") },
                text = {
                    Text(
                        "Are you sure you want to reset the remaining distance?\n" +
                                "After resetting, you will only be able to change the values from the hardware device."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onReset()
                            showDialog = false
                        }
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDialog = false }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

    }
}


@Composable
fun Fields(
    modifier: Modifier = Modifier,
    icon: Painter,
    label: String,
    value: String,
    unit: String? = null,
    backgroundColor: Color = MaterialTheme.colorScheme.primary,
    valueColor: Color = Color.Blue,
    unitColor: Color = Color.Black,
    iconRight : Painter

) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Icon
        Icon(
            painter = icon,
            contentDescription = label,
            modifier = Modifier
                .size(40.dp)
                .padding(end = 8.dp),
            tint = Color.Unspecified
        )

        // Label + Value
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }

        // Value + Unit
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = valueColor
            )
            Spacer(modifier = Modifier.width(10.dp))

            if (unit != null) {
                Text(
                    text = " $unit",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = unitColor
                )
            }
            Spacer(modifier = Modifier.width(25.dp))
            Icon(
                painter = iconRight,
                contentDescription = label,
                modifier = Modifier
                    .size(40.dp)
                    .padding(end = 8.dp).alpha(0f),
                tint = Color.Unspecified,
            )

        }
    }
}
@Composable
fun DeviceFieldWithSwitch(
    modifier: Modifier = Modifier,
    icon: Painter,
    label: String,
    value: String,
    valueColor: Color = Color.Blue,
    switchState: Boolean,
    onSwitchChange: (Boolean) -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Icon
        Icon(
            painter = icon,
            contentDescription = label,
            modifier = Modifier
                .size(40.dp)
                .padding(end = 8.dp),
            tint = Color.Unspecified
        )

        // Label + Value in same line, label wraps if long
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black,
                modifier = Modifier.weight(1f), // let it wrap
                maxLines = Int.MAX_VALUE,
                overflow = TextOverflow.Visible
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = valueColor
            )
        }
        Spacer(modifier = Modifier.width(25.dp))

        // Switch
        Switch(
            checked = switchState,
            onCheckedChange = { onSwitchChange(it) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color.Green,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color.Gray
            )
        )
    }
}
@Composable
fun UUIDField( uuid: String    ) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.colorScheme.primary)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Icon
        Icon(
            painter = painterResource(R.drawable.cloud_platform),
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .padding(end = 8.dp),
            tint = Color.Unspecified
        )

        // Label + Value
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "UUID",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black
            )
        }

        // Value + Unit
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = uuid,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Black,
                modifier = Modifier
                    .background(color = Color.White.copy(alpha = 0.3f), shape = RectangleShape).padding(8.dp)
            )

            Spacer(modifier = Modifier.width(25.dp))
            Icon(
                painter = painterResource(R.drawable.cloud_platform),
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .padding(end = 8.dp).alpha(0f),
                tint = Color.Unspecified,
            )

        }
    }
}
@Composable
fun bottomBar(onDelete: ()->Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.colorScheme.surfaceContainer).statusBarsPadding(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.padding(12.dp) .clickable { onDelete() },
            horizontalAlignment = Alignment.CenterHorizontally, // center icon & text horizontally
            verticalArrangement = Arrangement.Center // center content vertically in bottom bar
        ) {
            Icon(
                painter = painterResource(R.drawable.trash),
                contentDescription = null,
                modifier = Modifier.size(60.dp), // 👈 reduced to fit in bar
                tint = Color.Unspecified
            )
            Text(
                text = "Delete Device",
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 4.dp) // small spacing instead of Spacer(15dp)
            )
        }
    }
}
@Composable
fun rememberConnectivityState(): State<Boolean> {
    val context = LocalContext.current
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val networkInfo = cm.activeNetworkInfo
    val initial = networkInfo?.isConnectedOrConnecting == true
    val state = produceState(initialValue = initial) {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { value = true }
            override fun onLost(network: Network) { value = false }
        }
        val request = NetworkRequest.Builder().build()
        cm.registerNetworkCallback(request, callback)
        awaitDispose { cm.unregisterNetworkCallback(callback) }
    }
    return state
}

@Preview(showBackground = true)
@Composable
fun PreviewHome(){
    MqttHardwareAppTheme {

    }

}