package com.example.vektorgate.requests

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.example.vektorgate.requests.db.ApprovalRequestDatabase
import com.example.vektorgate.security.ApprovalRequest
import com.example.vektorgate.requests.db.ApprovalRequestEntity
import com.example.vektorgate.security.ToolInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import java.time.ZoneOffset

class RequestManager private constructor(context: Context) {
    private val db = Room.databaseBuilder(
        context.applicationContext,
        ApprovalRequestDatabase::class.java, "approval_requests"
    ).build()

    private val requestDao = db.approvalRequestDao()

    suspend fun insertRequest(request: ApprovalRequest) {
        val requestEntity = ApprovalRequestEntity(
            requestId = request.requestId,
            tool = request.tool.name,
            description = request.tool.description,
            riskLevel = request.tool.riskLevel,
            payloadHash = request.payloadHash,
            expiresAt = request.expiresAt.toEpochSecond(ZoneOffset.UTC),
            type = request.type,
            nonce = request.nonce,
            state = "pending"
        )

        requestDao.insert(requestEntity)

        Log.i("RequestManager", "Inserted request with id ${request.requestId}")
    }

    suspend fun getPendingRequests(): List<ApprovalRequest> {
        val pendingRequests = requestDao.getPending()
        val result = mutableListOf<ApprovalRequest>()

        for (request in pendingRequests) {
            result.add(requestFromEntity(request))
        }

        return result
    }

    fun getPendingRequestsFlow(): Flow<List<ApprovalRequest>> {
        return requestDao.getPendingFlow().map { entities ->
            entities.map { entity ->
                requestFromEntity(entity)
            }
        }
    }

    suspend fun getById(id: String): ApprovalRequest? {
        val requestEntity = requestDao.getById(id) ?: return null;

        return requestFromEntity(requestEntity)
    }

    suspend fun updateRequestState(id: String, state: String) {
        requestDao.updateState(id, state)
    }

    fun requestFromEntity(request: ApprovalRequestEntity): ApprovalRequest {
        return ApprovalRequest(
            type = request.type,
            requestId = request.requestId,
            tool = ToolInfo(
                name = request.tool,
                description = request.description,
                riskLevel = request.riskLevel
            ),
            payloadHash = request.payloadHash,
            nonce = request.nonce,
            expiresAt = LocalDateTime.ofEpochSecond(request.expiresAt, 0, ZoneOffset.UTC)
        )
    }

    companion object {
        @Volatile
        private var INSTANCE: RequestManager? = null

        fun getInstance(context: Context): RequestManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RequestManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}
