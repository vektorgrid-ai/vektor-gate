package com.example.vektorgate

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.vektorgate.screens.HomeScreen
import com.example.vektorgate.screens.SettingsScreen
import com.example.vektorgate.screens.VerifyScreen
import com.example.vektorgate.security.biometric.BiometricPromptManager
import com.example.vektorgate.ui.theme.VektorGateTheme

class MainActivity : AppCompatActivity() {
    val promptManager by lazy { BiometricPromptManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VektorGateTheme {
                VektorGateApp(this, promptManager)
            }
        }
    }
}

@Composable
fun VektorGateApp(activity: AppCompatActivity, promptManager: BiometricPromptManager) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.filter { it.inNavbar }.forEach {
                item(
                    icon = {
                        Icon(
                            it.icon,
                            contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        when (currentDestination) {
            AppDestinations.HOME -> HomeScreen(activity, onConfigureCoreUrl = {
                currentDestination = AppDestinations.SETTINGS
            })
            AppDestinations.VERIFY -> VerifyScreen(promptManager)
            AppDestinations.SETTINGS -> SettingsScreen(activity)
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