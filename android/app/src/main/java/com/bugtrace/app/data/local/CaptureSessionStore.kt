package com.bugtrace.app.data.local

import android.content.Context
import android.content.SharedPreferences
import com.bugtrace.app.model.CaptureSession
import org.json.JSONArray
import org.json.JSONObject

class CaptureSessionStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("bugtrace_session_history", Context.MODE_PRIVATE)

    private val sessionsKey = "capture_sessions_json"

    @Synchronized
    fun getAllSessions(): List<CaptureSession> {
        val jsonStr = prefs.getString(sessionsKey, null) ?: return emptyList()
        val list = mutableListOf<CaptureSession>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(parseSession(obj))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list.sortedByDescending { it.endTimeMs }
    }

    @Synchronized
    fun saveSession(session: CaptureSession) {
        val currentList = getAllSessions().toMutableList()
        val existingIdx = currentList.indexOfFirst { it.id == session.id }
        if (existingIdx >= 0) {
            currentList[existingIdx] = session
        } else {
            currentList.add(0, session)
        }
        saveAll(currentList)
    }

    @Synchronized
    fun deleteSession(sessionId: String) {
        val currentList = getAllSessions().filterNot { it.id == sessionId }
        saveAll(currentList)
    }

    @Synchronized
    fun updateSessionAnalysis(
        sessionId: String,
        status: String,
        reportId: String?,
        conditions: Map<String, String>,
        summary: String?
    ) {
        val currentList = getAllSessions().toMutableList()
        val idx = currentList.indexOfFirst { it.id == sessionId }
        if (idx >= 0) {
            val existing = currentList[idx]
            val updated = existing.copy(
                analysisStatus = status,
                reportId = reportId ?: existing.reportId,
                conditions = if (conditions.isNotEmpty()) conditions else existing.conditions,
                summary = summary ?: existing.summary
            )
            currentList[idx] = updated
            saveAll(currentList)
        }
    }

    private fun saveAll(list: List<CaptureSession>) {
        val jsonArray = JSONArray()
        list.forEach { session ->
            jsonArray.put(serializeSession(session))
        }
        prefs.edit().putString(sessionsKey, jsonArray.toString()).apply()
    }

    private fun serializeSession(session: CaptureSession): JSONObject {
        val obj = JSONObject()
        obj.put("id", session.id)
        obj.put("startTimeMs", session.startTimeMs)
        obj.put("endTimeMs", session.endTimeMs)
        obj.put("durationSeconds", session.durationSeconds)
        obj.put("batteryPercent", session.batteryPercent)
        obj.put("isCharging", session.isCharging)
        obj.put("orientation", session.orientation)
        obj.put("networkState", session.networkState)
        obj.put("cpuSummary", session.cpuSummary)
        obj.put("analysisStatus", session.analysisStatus)
        obj.put("reportId", session.reportId ?: JSONObject.NULL)
        obj.put("summary", session.summary ?: JSONObject.NULL)
        obj.put("isSimulated", session.isSimulated)
        obj.put("dataSource", session.dataSource)

        val condObj = JSONObject()
        session.conditions.forEach { (k, v) ->
            condObj.put(k, v)
        }
        obj.put("conditions", condObj)
        return obj
    }

    private fun parseSession(obj: JSONObject): CaptureSession {
        val condMap = mutableMapOf<String, String>()
        val condObj = obj.optJSONObject("conditions")
        condObj?.keys()?.forEach { k ->
            condMap[k] = condObj.optString(k, "")
        }

        val isSim = obj.optBoolean("isSimulated", false)
        val defaultSource = if (isSim) "SIMULATED DEMO DATA" else "REAL DEVICE TELEMETRY"
        val dSource = obj.optString("dataSource", defaultSource)

        return CaptureSession(
            id = obj.getString("id"),
            startTimeMs = obj.getLong("startTimeMs"),
            endTimeMs = obj.getLong("endTimeMs"),
            durationSeconds = obj.getInt("durationSeconds"),
            batteryPercent = obj.getInt("batteryPercent"),
            isCharging = obj.optBoolean("isCharging", false),
            orientation = obj.getString("orientation"),
            networkState = obj.getString("networkState"),
            cpuSummary = obj.getString("cpuSummary"),
            analysisStatus = obj.getString("analysisStatus"),
            reportId = if (obj.isNull("reportId")) null else obj.optString("reportId"),
            conditions = condMap,
            summary = if (obj.isNull("summary")) null else obj.optString("summary"),
            isSimulated = isSim,
            dataSource = dSource
        )
    }
}
