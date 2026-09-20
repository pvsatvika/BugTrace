package com.bugtrace.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bugtrace.app.model.CaptureSession
import com.bugtrace.app.model.StatusLevel
import com.bugtrace.app.model.TelemetryItem
import com.bugtrace.app.repository.CaptureHistoryRepository
import com.bugtrace.app.repository.ReportRepository
import com.bugtrace.app.telemetry.TelemetryCollector
import com.bugtrace.app.ui.components.BugTraceHeader
import com.bugtrace.app.ui.components.TelemetryCard
import com.bugtrace.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun CaptureScreen(
    collector: TelemetryCollector,
    reportRepository: ReportRepository,
    historyRepository: CaptureHistoryRepository,
    onViewReport: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val telemetryState by collector.telemetryState.collectAsState()
    val isLoading by reportRepository.isLoading.collectAsState()
    val errorMessage by reportRepository.errorMessage.collectAsState()
    val isBackendConnected by reportRepository.isBackendConnected.collectAsState()
    val recentSessions by historyRepository.sessions.collectAsState()
    val configuration = LocalConfiguration.current
    val coroutineScope = rememberCoroutineScope()

    var lastSentSessionId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(configuration.orientation) {
        collector.updateOrientation(configuration.orientation)
    }

    val batteryStatusLevel = if (telemetryState.isLowBattery) StatusLevel.ALERT else StatusLevel.NORMAL
    val batteryDetail = if (telemetryState.isCharging) {
        "CHARGING"
    } else if (telemetryState.isLowBattery) {
        "LOW (<20%)"
    } else {
        "NORMAL"
    }

    val telemetryItems = listOf(
        TelemetryItem(
            key = "battery",
            title = "Battery Sensor",
            value = "${telemetryState.batteryPercent}%",
            detail = batteryDetail,
            statusLevel = batteryStatusLevel,
            isPlaceholder = false
        ),
        TelemetryItem(
            key = "orientation",
            title = "Orientation",
            value = telemetryState.orientation,
            detail = if (telemetryState.orientation == "Landscape") "LANDSCAPE" else "PORTRAIT",
            statusLevel = if (telemetryState.orientation == "Landscape") StatusLevel.WARNING else StatusLevel.NORMAL,
            isPlaceholder = false
        ),
        TelemetryItem(
            key = "network",
            title = "Network State",
            value = telemetryState.networkState,
            detail = if (telemetryState.networkState == "Offline") "OFFLINE" else "CONNECTED",
            statusLevel = if (telemetryState.networkState == "Offline") StatusLevel.ALERT else StatusLevel.NORMAL,
            isPlaceholder = false
        ),
        TelemetryItem(
            key = "cpu",
            title = "CPU Hardware",
            value = telemetryState.cpuSummary,
            detail = "SAFE API",
            statusLevel = StatusLevel.NORMAL,
            isPlaceholder = false
        )
    )

    val infiniteTransition = rememberInfiniteTransition(label = "RecPulse")
    val recAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "RecPulseAlpha"
    )

    val buttonColor by animateColorAsState(
        targetValue = if (telemetryState.isCapturing) StatusRed else PrimaryBlue,
        label = "ButtonColor"
    )

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp, start = 16.dp, end = 16.dp)
    ) {
        // 1. Header Section
        item {
            BugTraceHeader(
                isCapturing = telemetryState.isCapturing,
                isBackendConnected = isBackendConnected
            )
        }

        // 2. Active Capture Recording Timer Bar
        item {
            AnimatedVisibility(
                visible = telemetryState.isCapturing,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(DarkSurfaceVariant, StatusRed.copy(alpha = 0.15f))
                            )
                        )
                        .border(1.dp, StatusRed.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(StatusRed)
                                    .alpha(recAlpha)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "REC",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                color = StatusRed,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Capturing device evidence...",
                                style = MaterialTheme.typography.bodyMedium,
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }

                        Text(
                            text = telemetryState.formattedElapsedTime,
                            style = MaterialTheme.typography.titleLarge,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = StatusRed,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }

        // 2b. Target App Crash Alert Banner
        val hasCrashEvent = telemetryState.events.any { it.eventType == "APP_CRASH" }
        if (hasCrashEvent) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(StatusRed.copy(alpha = 0.2f))
                        .border(1.dp, StatusRed, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Crash Detected",
                            tint = StatusRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "CRITICAL • TARGET APP CRASH DETECTED",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = StatusRed,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Target application process crash recorded in event timeline.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontSize = 11.sp,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }

        // 3. Post-Capture Sent Banner
        if (lastSentSessionId != null && !telemetryState.isCapturing && errorMessage == null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurfaceVariant)
                        .border(1.dp, StatusGreen.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Capture Sent",
                                tint = StatusGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "✓ CAPTURE SENT TO BACKEND",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = StatusGreen,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Analysis available on Developer Console",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        TextButton(onClick = { lastSentSessionId = null }) {
                            Text("DISMISS", color = TextMuted, fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        // 4. Error message banner
        if (errorMessage != null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurface)
                        .border(1.dp, StatusRed.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Backend Warning",
                            tint = StatusAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "OFFLINE • PENDING QUEUE",
                                style = MaterialTheme.typography.labelSmall,
                                color = StatusAmber,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = errorMessage ?: "Capture saved locally as offline fallback.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                        TextButton(onClick = { reportRepository.clearError() }) {
                            Text("DISMISS", color = TextMuted, fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        // 5. Real Device Telemetry Section Header
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                Text(
                    text = "// LIVE DEVICE TELEMETRY",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )

                Text(
                    text = "4 SENSORS",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontSize = 9.sp
                )
            }
        }

        // 6. Telemetry Cards
        items(telemetryItems) { item ->
            TelemetryCard(item = item)
        }

        // 7. Action Button: START CAPTURE / STOP CAPTURE
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = {
                    if (telemetryState.isCapturing) {
                        val capturedHistory = collector.stopCapture()
                        val finalTelemetry = collector.telemetryState.value.copy(telemetryHistory = capturedHistory)
                        val nowMs = System.currentTimeMillis()
                        val startMs = nowMs - (finalTelemetry.elapsedSeconds * 1000L)
                        val sessionId = historyRepository.generateSessionId()

                        val newSession = CaptureSession(
                            id = sessionId,
                            startTimeMs = startMs,
                            endTimeMs = nowMs,
                            durationSeconds = finalTelemetry.elapsedSeconds,
                            batteryPercent = finalTelemetry.batteryPercent,
                            isCharging = finalTelemetry.isCharging,
                            orientation = finalTelemetry.orientation,
                            networkState = finalTelemetry.networkState,
                            cpuSummary = finalTelemetry.cpuSummary,
                            analysisStatus = "PENDING",
                            telemetryHistory = capturedHistory
                        )

                        historyRepository.addSession(newSession)
                        lastSentSessionId = sessionId

                        coroutineScope.launch {
                            reportRepository.processCapturedTelemetry(sessionId, finalTelemetry)
                        }
                    } else {
                        lastSentSessionId = null
                        collector.startCapture()
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(10.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor,
                    contentColor = TextPrimary
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isLoading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = TextPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "SENDING TO BACKEND...",
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (telemetryState.isCapturing) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = if (telemetryState.isCapturing) "Stop Capture" else "Start Capture",
                            tint = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (telemetryState.isCapturing) "STOP CAPTURE" else "START CAPTURE",
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}
