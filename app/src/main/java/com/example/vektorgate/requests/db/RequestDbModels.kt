package com.example.vektorgate.requests.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "approval_request")
data class ApprovalRequestEntity(
    @PrimaryKey @ColumnInfo(name = "request_id") val requestId: String,
    @ColumnInfo(name = "payload_hash") val payloadHash: String,
    @ColumnInfo(name = "expires_at") val expiresAt: Long,
    val type: String,
    val nonce: String,
    val state: String,
    val tool: String,
    val description: String,
    @ColumnInfo(name= "risk_level") val riskLevel: String
)
