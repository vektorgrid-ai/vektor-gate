package com.example.vektorgate.requests.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "approval_request",
    foreignKeys = [
        ForeignKey(
            entity = ToolInfoEntity::class,
            parentColumns = ["id"],
            childColumns = ["tool_id"],
            onDelete = ForeignKey.CASCADE
    )]
)
data class ApprovalRequestEntity(
    @PrimaryKey @ColumnInfo(name = "request_id") val requestId: String,
    @ColumnInfo(name = "tool_id") val tool: String,
    @ColumnInfo(name = "payload_hash") val payloadHash: String,
    @ColumnInfo(name = "expires_at") val expiresAt: Long,
    val type: String,
    val nonce: String,
    val state: String
)

@Entity(tableName = "tool_info")
data class ToolInfoEntity(
    @PrimaryKey val id: String,
    val description: String,
    @ColumnInfo(name = "risk_level") val riskLevel: String
)
