package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.BluLinqViewModel
import com.example.ui.components.MinimalCard
import com.example.ui.components.RealtimeSignalCanvas
import com.example.ui.components.StatusBadge
import com.example.ui.theme.BluBlack
import com.example.ui.theme.BluBorder
import com.example.ui.theme.BluBorderSubtle
import com.example.ui.theme.BluCyan
import com.example.ui.theme.BluDarkNavy
import com.example.ui.theme.BluElectric
import com.example.ui.theme.BluElectricBright
import com.example.ui.theme.BluError
import com.example.ui.theme.BluSuccess
import com.example.ui.theme.BluSurface
import com.example.ui.theme.BluSurfaceElevated
import com.example.ui.theme.BluTextCyan
import com.example.ui.theme.BluTextPrimary
import com.example.ui.theme.BluTextSecondary
import com.example.ui.theme.BluTextTertiary
import com.example.ui.theme.BluCyanNeon
import com.example.ui.theme.BluSuccessBright
import com.example.ui.theme.BluBackgroundGradient
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.JetBrainsMonoFontFamily

@Composable
fun LogsScreen(
    viewModel: BluLinqViewModel
) {
    val context = LocalContext.current
    val logs by viewModel.filteredLogs.collectAsStateWithLifecycle()
    val logFilter by viewModel.logFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val signalHistory by viewModel.signalHistory.collectAsStateWithLifecycle()
    val linkStatus by viewModel.interNodeLink.collectAsStateWithLifecycle()

    var showClearConfirm by remember { mutableStateOf(false) }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = {
                Text("Clear All Telemetry Logs?", color = BluTextPrimary, fontFamily = JetBrainsMonoFontFamily)
            },
            text = {
                Text(
                    "This will delete all recorded transmissions, signal packets, and GPS coordinates from local storage.",
                    color = BluTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearLogs()
                        showClearConfirm = false
                        Toast.makeText(context, "All logs cleared", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BluError)
                ) {
                    Text("Delete All", color = BluBlack, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel", color = BluCyan)
                }
            },
            containerColor = BluDarkNavy
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BluBlack)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header & Actions Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "DATA LOGGER & SIGNAL MONITOR",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = BluCyan,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "${logs.size} recorded signal entries",
                        fontSize = 11.sp,
                        color = BluTextSecondary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Export CSV
                    IconButton(
                        onClick = {
                            val csv = viewModel.exportLogsToCsv()
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Blu-LinQ Telemetry Log Export")
                                putExtra(Intent.EXTRA_TEXT, csv)
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "Export Blu-LinQ Logs")
                            shareIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            context.startActivity(shareIntent)
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(BluSurfaceElevated)
                            .testTag("export_logs_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Export CSV",
                            tint = BluCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Copy to Clipboard
                    IconButton(
                        onClick = {
                            val csv = viewModel.exportLogsToCsv()
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Blu-LinQ Logs", csv)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Logs copied to clipboard as CSV", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(BluSurfaceElevated)
                            .testTag("copy_logs_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy CSV",
                            tint = BluTextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Clear
                    IconButton(
                        onClick = { showClearConfirm = true },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(BluSurfaceElevated)
                            .testTag("clear_logs_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear Logs",
                            tint = BluError,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Live Signal Monitor Canvas
        item {
            MinimalCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "REAL-TIME RSSI SIGNAL TELEMETRY",
                            fontSize = 11.sp,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = BluTextPrimary
                        )
                        Text(
                            text = "${linkStatus.averageLatencyMs}ms avg latency",
                            fontSize = 10.sp,
                            fontFamily = JetBrainsMonoFontFamily,
                            color = BluCyan
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    RealtimeSignalCanvas(history = signalHistory)
                }
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_logs_input"),
                placeholder = {
                    Text(
                        text = "Search logs by payload, status, time, node...",
                        fontSize = 12.sp,
                        color = BluTextTertiary
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = BluTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = BluTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BluCyan,
                    unfocusedBorderColor = BluBorder,
                    focusedTextColor = BluTextPrimary,
                    unfocusedTextColor = BluTextPrimary,
                    focusedContainerColor = BluDarkNavy,
                    unfocusedContainerColor = BluDarkNavy
                ),
                shape = RoundedCornerShape(8.dp)
            )
        }

        // Filter Chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("ALL", "Node A", "Node B", "ACK").forEach { filter ->
                    val isSelected = logFilter == filter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) BluCyan else BluSurfaceElevated)
                            .border(1.dp, if (isSelected) BluCyan else BluBorderSubtle, RoundedCornerShape(6.dp))
                            .clickable { viewModel.setLogFilter(filter) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = filter,
                            fontSize = 11.sp,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) BluBlack else BluTextSecondary
                        )
                    }
                }
            }
        }

        // Logs List
        if (logs.isEmpty()) {
            item {
                MinimalCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No telemetry records found",
                            fontSize = 13.sp,
                            fontFamily = JetBrainsMonoFontFamily,
                            color = BluTextSecondary
                        )
                        Text(
                            text = "Signals transmitted from Node A or received at Node B will automatically log here.",
                            fontSize = 11.sp,
                            color = BluTextTertiary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        } else {
            items(logs) { log ->
                MinimalCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = BluBorderSubtle
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
                                    text = "#${log.sequenceNumber}",
                                    fontSize = 10.sp,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    color = BluCyan,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${log.sourceNode} → ${log.targetNode}",
                                    fontSize = 11.sp,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (log.sourceNode.contains("Node A")) BluCyan else BluElectricBright
                                )
                            }
                            StatusBadge(status = log.status)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = log.payload,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = BluTextPrimary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // GPS & Time Details
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = log.latitude?.let { "📍 ${"%.5f".format(it)}, ${"%.5f".format(log.longitude ?: 0.0)}" } ?: "📍 No GPS Attached",
                                fontSize = 10.sp,
                                color = BluTextSecondary,
                                fontFamily = JetBrainsMonoFontFamily
                            )
                            Text(
                                text = "${log.formattedTime}  •  ${log.latencyMs}ms  •  ${log.rssi}dBm",
                                fontSize = 10.sp,
                                color = BluTextTertiary,
                                fontFamily = JetBrainsMonoFontFamily
                            )
                        }

                        if (log.rawHex.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "RAW: ${log.rawHex}",
                                fontSize = 9.sp,
                                color = BluTextTertiary,
                                fontFamily = JetBrainsMonoFontFamily
                            )
                        }
                    }
                }
            }
        }
    }
}
