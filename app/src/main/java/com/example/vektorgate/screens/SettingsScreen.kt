package com.example.vektorgate.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.database.sqlite.SQLiteConstraintException
import android.util.Log
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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vektorgate.data.SettingsManager
import com.example.vektorgate.requests.RequestManager
import com.example.vektorgate.security.ApprovalRequest
import com.example.vektorgate.security.ToolInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.InternalSerializationApi
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.LocalDateTime
import kotlin.random.Random

class SettingsScreenState(
    val urlState: TextFieldValue,
    val onUrlChange: (TextFieldValue) -> Unit,
    val deviceNameState: TextFieldValue,
    val onDeviceNameChange: (TextFieldValue) -> Unit,
    val firebaseToken: String,
    val deviceId: String,
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
    val deviceId by manager.deviceId.collectAsState(initial = "Loading...")

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
        Unit
    }

    val onDeviceNameChange = { newValue: TextFieldValue ->
        deviceNameState = newValue
        Unit
    }

    val onSave = {
        coroutineScope.launch {
            manager.saveCoreUrl(urlState.text)
            manager.saveDeviceName(deviceNameState.text)
            Toast.makeText(activity, "Settings Saved", Toast.LENGTH_SHORT).show()
        }
        Unit
    }

    return remember(urlState, deviceNameState, firebaseToken, deviceId) {
        SettingsScreenState(
            urlState = urlState,
            onUrlChange = onUrlChange,
            deviceNameState = deviceNameState,
            onDeviceNameChange = onDeviceNameChange,
            firebaseToken = firebaseToken,
            deviceId = deviceId,
            onSave = onSave
        )
    }
}

@Composable
fun SettingsScreen(activity: AppCompatActivity) {
    val state = rememberSettingsScreenState(activity = activity)
    val coroutineScope = rememberCoroutineScope()

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
            Spacer(modifier = Modifier.height(24.dp))
            DebugSettings(activity)
            
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    coroutineScope.launch {
                        testConnection(activity, state.urlState.text)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Test Connection")
            }
        }
        
        Button(
            onClick = state.onSave,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Text("Save Settings")
        }
    }
}

private fun testConnection(activity: AppCompatActivity, url: String) {
    if (url.isEmpty()) {
        Toast.makeText(activity, "URL is empty", Toast.LENGTH_SHORT).show()
        return
    }
    
    val client = OkHttpClient()
    val request = Request.Builder().url(url).build()
    
    try {
        client.newCall(request).execute().use { response ->
            if (response.isSuccessful || response.code == 404) { // 404 is fine, means we reached the server
                Toast.makeText(activity, "Connection Successful!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(activity, "Server reached, but returned ${response.code}", Toast.LENGTH_SHORT).show()
            }
        }
    } catch (e: Exception) {
        Log.e("SettingsScreen", "Connection test failed", e)
        Toast.makeText(activity, "Failed to reach server: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

@Composable
fun GeneralSettings(activity: AppCompatActivity, state: SettingsScreenState) {
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
        Spacer(modifier = Modifier.height(8.dp))
        Text("Device ID:", fontSize = 14.sp)
        Text(
            text = state.deviceId,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            modifier = Modifier.padding(top = 4.dp)
                .clickable(true) {
                    val clipboardManager: ClipboardManager =
                        activity.getSystemService(AppCompatActivity.CLIPBOARD_SERVICE) as ClipboardManager
                    val clipData = ClipData.newPlainText("Device ID", state.deviceId)
                    clipboardManager.setPrimaryClip(clipData)
                }
        )
    }
}

@Composable
fun FirebaseSettings(activity: AppCompatActivity, state: SettingsScreenState) {
    Column {
        Text("Firebase Messaging", fontSize = 20.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Registration Token:", fontSize = 14.sp)
        Text(
            text = state.firebaseToken,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            modifier = Modifier.padding(top = 4.dp)
                .clickable(true) {
                    val clipboardManager: ClipboardManager =
                        activity.getSystemService(AppCompatActivity.CLIPBOARD_SERVICE) as ClipboardManager
                    val clipData = ClipData.newPlainText("Firebase Token", state.firebaseToken)
                    clipboardManager.setPrimaryClip(clipData)
                }
        )
    }
}

@OptIn(InternalSerializationApi::class)
@Composable
fun DebugSettings(activity: AppCompatActivity) {
    val coroutineScope: CoroutineScope = rememberCoroutineScope()

    Column {
        Text("Debug Settings", fontSize = 20.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            val request = ApprovalRequest(
                type = "approval_request",
                requestId = Random.nextInt().toString(),
                nonce = "123",
                expiresAt = LocalDateTime.now().plusHours(1),
                payloadHash = "123",
                tool = ToolInfo(
                    name = Random.nextInt().toString(),
                    description = "A dummy tool for testing",
                    riskLevel = "none"
                )
            )
            val manager = RequestManager(activity)
            coroutineScope.launch {
                try {
                    manager.insertRequest(request)
                }
                catch (e: SQLiteConstraintException) {
                    Log.w("DebugSettings", "Can't insert request: ${e.message}. Id already exists")
                }
            }
        }) {
            Text("Send mock request")
        }
    }
}
