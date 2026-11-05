package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "message_logs")
data class MessageLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val formattedTime: String,
    val sourceNode: String, // "Node A" (TX) or "Node B" (RX)
    val targetNode: String,
    val messageType: String, // "SIGNAL", "TELEMETRY", "PING", "BEACON", "ACK"
    val payload: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitude: Double? = null,
    val accuracy: Float? = null,
    val rssi: Int = -65,
    val latencyMs: Long = 0L,
    val status: String = "SENT", // "SENT", "DELIVERED", "RECEIVED", "ACK"
    val sequenceNumber: Int = 0,
    val rawHex: String = ""
)
