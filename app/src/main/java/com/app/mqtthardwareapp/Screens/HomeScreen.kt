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
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.window.Dialog
import com.app.mqtthardwareapp.Data.Device
import com.app.mqtthardwareapp.DeviceData
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.text.style.TextAlign


@Composable
fun HomeScreen(
    navController: NavController,
    onMachineClick : () ->Unit,
    viewModel: DeviceViewModel,
    onCloudClick:()->Unit,
    startDeviceId: String? = null

){
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    var showDuplicateDialog by remember { mutableStateOf(false) }
    var duplicateDeviceId by remember { mutableStateOf("") }
    val enabledDevices by viewModel.enabledDevices.collectAsState()
    val selected by viewModel.selectedDevice.collectAsState()
    val device by viewModel.selectedDevice.collectAsState()
    val deviceDataMap by viewModel.deviceDataMap.collectAsState()
    val enabledDeviceView = viewModel.enabledDevicesWithData.collectAsState().value
    LaunchedEffect(startDeviceId, enabledDevices) {
        if (!startDeviceId.isNullOrEmpty()) {
            enabledDevices.firstOrNull { it.deviceId == startDeviceId }?.let {
                viewModel.selectDevice(it, context)
            }
        } else {

            viewModel.restoreSelectedDevice(context, enabledDevices)
        }
    }


    val currentData = selected?.let { deviceDataMap[it.deviceId] }


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
       ){ paddingValues->
        Column (modifier = Modifier.padding(paddingValues).padding(12.dp).verticalScroll(
            rememberScrollState()
        )){



            DeviceSelectorField(
                placeholder = getStringRes(R.string.select),
                enabledDevices = enabledDeviceView,
                selectedDevice = selected?.name ?: "Select Device",
                onDeviceSelected = { deviceId ->
                    enabledDeviceView.firstOrNull { it.device.deviceId == deviceId }?.let {
                        viewModel.selectDevice(it.device, context)
                    }
                },
                onCloudLongPress = onCloudClick
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
                label = getStringRes(R.string.currentSpeed),
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
                label = getStringRes(R.string.remain_time),
                value = String.format("%.2f", currentData?.remainingTime ?: 0.00),
                unit = "h",
                iconRight = painterResource(R.drawable.download_speed)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Fields(
                icon = painterResource(R.drawable.battery_3713201),
                label = getStringRes(R.string.battery_volt) ,
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
            UUIDField(uuid = device?.deviceId ?: getStringRes(R.string.no_device),)
            Spacer(modifier = Modifier.height(10.dp))
            bottomBar(
                onDelete = { showDeleteDialog = true }
            )

            if (showDuplicateDialog) {
                AlertDialog(
                    onDismissRequest = { showDuplicateDialog = false },
                    confirmButton = {
                        TextButton(onClick = { showDuplicateDialog = false }) {
                            Text(getStringRes(R.string.Ok),)
                        }
                    },
                    title = { Text(getStringRes(R.string.duplicateDevice)) },
                    text = { Text(" $duplicateDeviceId"+" "+getStringRes(R.string.q4),) }
                )
            }
            if (showNoInternetDialog) {
                AlertDialog(
                    onDismissRequest = { showNoInternetDialog = false },  // allow closing
                    title = { Text(getStringRes(R.string.no_internet)) },
                    text = { Text(getStringRes(R.string.internet_dialog)) },
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
                    title = { Text(getStringRes(R.string.del_device)) },
                    text = { Text(getStringRes(R.string.q3)) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.deleteSelectedDevice()
                                showDeleteDialog = false
                            }
                        ) {
                            Text((getStringRes(R.string.Ok)))
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showDeleteDialog = false }
                        ) {
                            Text(getStringRes(R.string.Cancel))
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
                text = getStringRes(R.string.positron),
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = getStringRes(R.string.irrigation),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Blue,
                letterSpacing = 1.sp
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
/*@OptIn(ExperimentalMaterial3Api::class)
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
}*/
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DeviceSelectorField(
    placeholder: String,
    enabledDevices: List<DeviceWithData>,
    selectedDevice: String,
    onCloudLongPress: () -> Unit,
    onDeviceSelected: (String) -> Unit
) {
    val displayText = if (selectedDevice.isBlank()) placeholder else selectedDevice

    // dialog (names + radios) and surface (grid) states
    var dialogOpen by remember { mutableStateOf(false) }          // opened by trailing icon inside textfield
    var surfaceExpanded by remember { mutableStateOf(false) }     // opened by the separate down-arrow icon

    // selected name inside dialog (keeps in sync with incoming selectedDevice)
    var dialogSelectedName by remember { mutableStateOf(selectedDevice) }
    LaunchedEffect(selectedDevice) { dialogSelectedName = selectedDevice }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.cloud_platform),
                contentDescription = "Device Icon",
                modifier = Modifier.size(40.dp).combinedClickable(onClick = {},
                    onLongClick = {onCloudLongPress()})
            )

            Spacer(modifier = Modifier.width(8.dp))

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

            // separate down-arrow icon: expands the surface below showing the grid of device boxes
            Icon(
                painter = painterResource(R.drawable.down_arrow),
                contentDescription = "Show device grid",
                modifier = Modifier
                    .size(40.dp)
                    .clickable {
                        surfaceExpanded = !surfaceExpanded
                        // close dialog if it's open
                        if (dialogOpen) dialogOpen = false
                    }
            )
        }

        // ---- Dialog with radio buttons for device NAMES ----
        if (dialogOpen) {
            AlertDialog(
                onDismissRequest = { dialogOpen = false },
                title = { Text(text = getStringRes(R.string.choose_dev)) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // show list of names as radio items
                        enabledDevices.forEach { deviceWithData ->
                            val name = deviceWithData.device.name
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        dialogSelectedName = name
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (dialogSelectedName == name),
                                    onClick = { dialogSelectedName = name }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = name, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        // map selected name -> deviceId and notify
                        enabledDevices.firstOrNull { it.device.name == dialogSelectedName }?.let {
                            onDeviceSelected(it.device.deviceId)
                        }
                        dialogOpen = false
                    }) {
                        Text(getStringRes(R.string.Ok))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { dialogOpen = false }) { Text(getStringRes(R.string.Cancel)) }
                }
            )
        }

        // ---- Surface expanded below the field: shows grid of DeviceBox cards ----
        val gridState = rememberLazyGridState()
        if (surfaceExpanded) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = MaterialTheme.colorScheme.surfaceContainer)
                    .heightIn(max = 360.dp)
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 6.dp
            ) {

                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(color = MaterialTheme.colorScheme.surfaceContainer)
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(getStringRes(R.string.enabled_dev), style = MaterialTheme.typography.titleMedium)
                            IconButton(onClick = { surfaceExpanded = false }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close"
                                )
                            }
                        }


                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                state = gridState,
                                modifier = Modifier.fillMaxSize()
                                    .background(color = MaterialTheme.colorScheme.surfaceContainer)
                                    .padding(horizontal = 8.dp),
                                contentPadding = PaddingValues(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(enabledDevices) { deviceWithData ->
                                    DeviceBox(
                                        deviceWithData = deviceWithData,
                                        onClick = {
                                            onDeviceSelected(deviceWithData.device.deviceId)
                                        }
                                    )
                                }
                            }





                    }



            }
        }
    }
}


@Composable
fun DeviceBox(deviceWithData: DeviceWithData, onClick: () -> Unit) {
    val d = deviceWithData.data

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, color = Color.Black),
        shape = RectangleShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                ,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(15.dp))
            Text(
                modifier = Modifier.fillMaxWidth().background(color = MaterialTheme.colorScheme.surfaceBright).padding(6.dp),
                text = deviceWithData.device.name,
                style = MaterialTheme.typography.titleMedium,
                color = Color.Black,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(5.dp))

            // Titles left, values right
            Row(
                modifier = Modifier.fillMaxWidth().padding(4.dp).border(1.dp, color = Color.Black),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val valueTextStyle = MaterialTheme.typography.bodySmall.copy(
                    color = Color.Blue,
                    fontSize = 12.sp
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(8.dp).weight(1f)
                ) {
                    Text(getStringRes(R.string.speed),color = Color.Black, style = valueTextStyle)
                    Text(getStringRes(R.string.speed),color = Color.Black, style = valueTextStyle)
                    Text(getStringRes(R.string.remaining_distance),color = Color.Black, style = valueTextStyle)
                    Text(getStringRes(R.string.pressure),color = Color.Black, style = valueTextStyle)
                    Text(getStringRes(R.string.battery),color = Color.Black, style = valueTextStyle)
                    Text(getStringRes(R.string.Finish),color = Color.Black, style = valueTextStyle)
                    Text(getStringRes(R.string.pressure),color = Color.Black, style = valueTextStyle)
                }

                Column(

                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(10.dp).weight(1f)
                ) {
                    Text("${d?.speedSetting ?: "-"}",color = Color.Blue, style = valueTextStyle)
                    Text("${d?.currentSpeed ?: "-"}",color = Color.Blue, style = valueTextStyle)
                    Text("${d?.remainingDistance ?: "-"}",color = Color.Blue, style = valueTextStyle)
                    Text("${d?.pressure ?: "-"}",color = Color.Blue, style = valueTextStyle)
                    Text("${d?.batteryVoltage ?: "-"}",color = Color.Blue, style = valueTextStyle)
                    Text(
                        when (d?.finalFinishNotification) {
                            0 -> getStringRes(R.string.Off)
                            1 -> getStringRes(R.string.On)
                            else -> "-"
                        },color = Color.Blue, style = valueTextStyle
                    )
                    Text(
                        when (d?.lowPressureAlarm) {
                            0 -> getStringRes(R.string.Off)
                            1 -> getStringRes(R.string.On)
                            else -> "-"
                        },color = Color.Blue, style = valueTextStyle
                    )
                }
            }
        }
    }
}


data class DeviceWithData(
    val device: Device,
    val data: DeviceData?
)

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
                fontSize = 18.sp,
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
                fontSize = 20.sp,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = valueColor
            )
            Spacer(modifier = Modifier.width(10.dp))

            if (unit != null) {
                Text(
                    text = " $unit",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,

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
            label = getStringRes(R.string.speedSetting),
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
                    text = getStringRes(R.string.changeSpeed),
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
                    placeholder = { Text(getStringRes(R.string.enter_speed)) },
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
                    Text(getStringRes(R.string.Ok), color = Color.Black, fontSize = 12.sp)
                }
            }
        }

        // Error dialog
        if (showErrorDialog) {
            AlertDialog(
                onDismissRequest = { showErrorDialog = false },
                confirmButton = {
                    TextButton(onClick = { showErrorDialog = false }) {
                        Text(getStringRes(R.string.Ok))
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
            label = getStringRes(R.string.press),
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
                        text = getStringRes(R.string.pre_limit),
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
                                getStringRes(R.string.new_value),
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
                title = { Text(getStringRes(R.string.invalid_pressure)) },
                text = { Text(getStringRes(R.string.pressure_range)) }
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

    val displayText = if (switchState) getStringRes(R.string.On) else getStringRes(R.string.Off)

    Column {
        DeviceFieldWithSwitch(
            modifier = Modifier.background(MaterialTheme.colorScheme.primary),
            icon = painterResource(id = R.drawable.notification),
            label = getStringRes(R.string.automaticReports),
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
    val displayText = if (switchState) getStringRes(R.string.Open) else getStringRes(R.string.Closed)

    Column {
        DeviceField(
            icon = painterResource(R.drawable.advance_control),
            label = getStringRes(R.string.advance_control),
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
                    label = getStringRes(R.string.main_valve),
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
            label = getStringRes(R.string.remain_dis),
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
                title = { Text(getStringRes(R.string.reset_dis)) },
                text = {
                    Text(
                        getStringRes(R.string.q1)+ "\n" +
                                getStringRes(R.string.q2)
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onReset()
                            showDialog = false
                        }
                    ) {
                        Text(getStringRes(R.string.Ok))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDialog = false }
                    ) {
                        Text(getStringRes(R.string.Cancel))
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
                fontSize = 18.sp,
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
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = valueColor
            )
            Spacer(modifier = Modifier.width(10.dp))

            if (unit != null) {
                Text(
                    text = " $unit",
                    fontSize = 18.sp,
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
                fontSize = 18.sp,
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
                fontSize = 18.sp,
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
                fontSize = 18.sp,
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
                fontSize = 18.sp,
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
                modifier = Modifier.size(60.dp),
                tint = Color.Unspecified
            )
            Text(
                text = getStringRes(R.string.del_device),
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