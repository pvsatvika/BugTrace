package com.bugtrace.app.telemetry

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import com.bugtrace.app.model.TelemetryEvent
import kotlinx.coroutines.*

class TargetAppMonitor(private val context: Context) {

    companion object {
        private const val TAG = "TargetAppMonitor"
        const val ACTION_APP_CRASH = "com.bugtrace.app.ACTION_APP_CRASH"
        const val TARGET_V1 = "com.example.shopdemo.v1"
        const val TARGET_V2 = "com.example.shopdemo.v2"
    }

    private var monitorJob: Job? = null
    private var sessionStartTimeMs: Long = 0L
    private var lastRecordedExitTimestampMs: Long = 0L
    private var crashReceiver: BroadcastReceiver? = null

    fun startMonitoring(
        targetPackage: String = TARGET_V1,
        getLastOrientation: () -> String,
        onCrashDetected: (TelemetryEvent) -> Unit,
        scope: CoroutineScope
    ) {
        stopMonitoring()

        sessionStartTimeMs = System.currentTimeMillis() - 1000L
        lastRecordedExitTimestampMs = sessionStartTimeMs

        Log.i(TAG, "[MONITOR START] Target app monitoring active. Configured target: $targetPackage, SessionStartMs: $sessionStartTimeMs")

        // 1. Register Crash BroadcastReceiver (Empirical crash signal receiver)
        crashReceiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                if (intent?.action == ACTION_APP_CRASH) {
                    val pkg = intent.getStringExtra("target_package") ?: targetPackage
                    val reason = intent.getStringExtra("exit_reason") ?: "UNHANDLED_EXCEPTION"
                    val desc = intent.getStringExtra("exit_description") ?: "Target application uncaught exception"
                    val excClass = intent.getStringExtra("exception_class") ?: "java.lang.IllegalStateException"
                    val lastOrientation = getLastOrientation()

                    val nowMs = System.currentTimeMillis()
                    lastRecordedExitTimestampMs = nowMs

                    val crashEvent = TelemetryEvent(
                        timestampMs = nowMs,
                        eventType = "APP_CRASH",
                        description = "Application crash detected for $pkg ($reason)",
                        details = mapOf(
                            "target_package" to pkg,
                            "exit_reason" to reason,
                            "exit_description" to "$excClass: $desc",
                            "last_orientation" to lastOrientation.uppercase()
                        )
                    )
                    Log.e(TAG, "[EMPIRICAL CRASH RECEIVED VIA BROADCAST] $crashEvent")
                    onCrashDetected(crashEvent)
                }
            }
        }

        try {
            val filter = IntentFilter(ACTION_APP_CRASH)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(crashReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(crashReceiver, filter)
            }
            Log.i(TAG, "[MONITOR] BroadcastReceiver registered for action: $ACTION_APP_CRASH")
        } catch (e: Exception) {
            Log.e(TAG, "[MONITOR ERROR] Failed to register crash BroadcastReceiver: ${e.message}")
        }

        // 2. Launch Background Polling Job (ExitInfo & Process Death Fallback)
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val targetPackagesToMonitor = listOf(targetPackage, TARGET_V1, TARGET_V2).distinct()

        monitorJob = scope.launch(Dispatchers.Default) {
            var wasV1Running = false

            while (isActive) {
                try {
                    val nowMs = System.currentTimeMillis()

                    // System ExitInfo Check (API 30+)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && activityManager != null) {
                        for (pkg in targetPackagesToMonitor) {
                            try {
                                val exitInfos = activityManager.getHistoricalProcessExitReasons(pkg, 0, 3)
                                for (info in exitInfos) {
                                    if (info.timestamp > lastRecordedExitTimestampMs) {
                                        val isCrash = info.reason == ApplicationExitInfo.REASON_CRASH ||
                                                info.reason == ApplicationExitInfo.REASON_CRASH_NATIVE ||
                                                info.reason == ApplicationExitInfo.REASON_ANR ||
                                                info.reason == ApplicationExitInfo.REASON_INITIALIZATION_FAILURE ||
                                                (info.reason == ApplicationExitInfo.REASON_OTHER && info.status != 0)

                                        if (isCrash) {
                                            lastRecordedExitTimestampMs = info.timestamp
                                            val reasonLabel = when (info.reason) {
                                                ApplicationExitInfo.REASON_CRASH -> "UNHANDLED_EXCEPTION"
                                                ApplicationExitInfo.REASON_CRASH_NATIVE -> "NATIVE_CRASH"
                                                ApplicationExitInfo.REASON_ANR -> "ANR"
                                                ApplicationExitInfo.REASON_INITIALIZATION_FAILURE -> "INIT_FAILURE"
                                                else -> "PROCESS_CRASH_SIGNAL (${info.reason})"
                                            }
                                            val exitDesc = info.description ?: "Target app $pkg process exit ($reasonLabel)"
                                            val lastOrientation = getLastOrientation()

                                            val crashEvent = TelemetryEvent(
                                                timestampMs = info.timestamp,
                                                eventType = "APP_CRASH",
                                                description = "Application crash detected for $pkg ($reasonLabel)",
                                                details = mapOf(
                                                    "target_package" to pkg,
                                                    "exit_reason" to reasonLabel,
                                                    "exit_description" to exitDesc,
                                                    "last_orientation" to lastOrientation.uppercase(),
                                                    "pid" to info.pid.toString()
                                                )
                                            )
                                            Log.e(TAG, "[CRASH DETECTED VIA SYSTEM EXIT_INFO] $crashEvent")
                                            onCrashDetected(crashEvent)
                                            break
                                        }
                                    }
                                }
                            } catch (se: SecurityException) {
                                Log.d(TAG, "[SECURITY_NOTE] getHistoricalProcessExitReasons restricted for $pkg (DUMP permission signature check)")
                            } catch (e: Exception) {
                                Log.d(TAG, "[EXIT_INFO NOTE] $pkg: ${e.message}")
                            }
                        }
                    }

                    // Process Death Polling Check
                    if (activityManager != null) {
                        try {
                            val runningProcesses = activityManager.runningAppProcesses ?: emptyList()
                            val isV1Running = runningProcesses.any { it.processName == TARGET_V1 }

                            if (wasV1Running && !isV1Running && nowMs > lastRecordedExitTimestampMs + 1000L) {
                                lastRecordedExitTimestampMs = nowMs
                                val crashEvent = TelemetryEvent(
                                    timestampMs = nowMs,
                                    eventType = "APP_CRASH",
                                    description = "Application process $TARGET_V1 terminated unexpectedly",
                                    details = mapOf(
                                        "target_package" to TARGET_V1,
                                        "exit_reason" to "PROCESS_DISAPPEARED",
                                        "exit_description" to "Target application process terminated unexpectedly during session",
                                        "last_orientation" to getLastOrientation().uppercase()
                                    )
                                )
                                Log.e(TAG, "[CRASH DETECTED VIA PROCESS DISAPPEARANCE] $crashEvent")
                                onCrashDetected(crashEvent)
                            }
                            wasV1Running = isV1Running
                        } catch (e: Exception) {
                            // Process list poll warning
                        }
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "[MONITOR LOOP ERROR] ${e.message}")
                }
                delay(300)
            }
        }
    }

    fun stopMonitoring() {
        Log.i(TAG, "[MONITOR STOP] Stopping target app monitoring.")
        crashReceiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (e: Exception) {
                // Ignore if unregister fails
            }
            crashReceiver = null
        }
        monitorJob?.cancel()
        monitorJob = null
    }
}
