package com.bugtrace.app.repository

import com.bugtrace.app.data.local.BugReportStore
import com.bugtrace.app.model.BugReport
import com.bugtrace.app.model.CaptureSession
import com.bugtrace.app.model.TelemetryData
import com.bugtrace.app.network.BugTraceApiClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ReportRepository(
    private val apiClient: BugTraceApiClient = BugTraceApiClient(),
    private val historyRepository: CaptureHistoryRepository? = null,
    private val reportStore: BugReportStore? = null
) {

    private val _allReports = MutableStateFlow<List<BugReport>>(emptyList())
    val allReports: StateFlow<List<BugReport>> = _allReports.asStateFlow()

    private val _latestReport = MutableStateFlow<BugReport?>(null)
    val latestReport: StateFlow<BugReport?> = _latestReport.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isBackendConnected = MutableStateFlow(false)
    val isBackendConnected: StateFlow<Boolean> = _isBackendConnected.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    init {
        startBackendHealthCheck()
        reloadReports()
    }

    fun reloadReports() {
        val loaded = reportStore?.getAllReports() ?: emptyList()
        _allReports.value = loaded
        if (_latestReport.value == null && loaded.isNotEmpty()) {
            _latestReport.value = loaded.first()
        }
    }

    private fun startBackendHealthCheck() {
        scope.launch {
            while (isActive) {
                val connected = apiClient.pingBackend()
                _isBackendConnected.value = connected
                delay(5000)
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    suspend fun loadReportById(reportId: String): Boolean {
        _isLoading.value = true
        _errorMessage.value = null

        // 1. Try local cached report first
        val cachedReport = reportStore?.getReportById(reportId)
        if (cachedReport != null) {
            _latestReport.value = cachedReport
            _isLoading.value = false
            return true
        }

        // 2. If not cached locally, attempt to fetch from backend
        val fetchResult = apiClient.getReport(reportId)
        if (fetchResult.isSuccess) {
            val fetched = fetchResult.getOrThrow()
            reportStore?.saveReport(fetched)
            reloadReports()
            _latestReport.value = fetched
            _isLoading.value = false
            return true
        } else {
            _errorMessage.value = "Unable to fetch bug report $reportId from backend or local cache."
            _isLoading.value = false
            return false
        }
    }

    fun deleteReport(reportId: String) {
        reportStore?.deleteReport(reportId)
        reloadReports()
        if (_latestReport.value?.id == reportId || _latestReport.value?.reportId == reportId) {
            _latestReport.value = _allReports.value.firstOrNull()
        }
    }

    suspend fun processCapturedTelemetry(sessionId: String, telemetry: TelemetryData): Boolean {
        _isLoading.value = true
        _errorMessage.value = null

        val logResult = apiClient.submitLog(telemetry)
        if (logResult.isFailure) {
            _errorMessage.value = "CAPTURE SAVED LOCALLY: Backend unavailable. Analysis saved as PENDING and can be retried when connected."
            _isBackendConnected.value = false
            historyRepository?.updateSessionAnalysis(
                sessionId = sessionId,
                status = "PENDING",
                reportId = null,
                conditions = emptyMap(),
                summary = "Capture saved locally. Backend offline — tap to retry analysis."
            )
            _isLoading.value = false
            return false
        }

        _isBackendConnected.value = true
        val logId = logResult.getOrThrow()

        val analyzeResult = apiClient.analyzeLog(logId)
        if (analyzeResult.isFailure) {
            _errorMessage.value = "ANALYSIS FAILED: ${analyzeResult.exceptionOrNull()?.localizedMessage}"
            historyRepository?.updateSessionAnalysis(
                sessionId = sessionId,
                status = "FAILED",
                reportId = null,
                conditions = emptyMap(),
                summary = "Backend analysis failed."
            )
            _isLoading.value = false
            return false
        }

        val generatedReport = analyzeResult.getOrThrow()

        val fetchResult = apiClient.getReport(generatedReport.id)
        val finalReport = fetchResult.getOrDefault(generatedReport)

        reportStore?.saveReport(finalReport)
        reloadReports()
        _latestReport.value = finalReport

        historyRepository?.updateSessionAnalysis(
            sessionId = sessionId,
            status = "ANALYZED",
            reportId = finalReport.id,
            conditions = finalReport.conditions,
            summary = finalReport.summary
        )

        _isLoading.value = false
        return true
    }

    suspend fun retrySessionAnalysis(session: CaptureSession): Boolean {
        _isLoading.value = true
        _errorMessage.value = null

        val telemetry = TelemetryData(
            batteryPercent = session.batteryPercent,
            isCharging = session.isCharging,
            orientation = session.orientation,
            networkState = session.networkState,
            cpuSummary = session.cpuSummary,
            isSimulated = session.isSimulated
        )

        val logResult = apiClient.submitLog(telemetry)
        if (logResult.isFailure) {
            val exception = logResult.exceptionOrNull()
            _errorMessage.value = "BACKEND UNREACHABLE: ${exception?.localizedMessage ?: "Failed to reach backend."}\nEnsure laptop backend is running on local Wi-Fi."
            _isBackendConnected.value = false
            historyRepository?.updateSessionAnalysis(
                sessionId = session.id,
                status = "PENDING",
                reportId = null,
                conditions = session.conditions,
                summary = "Capture saved locally. Backend offline — tap to retry analysis."
            )
            _isLoading.value = false
            return false
        }

        _isBackendConnected.value = true
        val logId = logResult.getOrThrow()

        val analyzeResult = apiClient.analyzeLog(logId)
        if (analyzeResult.isFailure) {
            _errorMessage.value = "ANALYSIS FAILED: ${analyzeResult.exceptionOrNull()?.localizedMessage}"
            historyRepository?.updateSessionAnalysis(
                sessionId = session.id,
                status = "FAILED",
                reportId = null,
                conditions = session.conditions,
                summary = "Backend analysis failed."
            )
            _isLoading.value = false
            return false
        }

        val generatedReport = analyzeResult.getOrThrow()
        val fetchResult = apiClient.getReport(generatedReport.id)
        val finalReport = fetchResult.getOrDefault(generatedReport)

        reportStore?.saveReport(finalReport)
        reloadReports()
        _latestReport.value = finalReport

        historyRepository?.updateSessionAnalysis(
            sessionId = session.id,
            status = "ANALYZED",
            reportId = finalReport.id,
            conditions = finalReport.conditions,
            summary = finalReport.summary
        )

        _isLoading.value = false
        return true
    }

    fun cleanUp() {
        scope.cancel()
    }
}
