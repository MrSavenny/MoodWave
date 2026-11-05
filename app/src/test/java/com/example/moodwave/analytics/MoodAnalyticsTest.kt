package com.example.moodwave.analytics

import com.example.moodwave.model.MoodEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import kotlin.math.sqrt

class MoodAnalyticsTest {
    private val today = LocalDate.of(2026, 11, 4)

    private fun entry(daysAgo: Long, mood: Int, energy: Int = 5, activity: String = ""): MoodEntry =
        MoodEntry(date = today.minusDays(daysAgo).toString(), moodLevel = mood,
            energyLevel = energy, activities = activity)

    @Test fun basicStatisticsMatchHandCalculation() {
        val records = listOf(entry(0, 1), entry(1, 2), entry(2, 2), entry(4, 3))
        val s = MoodAnalytics.stats(records, today)
        assertEquals(4, s.n)
        assertEquals(4, s.distinctDays)
        assertEquals(2.0, s.mean!!, 1e-9)
        assertEquals(2.0, s.median!!, 1e-9)
        assertEquals(listOf(2), s.modes)
        assertEquals(sqrt(2.0 / 3.0), s.sampleStdDev!!, 1e-9)
        assertEquals(3, s.currentStreak)
        assertEquals(3, s.longestStreak)
    }

    @Test fun missingDaysAreNotInventedAsZero() {
        val p = MoodAnalytics.trend(listOf(entry(0, 14), entry(2, 12)), 7, today)
        assertEquals(7, p.size)
        assertEquals(2, p.count { it.mean != null })
        assertEquals(null, p[5].mean)
        assertTrue(p.all { it.movingAverage == null }) // mínimo 3 días observados
    }

    @Test fun fewDaysNeverGenerateAnInsight() {
        val records = (0..2).map { entry(it.toLong(), 19, activity = "Ejercicio") }
        val report = MoodAnalytics.build(records, 30, today)
        assertTrue(report.insights.isEmpty())
        assertEquals(3, report.rangeStats.distinctDays)
    }

    @Test fun plantedExercisePatternIsDetected() {
        val demo = SyntheticDataGenerator.generate(today)
        assertEquals(90, demo.size)
        assertTrue(demo.all { MoodAnalytics.isDemo(it) })
        val results = MoodAnalytics.build(demo, 90, today)
        val exercise = results.activities.first { it.name == "Ejercicio" }
        assertEquals(30, exercise.n)
        assertEquals(60, exercise.otherN)
        assertTrue(exercise.adequate)
        assertTrue(exercise.contrastDifference!! > 3.0)
        assertTrue(results.insights.any { it.activityName == "Ejercicio" })
    }

    @Test fun generatedInsightsAvoidCausalAndDiagnosticClaims() {
        val insights = MoodAnalytics.build(SyntheticDataGenerator.generate(today), 90, today).insights
        assertTrue(insights.isNotEmpty())
        for (insight in insights) {
            val phrase = insight.text.lowercase()
            assertTrue(phrase.contains("se observa una relación"))
            assertTrue(phrase.contains("no demuestra causalidad"))
            assertFalse(phrase.contains("provoca"))
            assertFalse(phrase.contains("cura"))
            assertFalse(phrase.contains("diagnosticado"))
            assertFalse(phrase.contains("garantiza"))
        }
    }

    @Test fun evaluationComparedToAlwaysNoPatternBaseline() {
        val positive = SyntheticDataGenerator.generate(today)
        val negative = positive.map { it.copy(moodLevel = 21 - it.moodLevel) }
        val flat = (0 until 40).map { i ->
            entry(i.toLong(), 10 + i % 5, activity = if (i % 2 == 0) "Lectura" else "")
        }
        val sparse = (0 until 3).map { i -> entry(i.toLong(), 20, activity = "Ejercicio") }
        val examples = listOf(positive to true, negative to true, flat to false, sparse to false)
        val predictions = examples.map { (rows, _) -> MoodAnalytics.build(rows, 90, today).insights.isNotEmpty() }
        val truth = examples.map { it.second }
        val correct = predictions.zip(truth).count { (pred, expected) -> pred == expected }
        val baselineCorrect = truth.count { !it }  // baseline: nunca hay patrón
        assertEquals(4, correct)
        assertEquals(2, baselineCorrect)
    }

    @Test fun riskRuleIsLocalAndConservative() {
        assertTrue(MoodAnalytics.indicatesSelfHarm("Hoy quiero hacerme daño"))
        assertFalse(MoodAnalytics.indicatesSelfHarm("Hoy me siento cansado"))
    }
}
