package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bluetooth.BluetoothBridgeManager
import com.example.data.local.BluLinqDatabase
import com.example.data.local.MessageLogEntity
import com.example.data.model.EspDevice
import com.example.data.model.GpsLocationData
import com.example.data.model.InterNodeLinkStatus
import com.example.data.model.NodeRole
import com.example.data.model.NodeStatus
import com.example.data.model.SignalPoint
import com.example.util.LocationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BluLinqViewModel(application: Application) : AndroidViewModel(application) {
    private val database = BluLinqDatabase.getInstance(application)
    val locationHelper = LocationHelper(application)
    val bridgeManager = BluetoothBridgeManager(application, database, locationHelper)

    // Hardware Node Statuses
    val nodeAStatus: StateFlow<NodeStatus> = bridgeManager.nodeAStatus
    val nodeBStatus: StateFlow<NodeStatus> = bridgeManager.nodeBStatus
    val centralUnitStatus: StateFlow<NodeStatus> = bridgeManager.centralUnitStatus

    val interNodeLink: StateFlow<InterNodeLinkStatus> = bridgeManager.interNodeLink
    val signalHistory: StateFlow<List<SignalPoint>> = bridgeManager.signalHistory
    val isSimulationMode: StateFlow<Boolean> = bridgeManager.isSimulationMode
    val isBeaconActive: StateFlow<Boolean> = bridgeManager.isBeaconActive
    val availableDevices: StateFlow<List<EspDevice>> = bridgeManager.availableDevices

    // Filter & Search
    private val _logFilter = MutableStateFlow("ALL") // "ALL", "Source Node A", "Source Node B"
    val logFilter: StateFlow<String> = _logFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Current live GPS
    private val _currentLocation = MutableStateFlow<GpsLocationData?>(null)
    val currentLocation: StateFlow<GpsLocationData?> = _currentLocation.asStateFlow()

    // Active Transmitter Node Selection: "NODE_A" or "NODE_B" (Mutually Exclusive)
    private val _activeTransmitterNode = MutableStateFlow("NODE_A")
    val activeTransmitterNode: StateFlow<String> = _activeTransmitterNode.asStateFlow()

    // Transmitter Node A Input
    private val _messageInputA = MutableStateFlow("")
    val messageInputA: StateFlow<String> = _messageInputA.asStateFlow()

    // Transmitter Node B Input
    private val _messageInputB = MutableStateFlow("")
    val messageInputB: StateFlow<String> = _messageInputB.asStateFlow()

    // Shared / Legacy Message Input
    private val _messageInput = MutableStateFlow("")
    val messageInput: StateFlow<String> = _messageInput.asStateFlow()

    // Filtered logs
    val filteredLogs: StateFlow<List<MessageLogEntity>> = combine(
        database.messageLogDao().getAllLogs(),
        _logFilter,
        _searchQuery
    ) { logs, filter, query ->
        logs.filter { item ->
            val matchesFilter = when (filter) {
                "Source Node A", "Node A" -> item.sourceNode.contains("Node A", ignoreCase = true)
                "Source Node B", "Node B" -> item.sourceNode.contains("Node B", ignoreCase = true)
                "Central Unit" -> item.sourceNode.contains("Central", ignoreCase = true) || item.targetNode.contains("Central", ignoreCase = true)
                "ALERT" -> item.messageType == "ALERT"
                else -> true
            }
            val matchesQuery = if (query.isBlank()) {
                true
            } else {
                item.payload.contains(query, ignoreCase = true) ||
                        item.sourceNode.contains(query, ignoreCase = true) ||
                        item.status.contains(query, ignoreCase = true) ||
                        item.formattedTime.contains(query, ignoreCase = true)
            }
            matchesFilter && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refreshLocation()
        bridgeManager.scanForDevices()
    }

    fun setActiveTransmitterNode(node: String) {
        _activeTransmitterNode.value = if (node.contains("B", ignoreCase = true)) "NODE_B" else "NODE_A"
    }

    fun setMessageInputA(text: String) {
        _messageInputA.value = text
        _messageInput.value = text
    }

    fun setMessageInputB(text: String) {
        _messageInputB.value = text
        _messageInput.value = text
    }

    fun setMessageInput(text: String) {
        _messageInput.value = text
        if (_activeTransmitterNode.value == "NODE_A") {
            _messageInputA.value = text
        } else {
            _messageInputB.value = text
        }
    }

    fun setLogFilter(filter: String) {
        _logFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun refreshLocation() {
        viewModelScope.launch {
            val loc = locationHelper.getCurrentLocation()
            _currentLocation.value = loc
        }
    }

    fun sendMessageFromNodeA(customText: String? = null) {
        val text = (customText ?: _messageInputA.value).trim()
        if (text.isNotBlank()) {
            bridgeManager.sendSignalFromNodeA(
                messagePayload = text,
                messageType = "TEXT",
                customLocation = _currentLocation.value
            )
            _messageInputA.value = ""
            _messageInput.value = ""
        }
    }

    fun sendMessageFromNodeB(customText: String? = null) {
        val text = (customText ?: _messageInputB.value).trim()
        if (text.isNotBlank()) {
            bridgeManager.sendSignalFromNodeB(
                messagePayload = text,
                messageType = "TEXT",
                customLocation = _currentLocation.value
            )
            _messageInputB.value = ""
            _messageInput.value = ""
        }
    }

    fun sendCustomMessage() {
        if (_activeTransmitterNode.value == "NODE_A") {
            sendMessageFromNodeA()
        } else {
            sendMessageFromNodeB()
        }
    }

    fun sendSignalPreset(preset: String) {
        if (_activeTransmitterNode.value == "NODE_A") {
            sendPresetFromNodeA(preset)
        } else {
            sendPresetFromNodeB(preset)
        }
    }

    fun sendPresetFromNodeA(preset: String) {
        bridgeManager.sendSignalFromNodeA(
            messagePayload = preset,
            messageType = "SIGNAL",
            customLocation = _currentLocation.value
        )
    }

    fun sendPresetFromNodeB(preset: String) {
        bridgeManager.sendSignalFromNodeB(
            messagePayload = preset,
            messageType = "SIGNAL",
            customLocation = _currentLocation.value
        )
    }

    fun toggleBeacon(enabled: Boolean) {
        bridgeManager.toggleBeacon(enabled)
    }

    fun toggleSimulationMode(enabled: Boolean) {
        bridgeManager.toggleSimulationMode(enabled)
    }

    fun scanDevices() {
        bridgeManager.scanForDevices()
    }

    fun connectDevice(device: EspDevice, role: NodeRole) {
        bridgeManager.connectDeviceToNode(device, role)
    }

    fun disconnectDevice(role: NodeRole) {
        bridgeManager.disconnectNode(role)
    }

    fun connectSourceNodeA(device: EspDevice) {
        bridgeManager.connectDeviceToNode(device, NodeRole.NODE_A)
    }

    fun disconnectSourceNodeA() {
        bridgeManager.disconnectNode(NodeRole.NODE_A)
    }

    fun connectSourceNodeB(device: EspDevice) {
        bridgeManager.connectDeviceToNode(device, NodeRole.NODE_B)
    }

    fun disconnectSourceNodeB() {
        bridgeManager.disconnectNode(NodeRole.NODE_B)
    }

    fun connectCentralUnit(device: EspDevice) {
        bridgeManager.connectDeviceToNode(device, NodeRole.CENTRAL)
    }

    fun autoConnectNode(role: NodeRole) { bridgeManager.autoConnect(role) }

    fun connectCentralUnitAutomated() {
        bridgeManager.autoConnectCentralUnit()
    }

    fun disconnectCentralUnit() {
        bridgeManager.disconnectNode(NodeRole.CENTRAL)
    }

    fun clearLogs() {
        bridgeManager.clearLogs()
    }

    fun simulateIncomingBleNotification(rawText: String) {
        bridgeManager.simulateIncomingBleNotification(rawText)
    }

    fun exportLogsToCsv(): String {
        val logs = filteredLogs.value
        val sb = StringBuilder()
        sb.append("ID,Timestamp,Formatted_Time,Source,Target,Type,Status,RSSI_dBm,Latency_ms,Latitude,Longitude,Altitude,Accuracy,Payload\n")
        logs.forEach { log ->
            sb.append("${log.id},")
            sb.append("${log.timestamp},")
            sb.append("\"${log.formattedTime}\",")
            sb.append("\"${log.sourceNode}\",")
            sb.append("\"${log.targetNode}\",")
            sb.append("\"${log.messageType}\",")
            sb.append("\"${log.status}\",")
            sb.append("${log.rssi},")
            sb.append("${log.latencyMs},")
            sb.append("${log.latitude ?: ""},")
            sb.append("${log.longitude ?: ""},")
            sb.append("${log.altitude ?: ""},")
            sb.append("${log.accuracy ?: ""},")
            sb.append("\"${log.payload.replace("\"", "\"\"")}\"\n")
        }
        return sb.toString()
    }
}