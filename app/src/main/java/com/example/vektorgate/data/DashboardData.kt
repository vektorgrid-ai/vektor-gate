package com.example.vektorgate.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Worker(
    @SerialName("worker_id") val workerId: String,
    @SerialName("type") val type: String,
    @SerialName("last_seen") val lastSeen: String,
    @SerialName("capabilities") val capabilities: WorkerCapabilities
)
@Serializable
data class WorkerCapabilities(
    @SerialName("supports_streaming") val supportsStreaming: Boolean,
    @SerialName("supports_tools") val supportsTools: Boolean,
    @SerialName("models") val models: List<String>
)

@Serializable
data class Companion(
    @SerialName("device_id") val deviceId: String,
    @SerialName("device_name") val deviceName: String,
    @SerialName("is_approved") val isApproved: Boolean,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class Satellite(
    @SerialName("connection_id") val connectionId: String,
    @SerialName("connection_state") val connectionState: String,
    @SerialName("device_name") val deviceName: String
)
