package com.bugtrace.app.network

import com.bugtrace.app.model.BugReport
import com.bugtrace.app.model.TelemetryData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class BugTraceApiClient(private val baseUrl: String = ApiConfig.BASE_URL) {

    private val connectTimeoutMs = 5000
    private val readTimeoutMs = 5000

    suspend fun pingBackend(): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 2500
                readTimeout = 2500
            }
            connection.responseCode == 200
        } catch (e: Exception) {
            false
        }
    }

    suspend fun submitLog(telemetry: TelemetryData): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/logs")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
                doOutput = true
            }

            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val isoTimestamp = dateFormat.format(Date(telemetry.timestampMs))

            val cpuVal = if (telemetry.cpuSummary.contains("85%")) 85.0 else if (telemetry.isCapturing) 85.0 else 45.0

            val jsonPayload = JSONObject().apply {
                put("battery", telemetry.batteryPercent)
                put("orientation", telemetry.orientation.lowercase())
                put("network", telemetry.networkState.lowercase())
                put("cpu", cpuVal)
                put("timestamp", isoTimestamp)
                put("is_simulated", telemetry.isSimulated)

                val historyArray = JSONArray()
                telemetry.telemetryHistory.forEach { snap ->
                    val snapObj = JSONObject()
                    val snapIso = dateFormat.format(Date(snap.timestampMs))
                    snapObj.put("timestamp", snapIso)
                    snapObj.put("battery", snap.batteryPercent)
                    snapObj.put("is_charging", snap.isCharging)
                    snapObj.put("orientation", snap.orientation.lowercase())
                    snapObj.put("network", snap.networkState.lowercase())
                    val snapCpu = if (snap.cpuSummary.contains("85%")) 85.0 else 45.0
                    snapObj.put("cpu", snapCpu)
                    historyArray.put(snapObj)
                }
                put("telemetry_history", historyArray)
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(jsonPayload.toString())
                writer.flush()
            }

            val statusCode = connection.responseCode
            if (statusCode in 200..299) {
                val responseStr = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                val jsonResponse = JSONObject(responseStr)
                val logId = jsonResponse.optString("log_id", "")
                if (logId.isNotEmpty()) {
                    Result.success(logId)
                } else {
                    Result.failure(Exception("Invalid log ID returned by server."))
                }
            } else {
                val errStr = try {
                    BufferedReader(InputStreamReader(connection.errorStream)).use { it.readText() }
                } catch (e: Exception) {
                    "HTTP $statusCode"
                }
                Result.failure(Exception("Server returned status $statusCode: $errStr"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun analyzeLog(logId: String): Result<BugReport> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/analyze/$logId")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Accept", "application/json")
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
                doOutput = false
            }

            val statusCode = connection.responseCode
            if (statusCode in 200..299) {
                val responseStr = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                val jsonResponse = JSONObject(responseStr)
                val bugReport = parseBugReport(jsonResponse)
                Result.success(bugReport)
            } else {
                Result.failure(Exception("Analyze log failed with HTTP $statusCode"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getReport(reportId: String): Result<BugReport> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/report/$reportId")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
            }

            val statusCode = connection.responseCode
            if (statusCode in 200..299) {
                val responseStr = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                val jsonResponse = JSONObject(responseStr)
                val bugReport = parseBugReport(jsonResponse)
                Result.success(bugReport)
            } else {
                Result.failure(Exception("Get report failed with HTTP $statusCode"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseBugReport(json: JSONObject): BugReport {
        val idVal = json.optString("id", json.optString("report_id", ""))
        val reportIdVal = json.optString("report_id", idVal)
        val logIdVal = json.optString("log_id", "")
        val titleVal = json.optString("title", "Telemetry Analysis Report")
        val statusVal = json.optString("status", "No Anomaly Detected")
        val confidenceVal = json.optInt("confidence", 50)
        val summaryVal = json.optString("summary", "")

        val observedConditionsList = mutableListOf<String>()
        val observedArray = json.optJSONArray("observed_conditions")
        if (observedArray != null) {
            for (i in 0 until observedArray.length()) {
                observedConditionsList.add(observedArray.optString(i, ""))
            }
        }

        val conditionsMap = mutableMapOf<String, String>()
        val conditionsObj = json.optJSONObject("conditions")
        conditionsObj?.keys()?.forEach { key ->
            conditionsMap[key] = conditionsObj.optString(key, "")
        }

        val reproStepsList = mutableListOf<String>()
        val reproArray = json.optJSONArray("reproduction_steps") ?: json.optJSONArray("steps_to_reproduce")
        if (reproArray != null) {
            for (i in 0 until reproArray.length()) {
                reproStepsList.add(reproArray.optString(i, ""))
            }
        }

        val stepsToReproduceList = mutableListOf<String>()
        val stepsArray = json.optJSONArray("steps_to_reproduce")
        if (stepsArray != null) {
            for (i in 0 until stepsArray.length()) {
                stepsToReproduceList.add(stepsArray.optString(i, ""))
            }
        }

        val deviceContextMap = mutableMapOf<String, String>()
        val contextObj = json.optJSONObject("device_context")
        contextObj?.keys()?.forEach { key ->
            deviceContextMap[key] = contextObj.optString(key, "")
        }

        val evidenceList = mutableListOf<String>()
        val evidenceArray = json.optJSONArray("evidence")
        if (evidenceArray != null) {
            for (i in 0 until evidenceArray.length()) {
                evidenceList.add(evidenceArray.optString(i, ""))
            }
        }

        val dataSourceVal = json.optString("data_source", "REAL DEVICE TELEMETRY")
        val isSimulatedVal = json.optBoolean("is_simulated", dataSourceVal.contains("SIMULATED", ignoreCase = true))

        val orientationHistList = mutableListOf<String>()
        val orientArray = json.optJSONArray("orientation_history")
        if (orientArray != null) {
            for (i in 0 until orientArray.length()) {
                orientationHistList.add(orientArray.optString(i, ""))
            }
        }

        return BugReport(
            id = idVal,
            reportId = reportIdVal,
            logId = logIdVal,
            title = titleVal,
            status = statusVal,
            confidence = confidenceVal,
            summary = summaryVal,
            dataSource = dataSourceVal,
            isSimulated = isSimulatedVal,
            observedConditions = observedConditionsList,
            conditions = conditionsMap,
            reproductionSteps = if (reproStepsList.isNotEmpty()) reproStepsList else stepsToReproduceList,
            stepsToReproduce = stepsToReproduceList,
            deviceContext = deviceContextMap,
            evidence = evidenceList,
            timestamp = json.optString("timestamp", ""),
            orientationHistory = orientationHistList,
            orientationChangeCount = json.optInt("orientation_change_count", maxOf(0, orientationHistList.size - 1)),
            snapshotCount = json.optInt("snapshot_count", maxOf(1, orientationHistList.size)),
            scoreTitle = json.optString("score_title", "CONDITION SCORE")
        )
    }
}
