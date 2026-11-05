package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: MessageLogEntity): Long

    @Query("SELECT * FROM message_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<MessageLogEntity>>

    @Query("SELECT * FROM message_logs WHERE sourceNode = :nodeName OR targetNode = :nodeName ORDER BY timestamp DESC")
    fun getLogsByNode(nodeName: String): Flow<List<MessageLogEntity>>

    @Query("SELECT * FROM message_logs WHERE messageType = :type ORDER BY timestamp DESC")
    fun getLogsByType(type: String): Flow<List<MessageLogEntity>>

    @Query("SELECT COUNT(*) FROM message_logs")
    fun getTotalLogsCount(): Flow<Int>

    @Query("DELETE FROM message_logs")
    suspend fun clearAll()

    @Delete
    suspend fun deleteLog(log: MessageLogEntity)
}
