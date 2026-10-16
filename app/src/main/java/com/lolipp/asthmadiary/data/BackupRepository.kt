package com.lolipp.asthmadiary.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class BackupRepository(private val context: Context) {
    private val dao = AppDatabase.get(context).peakFlowDao()

    suspend fun exportToJson(): String {
        val entries = dao.getAllOnce()
        val root = JSONObject()
        root.put("version", 1)
        val arr = JSONArray()
        entries.forEach { e ->
            val o = JSONObject()
            o.put("id", e.id)
            o.put("timestamp", e.timestamp)
            o.put("value", e.value)
            o.put("timing", e.timing.name)
            o.put("period", e.period.name)
            o.put("note", e.note)
            arr.put(o)
        }
        root.put("entries", arr)
        return root.toString(2)
    }

    /** Возвращает количество импортированных записей. */
    suspend fun importFromJson(json: String): Int {
        val root = JSONObject(json)
        val arr = root.optJSONArray("entries") ?: JSONArray()
        val entries = mutableListOf<PeakFlowEntry>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            entries.add(
                PeakFlowEntry(
                    id = 0,
                    timestamp = o.getLong("timestamp"),
                    value = o.getInt("value"),
                    timing = Timing.valueOf(o.optString("timing", "BEFORE")),
                    period = Period.valueOf(o.optString("period", "MORNING")),
                    note = o.optString("note", "")
                )
            )
        }
        if (entries.isNotEmpty()) dao.insertAll(entries)
        return entries.size
    }
}
