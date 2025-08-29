package com.app.mqtthardwareapp.Screens

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.app.mqtthardwareapp.R
import com.app.mqtthardwareapp.Theme.MqttHardwareAppTheme
import com.app.mqtthardwareapp.ViewModels.DeviceViewModel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.selects.select

@Composable
fun HomeScreen(
    navController: NavController,
    onMachineClick : () ->Unit,
    viewModel: DeviceViewModel

){
    val state by viewModel.state.collectAsState()
    var showDuplicateDialog by remember { mutableStateOf(false) }
    var duplicateDeviceId by remember { mutableStateOf("") }

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
        // Start QR scanner when button clicked
        val options = ScanOptions()
        options.setPrompt("Scan Device QR")
        options.setBeepEnabled(true)
        options.setOrientationLocked(false)
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)

        barcodeLauncher.launch(options)
    },)},
        bottomBar = {bottomBar()}){ paddingValues->
        Column (modifier = Modifier.padding(paddingValues).padding(12.dp).verticalScroll(
            rememberScrollState()
        )){
            var selectedDevice by remember { mutableStateOf("") }   // <-- keeps track of chosen device
            val devices = listOf("Device A", "Device B", "Device C") // your device list
            DeviceSelectorField(
                placeholder = "Select Device",
                suggestions = devices,
                selectedDevice = selectedDevice,
                onDeviceSelected = { device ->
                    selectedDevice = device
                },

            )
            Spacer(modifier = Modifier.height(10.dp))

           SpeedSettingRow()
            Spacer(modifier = Modifier.height(10.dp))
            Fields(
               icon = painterResource(R.drawable.download_speed),
                label = "CURRENT SPEED",
                value = "0.00",
                unit = "m/h",
                iconRight = painterResource(R.drawable.download_speed)
            )
            Spacer(modifier = Modifier.height(10.dp))
            DistanceRow()
            Spacer(modifier = Modifier.height(10.dp))
            Fields(
                icon = painterResource(R.drawable.download_speed),
                label = "REMAINING TIME",
                value = "0.00",
                unit = "m/h",
                iconRight = painterResource(R.drawable.download_speed)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Fields(
                icon = painterResource(R.drawable.battery_3713201),
                label = "BATTERY VOLTAGE",
                value = "12.0",
                unit = "m/h",
                iconRight = painterResource(R.drawable.download_speed)
            )
            Spacer(modifier = Modifier.height(10.dp))
            PressureSettingRow()
            var switchState by remember { mutableStateOf(false) }
            var switchState2 by remember { mutableStateOf(false) }
            Spacer(modifier = Modifier.height(10.dp))
            DeviceFieldWithSwitch(
                icon = painterResource(id = R.drawable.notification), // your icon
                label = "AUTOMATIC REPORTS",
                value = "OFF",
                switchState = switchState,
                onSwitchChange = { switchState = it }
            )
            Spacer(modifier = Modifier.height(10.dp))
            DeviceFieldWithSwitch(
                icon = painterResource(id = R.drawable.pneumatic), // your icon
                label = "MAIN VALVE CONTROL STATUS",
                value = "CLOSED",
                switchState = switchState2,
                onSwitchChange = { switchState2 = it }
            )

            Spacer(modifier = Modifier.height(10.dp))
            UUIDField()
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
fun HomeTopbar(
    onQrClick: () -> Unit = {},
    onMachineClick : ()-> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.colorScheme.primary)
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
    placeholder: String = "Select Device",
    suggestions: List<String>,
    selectedDevice: String,
    onDeviceSelected: (String) -> Unit
) {
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
            value = selected,
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
                    Text("OK")
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
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = valueColor
            )
            Spacer(modifier = Modifier.width(10.dp))

            if (unit != null) {
                Text(
                    text = " $unit",
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
@Composable
fun SpeedSettingRow() {
    var showSpeedField by remember { mutableStateOf(false) }
    var speedInput by remember { mutableStateOf("") }

    Column {
        // Current Speed Row
        DeviceField(
            icon = painterResource(R.drawable.optimization),
            label = "SPEED SETTING",
            value = "0.00",
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
                        focusedBorderColor = MaterialTheme.colorScheme.surfaceContainer,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceContainer,
                        cursorColor = Color.Black,
                    )
                )

                Spacer(modifier = Modifier.width(25.dp))

                // OK Button
                Button(
                    onClick = {
                        // Handle speed input here
                        showSpeedField = false
                    },
                    modifier = Modifier.height(50.dp).width(50.dp)  .border(1.dp, Color.Gray, RoundedCornerShape(2.dp)), // Square button
                    shape = RoundedCornerShape(2.dp), // Rounded corners
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer // Grey background
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("OK", color = Color.Black, fontSize = 12.sp)
                }
            }
        }
    }
}
@Composable
fun PressureSettingRow() {
    var showField by remember { mutableStateOf(false) }
    var currentValue by remember { mutableStateOf("0.00") }
    var newValue by remember { mutableStateOf("") }

    Column {
        // Current Pressure Row (collapsible trigger)
        DeviceField(
            icon = painterResource(R.drawable.pressure_meter),
            label = "PRESSURE",
            value = currentValue,
            unit = "Pa",
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
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black
                    )
                }

                // Read-only field with current value
                OutlinedTextField(
                    value = currentValue,
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
                        onValueChange = { newValue = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("Enter new value") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = Color.Gray,
                            unfocusedBorderColor = Color.Gray,
                            cursorColor = Color.Black,
                        )
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    // OK Button
                    Button(
                        onClick = {
                            if (newValue.isNotBlank()) {
                                currentValue = newValue
                                newValue = "" // clear input
                                showField = false // collapse row
                            }
                        },
                        modifier = Modifier
                            .height(50.dp)
                            .width(50.dp)
                            .border(1.dp, Color.Gray, RoundedCornerShape(2.dp)), // grey border
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
    }
}


@Composable
fun DistanceRow() {
    var showDialog by remember { mutableStateOf(false) }
    var selectedOption by remember { mutableStateOf("Option 1") }

    Column {
        // Current Speed Row
        DeviceField(
            icon = painterResource(R.drawable.travel),
            label = "REMAINING DISTANCE",
            value = "15.00",
            unit = "m",
            iconRight = painterResource(R.drawable.arrows_13170375),
            onRightIconClick = {
                showDialog = !showDialog
            }
        )

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                confirmButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("OK")
                    }
                },
                title = { Text("Choose an Option") },
                text = {
                    Column {
                        val options = listOf("Option 1", "Option 2", "Option 3")
                        options.forEach { option ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedOption = option }
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = selectedOption == option,
                                    onClick = { selectedOption = option }
                                )
                                Text(option, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
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
                color = Color.Black
            )
        }

        // Value + Unit
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = valueColor
            )
            Spacer(modifier = Modifier.width(10.dp))

            if (unit != null) {
                Text(
                    text = " $unit",
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
    backgroundColor: Color = MaterialTheme.colorScheme.primary,
    valueColor: Color = Color.Blue,
    switchState: Boolean,
    onSwitchChange: (Boolean) -> Unit
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

        // Label + Value in same line, label wraps if long
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black,
                modifier = Modifier.weight(1f), // let it wrap
                maxLines = Int.MAX_VALUE,
                overflow = TextOverflow.Visible
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = value,
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
fun UUIDField() {
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
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black
            )
        }

        // Value + Unit
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "13032025678",
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
fun bottomBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.colorScheme.surfaceContainer),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
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













@Preview(showBackground = true)
@Composable
fun PreviewHome(){
    MqttHardwareAppTheme {

    }

}