package com.example.vektorgate

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.vektorgate.data.ConnectionStatus
import com.example.vektorgate.data.SettingsManager
import com.example.vektorgate.requests.EnrollmentManager
import com.example.vektorgate.screens.HomeScreen
import com.example.vektorgate.screens.SettingsScreen
import com.example.vektorgate.screens.VerifyScreen
import com.example.vektorgate.security.biometric.BiometricPromptManager
import com.example.vektorgate.ui.theme.VektorGateTheme
import com.example.vektorgate.security.SecurityManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var enrollmentManager: EnrollmentManager
    private lateinit var settingsManager: SettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        settingsManager = SettingsManager.getInstance(this)
        enrollmentManager = EnrollmentManager(SecurityManager(), settingsManager)

        askNotificationPermission()
        observeCoreUrlChanges()

        setContent {
            VektorGateTheme {
                VektorGateApp(this, settingsManager)
            }
        }
    }

    private fun observeCoreUrlChanges() {
        lifecycleScope.launch {
            settingsManager.coreUrl
                .distinctUntilChanged()
                .collectLatest { coreUrl ->
                    if (coreUrl.isNotEmpty()) {
                        val deviceName = settingsManager.deviceName.first()
                        val firebaseToken = settingsManager.firebaseToken.first()
                        enrollmentManager.enroll(coreUrl, deviceName, firebaseToken)
                    } else {
                        settingsManager.setConnectionStatus(ConnectionStatus.DISCONNECTED)
                        settingsManager.setDeviceId(null)
                    }
                }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted: Boolean ->
        if (isGranted) {
            // FCM SDK (and your app) can post notifications.
        } else {
            // TODO: Inform user that that your app will not show notifications.
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                // FCM SDK (and your app) can post notifications.
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                // TODO: display rationale
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Composable
fun VektorGateApp(activity: AppCompatActivity, settingsManager: SettingsManager) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    val promptManager = remember { BiometricPromptManager(activity) }
    val connectionStatus by settingsManager.connectionStatus.collectAsState()

    Column {
        if (connectionStatus != ConnectionStatus.CONNECTED) {
            val (statusText, statusColor) = when (connectionStatus) {
                ConnectionStatus.DISCONNECTED -> "Disconnected from Server" to Color.Red
                ConnectionStatus.CONNECTING -> "Connecting to Server..." to Color(0xFFFFA500) // Orange
                else -> "" to Color.Transparent
            }
            
            if (statusText.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(statusColor)
                        .statusBarsPadding()
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = statusText,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        NavigationSuiteScaffold(
            navigationSuiteItems = {
                AppDestinations.entries.filter { it.inNavbar }.forEach { item ->
                    item(
                        icon = {
                            Icon(
                                item.icon,
                                contentDescription = item.label
                            )
                        },
                        label = { Text(item.label) },
                        selected = item == currentDestination,
                        onClick = { currentDestination = item }
                    )
                }
            }
        ) {
            when (currentDestination) {
                AppDestinations.HOME -> HomeScreen(activity, onConfigureCoreUrl = {
                    currentDestination = AppDestinations.SETTINGS
                })
                AppDestinations.VERIFY -> VerifyScreen(activity, promptManager)
                AppDestinations.SETTINGS -> SettingsScreen(activity)
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
    val inNavbar: Boolean
) {
    HOME("Dashboard", Icons.Default.Home, true),
    VERIFY("Verify", Icons.Default.Lock, true),
    SETTINGS("Settings", Icons.Default.Settings, true),
}
