package com.example.vektorgate.security

import kotlinx.serialization.Serializable
import java.time.LocalDateTime

class ApprovalRequest(
    val type: String,
    val requestId: String,
    val tool: ToolInfo,
    val payloadHash: String,
    val nonce: String,
    val expiresAt: LocalDateTime
)

data class ToolInfo(
    val name: String,
    val description: String,
    val riskLevel: String
)

@Serializable
class ApprovalResponse(
    val request_id: String,
    val device_id: String,
    val decision: String,
    val timestamp: Long,
    val signature: String,
    val public_key: String,
    val payload_hash: String
)
