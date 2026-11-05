package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.MessageLogEntity
import com.example.data.model.ConnectionState
import com.example.data.model.EspDevice
import com.example.data.model.NodeRole
import com.example.ui.BluLinqViewModel
import com.example.ui.components.MinimalCard
import com.example.ui.components.SignalStrengthBar
import com.example.ui.components.StatusBadge
import com.example.ui.theme.BluBackgroundGradient
import com.example.ui.theme.BluBorderGlow
import com.example.ui.theme.BluBorderSubtle
import com.example.ui.theme.BluCyanNeon
import com.example.ui.theme.BluDarkNavy
import com.example.ui.theme.BluElectric
import com.example.ui.theme.BluElectricBright
import com.example.ui.theme.BluError
import com.example.ui.theme.BluSuccessBright
import com.example.ui.theme.BluSurfaceElevated
import com.example.ui.theme.BluTextPrimary
import com.example.ui.theme.BluTextSecondary
import com.example.ui.theme.BluTextTertiary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily

/**
 * Strips raw N1: and N2: prefixes from message text to ensure
 * they are strictly hidden from the final user interface.
 */
fun stripRawPrefix(text: String): String {
    var clean = text.trim()
    while (clean.startsWith("N1:", ignoreCase = true) || clean.startsWith("N2:", ignoreCase = true)) {
        clean = clean.substring(3).trim()
    }
    return clean
}

@Composable
fun ReceiverScreen(
    viewModel: BluLinqViewModel
) {
    val context = LocalContext.current
    val centralStatus by viewModel.centralUnitStatus.collectAsStateWithLifecycle()
    val logs by viewModel.filteredLogs.collectAsStateWithLifecycle()
    val availableDevices by viewModel.availableDevices.collectAsStateWithLifecycle()

    var selectedFilter by remember { mutableStateOf("ALL") } // "ALL", "Source Node A", "Source Node B"
    var showDevicePicker by remember { mutableStateOf(false) }
    var testSimulationInput by remember { mutableStateOf("") }

    // All incoming messages received at Central Unit
    val incomingMessages = logs.filter {
        it.status == "RECEIVED" ||
                it.targetNode.contains("Central", ignoreCase = true) ||
                it.sourceNode.contains("Node A", ignoreCase = true) ||
                it.sourceNode.contains("Node B", ignoreCase = true)
    }

    val displayedMessages = incomingMessages.filter { msg ->
        when (selectedFilter) {
            "Source Node A" -> msg.sourceNode.contains("Node A", ignoreCase = true)
            "Source Node B" -> msg.sourceNode.contains("Node B", ignoreCase = true)
            else -> true
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BluBackgroundGradient)
            .padding(horizontal = 14.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Central Unit Receiver Header & Dedicated Bluetooth Card
        item {
            MinimalCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = if (centralStatus.connectionState == ConnectionState.CONNECTED) BluElectricBright.copy(alpha = 0.8f) else BluBorderGlow,
                backgroundColor = BluDarkNavy
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Forum,
                                contentDescription = "Central Unit",
                                tint = BluElectricBright,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "CENTRAL UNIT CONSOLE",
                                    fontFamily = SpaceGroteskFontFamily,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BluElectricBright,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "Receiver • Characteristic 6E400003 Listener",
                                    fontSize = 10.sp,
                                    color = BluTextTertiary
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(BluElectricBright.copy(alpha = 0.15f))
                                .border(1.dp, BluElectricBright.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "RECEIVER ONLY",
                                fontSize = 9.sp,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = BluElectricBright
                            )
                        }
                    }

                    // Dedicated Bluetooth Connection Bar for Central Unit
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(BluSurfaceElevated)
                            .border(1.dp, BluBorderSubtle, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatusBadge(connectionState = centralStatus.connectionState)
                            Column {
                                Text(
                                    text = centralStatus.deviceName,
                                    fontSize = 11.sp,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BluTextPrimary
                                )
                                if (centralStatus.deviceAddress.isNotBlank()) {
                                    Text(
                                        text = centralStatus.deviceAddress,
                                        fontSize = 9.sp,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        color = BluTextTertiary
                                    )
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (centralStatus.connectionState == ConnectionState.CONNECTED) {
                                        viewModel.disconnectCentralUnit()
                                        Toast.makeText(context, "Disconnected Central Unit", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.connectCentralUnitAutomated()
                                        Toast.makeText(context, "Initiating automated connection to Central_Unit...", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = centralStatus.connectionState != ConnectionState.CONNECTING,
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (centralStatus.connectionState == ConnectionState.CONNECTED) BluError.copy(alpha = 0.2f) else BluElectricBright.copy(alpha = 0.15f),
                                    contentColor = if (centralStatus.connectionState == ConnectionState.CONNECTED) BluError else BluElectricBright
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (centralStatus.connectionState == ConnectionState.CONNECTED) BluError else BluElectricBright
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("central_unit_connect_button")
                            ) {
                                Icon(
                                    imageVector = if (centralStatus.connectionState == ConnectionState.CONNECTED) Icons.Default.BluetoothConnected else Icons.Default.Bluetooth,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = when (centralStatus.connectionState) {
                                        ConnectionState.CONNECTING -> "Connecting..."
                                        ConnectionState.CONNECTED -> "Disconnect"
                                        else -> "Connect"
                                    },
                                    fontSize = 11.5.sp,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Manual device selection button
                            IconButton(
                                onClick = { showDevicePicker = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Pick Device",
                                    tint = BluTextTertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Automated BLE Flow Status Indicator
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(BluSurfaceElevated)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = when (centralStatus.connectionState) {
                                ConnectionState.CONNECTING -> "Handshake: Identifying 'Central_Unit' • Discovering Service 6E400001 • Subscribing CCCD 0x2902..."
                                ConnectionState.CONNECTED -> "Active: Subscribed to 6E400003 • Silently listening in background for N1: & N2: messages"
                                ConnectionState.FAILED -> "Connection failed. Please ensure Central_Unit is powered on and within range."
                                else -> "Tap 'Connect' to automatically identify Central_Unit, discover Service 6E400001, and subscribe to 6E400003."
                            },
                            fontSize = 10.sp,
                            fontFamily = JetBrainsMonoFontFamily,
                            color = when (centralStatus.connectionState) {
                                ConnectionState.CONNECTING -> Color(0xFFFF9E0B)
                                ConnectionState.CONNECTED -> BluSuccessBright
                                ConnectionState.FAILED -> BluError
                                else -> BluTextTertiary
                            },
                            lineHeight = 14.sp
                        )
                    }

                    // Live Received Message Counter
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Received Packets: ${centralStatus.packetsReceived}",
                            fontSize = 11.sp,
                            fontFamily = JetBrainsMonoFontFamily,
                            color = BluTextSecondary
                        )

                        Text(
                            text = "RSSI: ${if (centralStatus.connectionState == ConnectionState.CONNECTED) "${centralStatus.rssi} dBm" else "--"}",
                            fontSize = 11.sp,
                            fontFamily = JetBrainsMonoFontFamily,
                            color = BluElectricBright
                        )
                    }
                }
            }
        }

        // 2. BLE Notification Parsing Test Suite (For Verifying N1 / N2 Parsing)
        item {
            MinimalCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = BluBorderSubtle,
                backgroundColor = BluDarkNavy
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "BLE NOTIFICATION TEST BENCH",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BluCyanNeon,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "UUID: 6E400003",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            color = BluTextTertiary
                        )
                    }

                    Text(
                        text = "Simulate raw incoming BLE text packets over 6E400003 to test parsing rule (N1 -> Source Node A, N2 -> Source Node B):",
                        fontSize = 10.sp,
                        color = BluTextSecondary
                    )

                    // Quick Test Buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SuggestionChip(
                            onClick = {
                                viewModel.simulateIncomingBleNotification("N1:Help - Medical support required")
                                Toast.makeText(context, "Simulated BLE notify: N1:Help", Toast.LENGTH_SHORT).show()
                            },
                            label = { Text("Test N1:Help", fontSize = 10.sp, fontFamily = JetBrainsMonoFontFamily, color = BluCyanNeon) },
                            colors = SuggestionChipDefaults.suggestionChipColors(containerColor = BluSurfaceElevated),
                            border = SuggestionChipDefaults.suggestionChipBorder(enabled = true, borderColor = BluCyanNeon.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.testTag("simulate_n1_button")
                        )

                        SuggestionChip(
                            onClick = {
                                viewModel.simulateIncomingBleNotification("N2:All clear - Area perimeter secured")
                                Toast.makeText(context, "Simulated BLE notify: N2:All clear", Toast.LENGTH_SHORT).show()
                            },
                            label = { Text("Test N2:All clear", fontSize = 10.sp, fontFamily = JetBrainsMonoFontFamily, color = Color(0xFFFF9E0B)) },
                            colors = SuggestionChipDefaults.suggestionChipColors(containerColor = BluSurfaceElevated),
                            border = SuggestionChipDefaults.suggestionChipBorder(enabled = true, borderColor = Color(0xFFFF9E0B).copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.testTag("simulate_n2_button")
                        )

                        SuggestionChip(
                            onClick = {
                                viewModel.simulateIncomingBleNotification("N1:MAYDAY: Structural collapse at Ridge")
                                Toast.makeText(context, "Simulated BLE notify: N1:MAYDAY", Toast.LENGTH_SHORT).show()
                            },
                            label = { Text("Test N1:MAYDAY", fontSize = 10.sp, fontFamily = JetBrainsMonoFontFamily, color = BluError) },
                            colors = SuggestionChipDefaults.suggestionChipColors(containerColor = BluSurfaceElevated),
                            border = SuggestionChipDefaults.suggestionChipBorder(enabled = true, borderColor = BluError.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(6.dp)
                        )

                        SuggestionChip(
                            onClick = {
                                viewModel.simulateIncomingBleNotification("N2:Battery voltage 4.10V OK")
                                Toast.makeText(context, "Simulated BLE notify: N2:Battery", Toast.LENGTH_SHORT).show()
                            },
                            label = { Text("Test N2:Battery", fontSize = 10.sp, fontFamily = JetBrainsMonoFontFamily, color = BluSuccessBright) },
                            colors = SuggestionChipDefaults.suggestionChipColors(containerColor = BluSurfaceElevated),
                            border = SuggestionChipDefaults.suggestionChipBorder(enabled = true, borderColor = BluSuccessBright.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(6.dp)
                        )
                    }

                    // Custom input test box
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = testSimulationInput,
                            onValueChange = { testSimulationInput = it },
                            placeholder = { Text("Enter raw string e.g. N1:Message", fontSize = 11.sp, color = BluTextTertiary) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("custom_raw_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BluCyanNeon,
                                unfocusedBorderColor = BluBorderSubtle,
                                focusedContainerColor = BluSurfaceElevated,
                                unfocusedContainerColor = BluSurfaceElevated,
                                focusedTextColor = BluTextPrimary,
                                unfocusedTextColor = BluTextPrimary
                            ),
                            shape = RoundedCornerShape(6.dp),
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                if (testSimulationInput.isNotBlank()) {
                                    viewModel.simulateIncomingBleNotification(testSimulationInput)
                                    testSimulationInput = ""
                                    Toast.makeText(context, "Injected packet to Central Unit", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = testSimulationInput.isNotBlank(),
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BluCyanNeon, contentColor = Color.Black),
                            modifier = Modifier.testTag("inject_packet_button")
                        ) {
                            Text("Inject", fontSize = 11.sp, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 3. Filter Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "INCOMING MESSAGES (${displayedMessages.size})",
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = BluTextPrimary,
                    letterSpacing = 0.5.sp
                )

                // Sender Filter Chips
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("ALL", "Source Node A", "Source Node B").forEach { filterOpt ->
                        val isSelected = selectedFilter == filterOpt
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) BluElectric.copy(alpha = 0.3f) else BluSurfaceElevated)
                                .border(
                                    1.dp,
                                    if (isSelected) BluElectricBright else BluBorderSubtle,
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { selectedFilter = filterOpt }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = when (filterOpt) {
                                    "Source Node A" -> "Node A"
                                    "Source Node B" -> "Node B"
                                    else -> "All"
                                },
                                fontSize = 10.sp,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) BluElectricBright else BluTextSecondary
                            )
                        }
                    }
                }
            }
        }

        // 4. Chat-style Messages List
        if (displayedMessages.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(BluSurfaceElevated)
                        .border(1.dp, BluBorderSubtle, RoundedCornerShape(10.dp))
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inbox,
                            contentDescription = null,
                            tint = BluTextTertiary,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "No Messages Received Yet",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = BluTextSecondary,
                            fontFamily = SpaceGroteskFontFamily
                        )
                        Text(
                            text = "Central Unit is listening on characteristic 6E400003 for incoming notifications from Source Node A (N1) and Source Node B (N2).",
                            fontSize = 11.sp,
                            color = BluTextTertiary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(displayedMessages.reversed(), key = { it.id }) { message ->
                ChatMessageBubble(message = message, context = context)
            }
        }
    }

    // Central Unit Device Picker Dialog
    if (showDevicePicker) {
        AlertDialog(
            onDismissRequest = { showDevicePicker = false },
            title = {
                Text(
                    text = "Connect Central Unit",
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = BluTextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Select your hardware ESP32 Central Unit receiver:",
                        fontSize = 12.sp,
                        color = BluTextSecondary
                    )

                    val candidateDevices = availableDevices.filter {
                        it.targetRole == NodeRole.CENTRAL || it.name.contains("CENTRAL", ignoreCase = true) || it.name.contains("RECEIVER", ignoreCase = true)
                    }.ifEmpty { availableDevices }

                    if (candidateDevices.isEmpty()) {
                        Text(
                            text = "No Bluetooth devices found. Tap Scan to discover nearby ESP32 units.",
                            fontSize = 11.sp,
                            color = BluTextTertiary
                        )
                    } else {
                        candidateDevices.forEach { dev ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BluSurfaceElevated)
                                    .border(1.dp, BluBorderSubtle, RoundedCornerShape(8.dp))
                                    .clickable {
                                        viewModel.connectDevice(dev, NodeRole.CENTRAL)
                                        showDevicePicker = false
                                        Toast.makeText(context, "Connecting to ${dev.name} as Central Unit...", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = dev.name,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BluTextPrimary
                                    )
                                    Text(
                                        text = dev.address,
                                        fontSize = 10.sp,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        color = BluTextTertiary
                                    )
                                }

                                Text(
                                    text = "Connect",
                                    fontSize = 11.sp,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = BluElectricBright
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.scanDevices()
                    }
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Scan Nearby")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDevicePicker = false }) {
                    Text("Cancel")
                }
            },
            containerColor = BluDarkNavy
        )
    }
}

@Composable
fun ChatMessageBubble(
    message: MessageLogEntity,
    context: Context
) {
    val isNodeA = message.sourceNode.contains("Node A", ignoreCase = true)
    val isEmergency = message.messageType == "ALERT" ||
            message.payload.contains("SOS", ignoreCase = true) ||
            message.payload.contains("HELP", ignoreCase = true) ||
            message.payload.contains("MAYDAY", ignoreCase = true)

    val senderColor = if (isNodeA) BluCyanNeon else Color(0xFFFF9E0B)
    val senderDisplayName = if (isNodeA) "Source Node A" else "Source Node B"

    // Strictly hide and strip raw N1: or N2: prefixes from the displayed message
    val cleanPayload = stripRawPrefix(message.payload)

    MinimalCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chat_bubble_${message.id}"),
        borderColor = if (isEmergency) BluError.copy(alpha = 0.8f) else senderColor.copy(alpha = 0.35f),
        backgroundColor = BluDarkNavy
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // SENDER NAME CLEARLY STATED DIRECTLY ABOVE MESSAGE
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(senderColor)
                    )
                    Text(
                        text = "Sender: $senderDisplayName",
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = senderColor
                    )
                    if (isEmergency) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(BluError.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "EMERGENCY",
                                fontSize = 8.sp,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = BluError
                            )
                        }
                    }
                }

                Text(
                    text = message.formattedTime,
                    fontSize = 10.sp,
                    fontFamily = JetBrainsMonoFontFamily,
                    color = BluTextTertiary
                )
            }

            // Actual Clean Message Text (Raw N1: and N2: are strictly hidden)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(BluSurfaceElevated)
                    .border(1.dp, BluBorderSubtle, RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Text(
                    text = cleanPayload,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = BluTextPrimary,
                    lineHeight = 18.sp
                )
            }

            // Bottom metadata bar: Target (Central Unit), RSSI, Latency, GPS coordinates copy
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Received at Central Unit • ${message.rssi} dBm • ${message.latencyMs}ms",
                    fontSize = 9.sp,
                    fontFamily = JetBrainsMonoFontFamily,
                    color = BluTextTertiary
                )

                if (message.latitude != null && message.longitude != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable {
                                val coords = "${message.latitude},${message.longitude}"
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Coordinates", coords))
                                Toast.makeText(context, "Copied coordinates: $coords", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "GPS Coordinates",
                            tint = BluCyanNeon,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "${"%.4f".format(message.latitude)}, ${"%.4f".format(message.longitude)}",
                            fontSize = 9.sp,
                            fontFamily = JetBrainsMonoFontFamily,
                            color = BluCyanNeon
                        )
                    }
                }
            }
        }
    }
}
