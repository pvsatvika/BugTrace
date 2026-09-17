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
    var selectedDemoScenario by remember { mutableStateOf<com.bugtrace.app.model.DemoScenario?>(null) }

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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Premium Header
        BugTraceHeader(
            isCapturing = telemetryState.isCapturing,
            isBackendConnected = isBackendConnected
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Active Capture Recording Timer Bar
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

        // Error message banner
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
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

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
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

            items(telemetryItems) { item ->
                TelemetryCard(item = item)
            }

            // DEMO SCENARIOS Section (Visually distinct with SIMULATED DATA badge)
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "// DEMO SCENARIOS",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(StatusAmber.copy(alpha = 0.15f))
                            .border(1.dp, StatusAmber.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "SIMULATED DATA",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = StatusAmber,
                            fontSize = 9.sp
                        )
                    }
                }
            }

            items(com.bugtrace.app.model.DemoScenarios.scenarios) { scenario ->
                DemoScenarioCard(
                    scenario = scenario,
                    onClick = { selectedDemoScenario = scenario }
                )
            }

            // Compact RECENT CAPTURES section
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
                            text = "No recent sessions. Press START CAPTURE or select a DEMO SCENARIO above.",
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

        Spacer(modifier = Modifier.height(14.dp))

        // Action Button: START CAPTURE / STOP & ANALYZE
        Button(
            onClick = {
                if (telemetryState.isCapturing) {
                    val currentTelemetry = telemetryState.copy()
                    val nowMs = System.currentTimeMillis()
                    val startMs = nowMs - (currentTelemetry.elapsedSeconds * 1000L)
                    val sessionId = historyRepository.generateSessionId()

                    val newSession = CaptureSession(
                        id = sessionId,
                        startTimeMs = startMs,
                        endTimeMs = nowMs,
                        durationSeconds = currentTelemetry.elapsedSeconds,
                        batteryPercent = currentTelemetry.batteryPercent,
                        isCharging = currentTelemetry.isCharging,
                        orientation = currentTelemetry.orientation,
                        networkState = currentTelemetry.networkState,
                        cpuSummary = currentTelemetry.cpuSummary,
                        analysisStatus = "PENDING"
                    )

                    historyRepository.addSession(newSession)
                    collector.stopCapture()

                    coroutineScope.launch {
                        reportRepository.processCapturedTelemetry(sessionId, currentTelemetry)
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

        Spacer(modifier = Modifier.height(14.dp))
    }

    // Demo Scenario Preview Dialog
    selectedDemoScenario?.let { scenario ->
        DemoScenarioPreviewDialog(
            scenario = scenario,
            isLoading = isLoading,
            onDismiss = { selectedDemoScenario = null },
            onRunDemo = {
                val simulatedTelemetry = scenario.telemetryData
                val nowMs = System.currentTimeMillis()
                val sessionId = historyRepository.generateSessionId()

                val newSession = CaptureSession(
                    id = sessionId,
                    startTimeMs = nowMs - 5000,
                    endTimeMs = nowMs,
                    durationSeconds = 5,
                    batteryPercent = simulatedTelemetry.batteryPercent,
                    isCharging = simulatedTelemetry.isCharging,
                    orientation = simulatedTelemetry.orientation,
                    networkState = simulatedTelemetry.networkState,
                    cpuSummary = simulatedTelemetry.cpuSummary,
                    analysisStatus = "PENDING",
                    isSimulated = true,
                    dataSource = "SIMULATED DEMO DATA"
                )

                historyRepository.addSession(newSession)
                selectedDemoScenario = null

                coroutineScope.launch {
                    val success = reportRepository.processCapturedTelemetry(sessionId, simulatedTelemetry)
                    if (success) {
                        onViewReport(newSession.id)
                    }
                }
            }
        )
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
fun DemoScenarioCard(
    scenario: com.bugtrace.app.model.DemoScenario,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurfaceVariant)
            .border(1.dp, StatusAmber.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
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
                        text = scenario.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = StatusAmber,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(DarkBackground)
                            .border(1.dp, StatusAmber.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "DEMO",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = StatusAmber,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = scenario.description,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 11.sp,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    scenario.conditionsSummary.forEach { (label, value) ->
                        Text(
                            text = "[$label: $value]",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = TextMuted,
                            fontSize = 9.sp
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Run Demo",
                tint = StatusAmber,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun DemoScenarioPreviewDialog(
    scenario: com.bugtrace.app.model.DemoScenario,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onRunDemo: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        shape = RoundedCornerShape(12.dp),
        title = {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "SIMULATED SCENARIO",
                        style = MaterialTheme.typography.labelSmall,
                        color = StatusAmber,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(StatusAmber.copy(alpha = 0.15f))
                            .border(1.dp, StatusAmber.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "SIMULATED DATA",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = StatusAmber,
                            fontSize = 9.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = scenario.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = scenario.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkBackground)
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "// SIMULATED CONDITIONS",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )

                        scenario.conditionsSummary.forEach { (key, value) ->
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = key,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = value,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentCyan,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "Running this demo scenario submits simulated telemetry to the backend analyzer without modifying live hardware measurements.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onRunDemo,
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = StatusAmber,
                    contentColor = DarkBackground
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = DarkBackground,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "RUN DEMO",
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "CANCEL",
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }
        }
    )
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
