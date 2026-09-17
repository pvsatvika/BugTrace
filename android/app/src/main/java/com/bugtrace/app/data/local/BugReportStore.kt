package com.bugtrace.app.data.local

import android.content.Context
import android.content.SharedPreferences
import com.bugtrace.app.model.BugReport
import org.json.JSONArray
import org.json.JSONObject

class BugReportStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("bugtrace_reports_history", Context.MODE_PRIVATE)

    private val reportsKey = "persistent_bug_reports_json"

    @Synchronized
    fun getAllReports(): List<BugReport> {
        val jsonStr = prefs.getString(reportsKey, null) ?: return emptyList()
        val list = mutableListOf<BugReport>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(parseReport(obj))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list.sortedByDescending { it.timestamp.ifBlank { it.id } }
    }

    @Synchronized
    fun getReportById(reportId: String): BugReport? {
        if (reportId.isBlank()) return null
        return getAllReports().firstOrNull { it.id == reportId || it.reportId == reportId }
    }

    @Synchronized
    fun saveReport(report: BugReport) {
        if (report.id.isBlank() && report.reportId.isBlank()) return
        val currentList = getAllReports().toMutableList()
        val targetId = if (report.id.isNotBlank()) report.id else report.reportId
        val existingIdx = currentList.indexOfFirst { it.id == targetId || it.reportId == targetId }

        val reportToSave = if (report.id.isBlank()) report.copy(id = targetId) else report

        if (existingIdx >= 0) {
            currentList[existingIdx] = reportToSave
        } else {
            currentList.add(0, reportToSave)
        }
        saveAll(currentList)
    }

    @Synchronized
    fun deleteReport(reportId: String) {
        val currentList = getAllReports().filterNot { it.id == reportId || it.reportId == reportId }
        saveAll(currentList)
    }

    private fun saveAll(list: List<BugReport>) {
        val jsonArray = JSONArray()
        list.forEach { report ->
            jsonArray.put(serializeReport(report))
        }
        prefs.edit().putString(reportsKey, jsonArray.toString()).apply()
    }

    private fun serializeReport(report: BugReport): JSONObject {
        val obj = JSONObject()
        val rId = if (report.id.isNotBlank()) report.id else report.reportId
        obj.put("id", rId)
        obj.put("report_id", report.reportId.ifBlank { rId })
        obj.put("log_id", report.logId)
        obj.put("title", report.title)
        obj.put("status", report.status)
        obj.put("confidence", report.confidence)
        obj.put("summary", report.summary)
        obj.put("data_source", report.dataSource)
        obj.put("is_simulated", report.isSimulated)
        obj.put("timestamp", report.timestamp)

        val obsArr = JSONArray()
        report.observedConditions.forEach { obsArr.put(it) }
        obj.put("observed_conditions", obsArr)

        val condObj = JSONObject()
        report.conditions.forEach { (k, v) -> condObj.put(k, v) }
        obj.put("conditions", condObj)

        val reproArr = JSONArray()
        report.reproductionSteps.forEach { reproArr.put(it) }
        obj.put("reproduction_steps", reproArr)

        val stepsArr = JSONArray()
        report.stepsToReproduce.forEach { stepsArr.put(it) }
        obj.put("steps_to_reproduce", stepsArr)

        val ctxObj = JSONObject()
        report.deviceContext.forEach { (k, v) -> ctxObj.put(k, v) }
        obj.put("device_context", ctxObj)

        val evArr = JSONArray()
        report.evidence.forEach { evArr.put(it) }
        obj.put("evidence", evArr)

        val orientArr = JSONArray()
        report.orientationHistory.forEach { orientArr.put(it) }
        obj.put("orientation_history", orientArr)
        obj.put("orientation_change_count", report.orientationChangeCount)
        obj.put("snapshot_count", report.snapshotCount)
        obj.put("score_title", report.scoreTitle)

        return obj
    }

    private fun parseReport(obj: JSONObject): BugReport {
        val idVal = obj.optString("id", obj.optString("report_id", ""))
        val reportIdVal = obj.optString("report_id", idVal)
        val logIdVal = obj.optString("log_id", "")
        val titleVal = obj.optString("title", "Telemetry Analysis Report")
        val statusVal = obj.optString("status", "ANALYZED")
        val confidenceVal = obj.optInt("confidence", 50)
        val summaryVal = obj.optString("summary", "")
        val dataSourceVal = obj.optString("data_source", "REAL DEVICE TELEMETRY")
        val isSimulatedVal = obj.optBoolean("is_simulated", dataSourceVal.contains("SIMULATED", ignoreCase = true))

        val observedConditionsList = mutableListOf<String>()
        val observedArray = obj.optJSONArray("observed_conditions")
        if (observedArray != null) {
            for (i in 0 until observedArray.length()) {
                observedConditionsList.add(observedArray.optString(i, ""))
            }
        }

        val conditionsMap = mutableMapOf<String, String>()
        val conditionsObj = obj.optJSONObject("conditions")
        conditionsObj?.keys()?.forEach { key ->
            conditionsMap[key] = conditionsObj.optString(key, "")
        }

        val reproStepsList = mutableListOf<String>()
        val reproArray = obj.optJSONArray("reproduction_steps") ?: obj.optJSONArray("steps_to_reproduce")
        if (reproArray != null) {
            for (i in 0 until reproArray.length()) {
                reproStepsList.add(reproArray.optString(i, ""))
            }
        }

        val stepsToReproduceList = mutableListOf<String>()
        val stepsArray = obj.optJSONArray("steps_to_reproduce")
        if (stepsArray != null) {
            for (i in 0 until stepsArray.length()) {
                stepsToReproduceList.add(stepsArray.optString(i, ""))
            }
        }

        val deviceContextMap = mutableMapOf<String, String>()
        val contextObj = obj.optJSONObject("device_context")
        contextObj?.keys()?.forEach { key ->
            deviceContextMap[key] = contextObj.optString(key, "")
        }

        val evidenceList = mutableListOf<String>()
        val evidenceArray = obj.optJSONArray("evidence")
        if (evidenceArray != null) {
            for (i in 0 until evidenceArray.length()) {
                evidenceList.add(evidenceArray.optString(i, ""))
            }
        }

        val orientationHistList = mutableListOf<String>()
        val orientArray = obj.optJSONArray("orientation_history")
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
            timestamp = obj.optString("timestamp", ""),
            orientationHistory = orientationHistList,
            orientationChangeCount = obj.optInt("orientation_change_count", maxOf(0, orientationHistList.size - 1)),
            snapshotCount = obj.optInt("snapshot_count", maxOf(1, orientationHistList.size)),
            scoreTitle = obj.optString("score_title", "CONDITION SCORE")
        )
    }
}
