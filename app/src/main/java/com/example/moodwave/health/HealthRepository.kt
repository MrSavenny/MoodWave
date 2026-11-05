package com.example.moodwave.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.LocalDate
import java.time.ZoneId

data class HealthDay(val date: LocalDate, val steps: Long?, val sleepMinutes: Long?)
data class HealthSummary(val permissions: Set<String>, val days: List<HealthDay>)

object HealthRepository {
    val stepsPermission = HealthPermission.getReadPermission(StepsRecord::class)
    val sleepPermission = HealthPermission.getReadPermission(SleepSessionRecord::class)
    val permissions = setOf(stepsPermission, sleepPermission)
    suspend fun readWeek(context: Context): HealthSummary {
        val client = HealthConnectClient.getOrCreate(context)
        val granted = client.permissionController.getGrantedPermissions()
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val days = (6L downTo 0L).map { offset ->
            val date = today.minusDays(offset)
            val range = TimeRangeFilter.between(date.atStartOfDay(zone).toInstant(), date.plusDays(1).atStartOfDay(zone).toInstant())
            val steps = if (stepsPermission in granted) client.aggregate(AggregateRequest(setOf(StepsRecord.COUNT_TOTAL), range))[StepsRecord.COUNT_TOTAL] else null
            val sleep = if (sleepPermission in granted) client.aggregate(AggregateRequest(setOf(SleepSessionRecord.SLEEP_DURATION_TOTAL), range))[SleepSessionRecord.SLEEP_DURATION_TOTAL]?.toMinutes() else null
            HealthDay(date, steps, sleep)
        }
        return HealthSummary(granted, days)
    }
}
