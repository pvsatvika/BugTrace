package com.bugtrace.app.model

data class BugReport(
    val id: String = "",
    val reportId: String = "",
    val logId: String = "",
    val title: String = "",
    val status: String = "",
    val confidence: Int = 0,
    val summary: String = "",
    val dataSource: String = "REAL DEVICE TELEMETRY",
    val isSimulated: Boolean = false,
    val observedConditions: List<String> = emptyList(),
    val conditions: Map<String, String> = emptyMap(),
    val reproductionSteps: List<String> = emptyList(),
    val stepsToReproduce: List<String> = emptyList(),
    val deviceContext: Map<String, String> = emptyMap(),
    val evidence: List<String> = emptyList(),
    val timestamp: String = ""
)
