package com.example.vektorgate.requests.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ToolInfoDao {
    @Insert
    fun insert(info: ToolInfo)

    @Query("SELECT * FROM tool_info")
    fun getAll(): List<ToolInfo>
}