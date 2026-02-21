package com.example.vektorgate.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vektorgate.data.ConnectionStatus
import com.example.vektorgate.data.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class SettingsScreenState(
    val urlState: TextFieldValue,
    val onUrlChange: (TextFieldValue) -> Unit,
    val deviceNameState: TextFieldValue,
    val onDeviceNameChange: (TextFieldValue) -> Unit,
    val firebaseToken: String,
    val deviceId: String?,
    val connectionStatus: ConnectionStatus,
    val onSave: () -> Unit
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

    val firebaseToken by manager.firebaseToken.collectAsState(initial = "Loading...")
    val deviceId by manager.deviceId.collectAsState(initial = "Not generated")
    val connectionStatus by manager.connectionStatus.collectAsState()

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
    }

    val onDeviceNameChange = { newValue: TextFieldValue ->
        deviceNameState = newValue
    }

    val onSave = {
        coroutineScope.launch {
            manager.saveCoreUrl(urlState.text)
            manager.saveDeviceName(deviceNameState.text)
            Toast.makeText(activity, "Settings Saved", Toast.LENGTH_SHORT).show()
        }
        Unit
    }

    return remember(urlState, deviceNameState, firebaseToken, deviceId, connectionStatus) {
        SettingsScreenState(
            urlState = urlState,
            onUrlChange = onUrlChange,
            deviceNameState = deviceNameState,
            onDeviceNameChange = onDeviceNameChange,
            firebaseToken = firebaseToken,
            deviceId = deviceId,
            connectionStatus = connectionStatus,
            onSave = onSave
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
                .weight(1f)
                .padding(top = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            GeneralSettings(activity, state)
            Spacer(modifier = Modifier.height(24.dp))
            FirebaseSettings(activity, state)
            Spacer(modifier = Modifier.height(100.dp)) // Padding for bottom button
        }
        
        Button(
            onClick = state.onSave,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Text("Save & Reconnect")
        }
    }
}

@Composable
fun TapToRevealText(
    text: String,
    label: String,
    activity: AppCompatActivity,
    fontSize: TextUnit = 12.sp
) {
    var isRevealed by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (!isRevealed) {
                    isRevealed = true
                } else {
                    val clipboardManager: ClipboardManager =
                        activity.getSystemService(AppCompatActivity.CLIPBOARD_SERVICE) as ClipboardManager
                    val clipData = ClipData.newPlainText(label, text)
                    clipboardManager.setPrimaryClip(clipData)
                    Toast.makeText(activity, "$label copied", Toast.LENGTH_SHORT).show()
                }
            }
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = if (isRevealed) text else "••••••••••••••••••••••••••••••••",
            fontSize = fontSize,
            fontFamily = FontFamily.Monospace,
            lineHeight = fontSize * 1.2
        )
        Text(
            text = if (isRevealed) "Tap to copy" else "Tap to reveal",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
fun GeneralSettings(activity: AppCompatActivity, state: SettingsScreenState) {
    Column {
        Text("General Settings", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        
        Text("Core URL:", style = MaterialTheme.typography.labelMedium)
        TextField(
            value = state.urlState,
            singleLine = true,
            placeholder = { Text("https://core.example.com") },
            modifier = Modifier.fillMaxWidth(),
            onValueChange = state.onUrlChange
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text("Device Name:", style = MaterialTheme.typography.labelMedium)
        TextField(
            value = state.deviceNameState,
            singleLine = true,
            placeholder = { Text(android.os.Build.MODEL) },
            modifier = Modifier.fillMaxWidth(),
            onValueChange = state.onDeviceNameChange
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text("Device ID:", style = MaterialTheme.typography.labelMedium)
        TapToRevealText(
            text = state.deviceId ?: "Not generated",
            label = "Device ID",
            activity = activity
        )
    }
}

@Composable
fun FirebaseSettings(activity: AppCompatActivity, state: SettingsScreenState) {
    Column {
        Text("Firebase Messaging", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Registration Token:", style = MaterialTheme.typography.labelMedium)
        TapToRevealText(
            text = state.firebaseToken,
            label = "Firebase Token",
            activity = activity,
            fontSize = 10.sp
        )
    }
}
