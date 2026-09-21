package com.bugtrace.app.telemetry

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Process
import android.os.SystemClock
import android.view.OrientationEventListener
import com.bugtrace.app.model.TelemetryData
import com.bugtrace.app.model.TelemetryEvent
import com.bugtrace.app.model.TelemetrySnapshot
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class TelemetryCollector private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: TelemetryCollector? = null

        fun getInstance(context: Context): TelemetryCollector {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TelemetryCollector(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val _telemetryState = MutableStateFlow(TelemetryData())
    val telemetryState: StateFlow<TelemetryData> = _telemetryState.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var captureJob: Job? = null
    private val currentSessionHistory = mutableListOf<TelemetrySnapshot>()
    private val currentSessionEvents = mutableListOf<TelemetryEvent>()
    private val targetAppMonitor = TargetAppMonitor(context.applicationContext)
    var currentTargetPackage: String = TargetAppMonitor.TARGET_V1

    private var captureStartRealtimeMs: Long = 0L
    private var orientationEventListener: OrientationEventListener? = null
    @Volatile
    private var currentHardwareOrientation: String = "Portrait"

    init {
        initOrientationEventListener()
        updateTelemetry()
    }

    private fun initOrientationEventListener() {
        try {
            orientationEventListener = object : OrientationEventListener(context) {
                override fun onOrientationChanged(orientationDegrees: Int) {
                    if (orientationDegrees == ORIENTATION_UNKNOWN) return
                    val newOrientation = when (orientationDegrees) {
                        in 45..135 -> "Landscape"
                        in 225..315 -> "Landscape"
                        else -> "Portrait"
                    }
                    if (newOrientation != currentHardwareOrientation) {
                        currentHardwareOrientation = newOrientation
                        handleOrientationChanged(newOrientation)
                    }
                }
            }
            if (orientationEventListener?.canDetectOrientation() == true) {
                orientationEventListener?.enable()
            }
        } catch (e: Exception) {
            // Fallback to configuration
        }
    }

    private fun handleOrientationChanged(newOrientation: String) {
        val event = TelemetryEvent(
            timestampMs = System.currentTimeMillis(),
            eventType = "ORIENTATION_CHANGE",
            description = "Device orientation changed to ${newOrientation.uppercase()}"
        )
        synchronized(currentSessionHistory) {
            if (_telemetryState.value.isCapturing) {
                currentSessionEvents.add(event)
                currentSessionHistory.add(createSnapshot())
            }
        }
        _telemetryState.value = _telemetryState.value.copy(
            orientation = newOrientation,
            events = if (_telemetryState.value.isCapturing) _telemetryState.value.events + event else _telemetryState.value.events
        )
    }

    private var lastProcessCpuMs = Process.getElapsedCpuTime()
    private var lastWallMs = System.currentTimeMillis()

    private fun getCpuLoadPercent(): Double {
        return try {
            val nowMs = System.currentTimeMillis()
            val cpuMs = Process.getElapsedCpuTime()
            val wallDelta = nowMs - lastWallMs
            val cpuDelta = cpuMs - lastProcessCpuMs
            lastWallMs = nowMs
            lastProcessCpuMs = cpuMs

            val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
            if (wallDelta > 100) {
                val pct = (cpuDelta.toDouble() / (wallDelta.toDouble() * cores)) * 100.0
                pct.coerceIn(5.0, 95.0)
            } else {
                12.0
            }
        } catch (e: Exception) {
            12.0
        }
    }

    private fun createSnapshot(): TelemetrySnapshot {
        val (batteryLevel, isCharging) = getBatteryInfo()
        val networkState = getNetworkState()
        val cpuSummary = getCpuSummary()
        val cpuPct = getCpuLoadPercent()
        val currentOrientation = getOrientationFromContext()
        return TelemetrySnapshot(
            timestampMs = System.currentTimeMillis(),
            batteryPercent = batteryLevel,
            isCharging = isCharging,
            orientation = currentOrientation,
            networkState = networkState,
            cpuSummary = cpuSummary,
            cpuPercent = cpuPct
        )
    }

    fun recordAppCrashEvent(event: TelemetryEvent) {
        val targetPkg = event.details["target_package"]?.toString() ?: ""
        val nowMs = System.currentTimeMillis()

        synchronized(currentSessionHistory) {
            // Deduplicate APP_CRASH within 3000ms window
            val isDuplicate = currentSessionEvents.any { existingEvt ->
                existingEvt.eventType == "APP_CRASH" &&
                (nowMs - existingEvt.timestampMs) < 3000L &&
                existingEvt.details["target_package"] == targetPkg
            }

            if (isDuplicate) {
                return
            }

            currentSessionEvents.add(event)

            // Force an immediate crash-time snapshot
            val crashOrient = event.details["last_orientation"]?.toString()
                ?.lowercase()
                ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
                ?: currentHardwareOrientation

            val crashSnapshot = createSnapshot().copy(
                timestampMs = nowMs,
                orientation = crashOrient
            )
            currentSessionHistory.add(crashSnapshot)

            _telemetryState.value = _telemetryState.value.copy(
                orientation = crashSnapshot.orientation,
                batteryPercent = crashSnapshot.batteryPercent,
                isCharging = crashSnapshot.isCharging,
                networkState = crashSnapshot.networkState,
                telemetryHistory = currentSessionHistory.toList(),
                events = currentSessionEvents.toList()
            )
        }
    }

    fun startCapture(targetPackage: String = currentTargetPackage) {
        if (captureJob?.isActive == true) {
            stopCapture()
        }
        currentTargetPackage = targetPackage
        captureStartRealtimeMs = SystemClock.elapsedRealtime()

        synchronized(currentSessionHistory) {
            currentSessionHistory.clear()
            currentSessionEvents.clear()
            val initialSnap = createSnapshot()
            currentSessionHistory.add(initialSnap)
            val startEvent = TelemetryEvent(
                eventType = "CAPTURE_START",
                description = "User started real telemetry capture for $targetPackage"
            )
            currentSessionEvents.add(startEvent)

            _telemetryState.value = _telemetryState.value.copy(
                isCapturing = true,
                elapsedSeconds = 0,
                telemetryHistory = listOf(initialSnap),
                events = listOf(startEvent)
            )
        }

        // Start target app crash monitoring
        targetAppMonitor.startMonitoring(
            targetPackage = currentTargetPackage,
            getLastOrientation = { currentHardwareOrientation },
            onCrashDetected = { crashEvent ->
                recordAppCrashEvent(crashEvent)
            },
            scope = scope
        )

        captureJob = scope.launch {
            while (isActive) {
                delay(200) // 200ms tick loop for monotonic capture timer
                val elapsedSecs = maxOf(0, ((SystemClock.elapsedRealtime() - captureStartRealtimeMs) / 1000L).toInt())
                val nowMs = System.currentTimeMillis()

                val (updatedHistory, updatedEvents) = synchronized(currentSessionHistory) {
                    val lastSnapMs = currentSessionHistory.lastOrNull()?.timestampMs ?: 0L
                    if (nowMs - lastSnapMs >= 1000L) {
                        currentSessionHistory.add(createSnapshot())
                    }
                    Pair(currentSessionHistory.toList(), currentSessionEvents.toList())
                }

                val (batteryLevel, isCharging) = getBatteryInfo()
                val networkState = getNetworkState()
                val cpuSummary = getCpuSummary()
                val cpuPct = getCpuLoadPercent()

                _telemetryState.value = _telemetryState.value.copy(
                    elapsedSeconds = elapsedSecs,
                    batteryPercent = batteryLevel,
                    isCharging = isCharging,
                    networkState = networkState,
                    cpuSummary = cpuSummary,
                    cpuPercent = cpuPct,
                    orientation = currentHardwareOrientation,
                    timestampMs = nowMs,
                    telemetryHistory = updatedHistory,
                    events = updatedEvents
                )
            }
        }
    }

    fun stopCapture(): List<TelemetrySnapshot> {
        targetAppMonitor.stopMonitoring()
        captureJob?.cancel()
        captureJob = null

        val finalElapsedSecs = if (captureStartRealtimeMs > 0L) {
            maxOf(0, ((SystemClock.elapsedRealtime() - captureStartRealtimeMs) / 1000L).toInt())
        } else {
            _telemetryState.value.elapsedSeconds
        }

        val stopEvent = TelemetryEvent(
            eventType = "CAPTURE_STOP",
            description = "User stopped telemetry capture session"
        )
        val (finalHistory, finalEvents) = synchronized(currentSessionHistory) {
            currentSessionEvents.add(stopEvent)
            Pair(currentSessionHistory.toList(), currentSessionEvents.toList())
        }

        _telemetryState.value = _telemetryState.value.copy(
            isCapturing = false,
            elapsedSeconds = finalElapsedSecs,
            telemetryHistory = finalHistory,
            events = finalEvents
        )
        updateTelemetry()
        return finalHistory
    }

    fun toggleCapture() {
        if (_telemetryState.value.isCapturing) {
            stopCapture()
        } else {
            startCapture()
        }
    }

    fun updateOrientation(orientationCode: Int) {
        val orientationStr = when (orientationCode) {
            Configuration.ORIENTATION_LANDSCAPE -> "Landscape"
            Configuration.ORIENTATION_PORTRAIT -> "Portrait"
            else -> currentHardwareOrientation
        }
        if (orientationStr != currentHardwareOrientation) {
            currentHardwareOrientation = orientationStr
            handleOrientationChanged(orientationStr)
        }
    }

    fun updateTelemetry() {
        val (batteryLevel, isCharging) = getBatteryInfo()
        val networkState = getNetworkState()
        val cpuSummary = getCpuSummary()
        val cpuPct = getCpuLoadPercent()
        val currentOrientation = getOrientationFromContext()

        _telemetryState.value = _telemetryState.value.copy(
            batteryPercent = batteryLevel,
            isCharging = isCharging,
            networkState = networkState,
            cpuSummary = cpuSummary,
            cpuPercent = cpuPct,
            orientation = currentOrientation,
            timestampMs = System.currentTimeMillis()
        )
    }

    private fun getBatteryInfo(): Pair<Int, Boolean> {
        return try {
            val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus: Intent? = context.registerReceiver(null, intentFilter)

            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1

            val batteryPct = if (level >= 0 && scale > 0) {
                ((level / scale.toFloat()) * 100).toInt()
            } else {
                0
            }

            val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

            Pair(batteryPct, isCharging)
        } catch (e: Exception) {
            Pair(0, false)
        }
    }

    private fun getNetworkState(): String {
        return try {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                    ?: return "Unknown"

            val activeNetwork = connectivityManager.activeNetwork ?: return "Offline"
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
                ?: return "Offline"

            when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile Data"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                else -> "Online"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun getCpuSummary(): String {
        return try {
            val cores = Runtime.getRuntime().availableProcessors()
            val processCpuMs = Process.getElapsedCpuTime()
            val cpuSeconds = processCpuMs / 1000.0
            "${cores} Cores (App: ${String.format("%.1f", cpuSeconds)}s)"
        } catch (e: Exception) {
            val cores = Runtime.getRuntime().availableProcessors()
            "${cores} Cores (Restricted)"
        }
    }

    private fun getOrientationFromContext(): String {
        return currentHardwareOrientation
    }

    fun cleanUp() {
        stopCapture()
        try {
            orientationEventListener?.disable()
        } catch (e: Exception) {}
        scope.cancel()
    }
}
