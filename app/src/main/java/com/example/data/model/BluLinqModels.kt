package com.example.data.model

enum class NodeRole(val displayName: String, val shortName: String, val chipId: String) {
    NODE_A("Source Node A", "Node A", "ESP32-TX-A"),
    NODE_B("Source Node B", "Node B", "ESP32-TX-B"),
    CENTRAL("Central Unit", "Central Unit", "ESP32-CENTRAL")
}

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    FAILED
}

data class EspDevice(
    val name: String,
    val address: String,
    val rssi: Int = -60,
    val isBonded: Boolean = false,
    val targetRole: NodeRole = NodeRole.NODE_A
)

data class NodeStatus(
    val role: NodeRole,
    val deviceName: String = "ESP32-Unassigned",
    val deviceAddress: String = "",
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val rssi: Int = -75,
    val lastSeenTimestamp: Long = 0L,
    val packetsSent: Int = 0,
    val packetsReceived: Int = 0,
    val packetLossRatePct: Float = 0f,
    val batteryPct: Int = 92
)

data class InterNodeLinkStatus(
    val isInterNodeActive: Boolean = true,
    val radioProtocol: String = "ESP-NOW / LoRa 2.4GHz Link",
    val linkRssi: Int = -68,
    val linkQualityPct: Int = 88,
    val averageLatencyMs: Long = 24L,
    val channel: Int = 6,
    val packetsTransmitted: Int = 0,
    val packetsAwaitedAck: Int = 0
)

data class GpsLocationData(
    val latitude: Double = 37.7749,
    val longitude: Double = -122.4194,
    val altitude: Double = 18.5,
    val accuracyMeters: Float = 4.2f,
    val timestamp: Long = System.currentTimeMillis(),
    val isRealGps: Boolean = false,
    val provider: String = "GPS/Network"
)

data class SignalPoint(
    val timestamp: Long,
    val rssiNodeA: Int,
    val rssiNodeB: Int,
    val latencyMs: Long
)
