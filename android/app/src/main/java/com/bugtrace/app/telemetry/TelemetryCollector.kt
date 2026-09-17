package com.bugtrace.app.telemetry

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Process
import com.bugtrace.app.model.TelemetryData
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

    init {
        // Initial single read
        updateTelemetry()
    }

    fun startCapture() {
        if (captureJob?.isActive == true) return

        _telemetryState.value = _telemetryState.value.copy(
            isCapturing = true,
            elapsedSeconds = 0
        )

        captureJob = scope.launch {
            while (isActive) {
                updateTelemetry()
                delay(1000) // Refresh telemetry every 1 second
                _telemetryState.value = _telemetryState.value.copy(
                    elapsedSeconds = _telemetryState.value.elapsedSeconds + 1
                )
            }
        }
    }

    fun stopCapture() {
        captureJob?.cancel()
        captureJob = null
        _telemetryState.value = _telemetryState.value.copy(isCapturing = false)
        updateTelemetry()
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
            else -> "Portrait"
        }
        _telemetryState.value = _telemetryState.value.copy(orientation = orientationStr)
    }

    fun updateTelemetry() {
        val (batteryLevel, isCharging) = getBatteryInfo()
        val networkState = getNetworkState()
        val cpuSummary = getCpuSummary()
        val currentOrientation = getOrientationFromContext()

        _telemetryState.value = _telemetryState.value.copy(
            batteryPercent = batteryLevel,
            isCharging = isCharging,
            networkState = networkState,
            cpuSummary = cpuSummary,
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
        val sysOrientation = android.content.res.Resources.getSystem().configuration.orientation
        val ctxOrientation = context.resources.configuration.orientation
        val orientationCode = if (sysOrientation != Configuration.ORIENTATION_UNDEFINED) sysOrientation else ctxOrientation
        return when (orientationCode) {
            Configuration.ORIENTATION_LANDSCAPE -> "Landscape"
            Configuration.ORIENTATION_PORTRAIT -> "Portrait"
            else -> _telemetryState.value.orientation.ifEmpty { "Portrait" }
        }
    }

    fun cleanUp() {
        stopCapture()
        scope.cancel()
    }
}
