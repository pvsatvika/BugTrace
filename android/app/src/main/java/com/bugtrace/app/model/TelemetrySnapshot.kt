package com.bugtrace.app.model

data class TelemetrySnapshot(
    val timestampMs: Long = System.currentTimeMillis(),
    val batteryPercent: Int = 0,
    val isCharging: Boolean = false,
    val orientation: String = "Portrait",
    val networkState: String = "Unknown",
    val cpuSummary: String = "N/A"
)
