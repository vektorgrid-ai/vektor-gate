package com.example.vektorgate.screens

import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vektorgate.data.ConnectionStatus
import com.example.vektorgate.data.SettingsManager
import com.example.vektorgate.requests.RequestManager
import com.example.vektorgate.security.ApprovalHandler
import com.example.vektorgate.security.ApprovalRequest
import com.example.vektorgate.security.ApprovalResponse
import com.example.vektorgate.security.SecurityManager
import com.example.vektorgate.security.biometric.BiometricPromptManager
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

@Composable
fun VerifyScreen(activity: AppCompatActivity, promptManager: BiometricPromptManager) {
    val settingsManager = remember { SettingsManager.getInstance(activity) }
    val connectionStatus by settingsManager.connectionStatus.collectAsState()
    val coreUrl by settingsManager.coreUrl.collectAsState(initial = "")
    val deviceId by settingsManager.deviceId.collectAsState(initial = null)
    
    val manager = remember { RequestManager.getInstance(activity) }
    val pending by manager.getPendingRequestsFlow().collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()
    
    var activeRequest by remember { mutableStateOf<ApprovalRequest?>(null) }

    val enrollLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { Log.d("VerifyScreen", "Enroll result: $it") }
    )

    val securityManager = remember { SecurityManager() }
    val handler = remember { ApprovalHandler(securityManager, promptManager) }

    if (!securityManager.hasKey()) {
        securityManager.generateKeyPair()
    }

    LaunchedEffect(Unit) {
        promptManager.promptResults.collect { result ->
            when (result) {
                is BiometricPromptManager.BiometricResult.AuthenticationSuccess -> {
                    activeRequest?.let { request ->
                        coroutineScope.launch {
                            val response = handler.processResult(result, request, deviceId ?: "")
                            if (response != null && coreUrl.isNotEmpty()) {
                                manager.updateRequestState(request.requestId, "approved")
                                sendResponseToServer("$coreUrl/companion/answer_request", response)
                            }
                            activeRequest = null
                        }
                    }
                }
                is BiometricPromptManager.BiometricResult.AuthenticationNotSet -> {
                    val enrollIntent = Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                        putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED, BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
                    }
                    enrollLauncher.launch(enrollIntent)
                }
                else -> {
                    activeRequest = null
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Pending requests", fontSize = 24.sp,
            modifier = Modifier.padding(top = 75.dp, bottom = 20.dp, start = 50.dp))
        
        if (connectionStatus != ConnectionStatus.CONNECTED) {
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = "Verification unavailable while disconnected from server.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                pending.forEach { request ->
                    Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)) {
                        Row(modifier = Modifier.fillMaxSize().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = request.tool.name, style = MaterialTheme.typography.labelLarge)
                                Text(text = "Risk: ${request.tool.riskLevel}", style = MaterialTheme.typography.bodySmall)
                            }
                            Button(onClick = {
                                coroutineScope.launch { manager.updateRequestState(request.requestId, "rejected") }
                            }) {
                                Text("Reject")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = {
                                if (coreUrl.isEmpty()) {
                                    Log.w("VerifyScreen", "Core URL not set")
                                } else {
                                    activeRequest = request
                                    handler.approveRequest(request) { error ->
                                        Log.e("VerifyScreen", "Approval init error: $error")
                                        activeRequest = null
                                    }
                                }
                            }) {
                                Text("Approve")
                            }
                        }
                    }
                }

                if (pending.isEmpty()) {
                    Text("No pending requests", modifier = Modifier.padding(top = 32.dp))
                }
            }
        }
    }
}

fun sendResponseToServer(url: String, response: ApprovalResponse) {
    val client = OkHttpClient()
    val body = Json.encodeToString(response).toRequestBody("application/json".toMediaTypeOrNull())
    val request = Request.Builder()
        .url(url)
        .post(body)
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("VerifyScreen", "Failed to send response", e)
        }
        override fun onResponse(call: Call, response: Response) {
            Log.d("VerifyScreen", "Server response: ${response.code}")
            response.close()
        }
    })
}
