package com.bugtrace.app.repository

import com.bugtrace.app.model.BugReport
import com.bugtrace.app.model.TelemetryData
import com.bugtrace.app.network.BugTraceApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ReportRepository(private val apiClient: BugTraceApiClient = BugTraceApiClient()) {

    private val _latestReport = MutableStateFlow<BugReport?>(null)
    val latestReport: StateFlow<BugReport?> = _latestReport.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun clearError() {
        _errorMessage.value = null
    }

    suspend fun processCapturedTelemetry(telemetry: TelemetryData): Boolean {
        _isLoading.value = true
        _errorMessage.value = null

        // 1. Submit telemetry log to POST /logs
        val logResult = apiClient.submitLog(telemetry)
        if (logResult.isFailure) {
            val exception = logResult.exceptionOrNull()
            _errorMessage.value = "BACKEND OFFLINE: ${exception?.localizedMessage ?: "Failed to reach backend."}\nMake sure laptop backend is running on local Wi-Fi."
            _isLoading.value = false
            return false
        }

        val logId = logResult.getOrThrow()

        // 2. Trigger analysis on POST /analyze/{log_id}
        val analyzeResult = apiClient.analyzeLog(logId)
        if (analyzeResult.isFailure) {
            _errorMessage.value = "ANALYSIS FAILED: ${analyzeResult.exceptionOrNull()?.localizedMessage}"
            _isLoading.value = false
            return false
        }

        val generatedReport = analyzeResult.getOrThrow()

        // 3. Verify retrieval via GET /report/{report_id}
        val fetchResult = apiClient.getReport(generatedReport.id)
        val finalReport = fetchResult.getOrDefault(generatedReport)

        _latestReport.value = finalReport
        _isLoading.value = false
        return true
    }
}
