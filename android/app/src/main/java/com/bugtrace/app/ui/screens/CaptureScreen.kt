package com.bugtrace.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bugtrace.app.model.StatusLevel
import com.bugtrace.app.model.TelemetryItem
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
    modifier: Modifier = Modifier
) {
    val telemetryState by collector.telemetryState.collectAsState()
    val isLoading by reportRepository.isLoading.collectAsState()
    val errorMessage by reportRepository.errorMessage.collectAsState()
    val configuration = LocalConfiguration.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(configuration.orientation) {
        collector.updateOrientation(configuration.orientation)
    }

    val batteryStatusLevel = if (telemetryState.isLowBattery) StatusLevel.ALERT else StatusLevel.NORMAL
    val batteryDetail = if (telemetryState.isCharging) {
        "Charging"
    } else if (telemetryState.isLowBattery) {
        "Low Battery (<20%)"
    } else {
        "Normal Level"
    }

    val telemetryItems = listOf(
        TelemetryItem(
            key = "battery",
            title = "Battery Level",
            value = "${telemetryState.batteryPercent}%",
            detail = batteryDetail,
            statusLevel = batteryStatusLevel,
            isPlaceholder = false
        ),
        TelemetryItem(
            key = "orientation",
            title = "Orientation",
            value = telemetryState.orientation,
            detail = if (telemetryState.orientation == "Landscape") "Landscape Condition" else "Portrait Normal",
            statusLevel = if (telemetryState.orientation == "Landscape") StatusLevel.WARNING else StatusLevel.NORMAL,
            isPlaceholder = false
        ),
        TelemetryItem(
            key = "network",
            title = "Network State",
            value = telemetryState.networkState,
            detail = if (telemetryState.networkState == "Offline") "No Connection" else "Active Connection",
            statusLevel = if (telemetryState.networkState == "Offline") StatusLevel.ALERT else StatusLevel.NORMAL,
            isPlaceholder = false
        ),
        TelemetryItem(
            key = "cpu",
            title = "CPU Metrics",
            value = telemetryState.cpuSummary,
            detail = "Safe Android API",
            statusLevel = StatusLevel.NORMAL,
            isPlaceholder = false
        )
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
        BugTraceHeader()

        Spacer(modifier = Modifier.height(6.dp))

        // Status bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(DarkSurfaceVariant)
                .border(1.dp, DarkBorder, RoundedCornerShape(6.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (telemetryState.isCapturing) StatusGreen else TextMuted)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (telemetryState.isCapturing) "LIVE CAPTURE ACTIVE" else "TELEMETRY MONITOR IDLE",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = if (telemetryState.isCapturing) StatusGreen else TextMuted,
                        fontSize = 11.sp
                    )
                }

                Text(
                    text = "HARDWARE SENSORS",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = TextMuted
                )
            }
        }

        // Error message banner (if backend offline or HTTP failure)
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(DarkSurface)
                    .border(1.dp, StatusRed, RoundedCornerShape(6.dp))
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

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "REAL DEVICE TELEMETRY",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(telemetryItems) { item ->
                TelemetryCard(item = item)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // START / STOP CAPTURE Button
        Button(
            onClick = {
                if (telemetryState.isCapturing) {
                    val currentTelemetry = telemetryState.copy()
                    collector.stopCapture()
                    coroutineScope.launch {
                        reportRepository.processCapturedTelemetry(currentTelemetry)
                    }
                } else {
                    collector.startCapture()
                }
            },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(8.dp)),
            colors = ButtonDefaults.buttonColors(
                containerColor = buttonColor,
                contentColor = TextPrimary
            ),
            shape = RoundedCornerShape(8.dp)
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
                        text = if (telemetryState.isCapturing) "STOP CAPTURE & ANALYZE" else "START CAPTURE",
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
