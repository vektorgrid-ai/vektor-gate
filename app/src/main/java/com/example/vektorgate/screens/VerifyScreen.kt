package com.example.vektorgate.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import com.example.vektorgate.security.biometric.BiometricPromptManager
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vektorgate.requests.RequestManager
import com.example.vektorgate.security.ApprovalRequest
import kotlinx.coroutines.launch
import kotlinx.serialization.InternalSerializationApi

@OptIn(InternalSerializationApi::class)
@Composable
fun VerifyScreen(activity: AppCompatActivity, promptManager: BiometricPromptManager) {
    val manager = remember { RequestManager(activity) }
    val pending by manager.getPendingRequestsFlow().collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()

    val biometricResult by promptManager.promptResults.collectAsState(initial = null)
    val enrollLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = {
            println("Activity result: $it")
        }
    )

    LaunchedEffect(biometricResult) {
        if (biometricResult is BiometricPromptManager.BiometricResult.AuthenticationNotSet) {
            val enrollIntent = Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                putExtra(
                    Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                    BIOMETRIC_STRONG or DEVICE_CREDENTIAL
                )
            }
            enrollLauncher.launch(enrollIntent)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Pending requests", fontSize = 24.sp,
            modifier = Modifier.padding(top = 75.dp, bottom = 20.dp, start = 50.dp))
        Column(
            modifier = Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            pending.forEach { request ->
                Surface(modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)) {
                    Row(modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "${request.tool.name} (Risk: ${request.tool.riskLevel}")
                        Spacer(modifier = Modifier.weight(1f))
                        Button(onClick = {
                            coroutineScope.launch { rejectRequest(request, manager) }
                        }) {
                            Text("Reject")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            coroutineScope.launch { approveRequest(request, manager) }
                        }) {
                            Text("Approve")
                        }
                    }
                }
                Spacer(modifier = Modifier.heightIn(min = 8.dp))
            }

            if (pending.isEmpty()) {
                Text("No pending requests")
            }

            Button(onClick = {
                promptManager.showBiometricPrompt("Authenticate", "Test auth")
            }) {
                Text(text = "Authenticate")
            }

            if (biometricResult != null) {
                Text(text = biometricResult.toString())
            }
        }
    }
}

@OptIn(InternalSerializationApi::class)
suspend fun rejectRequest(request: ApprovalRequest, manager: RequestManager) {
    manager.updateRequestState(request.requestId, "rejected")
}

@OptIn(InternalSerializationApi::class)
suspend fun approveRequest(request: ApprovalRequest, manager: RequestManager) {
    // TODO: biometric auth, send signed approval to server

    manager.updateRequestState(request.requestId, "approved")
}