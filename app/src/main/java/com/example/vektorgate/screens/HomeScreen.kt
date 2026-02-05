package com.example.vektorgate.screens

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.vektorgate.data.SettingsManager

@Composable
fun HomeScreen(
    activity: AppCompatActivity,
    onConfigureCoreUrl: () -> Unit
) {
    val coreUrl by SettingsManager.getInstance(activity).coreUrl
        .collectAsState(initial = null)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (coreUrl.isNullOrEmpty()) {
            Text("Please configure the core URL")
            Button(
                onClick = onConfigureCoreUrl,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Configure now")
            }
        } else {
            Text("Welcome to the Dashboard")
            Text(
                text = "Connected to: $coreUrl",
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
