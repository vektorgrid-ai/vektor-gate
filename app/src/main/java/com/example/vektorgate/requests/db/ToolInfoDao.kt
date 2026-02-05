package com.example.vektorgate.requests.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ToolInfoDao {
    @Insert
    suspend fun insert(info: ToolInfoEntity)

    @Query("SELECT * FROM tool_info")
    suspend fun getAll(): List<ToolInfoEntity>
}