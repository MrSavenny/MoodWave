package com.example.moodwave.analytics

import com.example.moodwave.model.MoodEntry
import java.time.LocalDate
import kotlin.random.Random

/** 90 registros reproducibles con efecto positivo 'plantado' en Ejercicio. */
object SyntheticDataGenerator {
    fun generate(today: LocalDate = LocalDate.now()): List<MoodEntry> {
        val random = Random(2027)
        return (0 until 90).map { offset ->
            val date = today.minusDays((89 - offset).toLong())
            val exercise = offset % 3 == 0
            val mood = if (exercise) (16 + random.nextInt(-1, 3)).coerceIn(1, 20)
                else (9 + random.nextInt(-2, 4)).coerceIn(1, 20)
            val energy = if (exercise) (8 + random.nextInt(-1, 2)).coerceIn(1, 10)
                else (5 + random.nextInt(-2, 2)).coerceIn(1, 10)
            val tags = buildList {
                if (exercise) add("Ejercicio")
                if (offset % 2 == 0) add("Estudio")
                if (offset % 5 == 0) add("Música")
                if (offset % 7 == 0) add("Lectura")
            }
            MoodEntry(
                id = 0,
                date = date.toString(),
                time = "08:30 PM",
                moodLevel = mood,
                moodIntensity = 3,
                energyLevel = energy,
                diaryText = "${MoodAnalytics.DEMO_PREFIX} Registro sintético $offset; no representa al usuario.",
                activities = tags.joinToString(",")
            )
        }
    }
}
