package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

@Composable
fun TransmitterScreen(
    viewModel: BluLinqViewModel
) {
    val context = LocalContext.current
    val nodeAStatus by viewModel.nodeAStatus.collectAsStateWithLifecycle()
    val nodeBStatus by viewModel.nodeBStatus.collectAsStateWithLifecycle()
    val activeNode by viewModel.activeTransmitterNode.collectAsStateWithLifecycle()
    val messageInputA by viewModel.messageInputA.collectAsStateWithLifecycle()
    val messageInputB by viewModel.messageInputB.collectAsStateWithLifecycle()
    val currentLocation by viewModel.currentLocation.collectAsStateWithLifecycle()
    val isBeaconActive by viewModel.isBeaconActive.collectAsStateWithLifecycle()
    val availableDevices by viewModel.availableDevices.collectAsStateWithLifecycle()

    var showDevicePickerRole by remember { mutableStateOf<NodeRole?>(null) }

    val isNodeAActive = activeNode == "NODE_A"
    val isNodeBActive = activeNode == "NODE_B"

    val presetsNodeA = listOf(
        "MAYDAY: SOS RESCUE REQUIRED",
        "STATUS: ALL SYSTEMS NORMAL",
        "WAYPOINT: ALPHA IN POSITION",
        "BATTERY: 3.92V NORMAL"
    )

    val presetsNodeB = listOf(
        "HAZARD: EVACUATE SECTOR B",
        "STATUS: NODE B CHECK-IN",
        "PERIMETER: SECURE ALL CLEAR",
        "BATTERY: 4.05V OPERATIONAL"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BluBackgroundGradient)
            .padding(horizontal = 14.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Mutual Exclusivity Controller Header
        item {
            MinimalCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = BluBorderGlow,
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
                                imageVector = Icons.Default.Sensors,
                                contentDescription = "Transmitter Selection",
                                tint = BluCyanNeon,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "TRANSMITTER CONTROLLER",
                                fontFamily = SpaceGroteskFontFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = BluCyanNeon,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(BluCyanNeon.copy(alpha = 0.15f))
                                .border(1.dp, BluCyanNeon.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "MUTUALLY EXCLUSIVE",
                                fontSize = 9.sp,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = BluCyanNeon
                            )
                        }
                    }

                    Text(
                        text = "Select which transmitter node is active. Only one transmitter can send at a time; the inactive node is automatically locked.",
                        fontSize = 11.sp,
                        color = BluTextSecondary,
                        lineHeight = 15.sp
                    )

                    // Mutual Exclusivity Toggle Selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(BluSurfaceElevated)
                            .border(1.dp, BluBorderSubtle, RoundedCornerShape(10.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Option A: Source Node A
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isNodeAActive) BluCyanNeon.copy(alpha = 0.2f) else Color.Transparent
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isNodeAActive) BluCyanNeon else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    viewModel.setActiveTransmitterNode("NODE_A")
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp)
                                .testTag("select_active_node_a"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (isNodeAActive) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (isNodeAActive) BluCyanNeon else BluTextTertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Source Node A",
                                    fontSize = 12.sp,
                                    fontFamily = SpaceGroteskFontFamily,
                                    fontWeight = if (isNodeAActive) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isNodeAActive) BluCyanNeon else BluTextSecondary
                                )
                            }
                        }

                        // Option B: Source Node B
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isNodeBActive) Color(0xFFFF9E0B).copy(alpha = 0.2f) else Color.Transparent
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isNodeBActive) Color(0xFFFF9E0B) else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    viewModel.setActiveTransmitterNode("NODE_B")
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp)
                                .testTag("select_active_node_b"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (isNodeBActive) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (isNodeBActive) Color(0xFFFF9E0B) else BluTextTertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Source Node B",
                                    fontSize = 12.sp,
                                    fontFamily = SpaceGroteskFontFamily,
                                    fontWeight = if (isNodeBActive) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isNodeBActive) Color(0xFFFF9E0B) else BluTextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Source Node A Transmitter Card
        item {
            TransmitterNodeSection(
                title = "Source Node A",
                subtitle = "Transmitter 1 • Formats with N1: prefix",
                role = NodeRole.NODE_A,
                status = nodeAStatus,
                isActive = isNodeAActive,
                accentColor = BluCyanNeon,
                messageText = messageInputA,
                onMessageChange = { viewModel.setMessageInputA(it) },
                onSend = { viewModel.sendMessageFromNodeA() },
                presets = presetsNodeA,
                onPresetClick = { viewModel.sendPresetFromNodeA(it) },
                onConnectClick = {
                    if (nodeAStatus.connectionState == ConnectionState.CONNECTED) {
                        viewModel.disconnectSourceNodeA()
                        Toast.makeText(context, "Disconnected Source Node A", Toast.LENGTH_SHORT).show()
                    } else {
                        showDevicePickerRole = NodeRole.NODE_A
                    }
                },
                testTagPrefix = "node_a"
            )
        }

        // 3. Source Node B Transmitter Card
        item {
            TransmitterNodeSection(
                title = "Source Node B",
                subtitle = "Transmitter 2 • Formats with N2: prefix",
                role = NodeRole.NODE_B,
                status = nodeBStatus,
                isActive = isNodeBActive,
                accentColor = Color(0xFFFF9E0B),
                messageText = messageInputB,
                onMessageChange = { viewModel.setMessageInputB(it) },
                onSend = { viewModel.sendMessageFromNodeB() },
                presets = presetsNodeB,
                onPresetClick = { viewModel.sendPresetFromNodeB(it) },
                onConnectClick = {
                    if (nodeBStatus.connectionState == ConnectionState.CONNECTED) {
                        viewModel.disconnectSourceNodeB()
                        Toast.makeText(context, "Disconnected Source Node B", Toast.LENGTH_SHORT).show()
                    } else {
                        showDevicePickerRole = NodeRole.NODE_B
                    }
                },
                testTagPrefix = "node_b"
            )
        }

        // 4. GPS & Auto-Beacon Tools Card
        item {
            MinimalCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = BluBorderSubtle,
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
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = null,
                                tint = BluCyanNeon,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "TELEMETRY & BEACON",
                                fontFamily = SpaceGroteskFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = BluTextPrimary
                            )
                        }

                        Button(
                            onClick = {
                                viewModel.refreshLocation()
                                Toast.makeText(context, "Acquired GPS fix", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BluSurfaceElevated, contentColor = BluCyanNeon),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("refresh_gps_button")
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Refresh GPS", fontSize = 11.sp, fontFamily = JetBrainsMonoFontFamily)
                        }
                    }

                    currentLocation?.let { loc ->
                        Text(
                            text = "LAT: ${"%.5f".format(loc.latitude)}° | LON: ${"%.5f".format(loc.longitude)}° | ACC: ${loc.accuracyMeters}m",
                            fontSize = 11.sp,
                            fontFamily = JetBrainsMonoFontFamily,
                            color = BluTextSecondary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Emergency Periodic Beacon",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BluTextPrimary
                            )
                            Text(
                                text = if (isBeaconActive) "Broadcasting pulse every 3s..." else "Pulse beacon is currently stopped",
                                fontSize = 10.sp,
                                color = if (isBeaconActive) BluSuccessBright else BluTextTertiary
                            )
                        }

                        Button(
                            onClick = {
                                viewModel.toggleBeacon(!isBeaconActive)
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isBeaconActive) BluError.copy(alpha = 0.2f) else BluCyanNeon.copy(alpha = 0.15f),
                                contentColor = if (isBeaconActive) BluError else BluCyanNeon
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isBeaconActive) BluError else BluCyanNeon
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("toggle_beacon_button")
                        ) {
                            Icon(
                                imageVector = if (isBeaconActive) Icons.Default.Timer else Icons.Default.Sensors,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBeaconActive) "Stop Beacon" else "Start Beacon",
                                fontSize = 11.sp,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // Bluetooth Connection Picker Dialog
    showDevicePickerRole?.let { role ->
        val roleName = if (role == NodeRole.NODE_A) "Source Node A" else "Source Node B"
        val expectedPrefix = if (role == NodeRole.NODE_A) "NODE_A" else "NODE_B"

        AlertDialog(
            onDismissRequest = { showDevicePickerRole = null },
            title = {
                Text(
                    text = "Connect $roleName",
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = BluTextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Select your hardware ESP32 unit to pair with $roleName:",
                        fontSize = 12.sp,
                        color = BluTextSecondary
                    )

                    val candidateDevices = availableDevices.filter {
                        it.targetRole == role || it.name.contains(expectedPrefix, ignoreCase = true)
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
                                        viewModel.connectDevice(dev, role)
                                        showDevicePickerRole = null
                                        Toast.makeText(context, "Connecting to ${dev.name} as $roleName...", Toast.LENGTH_SHORT).show()
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
                                    color = BluCyanNeon
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
                TextButton(onClick = { showDevicePickerRole = null }) {
                    Text("Cancel")
                }
            },
            containerColor = BluDarkNavy
        )
    }
}

@Composable
fun TransmitterNodeSection(
    title: String,
    subtitle: String,
    role: NodeRole,
    status: com.example.data.model.NodeStatus,
    isActive: Boolean,
    accentColor: Color,
    messageText: String,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit,
    presets: List<String>,
    onPresetClick: (String) -> Unit,
    onConnectClick: () -> Unit,
    testTagPrefix: String
) {
    val isConnected = status.connectionState == ConnectionState.CONNECTED
    val isConnecting = status.connectionState == ConnectionState.CONNECTING

    val containerAlpha = if (isActive) 1.0f else 0.45f
    val borderColor = if (isActive) {
        if (isConnected) accentColor.copy(alpha = 0.8f) else BluBorderGlow
    } else {
        BluBorderSubtle
    }

    MinimalCard(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(containerAlpha),
        borderColor = borderColor,
        backgroundColor = BluDarkNavy
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header: Title, Chip ID, Active Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = title,
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) accentColor else BluTextSecondary
                        )

                        if (!isActive) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Locked / Inactive",
                                tint = BluTextTertiary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Text(
                        text = subtitle,
                        fontSize = 10.sp,
                        color = BluTextTertiary
                    )
                }

                // Active / Inactive Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isActive) accentColor.copy(alpha = 0.15f) else Color.DarkGray.copy(alpha = 0.3f)
                        )
                        .border(
                            1.dp,
                            if (isActive) accentColor.copy(alpha = 0.5f) else Color.Transparent,
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (isActive) "ACTIVE TRANSMITTER" else "INACTIVE (LOCKED)",
                        fontSize = 9.sp,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) accentColor else BluTextTertiary
                    )
                }
            }

            // Dedicated Bluetooth Connection Bar
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
                    StatusBadge(connectionState = status.connectionState)
                    Column {
                        Text(
                            text = status.deviceName,
                            fontSize = 11.sp,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = BluTextPrimary
                        )
                        if (status.deviceAddress.isNotBlank()) {
                            Text(
                                text = status.deviceAddress,
                                fontSize = 9.sp,
                                fontFamily = JetBrainsMonoFontFamily,
                                color = BluTextTertiary
                            )
                        }
                    }
                }

                // Dedicated Bluetooth Connect / Disconnect Button
                Button(
                    onClick = onConnectClick,
                    enabled = !isConnecting,
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isConnected) BluError.copy(alpha = 0.2f) else accentColor.copy(alpha = 0.15f),
                        contentColor = if (isConnected) BluError else accentColor
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isConnected) BluError else accentColor
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("${testTagPrefix}_connect_button")
                ) {
                    Icon(
                        imageVector = if (isConnected) Icons.Default.BluetoothConnected else Icons.Default.Bluetooth,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = when {
                            isConnecting -> "Connecting..."
                            isConnected -> "Disconnect"
                            else -> "Connect"
                        },
                        fontSize = 11.sp,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (!isActive) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.35f))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = BluTextTertiary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "$title is inactive. Switch transmitter toggle above to enable typing and sending.",
                            fontSize = 11.sp,
                            color = BluTextTertiary,
                            fontFamily = SpaceGroteskFontFamily
                        )
                    }
                }
            }

            // Message Input Box
            OutlinedTextField(
                value = messageText,
                onValueChange = onMessageChange,
                enabled = isActive,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("${testTagPrefix}_message_input"),
                placeholder = {
                    Text(
                        text = if (isActive) "Type message to Central Unit..." else "Section disabled while inactive",
                        fontSize = 12.sp,
                        color = BluTextTertiary
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = BluBorderSubtle,
                    disabledBorderColor = BluBorderSubtle.copy(alpha = 0.5f),
                    focusedContainerColor = BluSurfaceElevated,
                    unfocusedContainerColor = BluSurfaceElevated,
                    disabledContainerColor = BluSurfaceElevated.copy(alpha = 0.4f),
                    focusedTextColor = BluTextPrimary,
                    unfocusedTextColor = BluTextPrimary,
                    disabledTextColor = BluTextTertiary
                ),
                shape = RoundedCornerShape(8.dp),
                minLines = 2,
                maxLines = 4
            )

            // Preset Quick Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                presets.forEach { preset ->
                    SuggestionChip(
                        onClick = { if (isActive) onPresetClick(preset) },
                        enabled = isActive,
                        label = {
                            Text(
                                text = preset,
                                fontSize = 10.sp,
                                fontFamily = JetBrainsMonoFontFamily,
                                color = if (isActive) BluTextSecondary else BluTextTertiary
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = BluSurfaceElevated
                        ),
                        border = SuggestionChipDefaults.suggestionChipBorder(
                            enabled = isActive,
                            borderColor = if (isActive) BluBorderSubtle else BluBorderSubtle.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(6.dp)
                    )
                }
            }

            // Send Button
            Button(
                onClick = onSend,
                enabled = isActive && messageText.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("${testTagPrefix}_send_button"),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = Color.Black,
                    disabledContainerColor = BluSurfaceElevated,
                    disabledContentColor = BluTextTertiary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Send from $title",
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
