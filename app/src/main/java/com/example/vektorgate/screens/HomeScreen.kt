package com.example.vektorgate.screens

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.vektorgate.data.ConnectionStatus
import com.example.vektorgate.data.SettingsManager

@Composable
fun HomeScreen(
    activity: AppCompatActivity,
    onConfigureCoreUrl: () -> Unit
) {
    val settingsManager = SettingsManager.getInstance(activity)
    val coreUrl by settingsManager.coreUrl.collectAsState(initial = "")
    val connectionStatus by settingsManager.connectionStatus.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
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
            Text("Welcome to the Dashboard", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = "Connected and active",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
