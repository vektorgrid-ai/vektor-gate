package com.example.vektorgate.requests

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.example.vektorgate.requests.db.ApprovalRequestDatabase
import com.example.vektorgate.requests.db.ToolInfoEntity
import com.example.vektorgate.security.ApprovalRequest
import com.example.vektorgate.requests.db.ApprovalRequestEntity
import com.example.vektorgate.security.ToolInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.InternalSerializationApi

class RequestManager(context: Context) {
    private val db = Room.databaseBuilder(
        context.applicationContext,
        ApprovalRequestDatabase::class.java, "approval_requests"
    ).build()

    private val requestDao = db.approvalRequestDao()
    private val toolInfoDao = db.toolInfoDao()

    @OptIn(InternalSerializationApi::class)
    suspend fun insertRequest(request: ApprovalRequest) {
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

        Log.i("RequestManager", "Inserted request with id ${request.request_id}")
    }

    @OptIn(InternalSerializationApi::class)
    suspend fun getPendingRequests(): List<ApprovalRequest> {
        val pendingRequests = requestDao.getPending()
        val result = mutableListOf<ApprovalRequest>()

        for (request in pendingRequests) {
            val toolInfo = toolInfoDao.getById(request.tool) ?: continue
            result.add(requestFromEntities(request, toolInfo))
        }

        return result
    }

    @OptIn(InternalSerializationApi::class)
    fun getPendingRequestsFlow(): Flow<List<ApprovalRequest>> {
        return requestDao.getPendingFlow().map { entities ->
            entities.mapNotNull { entity ->
                val toolInfo = toolInfoDao.getById(entity.tool)
                if (toolInfo != null) requestFromEntities(entity, toolInfo) else null
            }
        }
    }

    @OptIn(InternalSerializationApi::class)
    suspend fun getById(id: String): ApprovalRequest? {
        val requestEntity = requestDao.getById(id) ?: return null;
        val toolInfo = toolInfoDao.getById(requestEntity.tool) ?: return null

        return requestFromEntities(requestEntity, toolInfo)
    }

    @OptIn(InternalSerializationApi::class)
    suspend fun updateRequestState(id: String, state: String) {
        requestDao.updateState(id, state)
    }

    @OptIn(InternalSerializationApi::class)
    fun requestFromEntities(request: ApprovalRequestEntity, toolInfo: ToolInfoEntity): ApprovalRequest {
        return ApprovalRequest(
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
    }
}