package com.example.vektorgate.screens

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vektorgate.data.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class SettingsScreenState(
    val urlState: TextFieldValue,
    val onUrlChange: (TextFieldValue) -> Unit,
    val deviceNameState: TextFieldValue,
    val onDeviceNameChange: (TextFieldValue) -> Unit
)

@Composable
fun rememberSettingsScreenState(
    activity: AppCompatActivity,
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): SettingsScreenState {
    val manager = remember { SettingsManager.getInstance(activity) }

    val storedUrl by manager.coreUrl.collectAsState(initial = null)
    var urlState by remember { mutableStateOf(TextFieldValue("")) }
    var isUrlInitialized by remember { mutableStateOf(false) }

    val storedDeviceName by manager.deviceName.collectAsState(initial = null)
    var deviceNameState by remember { mutableStateOf(TextFieldValue("")) }
    var isDeviceNameInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(storedUrl) {
        if (storedUrl != null && !isUrlInitialized) {
            urlState = TextFieldValue(storedUrl!!)
            isUrlInitialized = true
        }
    }

    LaunchedEffect(storedDeviceName) {
        if (storedDeviceName != null && !isDeviceNameInitialized) {
            deviceNameState = TextFieldValue(storedDeviceName!!)
            isDeviceNameInitialized = true
        }
    }

    val onUrlChange = { newValue: TextFieldValue ->
        urlState = newValue
        coroutineScope.launch {
            manager.saveCoreUrl(newValue.text)
        }
        Unit
    }

    val onDeviceNameChange = { newValue: TextFieldValue ->
        deviceNameState = newValue
        coroutineScope.launch {
            manager.saveDeviceName(newValue.text)
        }
        Unit
    }

    return remember(urlState, onUrlChange, deviceNameState, onDeviceNameChange) {
        SettingsScreenState(
            urlState = urlState,
            onUrlChange = onUrlChange,
            deviceNameState = deviceNameState,
            onDeviceNameChange = onDeviceNameChange
        )
    }
}

@Composable
fun SettingsScreen(activity: AppCompatActivity) {
    val state = rememberSettingsScreenState(activity = activity)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Settings", fontSize = 24.sp, modifier = Modifier.padding(top = 40.dp))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            GeneralSettings(state = state)
            Spacer(modifier = Modifier.height(24.dp))
            DebugSettings()
        }
    }
}

@Composable
fun GeneralSettings(state: SettingsScreenState) {
    Column {
        Text("General Settings", fontSize = 20.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Core URL:")
            TextField(
                value = state.urlState,
                singleLine = true,
                placeholder = { Text("https://core.example.com") },
                modifier = Modifier.padding(start = 8.dp),
                onValueChange = state.onUrlChange
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Device Name:")
            TextField(
                value = state.deviceNameState,
                singleLine = true,
                placeholder = { Text(android.os.Build.MODEL) },
                modifier = Modifier.padding(start = 8.dp),
                onValueChange = state.onDeviceNameChange
            )
        }
    }
}

@Composable
fun DebugSettings() {
    Column {
        Text("Debug Settings", fontSize = 20.sp)
        // Add debug settings here in the future
    }
}
