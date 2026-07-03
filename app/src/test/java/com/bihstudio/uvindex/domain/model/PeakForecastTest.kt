package com.bihstudio.uvindex.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.LocalDate
import java.time.ZoneId

class PeakForecastTest {
    private val zone = ZoneId.of("Asia/Jerusalem")

    @Test
    fun `selects each day's forecast maximum instead of a fixed hour`() {
        val forecast = listOf(
            hourly(2026, 6, 29, 12, 7.0),
            hourly(2026, 6, 29, 13, 6.0),
            hourly(2026, 6, 30, 13, 5.0),
            hourly(2026, 6, 30, 14, 8.0)
        )

        val peak = nextNotifiablePeak(forecast, hourly(2026, 6, 29, 8, 0.0).timestamp, zone)

        assertEquals("12:00", peak?.hour)
        assertEquals(7.0, peak?.uvIndex ?: 0.0, 0.0)
    }

    @Test
    fun `skips today's peak when its thirty minute alert time passed`() {
        val forecast = listOf(
            hourly(2026, 6, 29, 13, 9.0),
            hourly(2026, 6, 30, 12, 8.0)
        )

        val peak = nextNotifiablePeak(forecast, instant(2026, 6, 29, 12, 31), zone)

        assertEquals("12:00", peak?.hour)
        assertEquals(hourly(2026, 6, 30, 12, 0.0).timestamp, peak?.timestamp)
    }

    @Test
    fun `returns null when every alert time has passed`() {
        val forecast = listOf(hourly(2026, 6, 29, 13, 9.0))

        assertNull(nextNotifiablePeak(forecast, instant(2026, 6, 29, 13, 0), zone))
    }

    @Test
    fun `uses the complete second calendar day when calculating its maximum`() {
        val forecast = listOf(
            hourly(2026, 6, 30, 13, 7.0),
            hourly(2026, 7, 1, 8, 2.0),
            hourly(2026, 7, 1, 13, 9.0),
            hourly(2026, 7, 1, 18, 1.0)
        )

        val peaks = dailyPeaksAfter(
            forecast = forecast,
            date = LocalDate.of(2026, 6, 29),
            numberOfDays = 2,
            zoneId = zone
        )

        assertEquals(2, peaks.size)
        assertEquals("13:00", peaks[1].second.hour)
        assertEquals(9.0, peaks[1].second.uvIndex, 0.0)
    }

    @Test
    fun `schedules one notification for every available future day including low UV`() {
        val forecast = listOf(
            hourly(2026, 7, 1, 12, 2.0),
            hourly(2026, 7, 1, 13, 1.5),
            hourly(2026, 7, 2, 13, 5.0),
            hourly(2026, 7, 3, 14, 7.0)
        )

        val peaks = notifiablePeaks(forecast, instant(2026, 7, 1, 8, 0), zone)

        assertEquals(3, peaks.size)
        assertEquals(2.0, peaks[0].uvIndex, 0.0)
        assertEquals(listOf("12:00", "13:00", "14:00"), peaks.map { it.hour })
    }

    private fun hourly(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        uv: Double
    ): UVHourly {
        val timestamp = LocalDateTime.of(year, month, day, hour, 0)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
        return UVHourly("%02d:00".format(hour), uv, timestamp)
    }

    private fun instant(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        LocalDateTime.of(year, month, day, hour, minute)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
}
