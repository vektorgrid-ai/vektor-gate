package com.example.vektorgate.requests

import android.content.Context
import androidx.room.Room
import com.example.vektorgate.requests.db.ApprovalRequestDatabase
import com.example.vektorgate.requests.db.ToolInfoEntity
import com.example.vektorgate.security.ApprovalRequest
import com.example.vektorgate.requests.db.ApprovalRequestEntity
import com.example.vektorgate.security.ToolInfo
import kotlinx.serialization.InternalSerializationApi

class RequestManager {
    // TODO: don't create a new db instance every time

    @OptIn(InternalSerializationApi::class)
    fun insertRequest(applicationContext: Context, request: ApprovalRequest) {
        val db = Room.databaseBuilder(
            applicationContext,
            ApprovalRequestDatabase::class.java, "approval_requests"
        ).build()

        val requestDao = db.approvalRequestDao()
        val toolInfoDao = db.toolInfoDao()

        val toolInfo = ToolInfoEntity(
            id = request.tool.id,
            description = request.tool.description,
            riskLevel = request.tool.risk_level
        )
        val requestEntity = ApprovalRequestEntity(
            requestId = request.request_id,
            tool = request.tool.id,
            payloadHash = request.payload_hash,
            expiresAt = request.expires_at,
            type = request.type,
            nonce = request.nonce,
            state = "pending"
        )

        toolInfoDao.insert(toolInfo)
        requestDao.insert(requestEntity)
    }

    @OptIn(InternalSerializationApi::class)
    fun getPendingRequests(applicationContext: Context): List<ApprovalRequest> {
        val db = Room.databaseBuilder(
            applicationContext,
            ApprovalRequestDatabase::class.java, "approval_requests"
        ).build()

        val requestDao = db.approvalRequestDao()
        val toolInfoDao = db.toolInfoDao()

        val pendingRequests = requestDao.getPending()
        val result = mutableListOf<ApprovalRequest>()

        for (request in pendingRequests) {
            val toolInfo = toolInfoDao.getAll().find { it.id == request.tool }
            if (toolInfo == null) continue

            val approvalRequest = ApprovalRequest(
                type = request.type,
                request_id = request.requestId,
                tool = ToolInfo(
                    id = toolInfo.id,
                    description = toolInfo.description,
                    risk_level = toolInfo.riskLevel
                ),
                payload_hash = request.payloadHash,
                nonce = request.nonce,
                expires_at = request.expiresAt
            )
            result.add(approvalRequest)
        }

        return result
    }
}