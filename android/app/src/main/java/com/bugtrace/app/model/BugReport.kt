package com.bugtrace.app.model

data class BugReport(
    val id: String = "",
    val status: String = "",
    val confidence: Int = 0,
    val summary: String = "",
    val conditions: Map<String, String> = emptyMap(),
    val stepsToReproduce: List<String> = emptyList(),
    val deviceContext: Map<String, String> = emptyMap(),
    val timestamp: String = ""
)
