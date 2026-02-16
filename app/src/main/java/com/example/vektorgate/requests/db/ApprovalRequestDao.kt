package com.example.vektorgate.requests.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ApprovalRequestDao {
    @Insert
    suspend fun insert(request: ApprovalRequestEntity)

    @Query("SELECT * FROM approval_request")
    suspend fun getAll(): List<ApprovalRequestEntity>

    @Query("SELECT * FROM approval_request WHERE request_id = :id")
    suspend fun getById(id: String): ApprovalRequestEntity?

    @Query("SELECT * FROM approval_request WHERE lower(state) = 'pending'")
    suspend fun getPending(): List<ApprovalRequestEntity>

    @Query("SELECT * FROM approval_request WHERE lower(state) = 'pending'")
    fun getPendingFlow(): Flow<List<ApprovalRequestEntity>>

    @Query("UPDATE approval_request SET state = :state WHERE request_id = :id")
    suspend fun updateState(id: String, state: String)
}