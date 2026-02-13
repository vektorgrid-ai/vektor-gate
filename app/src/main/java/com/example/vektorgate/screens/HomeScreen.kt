package com.example.vektorgate.screens

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vektorgate.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Dashboard(serverUrl: String, currentDeviceId: String) {
    var workers by remember { mutableStateOf<List<Worker>>(emptyList()) }
    var companions by remember { mutableStateOf<List<Companion>>(emptyList()) }
    var satellites by remember { mutableStateOf<List<Satellite>>(emptyList()) }
    var health by remember { mutableStateOf<ServerHealth?>(null) }
    var logs by remember { mutableStateOf<List<LogEntry>>(emptyList()) }
    
    var logSearchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val client = remember { OkHttpClient() }
    val json = remember { Json { ignoreUnknownKeys = true } }
    val scope = rememberCoroutineScope()

    val fetchData = suspend {
        try {
            isLoading = true
            errorMessage = null

            withContext(Dispatchers.IO) {
                val workersReq = Request.Builder().url("$serverUrl/worker").build()
                val companionsReq = Request.Builder().url("$serverUrl/companion").build()
                val satellitesReq = Request.Builder().url("$serverUrl/satellite").build()
                val healthReq = Request.Builder().url("$serverUrl/health").build()
                val logsReq = Request.Builder().url("$serverUrl/logs?limit=50").build()

                client.newCall(workersReq).execute().use { response ->
                    if (response.isSuccessful) workers = json.decodeFromString(response.body.string())
                }
                client.newCall(companionsReq).execute().use { response ->
                    if (response.isSuccessful) companions = json.decodeFromString(response.body.string())
                }
                client.newCall(satellitesReq).execute().use { response ->
                    if (response.isSuccessful) satellites = json.decodeFromString(response.body.string())
                }
                client.newCall(healthReq).execute().use { response ->
                    if (response.isSuccessful) health = json.decodeFromString(response.body.string())
                }
                client.newCall(logsReq).execute().use { response ->
                    if (response.isSuccessful) logs = json.decodeFromString(response.body.string())
                }
            }
        } catch (e: Exception) {
            errorMessage = e.message ?: "Failed to fetch data"
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(serverUrl) {
        fetchData()
    }

    PullToRefreshBox(
        isRefreshing = isLoading,
        onRefresh = { scope.launch { fetchData() } },
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp, top = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Vektor Gate Dashboard",
                    style = MaterialTheme.typography.headlineMedium
                )
                IconButton(onClick = { scope.launch { fetchData() } }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }

            if (isLoading && workers.isEmpty() && health == null) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else if (errorMessage != null && workers.isEmpty() && health == null) {
                Text("Error: $errorMessage", color = MaterialTheme.colorScheme.error)
            } else {
                if (errorMessage != null) {
                    Text(
                        "Update failed: $errorMessage",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                health?.let {
                    ServerHealthSection(it)
                }

                DashboardSection(
                    title = "Workers",
                    icon = Icons.Default.Build,
                    count = workers.size
                ) {
                    workers.forEach { worker ->
                        DashboardItem(
                            title = worker.workerId,
                            subtitle = "Type: ${worker.type}",
                            caption = "Last seen: ${formatTimestamp(worker.lastSeen)}"
                        )
                    }
                }

                DashboardSection(
                    title = "Connected Companions",
                    icon = Icons.Default.Phone,
                    count = companions.size
                ) {
                    companions.forEach { companion ->
                        val isThisDevice = currentDeviceId.startsWith(companion.truncDeviceId)
                        DashboardItem(
                            title = if (isThisDevice) "${companion.deviceName} (This Device)" else companion.deviceName,
                            subtitle = if (companion.isApproved) "Approved" else "Pending Approval",
                            caption = "ID: ${companion.truncDeviceId}...",
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

                LogsSection(
                    logs = logs,
                    searchQuery = logSearchQuery,
                    onSearchQueryChange = { logSearchQuery = it }
                )
            }
        }
    }
}

@Composable
fun ServerHealthSection(health: ServerHealth) {
    DashboardSection(
        title = "Server Health",
        icon = Icons.Default.Favorite,
        count = if (health.status == "ok") 1 else 0
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusColor = if (health.status == "ok") Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                    Box(modifier = Modifier.size(12.dp).background(statusColor, RoundedCornerShape(6.dp)))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Status: ${health.status.uppercase()}", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.weight(1f))
                    Text("v${health.application.version}", style = MaterialTheme.typography.bodySmall)
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text("Uptime: ${health.uptime.uptime}", style = MaterialTheme.typography.bodyMedium)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                ResourceMetric(
                    label = "CPU Usage",
                    value = "${health.server.cpu.cpuPercentSinceStart}%",
                    progress = (health.server.cpu.cpuPercentSinceStart / 100).toFloat(),
                    details = "${health.server.cpu.cpuCount} Cores"
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                val usedMemGB = health.server.memory.workingSetBytes / 1024.0 / 1024.0 / 1024.0
                val totalMemGB = health.server.memory.systemTotalBytes / 1024.0 / 1024.0 / 1024.0
                ResourceMetric(
                    label = "Memory Usage",
                    value = "%.2f / %.2f GB".format(usedMemGB, totalMemGB),
                    progress = (health.server.memory.percentOfSystem / 100).toFloat().coerceIn(0f, 1f),
                    details = "${health.server.memory.percentOfSystem}% of system"
                )
            }
        }
    }
}

@Composable
fun ResourceMetric(label: String, value: String, progress: Float, details: String) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(8.dp).padding(vertical = 2.dp),
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        Text(details, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun LogsSection(
    logs: List<LogEntry>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit
) {
    val filteredLogs = remember(logs, searchQuery) {
        if (searchQuery.isEmpty()) logs
        else logs.filter { it.message.contains(searchQuery, ignoreCase = true) || it.level.contains(searchQuery, ignoreCase = true) }
    }

    DashboardSection(
        title = "Recent Logs",
        icon = Icons.Default.List,
        count = filteredLogs.size
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            placeholder = { Text("Search logs...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Column(modifier = Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
            filteredLogs.forEach { log ->
                LogItem(log)
            }
        }
    }
}

@Composable
fun LogItem(log: LogEntry) {
    val levelColor = when (log.level.lowercase()) {
        "error", "critical" -> MaterialTheme.colorScheme.error
        "warning" -> Color(0xFFFFA000)
        "information" -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = log.level.uppercase(),
                    color = levelColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    modifier = Modifier.background(levelColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatTimestamp(log.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = log.message, style = MaterialTheme.typography.bodySmall)
        }
    }
}

fun formatTimestamp(timestamp: String): String {
    return try {
        val dt = OffsetDateTime.parse(timestamp)
        dt.format(DateTimeFormatter.ofPattern("MMM dd, HH:mm:ss"))
    } catch (e: Exception) {
        timestamp
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
