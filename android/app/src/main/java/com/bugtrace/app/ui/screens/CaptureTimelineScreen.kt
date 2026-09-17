package com.bugtrace.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bugtrace.app.model.CaptureSession
import com.bugtrace.app.repository.CaptureHistoryRepository
import com.bugtrace.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun CaptureTimelineScreen(
    historyRepository: CaptureHistoryRepository,
    reportRepository: com.bugtrace.app.repository.ReportRepository? = null,
    onViewReport: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val sessions by historyRepository.sessions.collectAsState()
    var selectedSessionForDetails by remember { mutableStateOf<CaptureSession?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "TIMELINE",
                    style = MaterialTheme.typography.headlineLarge,
                    letterSpacing = 1.sp,
                    color = TextPrimary
                )
                Text(
                    text = "Chronological Local Session Records",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(DarkSurfaceVariant)
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${sessions.size} SESSIONS",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (sessions.isEmpty()) {
            // Empty State
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(DarkSurfaceVariant, DarkSurface)
                        )
                    )
                    .border(1.dp, DarkCardBorderGlow, RoundedCornerShape(12.dp))
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(DarkBackground)
                            .border(1.dp, DarkCardBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = "No Captures",
                            tint = PrimaryBlue,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "NO CAPTURES YET",
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Start a capture session on the Capture screen to record your first reproduction history entry.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
        } else {
            // Timeline List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                itemsIndexed(sessions) { index, session ->
                    TimelineItemRow(
                        session = session,
                        isLast = index == sessions.lastIndex,
                        onClick = { selectedSessionForDetails = session }
                    )
                }
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
                reportRepository?.let { repo ->
                    coroutineScope.launch {
                        val success = repo.retrySessionAnalysis(sessionToRetry)
                        if (success) {
                            selectedSessionForDetails = null
                            val updatedReport = repo.latestReport.value
                            if (updatedReport != null) {
                                onViewReport(updatedReport.id)
                            }
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun TimelineItemRow(
    session: CaptureSession,
    isLast: Boolean,
    onClick: () -> Unit
) {
    val statusColor = when (session.analysisStatus) {
        "ANALYZED" -> StatusGreen
        "PENDING" -> StatusAmber
        else -> StatusRed
    }

    val conditionText = when {
        session.conditions.isNotEmpty() -> session.conditions.entries.joinToString(" + ") { "${it.key}: ${it.value}" }
        session.batteryPercent < 20 -> "Low battery condition"
        session.orientation == "Landscape" -> "Landscape condition"
        session.networkState == "Offline" -> "Offline condition"
        else -> "Normal device conditions"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        // Vertical Timeline Line & Dot Column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(28.dp)
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(statusColor)
                    .border(2.dp, DarkBackground, CircleShape)
            )

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(2.dp)
                        .background(DarkCardBorder)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Card Content
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(DarkSurfaceVariant, DarkSurface)
                    )
                )
                .border(1.dp, DarkCardBorder, RoundedCornerShape(10.dp))
                .padding(14.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = session.reportId ?: session.id,
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = AccentCyan,
                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(statusColor.copy(alpha = 0.12f))
                                .border(1.dp, statusColor.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = session.analysisStatus,
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Details",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = conditionText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 12.sp,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${session.formattedDuration} • ${session.timeAgo}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted,
                        fontSize = 10.sp
                    )

                    Text(
                        text = "${session.batteryPercent}% | ${session.orientation} | ${session.networkState}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
