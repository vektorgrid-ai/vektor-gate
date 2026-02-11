package com.example.vektorgate.screens

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vektorgate.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.OffsetDateTime

@Composable
fun HomeScreen(
    activity: AppCompatActivity,
    onConfigureCoreUrl: () -> Unit
) {
    val settingsManager = SettingsManager.getInstance(activity)
    val coreUrl by settingsManager.coreUrl.collectAsState(initial = "")
    val connectionStatus by settingsManager.connectionStatus.collectAsState()
    val currentDeviceId by settingsManager.deviceId.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = if (coreUrl.isEmpty() || connectionStatus != ConnectionStatus.CONNECTED) Arrangement.Center else Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (coreUrl.isEmpty()) {
            Text("Please configure the core URL in Settings")
            Button(
                onClick = onConfigureCoreUrl,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Go to Settings")
            }
        } else if (connectionStatus != ConnectionStatus.CONNECTED) {
            Text(
                text = "Dashboard unavailable.\nConnection to server could not be established.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Attempting to connect to: $coreUrl",
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            Dashboard(coreUrl, currentDeviceId ?: "")
        }
    }
}

@Composable
fun Dashboard(serverUrl: String, currentDeviceId: String) {
    var workers by remember { mutableStateOf<List<Worker>>(emptyList()) }
    var companions by remember { mutableStateOf<List<Companion>>(emptyList()) }
    var satellites by remember { mutableStateOf<List<Satellite>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val client = remember { OkHttpClient() }
    val json = remember { Json { ignoreUnknownKeys = true } }

    LaunchedEffect(serverUrl) {
        try {
            isLoading = true
            errorMessage = null

            withContext(Dispatchers.IO) {
                val workersReq = Request.Builder().url("$serverUrl/worker").build()
                val companionsReq = Request.Builder().url("$serverUrl/companion").build()
                val satellitesReq = Request.Builder().url("$serverUrl/satellite").build()

                client.newCall(workersReq).execute().use { response ->
                    if (response.isSuccessful) {
                        workers = json.decodeFromString<List<Worker>>(response.body.string())
                    }
                }
                client.newCall(companionsReq).execute().use { response ->
                    if (response.isSuccessful) {
                        companions = json.decodeFromString<List<Companion>>(response.body.string())
                    }
                }
                client.newCall(satellitesReq).execute().use { response ->
                    if (response.isSuccessful) {
                        satellites = json.decodeFromString<List<Satellite>>(response.body.string())
                    }
                }
            }
        } catch (e: Exception) {
            errorMessage = e.message ?: "Failed to fetch data"
        } finally {
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            "Vektor Gate Dashboard",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp, top = 24.dp)
        )

        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        } else if (errorMessage != null) {
            Text("Error: $errorMessage", color = MaterialTheme.colorScheme.error)
        } else {
            DashboardSection(
                title = "Workers",
                icon = Icons.Default.Build,
                count = workers.size
            ) {
                workers.forEach { worker ->
                    DashboardItem(
                        title = worker.workerId,
                        subtitle = "Type: ${worker.type}",
                        caption = "Last seen: ${OffsetDateTime.parse(worker.lastSeen).toLocalDateTime()}"
                    )
                }
            }

            DashboardSection(
                title = "Connected Companions",
                icon = Icons.Default.Phone,
                count = companions.size
            ) {
                companions.forEach { companion ->
                    val isThisDevice = companion.deviceId == currentDeviceId
                    DashboardItem(
                        title = if (isThisDevice) "${companion.deviceName} (This Device)" else companion.deviceName,
                        subtitle = if (companion.isApproved) "Approved" else "Pending Approval",
                        caption = "ID: ${companion.deviceId.take(8)}...",
                        highlight = isThisDevice
                    )
                }
            }

            DashboardSection(
                title = "Voice Satellites",
                icon = Icons.Default.LocationOn,
                count = satellites.size
            ) {
                satellites.forEach { satellite ->
                    DashboardItem(
                        title = satellite.deviceName,
                        subtitle = "State: ${satellite.connectionState}",
                        caption = "ID: ${satellite.connectionId.take(8)}..."
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardSection(
    title: String,
    icon: ImageVector,
    count: Int,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = title, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.weight(1f))
            Badge { Text(count.toString()) }
        }
        content()
        HorizontalDivider(modifier = Modifier.padding(top = 16.dp), thickness = 0.5.dp)
    }
}

@Composable
fun DashboardItem(
    title: String,
    subtitle: String,
    caption: String,
    highlight: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (highlight) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = caption,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
