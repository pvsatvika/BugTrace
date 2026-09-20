package com.bugtrace.app.telemetry

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import android.util.Log
import com.bugtrace.app.model.TelemetryEvent
import kotlinx.coroutines.*

class TargetAppMonitor(private val context: Context) {

    companion object {
        private const val TAG = "TargetAppMonitor"
        var defaultTargetPackage = "com.example.shopdemo.v1"
    }

    private var monitorJob: Job? = null
    private var lastRecordedExitTimestamp: Long = 0L

    fun startMonitoring(
        targetPackage: String = defaultTargetPackage,
        getLastOrientation: () -> String,
        onCrashDetected: (TelemetryEvent) -> Unit,
        scope: CoroutineScope
    ) {
        stopMonitoring()

        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return

        // Set baseline timestamp so we only detect exit events happening DURING this capture
        lastRecordedExitTimestamp = System.currentTimeMillis() - 1000L

        monitorJob = scope.launch(Dispatchers.Default) {
            Log.d(TAG, "Started target app monitoring for package: $targetPackage")
            var wasTargetRunning = false

            while (isActive) {
                try {
                    var crashDetected = false

                    // Mechanism 1: Historical Process Exit Reasons (API 30+)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        try {
                            val exitInfos = activityManager.getHistoricalProcessExitReasons(targetPackage, 0, 5)
                            for (info in exitInfos) {
                                if (info.timestamp > lastRecordedExitTimestamp) {
                                    val isCrashReason = info.reason == ApplicationExitInfo.REASON_CRASH ||
                                            info.reason == ApplicationExitInfo.REASON_CRASH_NATIVE ||
                                            info.reason == ApplicationExitInfo.REASON_ANR ||
                                            info.reason == ApplicationExitInfo.REASON_OTHER

                                    if (isCrashReason) {
                                        lastRecordedExitTimestamp = info.timestamp
                                        val reasonStr = when (info.reason) {
                                            ApplicationExitInfo.REASON_CRASH -> "UNHANDLED_EXCEPTION"
                                            ApplicationExitInfo.REASON_CRASH_NATIVE -> "NATIVE_CRASH"
                                            ApplicationExitInfo.REASON_ANR -> "ANR"
                                            else -> "PROCESS_TERMINATED"
                                        }
                                        val desc = info.description ?: "Target application process terminated ($reasonStr)"
                                        val lastOrientation = getLastOrientation()

                                        val crashEvent = TelemetryEvent(
                                            timestampMs = info.timestamp,
                                            eventType = "APP_CRASH",
                                            description = "Application crash detected for $targetPackage ($reasonStr)",
                                            details = mapOf(
                                                "target_package" to targetPackage,
                                                "exit_reason" to reasonStr,
                                                "exit_description" to desc,
                                                "last_orientation" to lastOrientation.uppercase(),
                                                "pid" to info.pid.toString()
                                            )
                                        )
                                        Log.i(TAG, "Target app crash detected via ExitInfo: $crashEvent")
                                        onCrashDetected(crashEvent)
                                        crashDetected = true
                                        break
                                    }
                                }
                            }
                        } catch (e: SecurityException) {
                            Log.w(TAG, "getHistoricalProcessExitReasons security restriction for $targetPackage: ${e.message}")
                        } catch (e: Exception) {
                            Log.w(TAG, "Error checking process exit info: ${e.message}")
                        }
                    }

                    // Mechanism 2: Running Process Death Fallback
                    if (!crashDetected) {
                        val runningProcesses = activityManager.runningAppProcesses ?: emptyList()
                        val isTargetCurrentlyRunning = runningProcesses.any { it.processName == targetPackage }

                        if (wasTargetRunning && !isTargetCurrentlyRunning) {
                            // Target app was running and suddenly disappeared
                            val nowMs = System.currentTimeMillis()
                            if (nowMs > lastRecordedExitTimestamp + 1000L) {
                                lastRecordedExitTimestamp = nowMs
                                val lastOrientation = getLastOrientation()
                                val crashEvent = TelemetryEvent(
                                    timestampMs = nowMs,
                                    eventType = "APP_CRASH",
                                    description = "Application process $targetPackage disappeared unexpectedly",
                                    details = mapOf(
                                        "target_package" to targetPackage,
                                        "exit_reason" to "PROCESS_DISAPPEARED",
                                        "exit_description" to "Target app process terminated unexpectedly during session",
                                        "last_orientation" to lastOrientation.uppercase()
                                    )
                                )
                                Log.i(TAG, "Target app crash detected via Process Death: $crashEvent")
                                onCrashDetected(crashEvent)
                            }
                        }
                        wasTargetRunning = isTargetCurrentlyRunning
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Error in target app monitor loop: ${e.message}")
                }
                delay(500) // Poll every 500ms
            }
        }
    }

    fun stopMonitoring() {
        monitorJob?.cancel()
        monitorJob = null
    }
}
