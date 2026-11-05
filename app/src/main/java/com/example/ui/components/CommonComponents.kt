package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ConnectionState
import com.example.data.model.InterNodeLinkStatus
import com.example.data.model.NodeRole
import com.example.data.model.NodeStatus
import com.example.data.model.SignalPoint
import com.example.ui.theme.BluBlack
import com.example.ui.theme.BluBorder
import com.example.ui.theme.BluBorderSubtle
import com.example.ui.theme.BluCyan
import com.example.ui.theme.BluCyanGlow
import com.example.ui.theme.BluDarkNavy
import com.example.ui.theme.BluElectric
import com.example.ui.theme.BluElectricBright
import com.example.ui.theme.BluError
import com.example.ui.theme.BluSuccess
import com.example.ui.theme.BluSurface
import com.example.ui.theme.BluSurfaceElevated
import com.example.ui.theme.BluTextPrimary
import com.example.ui.theme.BluTextSecondary
import com.example.ui.theme.BluBorderGlow
import com.example.ui.theme.BluCardElevatedGradient
import com.example.ui.theme.BluCardGradient
import com.example.ui.theme.BluCyanNeon
import com.example.ui.theme.BluSuccessBright
import com.example.ui.theme.BluTextCyan
import com.example.ui.theme.BluTextTertiary
import com.example.ui.theme.BluWarning
import com.example.ui.theme.BluBlackAlt
import com.example.ui.theme.BluErrorBright
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily

@Composable
fun MinimalCard(
    modifier: Modifier = Modifier,
    borderColor: Color = BluBorderSubtle,
    backgroundColor: Color = BluDarkNavy,
    useGradient: Boolean = true,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .then(
                if (useGradient) {
                    Modifier.background(
                        Brush.verticalGradient(
                            listOf(
                                backgroundColor.copy(alpha = 0.95f),
                                BluBlackAlt.copy(alpha = 0.98f)
                            )
                        )
                    )
                } else {
                    Modifier.background(backgroundColor)
                }
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        borderColor.copy(alpha = 0.8f),
                        borderColor.copy(alpha = 0.25f)
                    )
                ),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(16.dp)
    ) {
        content()
    }
}

@Composable
fun SignalStrengthBar(
    rssi: Int,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true
) {
    // RSSI ranges from -100 (terrible) to -40 (excellent)
    val percentage = ((rssi + 100).toFloat() / 60f).coerceIn(0.1f, 1.0f)
    val bars = 5
    val activeBars = (percentage * bars).toInt().coerceIn(1, bars)

    val barColor = when {
        rssi > -65 -> BluCyanNeon
        rssi > -80 -> BluElectricBright
        rssi > -90 -> BluWarning
        else -> BluError
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.height(18.dp)
        ) {
            for (i in 1..bars) {
                val isActive = i <= activeBars
                val barHeight = (5 + (i * 2.6)).dp
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(barHeight)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (isActive) {
                                Brush.verticalGradient(listOf(barColor, barColor.copy(alpha = 0.7f)))
                            } else {
                                Brush.verticalGradient(listOf(BluBorderSubtle, BluBorderSubtle.copy(alpha = 0.3f)))
                            }
                        )
                )
            }
        }
        if (showLabel) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$rssi dBm",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 11.sp,
                color = barColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val upper = status.uppercase()
    val isOnline = upper in listOf("CONNECTED", "SENT", "DELIVERED", "ACKNOWLEDGED", "OK")
    val isPending = upper in listOf("CONNECTING", "PENDING", "SYNCING")
    val isError = upper in listOf("FAILED", "LOST", "ERROR", "DISCONNECTED")

    val (bgColor, textColor, icon) = when {
        isOnline -> Triple(
            BluSuccess.copy(alpha = 0.14f),
            BluSuccessBright,
            Icons.Default.CheckCircle
        )
        isPending -> Triple(
            BluWarning.copy(alpha = 0.14f),
            BluWarning,
            Icons.Default.Sync
        )
        upper == "RECEIVED" -> Triple(
            BluCyan.copy(alpha = 0.16f),
            BluCyan,
            Icons.Default.Sensors
        )
        isError -> Triple(
            BluError.copy(alpha = 0.14f),
            BluError,
            Icons.Default.Warning
        )
        else -> Triple(
            BluBorderSubtle,
            BluTextSecondary,
            Icons.Default.Info
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "badgePulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(
                0.8.dp,
                textColor.copy(alpha = if (isPending) pulseAlpha else 0.45f),
                RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 7.dp, vertical = 3.5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.5.dp)
    ) {
        if (isOnline) {
            // Glowing green dot
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(BluSuccessBright)
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = status,
                tint = textColor.copy(alpha = if (isPending) pulseAlpha else 1f),
                modifier = Modifier.size(10.dp)
            )
        }
        Text(
            text = when (upper) {
                "CONNECTED" -> "ONLINE"
                "DISCONNECTED" -> "OFFLINE"
                else -> upper
            },
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            fontFamily = JetBrainsMonoFontFamily,
            letterSpacing = 0.5.sp,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
fun StatusBadge(
    connectionState: ConnectionState,
    modifier: Modifier = Modifier
) {
    StatusBadge(status = connectionState.name, modifier = modifier)
}

@Composable
fun NodeStatusMiniCard(
    status: NodeStatus,
    modifier: Modifier = Modifier
) {
    val isNodeA = status.role == NodeRole.NODE_A
    val accentColor = if (isNodeA) BluCyanNeon else BluElectricBright
    val isConnected = status.connectionState == ConnectionState.CONNECTED

    MinimalCard(
        modifier = modifier,
        borderColor = if (isConnected) accentColor.copy(alpha = 0.6f) else BluBorderSubtle,
        backgroundColor = if (isConnected) BluDarkNavy else BluBlackAlt
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
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isConnected) accentColor else BluTextTertiary)
                    )
                    Text(
                        text = if (isNodeA) "NODE A [TX]" else "NODE B [RX]",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = accentColor,
                        fontFamily = JetBrainsMonoFontFamily,
                        letterSpacing = 0.5.sp,
                        maxLines = 1
                    )
                }
                StatusBadge(status = status.connectionState.name)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = status.deviceName,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = BluTextPrimary,
                fontFamily = SpaceGroteskFontFamily,
                maxLines = 1
            )
            Text(
                text = if (status.deviceAddress.isNotBlank()) status.deviceAddress else "BT Unpaired",
                fontSize = 11.sp,
                color = BluTextTertiary,
                fontFamily = JetBrainsMonoFontFamily
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SignalStrengthBar(rssi = status.rssi)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(BluSurface.copy(alpha = 0.6f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isNodeA) "TX ${status.packetsSent}" else "RX ${status.packetsReceived}",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = BluTextCyan,
                        fontFamily = JetBrainsMonoFontFamily
                    )
                }
            }
        }
    }
}

@Composable
fun InterNodeLinkVisualizer(
    linkStatus: InterNodeLinkStatus,
    nodeAConnected: Boolean,
    nodeBConnected: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseProgress"
    )

    val isLinkActive = nodeAConnected && nodeBConnected

    MinimalCard(
        modifier = modifier.fillMaxWidth(),
        borderColor = if (isLinkActive) BluCyanNeon.copy(alpha = 0.5f) else BluBorderSubtle,
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isLinkActive) BluSuccessBright else BluWarning)
                    )
                    Text(
                        text = "ESP32 INTER-NODE MESH LINK",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BluCyanNeon,
                        fontFamily = SpaceGroteskFontFamily,
                        letterSpacing = 0.5.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(BluSurfaceElevated)
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "CH ${linkStatus.channel} • 2.4GHz BLE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = BluTextSecondary,
                        fontFamily = JetBrainsMonoFontFamily
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Visual link pipeline canvas
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                val startX = 28.dp.toPx()
                val endX = size.width - 28.dp.toPx()
                val centerY = size.height / 2

                // Background track line
                drawLine(
                    color = BluBorderSubtle,
                    start = Offset(startX, centerY),
                    end = Offset(endX, centerY),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Active signal carrier line with glowing gradient
                if (isLinkActive) {
                    drawLine(
                        brush = Brush.horizontalGradient(
                            listOf(BluCyanNeon, BluElectricBright, BluCyanGlow)
                        ),
                        start = Offset(startX, centerY),
                        end = Offset(endX, centerY),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    // Animated travelling packet pulse
                    val packetX = startX + (endX - startX) * pulseProgress
                    drawCircle(
                        color = Color.White,
                        radius = 4.5.dp.toPx(),
                        center = Offset(packetX, centerY)
                    )
                    drawCircle(
                        color = BluCyanNeon.copy(alpha = 0.45f),
                        radius = 10.dp.toPx(),
                        center = Offset(packetX, centerY)
                    )
                }

                // Left Node A endpoint
                drawCircle(
                    color = if (nodeAConnected) BluCyanNeon else BluTextTertiary,
                    radius = 9.dp.toPx(),
                    center = Offset(startX, centerY)
                )
                drawCircle(
                    color = BluBlack,
                    radius = 4.5.dp.toPx(),
                    center = Offset(startX, centerY)
                )

                // Right Node B endpoint
                drawCircle(
                    color = if (nodeBConnected) BluElectricBright else BluTextTertiary,
                    radius = 9.dp.toPx(),
                    center = Offset(endX, centerY)
                )
                drawCircle(
                    color = BluBlack,
                    radius = 4.5.dp.toPx(),
                    center = Offset(endX, centerY)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Node A [TX]",
                    fontSize = 11.sp,
                    color = BluCyanNeon,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${linkStatus.averageLatencyMs} ms latency",
                        fontSize = 11.sp,
                        color = if (linkStatus.averageLatencyMs < 60) BluSuccessBright else BluWarning,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "•",
                        fontSize = 11.sp,
                        color = BluTextTertiary
                    )
                    Text(
                        text = "${linkStatus.linkQualityPct}% quality",
                        fontSize = 11.sp,
                        color = BluTextSecondary,
                        fontFamily = JetBrainsMonoFontFamily
                    )
                }
                Text(
                    text = "Node B [RX]",
                    fontSize = 11.sp,
                    color = BluElectricBright,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun RealtimeSignalCanvas(
    history: List<SignalPoint>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(BluBlackAlt)
            .border(1.dp, BluBorderSubtle, RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        if (history.isEmpty()) {
            Text(
                text = "Awaiting live signal telemetry...",
                color = BluTextTertiary,
                fontSize = 11.sp,
                fontFamily = JetBrainsMonoFontFamily,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                val width = size.width
                val height = size.height

                // Draw high-contrast grid lines
                val gridLines = 4
                for (i in 0..gridLines) {
                    val y = height * (i.toFloat() / gridLines)
                    drawLine(
                        color = BluBorderSubtle.copy(alpha = 0.5f),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1f
                    )
                }

                // Min RSSI -100, Max RSSI -40
                fun rssiToY(rssi: Int): Float {
                    val normalized = (rssi + 100).toFloat() / 60f
                    return height - (normalized.coerceIn(0f, 1f) * (height - 12.dp.toPx())) - 6.dp.toPx()
                }

                val stepX = width / (history.size - 1).coerceAtLeast(1)

                // Fill Path for Node A
                val fillPathA = Path()
                val strokePathA = Path()
                history.forEachIndexed { index, point ->
                    val x = index * stepX
                    val y = rssiToY(point.rssiNodeA)
                    if (index == 0) {
                        fillPathA.moveTo(x, height)
                        fillPathA.lineTo(x, y)
                        strokePathA.moveTo(x, y)
                    } else {
                        fillPathA.lineTo(x, y)
                        strokePathA.lineTo(x, y)
                    }
                }
                fillPathA.lineTo(width, height)
                fillPathA.close()

                drawPath(
                    path = fillPathA,
                    brush = Brush.verticalGradient(
                        listOf(BluCyanNeon.copy(alpha = 0.25f), Color.Transparent)
                    )
                )

                drawPath(
                    path = strokePathA,
                    color = BluCyanNeon,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )

                // Fill Path for Node B
                val fillPathB = Path()
                val strokePathB = Path()
                history.forEachIndexed { index, point ->
                    val x = index * stepX
                    val y = rssiToY(point.rssiNodeB)
                    if (index == 0) {
                        fillPathB.moveTo(x, height)
                        fillPathB.lineTo(x, y)
                        strokePathB.moveTo(x, y)
                    } else {
                        fillPathB.lineTo(x, y)
                        strokePathB.lineTo(x, y)
                    }
                }
                fillPathB.lineTo(width, height)
                fillPathB.close()

                drawPath(
                    path = fillPathB,
                    brush = Brush.verticalGradient(
                        listOf(BluElectricBright.copy(alpha = 0.20f), Color.Transparent)
                    )
                )

                drawPath(
                    path = strokePathB,
                    color = BluElectricBright,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )

                // Draw live pulsating endpoint on latest measurement
                val lastIndex = history.size - 1
                val lastX = lastIndex * stepX
                val lastYA = rssiToY(history.last().rssiNodeA)
                val lastYB = rssiToY(history.last().rssiNodeB)

                drawCircle(color = BluCyanNeon, radius = 4.dp.toPx(), center = Offset(lastX, lastYA))
                drawCircle(color = Color.White, radius = 2.dp.toPx(), center = Offset(lastX, lastYA))
                drawCircle(color = BluElectricBright, radius = 4.dp.toPx(), center = Offset(lastX, lastYB))
                drawCircle(color = Color.White, radius = 2.dp.toPx(), center = Offset(lastX, lastYB))
            }

            // Axis dBm Labels Overlay on Left
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 2.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text("-40", fontSize = 8.5.sp, color = BluTextTertiary, fontFamily = JetBrainsMonoFontFamily)
                Spacer(modifier = Modifier.height(28.dp))
                Text("-70", fontSize = 8.5.sp, color = BluTextTertiary, fontFamily = JetBrainsMonoFontFamily)
                Spacer(modifier = Modifier.height(28.dp))
                Text("-100", fontSize = 8.5.sp, color = BluTextTertiary, fontFamily = JetBrainsMonoFontFamily)
            }

            // Legend
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(6.dp))
                    .background(BluDarkNavy.copy(alpha = 0.9f))
                    .border(0.5.dp, BluBorderSubtle, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(7.dp).background(BluCyanNeon, CircleShape))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("Node A", fontSize = 9.5.sp, color = BluTextPrimary, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(7.dp).background(BluElectricBright, CircleShape))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("Node B", fontSize = 9.5.sp, color = BluTextPrimary, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun EmergencyAlertHeroCard(
    message: String,
    timestamp: String,
    sourceNode: String,
    rssi: Int,
    onAcknowledge: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "emergencyGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF3B0707),
                        Color(0xFF1E0303)
                    )
                )
            )
            .border(
                1.5.dp,
                BluError.copy(alpha = glowAlpha),
                RoundedCornerShape(16.dp)
            )
            .padding(18.dp)
    ) {
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
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(BluError)
                    )
                    Text(
                        text = "PRIORITY DISTRESS / EMERGENCY ALERT",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = BluErrorBright,
                        letterSpacing = 0.5.sp
                    )
                }
                Text(
                    text = timestamp,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    color = BluTextTertiary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = message,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontFamily = SpaceGroteskFontFamily,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "FROM: $sourceNode",
                        fontSize = 11.sp,
                        color = BluTextSecondary,
                        fontFamily = JetBrainsMonoFontFamily
                    )
                    Text(
                        text = "RSSI: $rssi dBm",
                        fontSize = 11.sp,
                        color = BluWarning,
                        fontFamily = JetBrainsMonoFontFamily
                    )
                }

                androidx.compose.material3.Button(
                    onClick = onAcknowledge,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = BluError
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 14.dp,
                        vertical = 8.dp
                    )
                ) {
                    Text(
                        text = "ACK & SILENCE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = JetBrainsMonoFontFamily,
                        color = Color.White
                    )
                }
            }
        }
    }
}

