package com.bugtrace.app.model

data class TelemetryData(
    val batteryPercent: Int = 0,
    val isCharging: Boolean = false,
    val orientation: String = "Portrait",
    val networkState: String = "Unknown",
    val cpuSummary: String = "N/A",
    val isCapturing: Boolean = false,
    val timestampMs: Long = System.currentTimeMillis()
) {
    val isLowBattery: Boolean
        get() = batteryPercent in 1..19
}
