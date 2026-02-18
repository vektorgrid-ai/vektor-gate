package com.example.vektorgate.relay

import kotlinx.serialization.Serializable

@Serializable
class HelloMessage(
    val type: String,
    val protocol_version: Int,
    val satellite_id: String,
    val area: String,
    val language: String,
    val capabilities: RelayCapabilities,
    val audio_format: RelayAudioFormat
)
@Serializable
class RelayCapabilities(
    val speaker: Boolean,
    val display: Boolean,
    val supports_barge_in: Boolean,
    val supports_streaming_tts: Boolean
)
@Serializable
class RelayAudioFormat(
    val encoding: String,
    val sample_rate: Int,
    val channels: Int,
    val frame_ms: Int
)
@Serializable
class SessionStartMessage(
    val type: String,
    val timestamp: Int,
    val session_id: String
)
@Serializable
class AudioEndMessage(
    val type: String,
    val session_id: String,
    val reason: String
)