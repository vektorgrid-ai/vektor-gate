package com.example.vektorgate.requests.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ApprovalRequest::class, ToolInfo::class], version = 1, exportSchema = true)
abstract class ApprovalRequestDatabase : RoomDatabase() {
    abstract fun approvalRequestDao(): ApprovalRequestDao
    abstract fun toolInfoDao(): ToolInfoDao
}