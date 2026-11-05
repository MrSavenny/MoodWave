package com.example.moodwave.data

import com.example.moodwave.model.MoodEntry
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** Respaldo JSON versionado. Es legible: NO está cifrado. */
object MoodBackup {
    private const val FORMAT = "moodwave-backup"
    private const val VERSION = 1

    private const val MAX_BYTES = 15_000_000
    fun read(stream: java.io.InputStream): List<MoodEntry> {
        val bytes = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = stream.read(buffer)
            if (count < 0) break
            require(bytes.size() + count <= MAX_BYTES) { "El archivo supera el límite de 15 MB." }
            bytes.write(buffer, 0, count)
        }
        return decode(bytes.toString("UTF-8"))
    }

    fun export(entries: List<MoodEntry>): String {
        require(entries.size <= 30_000) { "Demasiados registros." }
        val rows = JSONArray()
        entries.forEach { mood ->
            rows.put(JSONObject().apply {
                put("date", mood.date)
                put("time", mood.time)
                put("moodLevel", mood.moodLevel)
                put("moodIntensity", mood.moodIntensity)
                put("energyLevel", mood.energyLevel)
                put("diaryText", mood.diaryText)
                put("activities", mood.activities)
            })
        }
        return JSONObject().apply {
            put("format", FORMAT)
            put("schemaVersion", VERSION)
            put("exportedAt", LocalDate.now().toString())
            put("entries", rows)
        }.toString(2).also { require(it.toByteArray().size <= MAX_BYTES) { "El respaldo supera 15 MB." } }
    }

    fun decode(content: String): List<MoodEntry> {
        require(content.length <= MAX_BYTES) { "El archivo supera el límite de 15 MB." }
        val root = JSONObject(content)
        require(root.getString("format") == FORMAT) { "No es un respaldo MoodWave." }
        require(root.getInt("schemaVersion") == VERSION) { "Versión de respaldo no compatible." }
        val rows = root.getJSONArray("entries")
        require(rows.length() <= 30_000) { "Demasiados registros." }
        return (0 until rows.length()).map { index ->
            val item = rows.getJSONObject(index)
            val date = item.getString("date")
            val level = item.getInt("moodLevel")
            val intensity = item.getInt("moodIntensity")
            val energy = item.getInt("energyLevel")
            require(runCatching { LocalDate.parse(date) }.isSuccess) { "Fecha inválida en fila ${index + 1}." }
            require(level in 1..20 && intensity in 1..5 && energy in 1..10) { "Escala inválida en fila ${index + 1}." }
            MoodEntry(
                id = 0, date = date,
                time = item.optString("time"),
                moodLevel = level, moodIntensity = intensity,
                energyLevel = energy,
                diaryText = item.optString("diaryText"),
                activities = item.optString("activities")
            )
        }
    }
}
