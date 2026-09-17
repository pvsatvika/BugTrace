package com.bugtrace.app.repository

import com.bugtrace.app.data.local.CaptureSessionStore
import com.bugtrace.app.model.CaptureSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CaptureHistoryRepository(private val sessionStore: CaptureSessionStore) {

    private val _sessions = MutableStateFlow<List<CaptureSession>>(emptyList())
    val sessions: StateFlow<List<CaptureSession>> = _sessions.asStateFlow()

    private var nextSessionCounter = 1

    init {
        reloadSessions()
    }

    fun reloadSessions() {
        val loaded = sessionStore.getAllSessions()
        _sessions.value = loaded
        if (loaded.isNotEmpty()) {
            val maxNum = loaded.mapNotNull { session ->
                session.id.removePrefix("CAP-").toIntOrNull()
            }.maxOrNull() ?: 0
            nextSessionCounter = maxNum + 1
        }
    }

    fun generateSessionId(): String {
        val id = String.format("CAP-%03d", nextSessionCounter)
        nextSessionCounter++
        return id
    }

    fun addSession(session: CaptureSession) {
        sessionStore.saveSession(session)
        reloadSessions()
    }

    fun updateSessionAnalysis(
        sessionId: String,
        status: String,
        reportId: String?,
        conditions: Map<String, String>,
        summary: String?
    ) {
        sessionStore.updateSessionAnalysis(sessionId, status, reportId, conditions, summary)
        reloadSessions()
    }

    fun deleteSession(sessionId: String) {
        sessionStore.deleteSession(sessionId)
        reloadSessions()
    }
}
