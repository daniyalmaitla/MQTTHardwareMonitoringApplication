package com.app.mqtthardwareapp.Screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.app.mqtthardwareapp.Data.Device
import com.app.mqtthardwareapp.R
import com.app.mqtthardwareapp.Theme.MqttHardwareAppTheme
import com.app.mqtthardwareapp.ViewModels.DeviceRepository
import com.app.mqtthardwareapp.ViewModels.DeviceViewModel
import com.app.mqtthardwareapp.ViewModels.DeviceViewModelFactory

@Composable
fun DeviceRegScreen(
    navController: NavController,
    repo: DeviceRepository,
    viewModel: DeviceViewModel
){
    val state by viewModel.state.collectAsState()
    var communicationInterval by rememberSaveable { mutableStateOf(state.devices.firstOrNull()?.interval?.toString() ?: "") }

    var showDuplicateDialog by remember { mutableStateOf(false) }
    var duplicateDeviceId by remember { mutableStateOf("") }

    val devices = remember {
        mutableStateListOf<Device>().apply {
            repeat(20) { slot ->
                add(
                    state.devices.find { it.slot == slot + 1 }
                        ?: Device(
                            id = 0,
                            slot = slot + 1, // use slot explicitly
                            deviceId = "",
                            name = "",
                            interval = 1000L,
                            enabled = false,
                            subscribedReadTopic = "",
                            subscribedWriteTopic = ""
                        )
                )
            }
        }
    }



    Scaffold(topBar = {RegTopbar()}) {paddingValues->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {


            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                item { Spacer(modifier = Modifier.height(12.dp)) }
                item {
                    TopButtons(
                        onSaveExit = {
                            var hasDuplicate = false

                            devices.forEach { device ->
                                if (device.deviceId.isNotBlank()) {
                                    val exists = state.devices.any { it.deviceId == device.deviceId && it.slot != device.slot }
                                    if (exists) {
                                        hasDuplicate = true
                                        duplicateDeviceId = device.deviceId
                                        return@forEach
                                    }
                                }
                            }

                            if (hasDuplicate) {
                                showDuplicateDialog = true
                            } else {
                                devices.forEach { device ->
                                    if (device.deviceId.isNotBlank()) { // Save only filled slots
                                        viewModel.saveDevice(
                                            id = device.id,
                                            slot = device.slot,
                                            deviceName = device.name,
                                            deviceId = device.deviceId,
                                            interval = device.interval,
                                            enabled = device.enabled
                                        )
                                    }
                                }
                                navController.popBackStack()
                            }
                        }
                        ,
                        onExit = { navController.popBackStack() }
                    )
                }
                item { Spacer(modifier = Modifier.height(2.dp)) }


                item {
                    DeviceManagementScreen()
                }

                // Device Rows

                itemsIndexed(devices) { index, device ->
                    DeviceRow(
                        device = device,
                        onDeviceChange = { updatedDevice ->
                            devices[index] = updatedDevice
                        }
                    )
                }
                item {
                    BottomFields(
                        interval = communicationInterval,
                        onIntervalChange = { newInterval ->
                            communicationInterval = newInterval.toString()
                            // Apply interval to all devices
                            devices.replaceAll { it.copy(interval = newInterval) }
                        }
                    )
                }

                item { Spacer(modifier = Modifier.height(12.dp)) }


                item { Spacer(modifier = Modifier.height(12.dp)) }


            }
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



        }
    }
}
@Composable
fun RegTopbar(

) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.colorScheme.primary)
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {




        // CENTER TITLE
        Column(
            modifier = Modifier
                .weight(1f) // push center to middle
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "POSITRON",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "IRRIGATION  MASTER",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Blue,
                letterSpacing = 2.sp
            )
        }


    }
}

@Composable
fun TopButtons(
    onSaveExit : ()->Unit,
    onExit : ()-> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.colorScheme.background)
    ) {
        // Top Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min) // match tallest child
                .background(MaterialTheme.colorScheme.background),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val buttonModifier = Modifier
                .weight(1f) // equal width
                .fillMaxHeight() // equal height
                .padding(5.dp)

            Button(
                onClick = { },
                shape = RoundedCornerShape(4.dp),
                modifier = buttonModifier,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,    // background
                    contentColor = Color.Black      // text color
                )
            ) {
                Text(
                    "Check for new version",
                    textAlign = TextAlign.Center,
                    softWrap = true,
                    lineHeight = 14.sp
                )
            }

            Button(
                onClick = onSaveExit,
                shape = RoundedCornerShape(4.dp),
                modifier = buttonModifier,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,    // background
                    contentColor = Color.Black      // text color
                )
            ) {
                Text(
                    "Save & Exit",
                    textAlign = TextAlign.Center,
                    softWrap = true
                )
            }

            Button(
                onClick = onExit,
                shape = RoundedCornerShape(4.dp),
                modifier = buttonModifier,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,    // background
                    contentColor = Color.Black      // text color
                )
            ) {
                Text(
                    "Exit",
                    textAlign = TextAlign.Center,
                    softWrap = true
                )
            }
        }



        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "DEVICE MANAGEMENT PANEL",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.background(color = MaterialTheme.colorScheme.background)
                .align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
fun DeviceManagementScreen() {


    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.colorScheme.primary)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = MaterialTheme.colorScheme.onBackground)
                .padding(6.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Text("Device Management Panel", color = Color.White, fontWeight = FontWeight.Bold)

        }
        Spacer(modifier = Modifier.height(10.dp))


        // Table Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = MaterialTheme.colorScheme.primary)
                .padding(6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("EN", color = Color.White, fontWeight = FontWeight.Bold)
            Text("DEVICE NAME", color = Color.White, fontWeight = FontWeight.Bold)
            Text("UID", color = Color.White, fontWeight = FontWeight.Bold)
        }


        Spacer(modifier = Modifier.height(12.dp))


    }
}
@Composable
fun BottomFields(
    interval: String,
    onIntervalChange: (Long) -> Unit
) {
    var intervalText by rememberSaveable { mutableStateOf(interval) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.colorScheme.primary)
    ) {
        Text("COMMUNICATION INTERVAL ", fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = intervalText,
            onValueChange = { newText ->
                intervalText = newText
                val intervalLong = newText.toLongOrNull() ?: 0L
                onIntervalChange(intervalLong) // update all devices
            },
            modifier = Modifier
                .width(100.dp)
                .padding(10.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Gray,
                unfocusedBorderColor = Color.Gray,
                disabledBorderColor = Color.Gray,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            ),
            shape = RectangleShape
        )
        Text(" Sec", fontWeight = FontWeight.Bold)
    }
}

@Composable
fun DeviceRow(
    device: Device,
    onDeviceChange: (Device) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .padding(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Compact checkbox
        Checkbox(
            checked = device.enabled,
            onCheckedChange = { onDeviceChange(device.copy(enabled = it)) },
            modifier = Modifier
                .size(24.dp)
                .padding(end = 2.dp)
                .clearAndSetSemantics { },
                    colors = CheckboxDefaults.colors(
                    checkedColor = Color.Black,
            uncheckedColor = Color.Black,
            checkmarkColor = Color.Green,
            disabledCheckedColor = Color.Transparent,
            disabledUncheckedColor = Color.Gray
        )// keep a11y without extra hit box
        )

        Text(
            text =  device.id.toString(),
            modifier = Modifier.width(24.dp),
            color = Color.White
        )

        OutlinedTextField(
            value = device.name,
            onValueChange = { onDeviceChange(device.copy(name = it)) },
            placeholder = { Text("Device Name") },
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 2.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                disabledContainerColor = Color.White
            ),
            shape = RectangleShape
        )

        OutlinedTextField(
            value = device.deviceId,
            onValueChange = { onDeviceChange(device.copy(deviceId = it)) },
            placeholder = { Text("UID") },
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 2.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                disabledContainerColor = Color.White
            ),
            shape = RectangleShape
        )
    }
}





@Preview(showBackground = true)
@Composable
fun DeviceRegPreview(){
    MqttHardwareAppTheme {

    }
}