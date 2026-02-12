package com.example.vektorgate.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

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

@Serializable
data class ServerHealth(
    val status: String,
    val timestamp: String,
    val uptime: Uptime,
    val server: ServerInfo,
    val application: ApplicationInfo
)

@Serializable
data class Uptime(
    @SerialName("process_start_utc") val processStartUtc: String,
    @SerialName("uptime_seconds") val uptimeSeconds: Long,
    val uptime: String
)

@Serializable
data class ServerInfo(
    val pid: Int,
    val cpu: CpuInfo,
    val memory: MemoryInfo
)

@Serializable
data class CpuInfo(
    @SerialName("cpu_count") val cpuCount: Int,
    @SerialName("total_processor_time_ms") val totalProcessorTimeMs: Long,
    @SerialName("cpu_percent_since_start") val cpuPercentSinceStart: Double
)

@Serializable
data class MemoryInfo(
    @SerialName("working_set_bytes") val workingSetBytes: Long,
    @SerialName("system_total_bytes") val systemTotalBytes: Long,
    @SerialName("percent_of_system") val percentOfSystem: Double
)

@Serializable
data class ApplicationInfo(
    val assembly: String,
    val version: String
)

@Serializable
data class LogEntry(
    val timestamp: String,
    val level: String,
    val message: String,
    val exception: String? = null,
    val properties: Map<String, JsonElement>? = null
)
