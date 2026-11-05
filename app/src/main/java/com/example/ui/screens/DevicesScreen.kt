package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ToggleOff
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
fun DevicesScreen(
    viewModel: BluLinqViewModel
) {
    val context = LocalContext.current
    val nodeAStatus by viewModel.nodeAStatus.collectAsStateWithLifecycle()
    val nodeBStatus by viewModel.nodeBStatus.collectAsStateWithLifecycle()
    val centralStatus by viewModel.centralUnitStatus.collectAsStateWithLifecycle()
    val availableDevices by viewModel.availableDevices.collectAsStateWithLifecycle()
    val isSimulationMode by viewModel.isSimulationMode.collectAsStateWithLifecycle()
    val interNodeLink by viewModel.interNodeLink.collectAsStateWithLifecycle()
    val logs by viewModel.filteredLogs.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BluBackgroundGradient)
            .padding(horizontal = 14.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header & Scan Action
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "HARDWARE & SETTINGS",
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = BluCyanNeon,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Manage dedicated Bluetooth links for all 3 nodes",
                        fontSize = 11.sp,
                        color = BluTextSecondary
                    )
                }

                Button(
                    onClick = {
                        viewModel.scanDevices()
                        Toast.makeText(context, "Scanning for ESP32 Bluetooth nodes...", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BluCyanNeon, contentColor = Color.Black),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("scan_devices_button")
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Scan Nearby", fontSize = 11.sp, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 3 Dedicated Hardware Nodes (Slot Cards)
        item {
            Text(
                text = "DEDICATED HARDWARE COMPONENT SLOTS",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = BluTextTertiary,
                letterSpacing = 0.5.sp
            )
        }

        // Slot 1: Source Node A (Transmitter 1)
        item {
            HardwareSlotCard(
                title = "Source Node A (Transmitter 1)",
                roleDesc = "Transmits text with N1: prefix",
                chipId = "ESP32_NODE_A",
                status = nodeAStatus,
                accentColor = BluCyanNeon,
                onConnect = {
                    viewModel.autoConnectNode(NodeRole.NODE_A)
                    Toast.makeText(context, "Connecting to Source Node A...", Toast.LENGTH_SHORT).show()
                },
                onDisconnect = {
                    viewModel.disconnectSourceNodeA()
                    Toast.makeText(context, "Disconnected Source Node A", Toast.LENGTH_SHORT).show()
                },
                testTagPrefix = "settings_node_a"
            )
        }

        // Slot 2: Source Node B (Transmitter 2)
        item {
            HardwareSlotCard(
                title = "Source Node B (Transmitter 2)",
                roleDesc = "Transmits text with N2: prefix",
                chipId = "ESP32_NODE_B",
                status = nodeBStatus,
                accentColor = Color(0xFFFF9E0B),
                onConnect = {
                    viewModel.autoConnectNode(NodeRole.NODE_B)
                    Toast.makeText(context, "Connecting to Source Node B...", Toast.LENGTH_SHORT).show()
                },
                onDisconnect = {
                    viewModel.disconnectSourceNodeB()
                    Toast.makeText(context, "Disconnected Source Node B", Toast.LENGTH_SHORT).show()
                },
                testTagPrefix = "settings_node_b"
            )
        }

        // Slot 3: Central Unit (Receiver)
        item {
            HardwareSlotCard(
                title = "Central Unit (Receiver)",
                roleDesc = "Listens on 6E400003 & parses N1 / N2",
                chipId = "Central_Unit",
                status = centralStatus,
                accentColor = BluElectricBright,
                onConnect = {
                    viewModel.connectCentralUnitAutomated()
                    Toast.makeText(context, "Initiating automated connection to Central_Unit...", Toast.LENGTH_SHORT).show()
                },
                onDisconnect = {
                    viewModel.disconnectCentralUnit()
                    Toast.makeText(context, "Disconnected Central Unit", Toast.LENGTH_SHORT).show()
                },
                testTagPrefix = "settings_central_unit"
            )
        }

        // Simulation Mode Card
        item {
            MinimalCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = if (isSimulationMode) BluCyanNeon.copy(alpha = 0.6f) else BluBorderSubtle,
                backgroundColor = BluDarkNavy
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Simulation Mode",
                                fontFamily = SpaceGroteskFontFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = BluTextPrimary
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (isSimulationMode) BluCyanNeon.copy(alpha = 0.2f) else BluSurfaceElevated
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isSimulationMode) "SIMULATED" else "HARDWARE GATT",
                                    fontSize = 8.sp,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSimulationMode) BluCyanNeon else BluTextTertiary
                                )
                            }
                        }
                        Text(
                            text = if (isSimulationMode)
                                "Mock Bluetooth links enabled for development and testing without physical ESP32 boards."
                            else
                                "Real physical Bluetooth GATT connection required with hardware ESP32 microcontrollers.",
                            fontSize = 11.sp,
                            color = BluTextSecondary,
                            lineHeight = 15.sp
                        )
                    }

                    Switch(
                        checked = isSimulationMode,
                        onCheckedChange = { viewModel.toggleSimulationMode(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = BluCyanNeon,
                            checkedTrackColor = BluCyanNeon.copy(alpha = 0.3f),
                            uncheckedThumbColor = BluTextTertiary,
                            uncheckedTrackColor = BluSurfaceElevated
                        ),
                        modifier = Modifier.testTag("simulation_mode_switch")
                    )
                }
            }
        }

        // Available Bluetooth Devices Section
        item {
            Text(
                text = "DISCOVERED BLUETOOTH DEVICES (${availableDevices.size})",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = BluTextTertiary,
                letterSpacing = 0.5.sp
            )
        }

        if (availableDevices.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(BluSurfaceElevated)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No Bluetooth devices found. Tap 'Scan Nearby' above.",
                        fontSize = 11.sp,
                        color = BluTextTertiary
                    )
                }
            }
        } else {
            items(availableDevices) { dev ->
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
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bluetooth,
                                        contentDescription = null,
                                        tint = BluCyanNeon,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = dev.name,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BluTextPrimary
                                    )
                                }
                                Text(
                                    text = dev.address,
                                    fontSize = 10.sp,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    color = BluTextTertiary
                                )
                            }
                            SignalStrengthBar(rssi = dev.rssi)
                        }

                        // Assign buttons for each of the 3 roles
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.connectDevice(dev, NodeRole.NODE_A)
                                    Toast.makeText(context, "Assigned ${dev.name} to Source Node A", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f).height(34.dp),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Assign Node A", fontSize = 10.sp, fontFamily = JetBrainsMonoFontFamily, color = BluCyanNeon)
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.connectDevice(dev, NodeRole.NODE_B)
                                    Toast.makeText(context, "Assigned ${dev.name} to Source Node B", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f).height(34.dp),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Assign Node B", fontSize = 10.sp, fontFamily = JetBrainsMonoFontFamily, color = Color(0xFFFF9E0B))
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.connectDevice(dev, NodeRole.CENTRAL)
                                    Toast.makeText(context, "Assigned ${dev.name} to Central Unit", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f).height(34.dp),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Assign Central", fontSize = 10.sp, fontFamily = JetBrainsMonoFontFamily, color = BluElectricBright)
                            }
                        }
                    }
                }
            }
        }

        // Data Logs Management Card
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
                        Text(
                            text = "DATA LOGS & AUDIT TRAIL",
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = BluTextPrimary
                        )

                        Text(
                            text = "${logs.size} Records Stored",
                            fontSize = 11.sp,
                            fontFamily = JetBrainsMonoFontFamily,
                            color = BluTextTertiary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val csv = viewModel.exportLogsToCsv()
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("BluLinq_Logs_CSV", csv))
                                Toast.makeText(context, "Exported CSV copied to clipboard (${logs.size} rows)", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f).height(38.dp),
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BluSurfaceElevated, contentColor = BluCyanNeon)
                        ) {
                            Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export CSV", fontSize = 11.sp, fontFamily = JetBrainsMonoFontFamily)
                        }

                        Button(
                            onClick = {
                                viewModel.clearLogs()
                                Toast.makeText(context, "Cleared all message logs", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f).height(38.dp),
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BluError.copy(alpha = 0.15f), contentColor = BluError)
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear Logs", fontSize = 11.sp, fontFamily = JetBrainsMonoFontFamily)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HardwareSlotCard(
    title: String,
    roleDesc: String,
    chipId: String,
    status: com.example.data.model.NodeStatus,
    accentColor: Color,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    testTagPrefix: String
) {
    val isConnected = status.connectionState == ConnectionState.CONNECTED
    val isConnecting = status.connectionState == ConnectionState.CONNECTING

    MinimalCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (isConnected) accentColor.copy(alpha = 0.6f) else BluBorderSubtle,
        backgroundColor = BluDarkNavy
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = title,
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                    Text(
                        text = roleDesc,
                        fontSize = 10.sp,
                        color = BluTextTertiary
                    )
                }

                StatusBadge(connectionState = status.connectionState)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(BluSurfaceElevated)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = status.deviceName.ifEmpty { chipId },
                        fontSize = 12.sp,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = BluTextPrimary
                    )
                    Text(
                        text = status.deviceAddress.ifEmpty { "Bluetooth address not paired" },
                        fontSize = 10.sp,
                        fontFamily = JetBrainsMonoFontFamily,
                        color = BluTextTertiary
                    )
                }

                if (isConnected) {
                    SignalStrengthBar(rssi = status.rssi)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (isConnected) {
                    OutlinedButton(
                        onClick = onDisconnect,
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BluError),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("${testTagPrefix}_disconnect_button")
                    ) {
                        Text("Disconnect", fontSize = 11.sp, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onConnect,
                        enabled = !isConnecting,
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("${testTagPrefix}_connect_button")
                    ) {
                        Text(
                            text = if (isConnecting) "Connecting..." else "Connect",
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