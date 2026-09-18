package com.bugtrace.app.model

data class TelemetryData(
    val batteryPercent: Int = 0,
    val isCharging: Boolean = false,
    val orientation: String = "Portrait",
    val networkState: String = "Unknown",
    val cpuSummary: String = "N/A",
    val cpuPercent: Double = 0.0,
    val isCapturing: Boolean = false,
    val elapsedSeconds: Int = 0,
    val timestampMs: Long = System.currentTimeMillis(),
    val isSimulated: Boolean = false,
    val telemetryHistory: List<TelemetrySnapshot> = emptyList(),
    val events: List<TelemetryEvent> = emptyList()
) {
    val isLowBattery: Boolean
        get() = batteryPercent in 1..19

    val formattedElapsedTime: String
        get() {
            val mins = elapsedSeconds / 60
            val secs = elapsedSeconds % 60
            return String.format("%02d:%02d", mins, secs)
        }
}
