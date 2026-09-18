package com.bugtrace.app.model

data class TelemetryEvent(
    val timestampMs: Long = System.currentTimeMillis(),
    val eventType: String = "EVENT",
    val description: String = "",
    val details: Map<String, String> = emptyMap()
)
