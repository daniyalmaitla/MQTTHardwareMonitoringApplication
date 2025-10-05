package com.app.mqtthardwareapp.Screens


import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.app.mqtthardwareapp.MqttBackgroundService
import com.app.mqtthardwareapp.MqttBackgroundService.Companion.ACTION_RELOAD_CONFIG
import com.app.mqtthardwareapp.MqttManager
import com.app.mqtthardwareapp.Utils.PrefsHelper
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/*@Composable
fun ServerScreen(
    onServerSet: (String) -> Unit
) {
    var serverText by remember { mutableStateOf(TextFieldValue("")) }
    var isValidJson by remember { mutableStateOf(true) }

    // Function to check JSON validity
    fun isJsonValid(input: String): Boolean {
        if (input.isBlank()) return false
        return try {
            JSONObject(input)
            true
        } catch (ex: JSONException) {
            try {
                JSONArray(input)
                true
            } catch (ex1: JSONException) {
                false
            }
        }
    }

    Scaffold(topBar = { RegTopbar() }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(paddingValues),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Enter Server details (JSON format only)",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = serverText,
                onValueChange = {
                    serverText = it
                    isValidJson = isJsonValid(it.text)
                },
                label = { Text("Server JSON") },
                modifier = Modifier.fillMaxWidth(),
                isError = !isValidJson,
                supportingText = {
                    if (!isValidJson) {
                        Text("Invalid JSON format", color = MaterialTheme.colorScheme.error)
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = { onServerSet(serverText.text) },
                    enabled = serverText.text.isNotBlank() && isValidJson
                ) {
                    Text("Save")
                }
            }
        }
    }
}*/
@Composable
fun ServerScreen(
    onServerSet: (String) -> Unit,
    navController: NavController,
) {
    val context = LocalContext.current
    var serverText by remember {
        mutableStateOf(TextFieldValue(PrefsHelper.getServerDetails(context)))
    }

    var isValidJson by remember { mutableStateOf(true) }

    fun isJsonValid(input: String): Boolean {
        if (input.isBlank()) return false
        return try {
            JSONObject(input)
            true
        } catch (ex: JSONException) {
            try {
                JSONArray(input)
                true
            } catch (ex1: JSONException) {
                false
            }
        }
    }

    Scaffold(topBar = { RegTopbar() }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(paddingValues),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Enter Server Details (JSON format only)",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 👇 Example JSON guide
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Example format:",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = """
{
    "serverUri": "tcp://ce47707f.ala.dedicated.gcp.emqxcloud.com:1883",
    "username": "positron",
    "password": "positron"
}
                        """.trimIndent(),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "use \"tcp:\\\\..... to establish MQTT TCP connection\n" +
                                "use \"ssl:\\\\..... to establish MQTT TLS connection"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = serverText,
                onValueChange = {
                    serverText = it
                    isValidJson = isJsonValid(it.text)
                },
                label = { Text("Server JSON") },
                modifier = Modifier.fillMaxWidth(),
                isError = !isValidJson,
                supportingText = {
                    if (!isValidJson) {
                        Text("Invalid JSON format", color = MaterialTheme.colorScheme.error)
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = {
                        PrefsHelper.saveServerDetails(context, serverText.text)
                        onServerSet(serverText.text)

                        val intent = Intent(context, MqttBackgroundService::class.java).apply {
                            action = ACTION_RELOAD_CONFIG
                        }
                        context.startService(intent)
                        navController.navigate("home")
                    },
                    enabled = serverText.text.isNotBlank() && isValidJson
                ) {
                    Text("Save")
                }
            }
        }
    }
}


