package com.example.vektorgate.requests.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ApprovalRequestDao {
    @Insert
    fun insert(request: ApprovalRequestEntity)

    @Query("SELECT * FROM approval_request")
    fun getAll(): List<ApprovalRequestEntity>

    @Query("SELECT * FROM approval_request WHERE request_id = :id")
    fun getById(id: String): List<ApprovalRequestEntity>

    @Query("SELECT * FROM approval_request WHERE lower(state) = \"pending\"")
    fun getPending(): List<ApprovalRequestEntity>
}