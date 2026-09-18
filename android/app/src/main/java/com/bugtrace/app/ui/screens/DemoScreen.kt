package com.bugtrace.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bugtrace.app.model.CaptureSession
import com.bugtrace.app.model.DemoScenario
import com.bugtrace.app.model.DemoScenarios
import com.bugtrace.app.repository.CaptureHistoryRepository
import com.bugtrace.app.repository.ReportRepository
import com.bugtrace.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun DemoScreen(
    reportRepository: ReportRepository,
    historyRepository: CaptureHistoryRepository,
    onViewReport: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isLoading by reportRepository.isLoading.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var selectedDemoScenario by remember { mutableStateOf<DemoScenario?>(null) }
    var lastDemoSentTitle by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "DEMO SCENARIOS",
                        style = MaterialTheme.typography.headlineLarge,
                        letterSpacing = 1.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(StatusAmber.copy(alpha = 0.15f))
                            .border(1.dp, StatusAmber.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "DEMO MODE",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = StatusAmber,
                            fontSize = 9.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Test BugTrace with simulated device conditions.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(DarkSurfaceVariant)
                    .border(1.dp, StatusAmber.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Science,
                        contentDescription = "Simulated Data",
                        tint = StatusAmber,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
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

        Spacer(modifier = Modifier.height(10.dp))

        // Post-Demo Sent Banner
        if (lastDemoSentTitle != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurfaceVariant)
                    .border(1.dp, StatusAmber.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
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
                            contentDescription = "Demo Sent",
                            tint = StatusAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "✓ DEMO CAPTURE SENT TO BACKEND",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = StatusAmber,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Scenario '${lastDemoSentTitle}' ready on Developer Console",
                                style = MaterialTheme.typography.bodyMedium,
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    TextButton(onClick = { lastDemoSentTitle = null }) {
                        Text("DISMISS", color = TextMuted, fontSize = 10.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Demo Scenarios Scrollable List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            item {
                Text(
                    text = "// SELECT SCENARIO TO TEST",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )
            }

            items(DemoScenarios.scenarios) { scenario ->
                DemoScenarioCardItem(
                    scenario = scenario,
                    onClick = { selectedDemoScenario = scenario }
                )
            }
        }
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
                val sTitle = scenario.title
                selectedDemoScenario = null

                coroutineScope.launch {
                    val success = reportRepository.processCapturedTelemetry(sessionId, simulatedTelemetry)
                    if (success) {
                        lastDemoSentTitle = sTitle
                        onViewReport?.invoke(newSession.id)
                    }
                }
            }
        )
    }
}

@Composable
fun DemoScenarioCardItem(
    scenario: DemoScenario,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurfaceVariant)
            .border(1.dp, StatusAmber.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
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
                        fontSize = 14.sp
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
                            text = "SIMULATED DEMO DATA",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = StatusAmber,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = scenario.description,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 12.sp,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

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

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Run Demo",
                tint = StatusAmber,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun DemoScenarioPreviewDialog(
    scenario: DemoScenario,
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
                            text = "SIMULATED DEMO DATA",
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
                    text = "Running this scenario transmits simulated demo telemetry to the backend analyzer. View results on the Developer Console.",
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
