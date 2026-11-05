package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ConnectionState
import com.example.ui.BluLinqViewModel
import com.example.ui.components.InterNodeLinkVisualizer
import com.example.ui.components.MinimalCard
import com.example.ui.components.NodeStatusMiniCard
import com.example.ui.components.RealtimeSignalCanvas
import com.example.ui.components.StatusBadge
import com.example.ui.theme.BluBlack
import com.example.ui.theme.BluBorder
import com.example.ui.theme.BluBorderSubtle
import com.example.ui.theme.BluCyan
import com.example.ui.theme.BluDarkNavy
import com.example.ui.theme.BluElectric
import com.example.ui.theme.BluElectricBright
import com.example.ui.theme.BluSuccess
import com.example.ui.theme.BluSurface
import com.example.ui.theme.BluSurfaceElevated
import com.example.ui.theme.BluTextPrimary
import com.example.ui.theme.BluTextSecondary
import com.example.ui.theme.BluTextTertiary

import com.example.ui.theme.BluCyanNeon
import com.example.ui.theme.BluSuccessBright
import com.example.ui.theme.BluBackgroundGradient
import com.example.ui.theme.BluTextCyan
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.components.EmergencyAlertHeroCard

@Composable
fun DashboardScreen(
    viewModel: BluLinqViewModel,
    onNavigateToTransmitter: () -> Unit,
    onNavigateToReceiver: () -> Unit,
    onNavigateToDevices: () -> Unit
) {
    val nodeAStatus by viewModel.nodeAStatus.collectAsStateWithLifecycle()
    val nodeBStatus by viewModel.nodeBStatus.collectAsStateWithLifecycle()
    val linkStatus by viewModel.interNodeLink.collectAsStateWithLifecycle()
    val signalHistory by viewModel.signalHistory.collectAsStateWithLifecycle()
    val isSimulationMode by viewModel.isSimulationMode.collectAsStateWithLifecycle()
    val isBeaconActive by viewModel.isBeaconActive.collectAsStateWithLifecycle()
    val logs by viewModel.filteredLogs.collectAsStateWithLifecycle()
    val currentLocation by viewModel.currentLocation.collectAsStateWithLifecycle()

    val latestEmergency = logs.firstOrNull {
        it.payload.contains("SOS", ignoreCase = true) ||
        it.payload.contains("HELP", ignoreCase = true) ||
        it.payload.contains("FIRE", ignoreCase = true) ||
        it.payload.contains("EMERGENCY", ignoreCase = true) ||
        it.payload.contains("MAYDAY", ignoreCase = true)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BluBackgroundGradient)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top App Bar / Title with Quick Simulation Toggle
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(11.dp)
                                .clip(CircleShape)
                                .background(BluCyanNeon)
                        )
                        Text(
                            text = "BLU-LINQ",
                            fontFamily = SpaceGroteskFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = BluTextPrimary,
                            letterSpacing = 1.2.sp
                        )
                    }
                    Text(
                        text = "ESP32 DUAL-NODE TELEMETRY & MESH",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = BluTextSecondary,
                        letterSpacing = 0.5.sp
                    )
                }

                // Demo / Hardware Switch
                MinimalCard(
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)),
                    backgroundColor = BluSurface
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isSimulationMode) "SIM MODE" else "HW MODE",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSimulationMode) BluCyanNeon else BluSuccessBright
                        )
                        Switch(
                            checked = isSimulationMode,
                            onCheckedChange = { viewModel.toggleSimulationMode(it) },
                            modifier = Modifier
                                .size(38.dp, 22.dp)
                                .testTag("sim_mode_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = BluCyanNeon,
                                checkedTrackColor = BluBorder,
                                uncheckedThumbColor = BluSuccessBright,
                                uncheckedTrackColor = BluBorderSubtle
                            )
                        )
                    }
                }
            }
        }

        // Emergency Alert Hero Banner if detected
        if (latestEmergency != null) {
            item {
                EmergencyAlertHeroCard(
                    message = latestEmergency.payload,
                    timestamp = latestEmergency.formattedTime,
                    sourceNode = latestEmergency.sourceNode,
                    rssi = latestEmergency.rssi,
                    onAcknowledge = {
                        viewModel.sendSignalPreset("ACK: ${latestEmergency.sequenceNumber} CONFIRMED")
                    }
                )
            }
        }

        // Live Dual Nodes Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NodeStatusMiniCard(
                    status = nodeAStatus,
                    modifier = Modifier.weight(1f)
                )
                NodeStatusMiniCard(
                    status = nodeBStatus,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Inter-Node Link Pipeline Visualizer
        item {
            InterNodeLinkVisualizer(
                linkStatus = linkStatus,
                nodeAConnected = nodeAStatus.connectionState == ConnectionState.CONNECTED,
                nodeBConnected = nodeBStatus.connectionState == ConnectionState.CONNECTED
            )
        }

        // Real-Time Signal Monitoring
        item {
            MinimalCard(modifier = Modifier.fillMaxWidth()) {
                Column {
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
                                contentDescription = "Signal Graph",
                                tint = BluCyanNeon,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "REAL-TIME SIGNAL TELEMETRY",
                                fontFamily = SpaceGroteskFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = BluCyanNeon,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(BluSurfaceElevated)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "LIVE dBm",
                                fontSize = 9.5.sp,
                                fontFamily = JetBrainsMonoFontFamily,
                                color = BluTextCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    RealtimeSignalCanvas(history = signalHistory)

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Node A RSSI", fontSize = 10.sp, color = BluTextTertiary, fontFamily = SpaceGroteskFontFamily)
                            Text("${nodeAStatus.rssi} dBm", fontSize = 13.sp, fontFamily = JetBrainsMonoFontFamily, color = BluCyanNeon, fontWeight = FontWeight.SemiBold)
                        }
                        Column {
                            Text("Node B RSSI", fontSize = 10.sp, color = BluTextTertiary, fontFamily = SpaceGroteskFontFamily)
                            Text("${nodeBStatus.rssi} dBm", fontSize = 13.sp, fontFamily = JetBrainsMonoFontFamily, color = BluElectricBright, fontWeight = FontWeight.SemiBold)
                        }
                        Column {
                            Text("Mesh Quality", fontSize = 10.sp, color = BluTextTertiary, fontFamily = SpaceGroteskFontFamily)
                            Text("${linkStatus.linkQualityPct}%", fontSize = 13.sp, fontFamily = JetBrainsMonoFontFamily, color = BluSuccessBright, fontWeight = FontWeight.SemiBold)
                        }
                        Column {
                            Text("Latency", fontSize = 10.sp, color = BluTextTertiary, fontFamily = SpaceGroteskFontFamily)
                            Text("${linkStatus.averageLatencyMs} ms", fontSize = 13.sp, fontFamily = JetBrainsMonoFontFamily, color = BluTextPrimary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Quick Signal Dispatch & Actions
        item {
            val isNodeAConnected = nodeAStatus.connectionState == ConnectionState.CONNECTED

            MinimalCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DISPATCH COMMANDS [NODE A → B]",
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = BluTextPrimary,
                            letterSpacing = 0.5.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = "GPS",
                                tint = BluCyanNeon,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = currentLocation?.let { "GPS ${"%.4f".format(it.latitude)}, ${"%.4f".format(it.longitude)}" } ?: "GPS Syncing",
                                fontSize = 10.sp,
                                fontFamily = JetBrainsMonoFontFamily,
                                color = BluTextSecondary
                            )
                        }
                    }

                    if (!isNodeAConnected) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(com.example.ui.theme.BluWarning.copy(alpha = 0.12f))
                                .border(0.5.dp, com.example.ui.theme.BluWarning.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "⚠️ Node A is offline. Bind ESP32_NODE_A in the Bluetooth tab to transmit.",
                                fontSize = 10.5.sp,
                                color = com.example.ui.theme.BluWarning,
                                fontFamily = JetBrainsMonoFontFamily
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Preset buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.sendSignalPreset("PING_TELEMETRY: TX_ALIVE") },
                            enabled = isNodeAConnected,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("quick_ping_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BluSurfaceElevated,
                                contentColor = BluCyanNeon,
                                disabledContainerColor = BluSurfaceElevated.copy(alpha = 0.4f),
                                disabledContentColor = BluTextTertiary
                            )
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Quick Ping", fontSize = 12.sp, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.sendSignalPreset("STATUS_SYNC: BAT=${nodeAStatus.batteryPct}%") },
                            enabled = isNodeAConnected,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("quick_sync_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BluSurfaceElevated,
                                contentColor = BluElectricBright,
                                disabledContainerColor = BluSurfaceElevated.copy(alpha = 0.4f),
                                disabledContentColor = BluTextTertiary
                            )
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Sync Node", fontSize = 12.sp, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Beacon Trigger
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.toggleBeacon(!isBeaconActive) },
                            enabled = isNodeAConnected,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("beacon_toggle_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (isBeaconActive) BluCyanNeon else BluTextSecondary,
                                disabledContentColor = BluTextTertiary
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isBeaconActive) BluCyanNeon else BluBorderSubtle
                            )
                        ) {
                            Icon(
                                imageVector = if (isBeaconActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isBeaconActive) "Stop Beacon" else "Auto Beacon",
                                fontSize = 12.sp,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Button(
                            onClick = onNavigateToTransmitter,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("compose_message_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BluElectric,
                                contentColor = BluTextPrimary
                            )
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Compose TX", fontSize = 12.sp, fontFamily = SpaceGroteskFontFamily, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Recent Transmission Log Preview (Last 3 events)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RECENT SIGNAL ACTIVITY",
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = BluTextSecondary,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "${logs.size} total packets",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.5.sp,
                    color = BluCyanNeon,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        items(logs.take(3)) { log ->
            MinimalCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = BluBorderSubtle,
                backgroundColor = BluDarkNavy
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "${log.sourceNode} → ${log.targetNode}",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (log.sourceNode.contains("Node A")) BluCyanNeon else BluElectricBright
                            )
                            Text(
                                text = "#${log.sequenceNumber}",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                color = BluTextTertiary
                            )
                        }
                        StatusBadge(status = log.status)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = log.payload,
                        color = BluTextPrimary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = SpaceGroteskFontFamily
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = log.latitude?.let { "📍 ${"%.4f".format(it)}, ${"%.4f".format(log.longitude ?: 0.0)}" } ?: "📍 No GPS Fix",
                            fontSize = 10.sp,
                            color = BluTextSecondary,
                            fontFamily = JetBrainsMonoFontFamily
                        )
                        Text(
                            text = "${log.formattedTime}  •  ${log.latencyMs}ms",
                            fontSize = 10.sp,
                            color = BluTextTertiary,
                            fontFamily = JetBrainsMonoFontFamily
                        )
                    }
                }
            }
        }
    }
}
