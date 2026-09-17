package com.bugtrace.app.model

data class DemoScenario(
    val id: String,
    val title: String,
    val description: String,
    val batteryPercent: Int,
    val isCharging: Boolean = false,
    val orientation: String = "Portrait",
    val networkState: String = "Wi-Fi",
    val cpuSummary: String = "Standard Core Info",
    val cpuLoadPercent: Double = 45.0
) {
    val telemetryData: TelemetryData
        get() = TelemetryData(
            batteryPercent = batteryPercent,
            isCharging = isCharging,
            orientation = orientation,
            networkState = networkState,
            cpuSummary = cpuSummary,
            isCapturing = false,
            elapsedSeconds = 5,
            timestampMs = System.currentTimeMillis(),
            isSimulated = true
        )

    val conditionsSummary: List<Pair<String, String>>
        get() = buildList {
            add("Battery" to "$batteryPercent%${if (isCharging) " (Charging)" else ""}")
            add("Orientation" to orientation.uppercase())
            add("Network" to networkState.uppercase())
            if (cpuLoadPercent > 80.0) {
                add("CPU Load" to ">80% (HIGH)")
            } else {
                add("CPU Load" to "Standard (45%)")
            }
        }
}

object DemoScenarios {
    val scenarios = listOf(
        DemoScenario(
            id = "demo-low-battery",
            title = "LOW BATTERY",
            description = "Simulated low-battery condition",
            batteryPercent = 15,
            isCharging = false,
            orientation = "Portrait",
            networkState = "Wi-Fi",
            cpuLoadPercent = 45.0
        ),
        DemoScenario(
            id = "demo-landscape",
            title = "LANDSCAPE",
            description = "Simulated landscape condition",
            batteryPercent = 72,
            isCharging = false,
            orientation = "Landscape",
            networkState = "Wi-Fi",
            cpuLoadPercent = 45.0
        ),
        DemoScenario(
            id = "demo-weak-network",
            title = "WEAK NETWORK",
            description = "Simulated weak-network condition",
            batteryPercent = 54,
            isCharging = false,
            orientation = "Portrait",
            networkState = "Weak",
            cpuLoadPercent = 45.0
        ),
        DemoScenario(
            id = "demo-high-cpu",
            title = "HIGH CPU",
            description = "Simulated high-CPU condition",
            batteryPercent = 61,
            isCharging = false,
            orientation = "Portrait",
            networkState = "Wi-Fi",
            cpuSummary = "High Load (85%)",
            cpuLoadPercent = 85.0
        ),
        DemoScenario(
            id = "demo-low-bat-landscape",
            title = "LOW BATTERY + LANDSCAPE",
            description = "Simulated combined conditions",
            batteryPercent = 15,
            isCharging = false,
            orientation = "Landscape",
            networkState = "Wi-Fi",
            cpuLoadPercent = 45.0
        )
    )
}
