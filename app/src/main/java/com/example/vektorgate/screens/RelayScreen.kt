package com.example.vektorgate.screens

import android.annotation.SuppressLint
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.vektorgate.data.SettingsManager
import com.example.vektorgate.relay.RelayWebsocketClient
import com.example.vektorgate.relay.audio.AudioManager
import kotlinx.coroutines.launch

@Composable
fun RelayScreen(activity: ComponentActivity) {
    val settingsManager = SettingsManager.getInstance(activity)
    val coreUrl by settingsManager.coreUrl.collectAsState(initial = "")
    val client = remember { RelayWebsocketClient() }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize(),
        Arrangement.Center,
        Alignment.CenterHorizontally) {
        Button(onClick = {
            client.connect("$coreUrl/ws/satellite")
        }) {
            Text(text = "Connect to server")
        }

        // MissingPermission already checked in ensureRecordPermissionGranted
        Button(onClick = @SuppressLint("MissingPermission") {
            scope.launch {
                if (AudioManager.ensureRecordingPermissionGranted(activity)) {
                    client.startSession()
                }
            }
        }) {
            Text(text = "Start session")
        }

        Button(onClick = {
            client.endSession()
        }) {
            Text(text = "End session")
        }

        Button(onClick = {
            client.disconnect()
        }) {
            Text(text = "Disconnect")
        }
    }
}