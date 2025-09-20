package com.app.mqtthardwareapp.Screens

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.modifier.modifierLocalConsumer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.app.mqtthardwareapp.Data.Device
import com.app.mqtthardwareapp.Events.DeviceEvent
import com.app.mqtthardwareapp.MainActivity
import com.app.mqtthardwareapp.MqttBackgroundService
import com.app.mqtthardwareapp.R
import com.app.mqtthardwareapp.Theme.MqttHardwareAppTheme
import com.app.mqtthardwareapp.Utils.PrefsHelper
import com.app.mqtthardwareapp.ViewModels.DeviceRepository
import com.app.mqtthardwareapp.ViewModels.DeviceViewModel
import org.intellij.lang.annotations.Language
@SuppressLint("ComposeLocalContext")
@Composable
fun getStringRes(@StringRes resId: Int): String {
    val context = LocalContext.current
    val locale = MainActivity.LocalAppLocale.current
    val currentConfig = LocalConfiguration.current

    val config = Configuration(currentConfig)
    config.setLocale(locale)

    val localizedContext = context.createConfigurationContext(config)
    return localizedContext.resources.getString(resId)
}
@Composable
fun DeviceRegScreen(
    navController: NavController,
    repo: DeviceRepository,
    viewModel: DeviceViewModel,
    onChangeLanguage: (String) -> Unit
) {
    val context = LocalContext.current
    var clientId by rememberSaveable {
        mutableStateOf(PrefsHelper.getClientId(context).removePrefix("IrrigationMaster_"))
    }

    LaunchedEffect(Unit) {
        viewModel.onEvent(DeviceEvent.LoadDevices)
    }

    val state by viewModel.state.collectAsState()
    val globalInterval by viewModel.globalInterval.collectAsState()


    var communicationInterval = (globalInterval / 1000).toString()
    var showDuplicateDialog by remember { mutableStateOf(false) }
    var showClientIdDialog by remember { mutableStateOf(false) }
    var duplicateDeviceId by remember { mutableStateOf("") }


    val devices = remember {
        mutableStateListOf<Device>()
    }

    LaunchedEffect(state.devices) {
        devices.clear()
        devices.addAll(
            List(20) { slot ->
                state.devices.find { it.slot == slot + 1 } ?: Device(
                    slot = slot + 1,
                    id = 0,
                    name = "",
                    deviceId = "",
                    interval = 50_000L,
                    enabled = false,
                    subscribedReadTopic = "",
                    subscribedWriteTopic = ""
                )
            }
        )
    }


    Scaffold(topBar = { RegTopbar() }) { paddingValues ->
        Column(Modifier.fillMaxSize().padding(paddingValues)) {

            LazyColumn(Modifier.fillMaxSize()) {
                item { Spacer(modifier = Modifier.height(10.dp)) }
                item { LanguageButtons(onChangeLanguage = onChangeLanguage) }
                item { Spacer(modifier = Modifier.height(2.dp)) }
                item {
                    TopButtons(
                        onSaveExit = {
                            if (clientId.isBlank()) {

                                showClientIdDialog = true
                                return@TopButtons
                            }
                            PrefsHelper.saveClientId(context, "IrrigationMaster_${clientId.trim()}")
                            MqttBackgroundService.startIfClientIdSet(context)
                            var hasDuplicate = false
                            for (d in devices) {
                                if (d.deviceId.isNotBlank()) {
                                    if (state.devices.any { it.deviceId == d.deviceId && it.slot != d.slot && it.name == d.name }) {
                                        hasDuplicate = true
                                        duplicateDeviceId = d.deviceId
                                        break
                                    }
                                }
                            }
                            if (hasDuplicate) {
                                showDuplicateDialog = true
                            } else {
                                devices.filter { it.deviceId.isNotBlank() }.forEach { dev ->
                                    viewModel.saveDevice(dev.id, dev.slot, dev.name, dev.deviceId, dev.interval, dev.enabled)
                                }
                                navController.popBackStack()
                            }
                            communicationInterval.toLongOrNull()?.let { intervalSec ->
                                viewModel.setIntervalForAll(intervalSec)
                            }


                        },
                        onExit = { navController.popBackStack() },
                        onCheckVersion = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://drive.google.com/file/d/1ZKe9aRmPgI8L-ucKhQu5BDgmPq443jXC/view?usp=sharing"))
                            context.startActivity(intent)
                        }

                    )
                }

                item { Spacer(modifier = Modifier.height(10.dp)) }
                item{
                    ClientIdField(
                        clientId = clientId,
                        onClientIdChange = { clientId = it }
                    )
                }

                item {

                    BottomFields(
                        interval = communicationInterval,
                        onIntervalChange = { newVal -> communicationInterval =
                            newVal.toString()
                        }
                    )

                }
                item { Spacer(modifier = Modifier.height(10.dp)) }
                item { DeviceManagementScreen() }
                itemsIndexed(devices) { index, device ->
                    DeviceRow(
                        device = device,
                        onDeviceChange = { updated ->
                            devices[index] = updated

                            viewModel.saveDevice(
                                updated.id,
                                updated.slot,
                                updated.name,
                                updated.deviceId,
                                updated.interval,
                                updated.enabled
                            )
                        }
                    )
                }



            }
            if (showClientIdDialog) {
                AlertDialog(
                    onDismissRequest = { showClientIdDialog = false },
                    confirmButton = {
                        TextButton(onClick = { showClientIdDialog = false }) {
                            Text("OK")
                        }
                    },
                    title = { Text(getStringRes(R.string.ClientID)) },
                    text = { Text(getStringRes(R.string.clientDialog)) }
                )
            }


            if (showDuplicateDialog) {
                AlertDialog(
                    onDismissRequest = { showDuplicateDialog = false },
                    confirmButton = { TextButton({ showDuplicateDialog = false }) { Text("OK") } },
                    title = { Text(getStringRes(R.string.duplicateDevice)) },
                    text = { Text("$duplicateDeviceId"+" "+getStringRes(R.string.q4)) }
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
                text = getStringRes(R.string.positron),
                fontSize = 35.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = getStringRes(R.string.irrigation),
                fontSize = 25.sp,
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
    onExit : ()-> Unit,
    onCheckVersion : ()->Unit
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
                onClick = onCheckVersion,
                shape = RoundedCornerShape(4.dp),
                modifier = buttonModifier,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,    // background
                    contentColor = Color.Black      // text color
                )
            ) {
                Text(
                    getStringRes(R.string.check_version),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
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
                    getStringRes(R.string.save_exit),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
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
                    getStringRes(R.string.exit),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    softWrap = true
                )
            }
        }



        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = getStringRes(R.string.Device_mgt),
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
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
            Text(getStringRes(R.string.Device_mgt), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)

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
            Text(getStringRes(R.string.enabled), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(getStringRes(R.string.Device_name), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(getStringRes(R.string.UUID), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
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
    Row(modifier = Modifier.padding(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .background(color = MaterialTheme.colorScheme.primary)
        ) {
            Text(getStringRes(R.string.com_interval), fontWeight = FontWeight.Bold)
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
            text = device.slot.toString(),
            modifier = Modifier.width(24.dp),
            color = Color.White
        )

            OutlinedTextField(
                value = device.name,
                onValueChange = { onDeviceChange(device.copy(name = it)) },
                placeholder = {
                    Text(
                        getStringRes(R.string.Device_name),
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 2.dp),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                    textDirection = TextDirection.Ltr,
                    textAlign = TextAlign.Start
                ),
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
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
                placeholder = { Text(getStringRes(R.string.UUID)) },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 2.dp),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                    textDirection = TextDirection.Ltr,
                    textAlign = TextAlign.Start
                ),
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    disabledContainerColor = Color.White
                ),
                shape = RectangleShape
            )

    }
}

@Composable
fun ClientIdField(
    clientId: String,
    onClientIdChange: (String) -> Unit
) {
    OutlinedTextField(
        value = clientId,
        onValueChange = { newValue ->

            if (newValue.length <= 15) {
                onClientIdChange(newValue)
            }
        },
        label = { Text(getStringRes(R.string.mobi_no)) },
        placeholder = { Text(getStringRes(R.string.mob_label)) },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        keyboardOptions = KeyboardOptions.Default.copy(
            keyboardType = KeyboardType.Phone,
            imeAction = ImeAction.Done
        ),
        isError = clientId.isNotEmpty() && clientId.length < 10,
        supportingText = {
            if (clientId.isNotEmpty() && clientId.length < 10) {
                Text(getStringRes(R.string.placeholder))
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            focusedBorderColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        shape = RectangleShape
    )
}
@Composable
fun LanguageButtons(onChangeLanguage: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Greek flag
        Button(
            onClick = { onChangeLanguage("el") }, // Greek
            shape = RoundedCornerShape(4.dp),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.size(width = 30.dp, height = 20.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.greece),
                contentDescription = "Greek flag",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // English flag
        Button(
            onClick = { onChangeLanguage("en") }, // English
            shape = RoundedCornerShape(4.dp),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.size(width = 30.dp, height = 20.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.united_kingdom),
                contentDescription = "UK flag",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}






@Preview(showBackground = true)
@Composable
fun DeviceRegPreview(){
    MqttHardwareAppTheme {

    }
}