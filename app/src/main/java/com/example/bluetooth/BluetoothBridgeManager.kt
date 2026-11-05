package com.example.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothSocket
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.local.BluLinqDatabase
import com.example.data.local.MessageLogEntity
import com.example.data.model.ConnectionState
import com.example.data.model.EspDevice
import com.example.data.model.GpsLocationData
import com.example.data.model.InterNodeLinkStatus
import com.example.data.model.NodeRole
import com.example.data.model.NodeStatus
import com.example.data.model.SignalPoint
import com.example.util.LocationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.random.Random

private const val TAG = "BluLinq-BT"

// Nordic UART Service UUIDs
val NUS_SERVICE_UUID: UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
val NUS_RX_CHAR_UUID: UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E") // Write to ESP32
val NUS_TX_CHAR_UUID: UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E") // Notify from ESP32
val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")
private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

data class ParsedBlePacket(
    val senderName: String,  // "Source Node A" or "Source Node B"
    val prefix: String,      // "N1" or "N2"
    val messageText: String, // stripped message body
    val isEmergency: Boolean
)

/**
 * Parses incoming BLE string from characteristic 6E400003-B5A3-F393-E0A9-E50E24DCCA9E.
 * Splits at colon (:) and maps N1 -> "Source Node A" and N2 -> "Source Node B".
 * Strictly hides and strips raw N1: and N2: prefixes from the user interface.
 */
fun parseIncomingBleString(raw: String): ParsedBlePacket {
    var clean = raw.trim()

    // 1. Unwrap JSON if message payload was wrapped
    if (clean.startsWith("{") && clean.endsWith("}")) {
        try {
            val json = JSONObject(clean)
            if (json.has("msg")) {
                clean = json.getString("msg").trim()
            }
        } catch (_: Exception) {}
    }

    val isEmergencyAlert = clean.contains("SOS", ignoreCase = true) ||
            clean.contains("HELP", ignoreCase = true) ||
            clean.contains("MAYDAY", ignoreCase = true) ||
            clean.contains("FIRE", ignoreCase = true) ||
            clean.contains("EMERGENCY", ignoreCase = true)

    // 2. Parse by splitting at colon (:) and map N1 / N2 prefixes
    return if (clean.contains(":")) {
        val parts = clean.split(":", limit = 2)
        val prefix = parts[0].trim()
        var body = if (parts.size > 1) parts[1].trim() else ""

        // Strictly hide and strip any raw N1: or N2: prefixes from the user-facing message body
        while (body.startsWith("N1:", ignoreCase = true) || body.startsWith("N2:", ignoreCase = true)) {
            body = body.substring(3).trim()
        }

        val mappedSender = when (prefix.uppercase()) {
            "N1" -> "Source Node A"
            "N2" -> "Source Node B"
            else -> if (prefix.startsWith("N", ignoreCase = true)) {
                "Source Node ${prefix.substring(1).trim()}"
            } else {
                "Source Node ($prefix)"
            }
        }

        ParsedBlePacket(
            senderName = mappedSender,
            prefix = prefix,
            messageText = body,
            isEmergency = isEmergencyAlert
        )
    } else {
        var body = clean
        val mappedSender = when {
            body.startsWith("N1", ignoreCase = true) -> {
                body = body.removePrefix("N1").trim()
                "Source Node A"
            }
            body.startsWith("N2", ignoreCase = true) -> {
                body = body.removePrefix("N2").trim()
                "Source Node B"
            }
            else -> "Source Node A"
        }
        while (body.startsWith("N1:", ignoreCase = true) || body.startsWith("N2:", ignoreCase = true)) {
            body = body.substring(3).trim()
        }

        ParsedBlePacket(
            senderName = mappedSender,
            prefix = if (mappedSender == "Source Node A") "N1" else "N2",
            messageText = body,
            isEmergency = isEmergencyAlert
        )
    }
}

class BluetoothBridgeManager(
    private val context: Context,
    private val database: BluLinqDatabase,
    private val locationHelper: LocationHelper
) {
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    // 1. Source Node A (Transmitter 1) - Strictly DISCONNECTED by default
    private val _nodeAStatus = MutableStateFlow(
        NodeStatus(
            role = NodeRole.NODE_A,
            deviceName = "ESP32_NODE_A",
            deviceAddress = "",
            connectionState = ConnectionState.DISCONNECTED,
            rssi = -95,
            lastSeenTimestamp = 0L
        )
    )
    val nodeAStatus: StateFlow<NodeStatus> = _nodeAStatus.asStateFlow()

    // 2. Source Node B (Transmitter 2) - Strictly DISCONNECTED by default
    private val _nodeBStatus = MutableStateFlow(
        NodeStatus(
            role = NodeRole.NODE_B,
            deviceName = "ESP32_NODE_B",
            deviceAddress = "",
            connectionState = ConnectionState.DISCONNECTED,
            rssi = -95,
            lastSeenTimestamp = 0L
        )
    )
    val nodeBStatus: StateFlow<NodeStatus> = _nodeBStatus.asStateFlow()

    // 3. Central Unit (Receiver) - Strictly DISCONNECTED by default; broadcasts as Central_Unit
    private val _centralUnitStatus = MutableStateFlow(
        NodeStatus(
            role = NodeRole.CENTRAL,
            deviceName = "Central_Unit",
            deviceAddress = "",
            connectionState = ConnectionState.DISCONNECTED,
            rssi = -95,
            lastSeenTimestamp = 0L
        )
    )
    val centralUnitStatus: StateFlow<NodeStatus> = _centralUnitStatus.asStateFlow()

    // Inter-Node Radio Link (ESP-NOW / LoRa wireless connection)
    private val _interNodeLink = MutableStateFlow(InterNodeLinkStatus(isInterNodeActive = false))
    val interNodeLink: StateFlow<InterNodeLinkStatus> = _interNodeLink.asStateFlow()

    // Available Discovered Devices
    private val _availableDevices = MutableStateFlow<List<EspDevice>>(emptyList())
    val availableDevices: StateFlow<List<EspDevice>> = _availableDevices.asStateFlow()

    // Signal History Points for live chart
    private val _signalHistory = MutableStateFlow<List<SignalPoint>>(emptyList())
    val signalHistory: StateFlow<List<SignalPoint>> = _signalHistory.asStateFlow()

    // Simulation vs Hardware Mode - Default strictly to HARDWARE MODE
    private val _isSimulationMode = MutableStateFlow(false)
    val isSimulationMode: StateFlow<Boolean> = _isSimulationMode.asStateFlow()

    // Auto Beacon Interval Job
    private var beaconJob: Job? = null
    private val _isBeaconActive = MutableStateFlow(false)
    val isBeaconActive: StateFlow<Boolean> = _isBeaconActive.asStateFlow()

    // Real Hardware BLE GATT and Sockets for each independent node
    private var gattA: BluetoothGatt? = null
    private var gattB: BluetoothGatt? = null
    private var gattCentral: BluetoothGatt? = null

    private var rxCharA: BluetoothGattCharacteristic? = null
    private var rxCharB: BluetoothGattCharacteristic? = null
    private var rxCharCentral: BluetoothGattCharacteristic? = null

    private var txCharA: BluetoothGattCharacteristic? = null
    private var txCharB: BluetoothGattCharacteristic? = null
    private var txCharCentral: BluetoothGattCharacteristic? = null

    private var socketA: BluetoothSocket? = null
    private var outputStreamA: OutputStream? = null
    private var socketB: BluetoothSocket? = null
    private var outputStreamB: OutputStream? = null
    private var socketCentral: BluetoothSocket? = null
    private var outputStreamCentral: OutputStream? = null

    private var packetSequenceCounter = 100

    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    init {
        // Generate initial signal telemetry history
        val initialHistory = mutableListOf<SignalPoint>()
        val now = System.currentTimeMillis()
        for (i in 20 downTo 0) {
            initialHistory.add(
                SignalPoint(
                    timestamp = now - (i * 2000L),
                    rssiNodeA = -58 + Random.nextInt(-6, 7),
                    rssiNodeB = -65 + Random.nextInt(-8, 9),
                    latencyMs = 20L + Random.nextLong(2, 18)
                )
            )
        }
        _signalHistory.value = initialHistory

        // Seed initial message logs if database is empty
        scope.launch {
            seedInitialLogsIfEmpty()
        }

        // Start background telemetry & heartbeat monitor
        startSignalHeartbeat()
    }

    private suspend fun seedInitialLogsIfEmpty() {
        val existing = database.messageLogDao().getAllLogs()
        val now = System.currentTimeMillis()
        val loc = locationHelper.getSimulatedLocation(isReal = false)

        database.messageLogDao().insertLog(
            MessageLogEntity(
                timestamp = now - 18000,
                formattedTime = timeFormat.format(Date(now - 18000)),
                sourceNode = "Source Node A",
                targetNode = "Central Unit",
                messageType = "SIGNAL",
                payload = "LINK_INIT: Channel 6 [ESP-NOW Synced]",
                latitude = loc.latitude,
                longitude = loc.longitude,
                altitude = loc.altitude,
                accuracy = loc.accuracyMeters,
                rssi = -60,
                latencyMs = 22,
                status = "RECEIVED",
                sequenceNumber = 101,
                rawHex = "4C 49 4E 4B 5F 49 4E 49 54"
            )
        )
        database.messageLogDao().insertLog(
            MessageLogEntity(
                timestamp = now - 12000,
                formattedTime = timeFormat.format(Date(now - 12000)),
                sourceNode = "Source Node B",
                targetNode = "Central Unit",
                messageType = "TEXT",
                payload = "All clear: Link Quality 94% / Delay 22ms",
                latitude = loc.latitude + 0.0001,
                longitude = loc.longitude + 0.0001,
                altitude = loc.altitude + 0.4,
                accuracy = loc.accuracyMeters,
                rssi = -64,
                latencyMs = 21,
                status = "RECEIVED",
                sequenceNumber = 102,
                rawHex = "06 41 43 4B 5F 4F 4B"
            )
        )
    }

    fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val connect = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
            val scan = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
            connect && scan
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun toggleSimulationMode(enabled: Boolean) {
        _isSimulationMode.value = enabled
        if (enabled) {
            _nodeAStatus.update {
                it.copy(
                    deviceName = "ESP32-TX-A (Simulated)",
                    connectionState = ConnectionState.CONNECTED,
                    rssi = -58
                )
            }
            _nodeBStatus.update {
                it.copy(
                    deviceName = "ESP32-TX-B (Simulated)",
                    connectionState = ConnectionState.CONNECTED,
                    rssi = -62
                )
            }
            _centralUnitStatus.update {
                it.copy(
                    deviceName = "Central_Unit (Simulated)",
                    connectionState = ConnectionState.CONNECTED,
                    rssi = -55
                )
            }
        }
    }

    private val scanLock = Any()

    private fun roleForName(name: String): NodeRole? = when {
        name.contains("Central", true) -> NodeRole.CENTRAL
        name.contains("Node_B", true) || name.contains("NODE_B", true) -> NodeRole.NODE_B
        name.contains("Node_A", true) || name.contains("NODE_A", true) -> NodeRole.NODE_A
        else -> null
    }

    /** Real BLE scan (about 4 s). Fills availableDevices with the ESP32s actually in range. */
    @SuppressLint("MissingPermission")
    fun scanForDevices() {
        if (!hasBluetoothPermissions() || bluetoothAdapter == null || !bluetoothAdapter.isEnabled) return
        val scanner = bluetoothAdapter.bluetoothLeScanner ?: return
        val found = LinkedHashMap<String, EspDevice>()
        val cb = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                val r = result ?: return
                val name = r.scanRecord?.deviceName ?: r.device.name ?: return
                val role = roleForName(name) ?: return
                synchronized(scanLock) {
                    found[r.device.address] = EspDevice(name, r.device.address, r.rssi, false, role)
                    _availableDevices.value = found.values.toList()
                }
            }
        }
        try { scanner.startScan(cb) } catch (e: Exception) { Log.w(TAG, "scan failed", e); return }
        scope.launch {
            delay(4000)
            try { scanner.stopScan(cb) } catch (_: Exception) {}
        }
    }

    /** Scan for a device matching the role by name, then connect to its REAL address. */
    @SuppressLint("MissingPermission")
    fun autoConnect(role: NodeRole) {
        scope.launch {
            setState(role, ConnectionState.CONNECTING)
            if (_isSimulationMode.value) {
                val n = when (role) { NodeRole.NODE_A -> "ESP32_NODE_A"; NodeRole.NODE_B -> "ESP32_NODE_B"; else -> "Central_Unit" }
                connectDeviceToNode(EspDevice(n, "00:00:00:00:00:00", -60, true, role), role); return@launch
            }
            if (!hasBluetoothPermissions() || bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
                setConnectionFailed(role); return@launch
            }
            val scanner = bluetoothAdapter.bluetoothLeScanner
            if (scanner == null) { setConnectionFailed(role); return@launch }
            var target: ScanResult? = null
            val cb = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult?) {
                    val r = result ?: return
                    val name = r.scanRecord?.deviceName ?: r.device.name ?: return
                    if (roleForName(name) == role) target = r
                }
            }
            try { scanner.startScan(cb) } catch (e: Exception) { setConnectionFailed(role); return@launch }
            for (i in 1..80) { if (target != null) break; delay(100) }   // up to 8 s
            try { scanner.stopScan(cb) } catch (_: Exception) {}
            val t = target
            if (t == null) { Log.w(TAG, "No device found for $role"); setConnectionFailed(role); return@launch }
            val name = t.scanRecord?.deviceName ?: t.device.name ?: role.displayName
            connectDeviceToNode(EspDevice(name, t.device.address, t.rssi, false, role), role)
        }
    }

    private fun setState(role: NodeRole, st: ConnectionState) {
        when (role) {
            NodeRole.NODE_A -> _nodeAStatus.update { it.copy(connectionState = st) }
            NodeRole.NODE_B -> _nodeBStatus.update { it.copy(connectionState = st) }
            NodeRole.CENTRAL -> _centralUnitStatus.update { it.copy(connectionState = st) }
        }
    }

    fun autoConnectCentralUnit() = autoConnect(NodeRole.CENTRAL)

    private fun createGattCallback(role: NodeRole): BluetoothGattCallback {
        return object : BluetoothGattCallback() {
            @SuppressLint("MissingPermission")
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                Log.d(TAG, "GATT state change for $role: status=$status, newState=$newState")
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.i(TAG, "GATT physically connected to $role (${gatt.device.name}). Step 2: Auto-discovering services...")
                    gatt.requestMtu(64)
                    scope.launch { delay(600); try { gatt.discoverServices() } catch (_: Exception) {} }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.w(TAG, "GATT disconnected from $role (status=$status)")
                    disconnectNode(role)
                    if (status != BluetoothGatt.GATT_SUCCESS) setConnectionFailed(role)
                }
            }

            @SuppressLint("MissingPermission")
            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.i(TAG, "GATT service discovery succeeded on $role")
                    val service = gatt.getService(NUS_SERVICE_UUID)
                    if (service != null) {
                        Log.i(TAG, "Found Nordic UART Service: $NUS_SERVICE_UUID on $role")
                        val tx = service.getCharacteristic(NUS_TX_CHAR_UUID)
                        val rx = service.getCharacteristic(NUS_RX_CHAR_UUID)
                        when (role) {
                            NodeRole.NODE_A -> {
                                txCharA = tx
                                rxCharA = rx
                            }
                            NodeRole.NODE_B -> {
                                txCharB = tx
                                rxCharB = rx
                            }
                            NodeRole.CENTRAL -> {
                                txCharCentral = tx
                                rxCharCentral = rx
                            }
                        }

                        // Step 3: Programmatically subscribe to Characteristic 6E400003-B5A3-F393-E0A9-E50E24DCCA9E
                        // by writing 0x01, 0x00 to its CCCD (0x2902)
                        if (tx != null) {
                            val notifyEnabled = gatt.setCharacteristicNotification(tx, true)
                            Log.i(TAG, "Local characteristic notification enabled on $NUS_TX_CHAR_UUID: $notifyEnabled")

                            val descriptor = tx.getDescriptor(CCCD_UUID)
                            if (descriptor != null) {
                                val cccdPayload = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE // byteArrayOf(0x01, 0x00)
                                Log.i(TAG, "Writing CCCD descriptor 0x2902 [0x01, 0x00] for $role...")
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    val writeResult = gatt.writeDescriptor(descriptor, cccdPayload)
                                    Log.d(TAG, "writeDescriptor (API 33+) returned: $writeResult")
                                } else {
                                    @Suppress("DEPRECATION")
                                    descriptor.value = cccdPayload
                                    @Suppress("DEPRECATION")
                                    val writeResult = gatt.writeDescriptor(descriptor)
                                    Log.d(TAG, "writeDescriptor (legacy) returned: $writeResult")
                                }
                            } else {
                                Log.w(TAG, "CCCD descriptor (0x2902) not found on $NUS_TX_CHAR_UUID")
                            }
                        }
                    } else {
                        Log.w(TAG, "Service $NUS_SERVICE_UUID not found on $role")
                    }
                    if (service == null) { setConnectionFailed(role) }
                    else if (role != NodeRole.CENTRAL) {
                        // transmitters only need the RX (write) characteristic
                        updateNodeConnected(role, gatt.device.name ?: role.displayName, gatt.device.address, -55)
                    }
                } else {
                    Log.e(TAG, "Service discovery failed on $role: status=$status")
                    setConnectionFailed(role)
                }
            }

            @SuppressLint("MissingPermission")
            override fun onDescriptorWrite(
                gatt: BluetoothGatt,
                descriptor: BluetoothGattDescriptor,
                status: Int
            ) {
                Log.d(TAG, "onDescriptorWrite for $role: uuid=${descriptor.uuid}, status=$status")
                if (descriptor.uuid == CCCD_UUID) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        Log.i(TAG, "CCCD subscription successfully established on $role! Background listening active.")
                        updateNodeConnected(role, gatt.device.name ?: role.displayName, gatt.device.address, -52)
                    } else {
                        Log.w(TAG, "CCCD subscription failed on $role with status $status")
                    }
                }
            }

            // Step 4: Background Listening - Receive incoming notifications on characteristic 6E400003
            // Android 13+ (API 33+)
            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray
            ) {
                if (characteristic.uuid == NUS_TX_CHAR_UUID) {
                    val rawText = String(value, Charsets.UTF_8)
                    Log.d(TAG, "BLE Notification on 6E400003 ($role): $rawText")
                    handleIncomingRawPacket(rawText, role)
                }
            }

            // Android < 13
            @Deprecated("Deprecated in Java")
            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic
            ) {
                if (characteristic.uuid == NUS_TX_CHAR_UUID) {
                    @Suppress("DEPRECATION")
                    val rawBytes = characteristic.value ?: return
                    val rawText = String(rawBytes, Charsets.UTF_8)
                    Log.d(TAG, "BLE Notification (legacy) on 6E400003 ($role): $rawText")
                    handleIncomingRawPacket(rawText, role)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun connectDeviceToNode(device: EspDevice, role: NodeRole) {
        scope.launch {
            when (role) {
                NodeRole.NODE_A -> {
                    _nodeAStatus.update {
                        it.copy(
                            deviceName = device.name,
                            deviceAddress = device.address,
                            connectionState = ConnectionState.CONNECTING
                        )
                    }
                }
                NodeRole.NODE_B -> {
                    _nodeBStatus.update {
                        it.copy(
                            deviceName = device.name,
                            deviceAddress = device.address,
                            connectionState = ConnectionState.CONNECTING
                        )
                    }
                }
                NodeRole.CENTRAL -> {
                    _centralUnitStatus.update {
                        it.copy(
                            deviceName = device.name,
                            deviceAddress = device.address,
                            connectionState = ConnectionState.CONNECTING
                        )
                    }
                }
            }

            if (!_isSimulationMode.value) {
                if (hasBluetoothPermissions() && bluetoothAdapter != null) {
                    try {
                        val remoteDevice = bluetoothAdapter.getRemoteDevice(device.address)
                        val gattCallback = createGattCallback(role)
                        val gatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            remoteDevice.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
                        } else {
                            remoteDevice.connectGatt(context, false, gattCallback)
                        }

                        when (role) {
                            NodeRole.NODE_A -> gattA = gatt
                            NodeRole.NODE_B -> gattB = gatt
                            NodeRole.CENTRAL -> gattCentral = gatt
                        }
                        return@launch
                    } catch (e: Exception) {
                        Log.e(TAG, "BLE GATT connection attempt failed for $role, trying RFCOMM: ${e.message}", e)
                        try {
                            val remoteDevice = bluetoothAdapter.getRemoteDevice(device.address)
                            val socket = remoteDevice.createRfcommSocketToServiceRecord(SPP_UUID)
                            socket.connect()
                            val output = socket.outputStream

                            when (role) {
                                NodeRole.NODE_A -> {
                                    socketA = socket
                                    outputStreamA = output
                                    listenToSocket(socket, NodeRole.NODE_A)
                                }
                                NodeRole.NODE_B -> {
                                    socketB = socket
                                    outputStreamB = output
                                    listenToSocket(socket, NodeRole.NODE_B)
                                }
                                NodeRole.CENTRAL -> {
                                    socketCentral = socket
                                    outputStreamCentral = output
                                    listenToSocket(socket, NodeRole.CENTRAL)
                                }
                            }

                            updateNodeConnected(role, device.name, device.address, -60)
                            return@launch
                        } catch (e2: Exception) {
                            Log.e(TAG, "Both GATT and RFCOMM connection failed for $role", e2)
                            setConnectionFailed(role)
                            return@launch
                        }
                    }
                } else {
                    Log.w(TAG, "Bluetooth permissions missing or adapter unavailable for $role")
                    setConnectionFailed(role)
                    return@launch
                }
            }

            // Only reached if in Simulation Mode
            delay(500)
            updateNodeConnected(role, device.name, device.address, device.rssi)
        }
    }

    private fun setConnectionFailed(role: NodeRole) {
        when (role) {
            NodeRole.NODE_A -> _nodeAStatus.update { it.copy(connectionState = ConnectionState.FAILED) }
            NodeRole.NODE_B -> _nodeBStatus.update { it.copy(connectionState = ConnectionState.FAILED) }
            NodeRole.CENTRAL -> _centralUnitStatus.update { it.copy(connectionState = ConnectionState.FAILED) }
        }
    }

    private fun updateNodeConnected(role: NodeRole, name: String, address: String, rssi: Int) {
        val now = System.currentTimeMillis()
        when (role) {
            NodeRole.NODE_A -> {
                _nodeAStatus.update {
                    it.copy(
                        deviceName = name,
                        deviceAddress = address,
                        connectionState = ConnectionState.CONNECTED,
                        rssi = rssi,
                        lastSeenTimestamp = now
                    )
                }
            }
            NodeRole.NODE_B -> {
                _nodeBStatus.update {
                    it.copy(
                        deviceName = name,
                        deviceAddress = address,
                        connectionState = ConnectionState.CONNECTED,
                        rssi = rssi,
                        lastSeenTimestamp = now
                    )
                }
            }
            NodeRole.CENTRAL -> {
                _centralUnitStatus.update {
                    it.copy(
                        deviceName = name,
                        deviceAddress = address,
                        connectionState = ConnectionState.CONNECTED,
                        rssi = rssi,
                        lastSeenTimestamp = now
                    )
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnectNode(role: NodeRole) {
        when (role) {
            NodeRole.NODE_A -> {
                try { gattA?.disconnect(); gattA?.close() } catch (_: Exception) {}
                gattA = null
                txCharA = null
                rxCharA = null
                try { socketA?.close() } catch (_: Exception) {}
                socketA = null
                outputStreamA = null
                _nodeAStatus.update { it.copy(connectionState = ConnectionState.DISCONNECTED) }
            }
            NodeRole.NODE_B -> {
                try { gattB?.disconnect(); gattB?.close() } catch (_: Exception) {}
                gattB = null
                txCharB = null
                rxCharB = null
                try { socketB?.close() } catch (_: Exception) {}
                socketB = null
                outputStreamB = null
                _nodeBStatus.update { it.copy(connectionState = ConnectionState.DISCONNECTED) }
            }
            NodeRole.CENTRAL -> {
                try { gattCentral?.disconnect(); gattCentral?.close() } catch (_: Exception) {}
                gattCentral = null
                txCharCentral = null
                rxCharCentral = null
                try { socketCentral?.close() } catch (_: Exception) {}
                socketCentral = null
                outputStreamCentral = null
                _centralUnitStatus.update { it.copy(connectionState = ConnectionState.DISCONNECTED) }
            }
        }
    }

    private fun listenToSocket(socket: BluetoothSocket, role: NodeRole) {
        scope.launch {
            try {
                val reader = BufferedReader(InputStreamReader(socket.inputStream))
                while (isActive) {
                    val line = reader.readLine() ?: break
                    handleIncomingRawPacket(line, role)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Socket reader disconnected for $role", e)
                disconnectNode(role)
            }
        }
    }

    /**
     * Handles incoming raw text packet received over BLE characteristic 6E400003 or RFCOMM.
     * Parses N1:<message> as 'Source Node A' and N2:<message> as 'Source Node B'.
     */
    fun handleIncomingRawPacket(raw: String, receivedAt: NodeRole = NodeRole.CENTRAL) {
        scope.launch {
            val now = System.currentTimeMillis()
            val formatted = timeFormat.format(Date(now))
            try {
                if (raw.startsWith("{") && raw.endsWith("}")) {
                    val json = JSONObject(raw)
                    val rawMsg = json.optString("msg", raw)
                    val parsed = parseIncomingBleString(rawMsg)
                    val lat = json.optDouble("lat", 0.0).takeIf { !it.isNaN() && it != 0.0 }
                    val lng = json.optDouble("lng", 0.0).takeIf { !it.isNaN() && it != 0.0 }
                    val seq = json.optInt("seq", ++packetSequenceCounter)

                    database.messageLogDao().insertLog(
                        MessageLogEntity(
                            timestamp = now,
                            formattedTime = formatted,
                            sourceNode = parsed.senderName,
                            targetNode = "Central Unit",
                            messageType = if (parsed.isEmergency) "ALERT" else "SIGNAL",
                            payload = parsed.messageText.ifBlank { rawMsg },
                            latitude = lat,
                            longitude = lng,
                            rssi = -62,
                            latencyMs = 24,
                            status = "RECEIVED",
                            sequenceNumber = seq,
                            rawHex = toHex(raw)
                        )
                    )
                } else {
                    val parsed = parseIncomingBleString(raw)
                    packetSequenceCounter++
                    database.messageLogDao().insertLog(
                        MessageLogEntity(
                            timestamp = now,
                            formattedTime = formatted,
                            sourceNode = parsed.senderName,
                            targetNode = "Central Unit",
                            messageType = if (parsed.isEmergency) "ALERT" else "TEXT",
                            payload = parsed.messageText.ifBlank { raw },
                            rssi = -60,
                            latencyMs = 22,
                            status = "RECEIVED",
                            sequenceNumber = packetSequenceCounter,
                            rawHex = toHex(raw)
                        )
                    )
                }

                _centralUnitStatus.update {
                    it.copy(
                        packetsReceived = it.packetsReceived + 1,
                        lastSeenTimestamp = now
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling incoming packet: $raw", e)
            }
        }
    }

    /**
     * Testing hook to simulate an incoming raw BLE notification string like "N1:Help" or "N2:All clear".
     */
    fun simulateIncomingBleNotification(rawText: String) {
        handleIncomingRawPacket(rawText, NodeRole.CENTRAL)
    }

    /**
     * Send Signal / Message from Source Node A to Central Unit formatted with prefix N1:<message>
     */
    fun sendSignalFromNodeA(
        messagePayload: String,
        messageType: String = "SIGNAL",
        customLocation: GpsLocationData? = null
    ) {
        scope.launch {
            packetSequenceCounter++
            val sequenceNumber = packetSequenceCounter
            val sendTime = System.currentTimeMillis()
            val formattedSendTime = timeFormat.format(Date(sendTime))

            val location = customLocation ?: locationHelper.getCurrentLocation()
            val latency = Random.nextLong(16, 32)
            val txRssi = _nodeAStatus.value.rssi.takeIf { it > -90 } ?: -60
            val rxRssi = _centralUnitStatus.value.rssi.takeIf { it > -90 } ?: -62

            val blePayload = messagePayload.trim().take(17)   // node adds its own ID; 17-char limit
            val hexString = toHex(messagePayload)

            // Hardware transmission via BLE GATT RX Characteristic (6E400002) or RFCOMM
            rxCharA?.let { char ->
                gattA?.let { gatt ->
                    try {
                        val bytes = blePayload.toByteArray(Charsets.UTF_8)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            gatt.writeCharacteristic(char, bytes, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                        } else {
                            @Suppress("DEPRECATION")
                            char.value = bytes
                            @Suppress("DEPRECATION")
                            gatt.writeCharacteristic(char)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed writing to BLE GATT Node A RX characteristic", e)
                    }
                }
            }

            outputStreamA?.let { out ->
                try {
                    withContext(Dispatchers.IO) {
                        out.write(blePayload.toByteArray())
                        out.flush()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed writing to real Node A socket", e)
                }
            }

            // Log transmission event at Source Node A
            val txLog = MessageLogEntity(
                timestamp = sendTime,
                formattedTime = formattedSendTime,
                sourceNode = "Source Node A",
                targetNode = "Central Unit",
                messageType = messageType,
                payload = messagePayload,
                latitude = location.latitude,
                longitude = location.longitude,
                altitude = location.altitude,
                accuracy = location.accuracyMeters,
                rssi = txRssi,
                latencyMs = latency,
                status = "SENT",
                sequenceNumber = sequenceNumber,
                rawHex = hexString
            )
            database.messageLogDao().insertLog(txLog)

            _nodeAStatus.update {
                it.copy(
                    packetsSent = it.packetsSent + 1,
                    lastSeenTimestamp = sendTime
                )
            }

            // Hardware mode: the real RX log is created only when Central_Unit notifies the phone
            if (!_isSimulationMode.value) return@launch
            delay(latency)
            val receiveTime = sendTime + latency
            val formattedReceiveTime = timeFormat.format(Date(receiveTime))

            val rxLog = MessageLogEntity(
                timestamp = receiveTime,
                formattedTime = formattedReceiveTime,
                sourceNode = "Source Node A",
                targetNode = "Central Unit",
                messageType = messageType,
                payload = messagePayload,
                latitude = location.latitude,
                longitude = location.longitude,
                altitude = location.altitude,
                accuracy = location.accuracyMeters,
                rssi = rxRssi,
                latencyMs = latency,
                status = "RECEIVED",
                sequenceNumber = sequenceNumber,
                rawHex = hexString
            )
            database.messageLogDao().insertLog(rxLog)

            _centralUnitStatus.update {
                it.copy(
                    packetsReceived = it.packetsReceived + 1,
                    lastSeenTimestamp = receiveTime
                )
            }

            _interNodeLink.update {
                it.copy(
                    packetsTransmitted = it.packetsTransmitted + 1,
                    averageLatencyMs = (it.averageLatencyMs * 4 + latency) / 5,
                    linkRssi = rxRssi
                )
            }
        }
    }

    /**
     * Send Signal / Message from Source Node B to Central Unit formatted with prefix N2:<message>
     */
    fun sendSignalFromNodeB(
        messagePayload: String,
        messageType: String = "SIGNAL",
        customLocation: GpsLocationData? = null
    ) {
        scope.launch {
            packetSequenceCounter++
            val sequenceNumber = packetSequenceCounter
            val sendTime = System.currentTimeMillis()
            val formattedSendTime = timeFormat.format(Date(sendTime))

            val location = customLocation ?: locationHelper.getCurrentLocation()
            val latency = Random.nextLong(16, 32)
            val txRssi = _nodeBStatus.value.rssi.takeIf { it > -90 } ?: -64
            val rxRssi = _centralUnitStatus.value.rssi.takeIf { it > -90 } ?: -62

            val blePayload = messagePayload.trim().take(17)   // node adds its own ID; 17-char limit
            val hexString = toHex(messagePayload)

            // Hardware transmission via BLE GATT RX Characteristic (6E400002) or RFCOMM
            rxCharB?.let { char ->
                gattB?.let { gatt ->
                    try {
                        val bytes = blePayload.toByteArray(Charsets.UTF_8)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            gatt.writeCharacteristic(char, bytes, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                        } else {
                            @Suppress("DEPRECATION")
                            char.value = bytes
                            @Suppress("DEPRECATION")
                            gatt.writeCharacteristic(char)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed writing to BLE GATT Node B RX characteristic", e)
                    }
                }
            }

            outputStreamB?.let { out ->
                try {
                    withContext(Dispatchers.IO) {
                        out.write(blePayload.toByteArray())
                        out.flush()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed writing to real Node B socket", e)
                }
            }

            // Log transmission event at Source Node B
            val txLog = MessageLogEntity(
                timestamp = sendTime,
                formattedTime = formattedSendTime,
                sourceNode = "Source Node B",
                targetNode = "Central Unit",
                messageType = messageType,
                payload = messagePayload,
                latitude = location.latitude,
                longitude = location.longitude,
                altitude = location.altitude,
                accuracy = location.accuracyMeters,
                rssi = txRssi,
                latencyMs = latency,
                status = "SENT",
                sequenceNumber = sequenceNumber,
                rawHex = hexString
            )
            database.messageLogDao().insertLog(txLog)

            _nodeBStatus.update {
                it.copy(
                    packetsSent = it.packetsSent + 1,
                    lastSeenTimestamp = sendTime
                )
            }

            // Hardware mode: the real RX log is created only when Central_Unit notifies the phone
            if (!_isSimulationMode.value) return@launch
            delay(latency)
            val receiveTime = sendTime + latency
            val formattedReceiveTime = timeFormat.format(Date(receiveTime))

            val rxLog = MessageLogEntity(
                timestamp = receiveTime,
                formattedTime = formattedReceiveTime,
                sourceNode = "Source Node B",
                targetNode = "Central Unit",
                messageType = messageType,
                payload = messagePayload,
                latitude = location.latitude,
                longitude = location.longitude,
                altitude = location.altitude,
                accuracy = location.accuracyMeters,
                rssi = rxRssi,
                latencyMs = latency,
                status = "RECEIVED",
                sequenceNumber = sequenceNumber,
                rawHex = hexString
            )
            database.messageLogDao().insertLog(rxLog)

            _centralUnitStatus.update {
                it.copy(
                    packetsReceived = it.packetsReceived + 1,
                    lastSeenTimestamp = receiveTime
                )
            }

            _interNodeLink.update {
                it.copy(
                    packetsTransmitted = it.packetsTransmitted + 1,
                    averageLatencyMs = (it.averageLatencyMs * 4 + latency) / 5,
                    linkRssi = rxRssi
                )
            }
        }
    }

    /**
     * Start/Stop periodic beacon signal
     */
    fun toggleBeacon(enabled: Boolean, intervalMs: Long = 3000L) {
        _isBeaconActive.value = enabled
        beaconJob?.cancel()
        if (enabled) {
            beaconJob = scope.launch {
                var beaconIndex = 1
                while (isActive) {
                    sendSignalFromNodeA(
                        messagePayload = "BEACON_PULSE #$beaconIndex [BAT: ${_nodeAStatus.value.batteryPct}%]",
                        messageType = "BEACON"
                    )
                    beaconIndex++
                    delay(intervalMs)
                }
            }
        }
    }

    fun clearLogs() {
        scope.launch {
            database.messageLogDao().clearAll()
        }
    }

    private fun startSignalHeartbeat() {
        scope.launch {
            while (isActive) {
                delay(2000)
                val now = System.currentTimeMillis()

                val jitterA = Random.nextInt(-2, 3)
                val jitterB = Random.nextInt(-2, 3)
                val jitterCentral = Random.nextInt(-2, 3)

                if (_nodeAStatus.value.connectionState == ConnectionState.CONNECTED) {
                    _nodeAStatus.update {
                        val newRssi = (it.rssi + jitterA).coerceIn(-95, -42)
                        it.copy(rssi = newRssi, lastSeenTimestamp = now)
                    }
                }

                if (_nodeBStatus.value.connectionState == ConnectionState.CONNECTED) {
                    _nodeBStatus.update {
                        val newRssi = (it.rssi + jitterB).coerceIn(-95, -45)
                        it.copy(rssi = newRssi, lastSeenTimestamp = now)
                    }
                }

                if (_centralUnitStatus.value.connectionState == ConnectionState.CONNECTED) {
                    _centralUnitStatus.update {
                        val newRssi = (it.rssi + jitterCentral).coerceIn(-95, -40)
                        it.copy(rssi = newRssi, lastSeenTimestamp = now)
                    }
                }

                val newPoint = SignalPoint(
                    timestamp = now,
                    rssiNodeA = _nodeAStatus.value.rssi,
                    rssiNodeB = _nodeBStatus.value.rssi,
                    latencyMs = _interNodeLink.value.averageLatencyMs + Random.nextLong(-3, 4)
                )

                _signalHistory.update { history ->
                    (history + newPoint).takeLast(30)
                }
            }
        }
    }

    private fun toHex(input: String): String {
        return input.toByteArray().joinToString(" ") { "%02X".format(it) }
    }
}