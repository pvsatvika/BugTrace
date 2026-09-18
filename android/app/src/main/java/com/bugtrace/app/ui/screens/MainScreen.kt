package com.bugtrace.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bugtrace.app.repository.CaptureHistoryRepository
import com.bugtrace.app.repository.ReportRepository
import com.bugtrace.app.telemetry.TelemetryCollector
import com.bugtrace.app.ui.theme.*

enum class NavigationTab(val title: String, val icon: ImageVector) {
    CAPTURE("Capture", Icons.Default.Sensors),
    DEMO("Demo", Icons.Default.Science)
}

@Composable
fun MainScreen(
    collector: TelemetryCollector,
    reportRepository: ReportRepository,
    historyRepository: CaptureHistoryRepository
) {
    var selectedTab by remember { mutableStateOf(NavigationTab.CAPTURE) }

    Scaffold(
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .border(1.dp, DarkCardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NavigationTab.entries.forEach { tab ->
                        val selected = selectedTab == tab
                        val iconColor by animateColorAsState(
                            targetValue = if (selected) AccentCyan else TextMuted,
                            label = "NavIconColor"
                        )
                        val textColor by animateColorAsState(
                            targetValue = if (selected) TextPrimary else TextMuted,
                            label = "NavTextColor"
                        )

                        TextButton(
                            onClick = { selectedTab = tab },
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                if (selected) {
                                    Box(
                                        modifier = Modifier
                                            .width(24.dp)
                                            .height(2.dp)
                                            .clip(RoundedCornerShape(1.dp))
                                            .background(AccentCyan)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                } else {
                                    Spacer(modifier = Modifier.height(6.dp))
                                }

                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title,
                                    tint = iconColor,
                                    modifier = Modifier.size(20.dp)
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = tab.title.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = textColor,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .background(DarkBackground)
        ) {
            when (selectedTab) {
                NavigationTab.CAPTURE -> CaptureScreen(
                    collector = collector,
                    reportRepository = reportRepository,
                    historyRepository = historyRepository
                )
                NavigationTab.DEMO -> DemoScreen(
                    reportRepository = reportRepository,
                    historyRepository = historyRepository,
                    onViewReport = { }
                )
            }
        }
    }
}
