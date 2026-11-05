package com.example.moodwave
import com.example.moodwave.reminders.ReminderSchedule
import java.time.ZonedDateTime
import org.junit.Assert.*
import org.junit.Test
class ReminderScheduleTest {
    @Test fun nextReminderIsTodayOrTomorrow() {
        val now = ZonedDateTime.parse("2026-11-04T19:00:00-06:00[America/Mexico_City]")
        assertEquals(now.withHour(20),ReminderSchedule.next(20,0,now))
        assertEquals(now.plusDays(1).withHour(18),ReminderSchedule.next(18,0,now))
        assertEquals(now.plusDays(1),ReminderSchedule.next(19,0,now))
    }
    @Test fun daylightSavingGapStillProducesFutureLocalReminder() {
        val now = ZonedDateTime.parse("2026-03-08T00:00:00-05:00[America/New_York]")
        val next = ReminderSchedule.next(2,30,now)
        assertTrue(next.isAfter(now)); assertEquals(3,next.hour); assertEquals(30,next.minute)
        assertTrue(runCatching { ReminderSchedule.next(24,0,now) }.isFailure)
    }
}
