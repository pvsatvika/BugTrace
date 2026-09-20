package com.bugtrace.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.bugtrace.app.data.local.BugReportStore
import com.bugtrace.app.data.local.CaptureSessionStore
import com.bugtrace.app.repository.CaptureHistoryRepository
import com.bugtrace.app.repository.ReportRepository
import com.bugtrace.app.telemetry.TelemetryCollector
import com.bugtrace.app.ui.screens.MainScreen
import com.bugtrace.app.ui.theme.BugTraceTheme
import com.bugtrace.app.ui.theme.DarkBackground

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var telemetryCollector: TelemetryCollector
    private lateinit var sessionStore: CaptureSessionStore
    private lateinit var historyRepository: CaptureHistoryRepository
    private lateinit var reportStore: BugReportStore
    private lateinit var reportRepository: ReportRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        telemetryCollector = TelemetryCollector.getInstance(applicationContext)
        sessionStore = CaptureSessionStore(applicationContext)
        historyRepository = CaptureHistoryRepository(sessionStore)
        reportStore = BugReportStore(applicationContext)
        reportRepository = ReportRepository(historyRepository = historyRepository, reportStore = reportStore)

        handleIntent(intent)

        setContent {
            BugTraceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    MainScreen(
                        collector = telemetryCollector,
                        reportRepository = reportRepository,
                        historyRepository = historyRepository
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: android.content.Intent?) {
        val action = intent?.action ?: return
        when (action) {
            "com.bugtrace.app.START_CAPTURE" -> {
                val targetPkg = intent.getStringExtra("target_package") ?: "com.example.shopdemo.v1"
                telemetryCollector.startCapture(targetPkg)
            }
            "com.bugtrace.app.STOP_CAPTURE" -> {
                val capturedHistory = telemetryCollector.stopCapture()
                val finalTelemetry = telemetryCollector.telemetryState.value.copy(telemetryHistory = capturedHistory)
                val nowMs = System.currentTimeMillis()
                val startMs = nowMs - (finalTelemetry.elapsedSeconds * 1000L)
                val sessionId = historyRepository.generateSessionId()

                val newSession = com.bugtrace.app.model.CaptureSession(
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
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                    reportRepository.processCapturedTelemetry(sessionId, finalTelemetry)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            if (::telemetryCollector.isInitialized) {
                telemetryCollector.cleanUp()
            }
            if (::reportRepository.isInitialized) {
                reportRepository.cleanUp()
            }
        }
    }
}
