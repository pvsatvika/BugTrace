package com.bugtrace.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.bugtrace.app.repository.ReportRepository
import com.bugtrace.app.telemetry.TelemetryCollector
import com.bugtrace.app.ui.theme.*

enum class NavigationTab(val title: String, val icon: ImageVector) {
    CAPTURE("Capture", Icons.Default.Sensors),
    REPORTS("Reports", Icons.Default.Assessment)
}

@Composable
fun MainScreen(
    collector: TelemetryCollector,
    reportRepository: ReportRepository
) {
    var selectedTab by remember { mutableStateOf(NavigationTab.CAPTURE) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                contentColor = TextPrimary,
                tonalElevation = 0.dp,
                modifier = Modifier.border(1.dp, DarkBorder)
            ) {
                NavigationTab.entries.forEach { tab ->
                    val selected = selectedTab == tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryBlue,
                            selectedTextColor = PrimaryBlue,
                            indicatorColor = DarkSurfaceVariant,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        )
                    )
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
                NavigationTab.CAPTURE -> CaptureScreen(collector = collector, reportRepository = reportRepository)
                NavigationTab.REPORTS -> ReportsScreen(reportRepository = reportRepository)
            }
        }
    }
}
