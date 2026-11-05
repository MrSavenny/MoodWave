package com.example.moodwave.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Entity(tableName = "mood_entries")
data class MoodEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // Fecha del registro (ej. 2026-08-24)
    val date: String = LocalDate.now().toString(),

    // Hora del registro (ej. 10:25 AM)
    val time: String = try {
        LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a", Locale.forLanguageTag("es")))
    } catch (_: Exception) { "" },

    // Tipo de Emoción (1 a 16)
    val moodLevel: Int,

    // Escalado/Intensidad de la emoción (1 al 5)
    val moodIntensity: Int = 3,

    // Nivel de energía (1 al 10)
    val energyLevel: Int,

    // Reflexión personal
    val diaryText: String = "",

    // Actividades separadas por coma
    val activities: String = ""
)
