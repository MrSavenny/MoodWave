package com.example.moodwave.analytics

import com.example.moodwave.model.MoodEntry
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class NextDayForecastTest {
    private val start = LocalDate.of(2026, 1, 1)
    private fun row(i: Int, mood: Int) = MoodEntry(date = start.plusDays(i.toLong()).toString(),
        moodLevel = mood, energyLevel = 5, activities = "Lectura")

    @Test fun firstEvaluationUsesOnlyEarlierTargets() {
        val rows = (0..15).map { row(it, if (it == 15) 20 else 5) }
        val result = NextDayForecast.evaluate(rows, start.plusDays(15))
        assertEquals(1, result.evaluated)
        assertEquals(15.0, result.modelMae!!, 0.00001)
        assertEquals(result.baselineMae, result.modelMae)
        assertEquals(result, NextDayForecast.evaluate(rows.reversed() + row(16, 1), start.plusDays(15)))
    }

    @Test fun duplicatesAndGapsDoNotSupplyTrainingPairs() {
        val rows = (0..20).flatMap { listOf(row(it * 2, 5), row(it * 2, 15)) }
        val result = NextDayForecast.evaluate(rows, start.plusDays(40))
        assertEquals(0, result.pairs)
        assertNull(result.estimate)
    }

    @Test fun demoReportsComparableErrorsAndNeedsTodayForTomorrow() {
        val demo = SyntheticDataGenerator.generate(start)
        val result = NextDayForecast.evaluate(demo, start)
        assertEquals(75, result.evaluated)
        assertTrue(result.estimate!! in 1.0..20.0)
        assertNull(NextDayForecast.evaluate(demo, start.plusDays(1)).estimate)
        println("Demo MAE model=${result.modelMae} baseline=${result.baselineMae}")
    }
}
