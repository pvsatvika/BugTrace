package com.bugtrace.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.bugtrace.app.model.CaptureSession
import com.bugtrace.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CaptureDetailsDialog(
    session: CaptureSession,
    onDismiss: () -> Unit,
    onViewReport: (String) -> Unit,
    onDeleteSession: (String) -> Unit,
    onRetryAnalysis: ((CaptureSession) -> Unit)? = null
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var isRetrying by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(DarkSurfaceVariant, DarkSurface)
                    )
                )
                .border(1.dp, DarkCardBorderGlow, RoundedCornerShape(14.dp))
                .padding(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = session.id,
                                style = MaterialTheme.typography.titleLarge,
                                fontFamily = FontFamily.Monospace,
                                color = AccentCyan
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            val statusColor = when (session.analysisStatus) {
                                "ANALYZED" -> StatusGreen
                                "PENDING" -> StatusAmber
                                else -> StatusRed
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(statusColor.copy(alpha = 0.15f))
                                    .border(1.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = session.analysisStatus,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = statusColor
                                )
                            }
                        }
                        Text(
                            text = "Recorded ${session.timeAgo}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Data Source Badge
                val isDemoData = session.isSimulated || session.dataSource.contains("SIMULATED", ignoreCase = true)
                val badgeBg = if (isDemoData) StatusAmber.copy(alpha = 0.15f) else PrimaryBlue.copy(alpha = 0.15f)
                val badgeBorder = if (isDemoData) StatusAmber.copy(alpha = 0.4f) else PrimaryBlue.copy(alpha = 0.4f)
                val badgeText = if (isDemoData) "DATA SOURCE: SIMULATED DEMO DATA" else "DATA SOURCE: REAL DEVICE TELEMETRY"
                val textColor = if (isDemoData) StatusAmber else PrimaryBlue

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeBg)
                        .border(1.dp, badgeBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        fontSize = 10.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Time metadata
                val dateFormat = SimpleDateFormat("MMM dd, yyyy • HH:mm:ss", Locale.US)
                val startStr = dateFormat.format(Date(session.startTimeMs))
                val endStr = dateFormat.format(Date(session.endTimeMs))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkBackground)
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("DURATION", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text(session.formattedDuration, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = TextPrimary)
                        }
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("START TIME", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text(startStr, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = TextSecondary, fontSize = 10.sp)
                        }
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("END TIME", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text(endStr, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = TextSecondary, fontSize = 10.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Device Context Section
                Text(
                    text = "// DEVICE CONTEXT",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkBackground)
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("BATTERY", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text("${session.batteryPercent}% ${if (session.isCharging) "(Charging)" else ""}", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = TextPrimary)
                        }
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("ORIENTATION", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text(session.orientation, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = TextPrimary)
                        }
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("NETWORK", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text(session.networkState, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = TextPrimary)
                        }
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("CPU METRICS", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text(session.cpuSummary, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = TextSecondary)
                        }
                    }
                }

                // Detected conditions section
                if (session.conditions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "// DETECTED CONDITIONS",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkBackground)
                            .border(1.dp, DarkCardBorder, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            session.conditions.forEach { (k, v) ->
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text(k.uppercase(), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    Text(v, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = StatusAmber, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Summary text if available
                if (!session.summary.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = session.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (session.reportId != null && session.analysisStatus == "ANALYZED") {
                        Button(
                            onClick = { onViewReport(session.reportId) },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryBlue,
                                contentColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Assessment, contentDescription = "View Report", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("VIEW BUG REPORT", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else if (onRetryAnalysis != null) {
                        Button(
                            onClick = {
                                isRetrying = true
                                onRetryAnalysis(session)
                            },
                            enabled = !isRetrying,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StatusAmber,
                                contentColor = DarkBackground
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isRetrying) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = DarkBackground, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("ANALYZING...", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(imageVector = Icons.Default.Assessment, contentDescription = "Retry Analysis", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("RETRY ANALYSIS", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.height(44.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusRed),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(StatusRed, StatusRed))),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Text(
                    text = "DELETE CAPTURE SESSION?",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete local session ${session.id}? This will remove local history only.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteSession(session.id)
                        onDismiss()
                    }
                ) {
                    Text("DELETE", color = StatusRed, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("CANCEL", color = TextMuted)
                }
            },
            containerColor = DarkSurface,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary
        )
    }
}
