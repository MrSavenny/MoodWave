package com.example.moodwave.analytics

import com.example.moodwave.data.missingBackupRows
import com.example.moodwave.model.MoodEntry
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class Integration2Test {
    private val row = MoodEntry(date = "2024-02-29", time = "10:00", moodLevel = 12, energyLevel = 6)

    @Test fun leapMonthKeepsBothBoundariesAndCountsDistinctDays() {
        val end = LocalDate.parse(row.date)
        val entries = listOf(row, row, row.copy(date = "2024-02-01"), row.copy(date = "2024-01-31"), row.copy(date = "2024-03-01"))
        val month = MoodAnalytics.inPeriod(entries, end.withDayOfMonth(1), end)
        assertEquals(3, month.size)
        assertEquals(2, MoodAnalytics.stats(month, end).distinctDays)
    }

    @Test fun movingAverageUsesPrecedingDaysAcrossDisplayBoundary() {
        val end = LocalDate.parse(row.date)
        val entries = listOf(6L, 7L, 8L).map { row.copy(date = end.minusDays(it).toString()) }
        assertEquals(12.0, MoodAnalytics.trend(entries, 7, end).first().movingAverage!!, 0.0)
    }

    @Test fun restoringTwicePreservesMultiplicityAndUnrelatedEntries() {
        val existing = listOf(row.copy(id = 13), row.copy(id = 4, activities = "Lectura"))
        val backup = listOf(row, row, row.copy(diaryText = "Otra entrada"))
        val additions = missingBackupRows(backup, existing)
        assertEquals(listOf(row, backup.last()), additions)
        assertEquals(emptyList<MoodEntry>(), missingBackupRows(backup, existing + additions))
    }
}
