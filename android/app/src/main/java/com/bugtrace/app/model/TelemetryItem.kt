package com.bugtrace.app.model

data class TelemetryItem(
    val key: String,
    val title: String,
    val value: String,
    val detail: String,
    val statusLevel: StatusLevel = StatusLevel.NORMAL,
    val isPlaceholder: Boolean = true
)

enum class StatusLevel {
    NORMAL,
    WARNING,
    ALERT
}
