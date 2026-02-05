package com.example.vektorgate.security

import kotlinx.serialization.Serializable

@Serializable
@kotlinx.serialization.InternalSerializationApi
data class ApprovalRequest(
    val type: String,
    val request_id: String,
    val tool: ToolInfo,
    val payload_hash: String,
    val nonce: String,
    val expires_at: Long
)

@Serializable
@kotlinx.serialization.InternalSerializationApi
data class ToolInfo(
    val id: String,
    val description: String,
    val risk_level: String
)

@Serializable
@kotlinx.serialization.InternalSerializationApi
class ApprovalResponse(
    val type: String = "approval_response",
    val request_id: String,
    val decision: String,
    val timestamp: Long,
    val signature: String,
    val public_key: String
)
