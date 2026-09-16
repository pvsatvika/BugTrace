package com.bugtrace.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.bugtrace.app.repository.ReportRepository
import com.bugtrace.app.telemetry.TelemetryCollector
import com.bugtrace.app.ui.screens.MainScreen
import com.bugtrace.app.ui.theme.BugTraceTheme
import com.bugtrace.app.ui.theme.DarkBackground

class MainActivity : ComponentActivity() {

    private lateinit var telemetryCollector: TelemetryCollector
    private lateinit var reportRepository: ReportRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        telemetryCollector = TelemetryCollector(applicationContext)
        reportRepository = ReportRepository()

        setContent {
            BugTraceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    MainScreen(
                        collector = telemetryCollector,
                        reportRepository = reportRepository
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::telemetryCollector.isInitialized) {
            telemetryCollector.cleanUp()
        }
    }
}
