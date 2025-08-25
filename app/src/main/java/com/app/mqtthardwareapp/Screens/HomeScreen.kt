package com.app.mqtthardwareapp.Screens

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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.modifier.modifierLocalConsumer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.mqtthardwareapp.R
import com.app.mqtthardwareapp.Theme.MqttHardwareAppTheme
import kotlinx.coroutines.selects.select

@Composable
fun HomeScreen(){
    Scaffold(topBar = {HomeTopbar()}){ paddingValues->
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
                .clickable(onClick = onMachineClick),
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
            label = "Current Speed",
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
                    text = "Change Speed",
                    modifier = Modifier.width(80.dp),
                    style = MaterialTheme.typography.bodyMedium
                )

                // Input field
                OutlinedTextField(
                    value = speedInput,
                    onValueChange = { speedInput = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("Enter speed") },
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










@Preview(showBackground = true)
@Composable
fun PreviewHome(){
    MqttHardwareAppTheme {
        HomeScreen()
    }

}