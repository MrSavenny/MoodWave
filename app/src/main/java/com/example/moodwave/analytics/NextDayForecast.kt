package com.example.moodwave.analytics

import com.example.moodwave.model.MoodEntry
import java.time.LocalDate
import java.util.Locale
import kotlin.math.abs

object NextDayForecast {
    const val MIN_PAIRS = 14
    private data class Day(val mood: Double, val energy: Double, val activities: Set<String>)
    private data class Pairing(val input: Day, val nextMood: Double)
    data class Result(val estimate: Double?, val pairs: Int, val evaluated: Int,
                      val modelMae: Double?, val baselineMae: Double?)

    private fun estimate(input: Day, history: List<Pairing>): Double {
        val neighbors = history.sortedBy { example ->
            val past = example.input
            val union = input.activities union past.activities
            val activityDistance = if (union.isEmpty()) 0.0 else
                1.0 - (input.activities intersect past.activities).size.toDouble() / union.size
            abs(input.mood - past.mood) / 19 + abs(input.energy - past.energy) / 9 + activityDistance
        }.take(5)
        return neighbors.map { it.nextMood }.average()
    }

    fun evaluate(rows: List<MoodEntry>, today: LocalDate): Result {
        val days = rows.mapNotNull { row ->
            val date = runCatching { LocalDate.parse(row.date) }.getOrNull()
            if (date == null || date > today) null else date to row
        }.groupBy({ it.first }, { it.second }).toSortedMap().mapValues { (_, records) ->
            Day(records.map { it.moodLevel }.average(), records.map { it.energyLevel }.average(),
                records.flatMap { it.activities.split(',') }.map { it.trim().lowercase(Locale.ROOT) }
                    .filter { it.isNotEmpty() }.toSet())
        }
        val history = mutableListOf<Pairing>()
        var modelError = 0.0
        var baselineError = 0.0
        var evaluated = 0
        for ((date, input) in days) {
            val target = days[date.plusDays(1)] ?: continue
            if (history.size >= MIN_PAIRS) {
                modelError += abs(estimate(input, history) - target.mood)
                baselineError += abs(input.mood - target.mood)
                evaluated++
            }
            history.add(Pairing(input, target.mood))
        }
        val prediction = days[today]?.takeIf { history.size >= MIN_PAIRS }?.let { estimate(it, history) }
        return Result(prediction, history.size, evaluated,
            if (evaluated > 0) modelError / evaluated else null,
            if (evaluated > 0) baselineError / evaluated else null)
    }
}
