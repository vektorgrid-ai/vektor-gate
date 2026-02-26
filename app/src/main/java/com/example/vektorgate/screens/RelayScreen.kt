package com.example.vektorgate.screens

import android.annotation.SuppressLint
import androidx.activity.ComponentActivity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.vektorgate.data.SettingsManager
import com.example.vektorgate.relay.RelayWebsocketClient
import com.example.vektorgate.relay.audio.AudioManager
import kotlinx.coroutines.launch
import com.example.vektorgate.relay.RelayWebsocketClient.RelayStatus.*

@Composable
fun RelayScreen(activity: ComponentActivity) {
    val settingsManager = remember { SettingsManager.getInstance(activity) }
    val coreUrl by settingsManager.coreUrl.collectAsState(initial = "")
    val client = RelayWebsocketClient
    val status by client.status.collectAsState()
    val scope = rememberCoroutineScope()

    // Implicitly handle connection when URL is available or changes
    LaunchedEffect(coreUrl) {
        if (coreUrl.isNotEmpty() && status == DISCONNECTED) {
            client.connect("$coreUrl/ws/satellite")
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (coreUrl.isEmpty()) {
            Text(
                text = "Please configure Core URL in Settings",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
        } else {
            RelayButton(
                status = status,
                onClick = @SuppressLint("MissingPermission") {
                    when (status) {
                        DISCONNECTED -> {
                            client.connect("$coreUrl/ws/satellite")
                        }
                        READY, CONNECTED -> {
                            scope.launch {
                                if (AudioManager.ensureRecordingPermissionGranted(activity)) {
                                    client.startSession()
                                }
                            }
                        }
                        STREAMING_AUDIO -> {
                            client.endSession()
                        }
                        else -> {
                            // Button is deactivated for PROCESSING, CONNECTING, etc.
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(48.dp))

            StatusIndicator(status)
        }
    }
}

@Composable
fun RelayButton(
    status: RelayWebsocketClient.RelayStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isActive = status == STREAMING_AUDIO || 
                   status == PLAYING_TTS
    
    val isDeactivated = status == PROCESSING || 
                        status == CONNECTING

    // Animations
    val backgroundColor by animateColorAsState(
        targetValue = when (status) {
            STREAMING_AUDIO -> Color(0xFFE53935) // Red
            PLAYING_TTS -> Color(0xFF1E88E5) // Blue
            PROCESSING -> Color.Gray
            DISCONNECTED -> Color(0xFF546E7A) // Slate
            CONNECTING -> Color(0xFFB0BEC5)
            else -> MaterialTheme.colorScheme.primary
        },
        animationSpec = tween(500),
        label = "buttonColor"
    )

    val size by animateDpAsState(
        targetValue = if (isActive) 160.dp else 140.dp,
        animationSpec = tween(300),
        label = "buttonSize"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val finalScale = if (isActive) pulseScale else 1f

    Box(
        modifier = modifier
            .size(size)
            .scale(finalScale)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(enabled = !isDeactivated) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        val icon: ImageVector = when (status) {
            STREAMING_AUDIO -> Icons.Rounded.Mic
            PLAYING_TTS -> Icons.AutoMirrored.Rounded.VolumeUp
            PROCESSING -> Icons.Rounded.HourglassEmpty
            DISCONNECTED -> Icons.Rounded.CloudOff
            CONNECTING -> Icons.Rounded.Refresh
            else -> Icons.Rounded.PlayArrow
        }

        Icon(
            imageVector = icon,
            contentDescription = status.name,
            tint = Color.White,
            modifier = Modifier.size(size / 2.5f)
        )
    }
}

@Composable
fun StatusIndicator(status: RelayWebsocketClient.RelayStatus) {
    val text = when (status) {
        DISCONNECTED -> "Disconnected"
        CONNECTING, CONNECTED -> "Connecting..."
        READY -> "Ready to Talk"
        STREAMING_AUDIO -> "Listening..."
        PROCESSING -> "Processing..."
        PLAYING_TTS -> "Speaking..."
    }

    val color by animateColorAsState(
        targetValue = if (status == STREAMING_AUDIO) 
            Color(0xFFE53935) else MaterialTheme.colorScheme.onSurface,
        label = "textColor"
    )

    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Medium,
        color = color
    )
}
