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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.app.mqtthardwareapp.R
import com.app.mqtthardwareapp.Theme.MqttHardwareAppTheme

@Composable
fun DeviceRegScreen(){
    val devices = remember {
        mutableStateListOf(
            *Array(20) { DeviceRowState(enabled = false, deviceName = "", uid = "") }
        )
    }
    Scaffold(topBar = {RegTopbar()}) {paddingValues->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {


            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                item { Spacer(modifier = Modifier.height(12.dp)) }
                item{TopButtons()}
                item { Spacer(modifier = Modifier.height(2.dp)) }


                item {
                    DeviceManagementScreen()
                }

                // Device Rows
                itemsIndexed(devices) { index, device ->
                    DeviceRow(
                        index = index + 1,
                        state = device,
                        onCheckedChange = { devices[index] = devices[index].copy(enabled = it) },
                        onDeviceNameChange = { devices[index] = devices[index].copy(deviceName = it) },
                        onUidChange = { devices[index] = devices[index].copy(uid = it) }
                    )
                }
                item { Spacer(modifier = Modifier.height(12.dp)) }

                item{ BottomFields()}
                item { Spacer(modifier = Modifier.height(12.dp)) }



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
fun TopButtons() {
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
                onClick = { },
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
                onClick = { },
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
fun BottomFields(){
    var communicationInterval by remember { mutableStateOf("") }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth().background(color = MaterialTheme.colorScheme.primary)
    ) {
        Text("COMMUNICATION INTERVAL ", fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = communicationInterval,
            onValueChange = { communicationInterval = it },
            modifier = Modifier
                .width(100.dp)
                .padding(10.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Gray,    // stays black when focused
                unfocusedBorderColor = Color.Gray,  // stays black when unfocused
                disabledBorderColor = Color.Gray,    // optional
                focusedContainerColor = Color.Transparent, // white background
                unfocusedContainerColor = Color.Transparent
            ),
            shape = RectangleShape // no rounded corners if you want it square
        )
        Text(" Sec", fontWeight = FontWeight.Bold)
    }
}

/*@Composable
fun DeviceRow(
    index: Int,
    state: DeviceRowState,
    onCheckedChange: (Boolean) -> Unit,
    onDeviceNameChange: (String) -> Unit,
    onUidChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth().background(color = MaterialTheme.colorScheme.primary).padding( 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = state.enabled,
            onCheckedChange = onCheckedChange
        )
        Text(text = index.toString(), modifier = Modifier.width(15.dp))
        OutlinedTextField(
            value = state.deviceName,
            onValueChange = onDeviceNameChange,
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
            shape = RectangleShape // <-- removes rounded corners
        )

        OutlinedTextField(
            value = state.uid,
            onValueChange = onUidChange,
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
}*/
@Composable
fun DeviceRow(
    index: Int,
    state: DeviceRowState,
    onCheckedChange: (Boolean) -> Unit,
    onDeviceNameChange: (String) -> Unit,
    onUidChange: (String) -> Unit
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
            checked = state.enabled,
            onCheckedChange = onCheckedChange,
            modifier = Modifier
                .size(24.dp)                              // shrink visual box
                .padding(end = 2.dp)                      // small gap before index
                .clearAndSetSemantics { },
                    colors = CheckboxDefaults.colors(
                    checkedColor = Color.Black,      // keep box transparent when checked
            uncheckedColor = Color.Black,          // border color when unchecked
            checkmarkColor = Color.Green,          // ✅ green tick
            disabledCheckedColor = Color.Transparent,
            disabledUncheckedColor = Color.Gray
        )// keep a11y without extra hit box
        )

        Text(
            text = index.toString(),
            modifier = Modifier.width(24.dp),
            color = Color.White
        )

        OutlinedTextField(
            value = state.deviceName,
            onValueChange = onDeviceNameChange,
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
            value = state.uid,
            onValueChange = onUidChange,
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


data class DeviceRowState(
    val enabled: Boolean,
    val deviceName: String,
    val uid: String
)


@Preview(showBackground = true)
@Composable
fun DeviceRegPreview(){
    MqttHardwareAppTheme {
        DeviceRegScreen()
    }
}