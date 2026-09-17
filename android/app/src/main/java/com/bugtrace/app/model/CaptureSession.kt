package com.bugtrace.app.model

data class CaptureSession(
    val id: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val durationSeconds: Int,
    val batteryPercent: Int,
    val isCharging: Boolean,
    val orientation: String,
    val networkState: String,
    val cpuSummary: String,
    val analysisStatus: String, // "ANALYZED", "PENDING", "FAILED", "BACKEND_OFFLINE"
    val reportId: String? = null,
    val conditions: Map<String, String> = emptyMap(),
    val summary: String? = null,
    val isSimulated: Boolean = false,
    val dataSource: String = "REAL DEVICE TELEMETRY"
) {
    val formattedDuration: String
        get() {
            val mins = durationSeconds / 60
            val secs = durationSeconds % 60
            return String.format("%02d:%02d", mins, secs)
        }

    val timeAgo: String
        get() {
            val diffMs = System.currentTimeMillis() - endTimeMs
            val seconds = diffMs / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            val days = hours / 24

            return when {
                seconds < 30 -> "Just now"
                seconds < 60 -> "${seconds}s ago"
                minutes < 60 -> "${minutes}m ago"
                hours < 24 -> "${hours}h ago"
                else -> "${days}d ago"
            }
        }
}
