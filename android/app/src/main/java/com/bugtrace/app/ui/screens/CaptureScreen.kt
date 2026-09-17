package com.bugtrace.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
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
    onViewReport: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val telemetryState by collector.telemetryState.collectAsState()
    val isLoading by reportRepository.isLoading.collectAsState()
    val errorMessage by reportRepository.errorMessage.collectAsState()
    val isBackendConnected by reportRepository.isBackendConnected.collectAsState()
    val recentSessions by historyRepository.sessions.collectAsState()
    val configuration = LocalConfiguration.current
    val coroutineScope = rememberCoroutineScope()

    var selectedSessionForDetails by remember { mutableStateOf<CaptureSession?>(null) }

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
                                text = "Collecting live device telemetry...",
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

        // 3. Error message banner
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
                            contentDescription = "Backend Error",
                            tint = StatusRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "BACKEND DISCONNECTED",
                                style = MaterialTheme.typography.labelSmall,
                                color = StatusRed,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = errorMessage ?: "",
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

        // 4. Real Device Telemetry Section Header
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                Text(
                    text = "// REAL DEVICE TELEMETRY",
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

        // 5. Telemetry Cards
        items(telemetryItems) { item ->
            TelemetryCard(item = item)
        }

        // 6. Action Button: START CAPTURE / STOP & ANALYZE (Primary Action immediately following telemetry)
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = {
                    if (telemetryState.isCapturing) {
                        val currentTelemetry = telemetryState.copy()
                        val capturedHistory = collector.stopCapture()
                        val finalTelemetry = currentTelemetry.copy(telemetryHistory = capturedHistory)
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

                        coroutineScope.launch {
                            reportRepository.processCapturedTelemetry(sessionId, finalTelemetry)
                        }
                    } else {
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
                            text = "ANALYZING LOG...",
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
                            text = if (telemetryState.isCapturing) "STOP & ANALYZE" else "START CAPTURE",
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }

        // 7. Recent Captures Section Header
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "// RECENT CAPTURES",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )

                if (recentSessions.isNotEmpty()) {
                    Text(
                        text = "${recentSessions.size} SESSIONS",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontSize = 9.sp
                    )
                }
            }
        }

        // 8. Recent Capture Session Cards
        if (recentSessions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurface)
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "No recent sessions. Press START CAPTURE above to begin recording.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }
        } else {
            items(recentSessions.take(2)) { session ->
                CompactSessionCard(
                    session = session,
                    onClick = { selectedSessionForDetails = session }
                )
            }
        }
    }

    // Capture Details Dialog
    selectedSessionForDetails?.let { session ->
        CaptureDetailsDialog(
            session = session,
            onDismiss = { selectedSessionForDetails = null },
            onViewReport = { reportId ->
                selectedSessionForDetails = null
                onViewReport(reportId)
            },
            onDeleteSession = { id ->
                historyRepository.deleteSession(id)
                selectedSessionForDetails = null
            },
            onRetryAnalysis = { sessionToRetry ->
                coroutineScope.launch {
                    val success = reportRepository.retrySessionAnalysis(sessionToRetry)
                    if (success) {
                        selectedSessionForDetails = null
                        val updatedReport = reportRepository.latestReport.value
                        if (updatedReport != null) {
                            onViewReport(updatedReport.id)
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun CompactSessionCard(
    session: CaptureSession,
    onClick: () -> Unit
) {
    val statusColor = when (session.analysisStatus) {
        "ANALYZED" -> StatusGreen
        "PENDING" -> StatusAmber
        else -> StatusRed
    }

    val headlineText = when {
        session.conditions.isNotEmpty() -> session.conditions.entries.joinToString(" + ") { "${it.key}: ${it.value}" }
        session.batteryPercent < 20 -> "Low battery condition"
        session.orientation == "Landscape" -> "Landscape condition"
        session.networkState == "Offline" -> "Offline condition"
        else -> "Normal device conditions"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurface)
            .border(1.dp, DarkCardBorder, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = session.reportId ?: session.id,
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = AccentCyan,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = headlineText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 11.sp,
                    color = TextPrimary
                )

                Text(
                    text = "${session.formattedDuration} • ${session.timeAgo}",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Details",
                tint = TextMuted,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
