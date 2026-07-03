package com.bihstudio.uvindex.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

const val PEAK_NOTIFICATION_LEAD_TIME_MS = 30 * 60 * 1000L

fun dailyPeak(
    forecast: List<UVHourly>,
    date: LocalDate,
    zoneId: ZoneId = ZoneId.systemDefault()
): UVHourly? = forecast
    .asSequence()
    .filter { Instant.ofEpochMilli(it.timestamp).atZone(zoneId).toLocalDate() == date }
    .sortedBy { it.timestamp }
    .maxByOrNull { it.uvIndex }

fun dailyPeaksAfter(
    forecast: List<UVHourly>,
    date: LocalDate,
    numberOfDays: Int,
    zoneId: ZoneId = ZoneId.systemDefault()
): List<Pair<LocalDate, UVHourly>> = forecast
    .groupBy { Instant.ofEpochMilli(it.timestamp).atZone(zoneId).toLocalDate() }
    .filterKeys { it.isAfter(date) }
    .toSortedMap()
    .entries
    .take(numberOfDays)
    .mapNotNull { (forecastDate, hours) ->
        hours.sortedBy { it.timestamp }
            .maxByOrNull { it.uvIndex }
            ?.let { forecastDate to it }
    }

fun nextNotifiablePeak(
    forecast: List<UVHourly>,
    nowMillis: Long = System.currentTimeMillis(),
    zoneId: ZoneId = ZoneId.systemDefault()
): UVHourly? = notifiablePeaks(forecast, nowMillis, zoneId).firstOrNull()

fun notifiablePeaks(
    forecast: List<UVHourly>,
    nowMillis: Long = System.currentTimeMillis(),
    zoneId: ZoneId = ZoneId.systemDefault()
): List<UVHourly> = forecast
    .groupBy { Instant.ofEpochMilli(it.timestamp).atZone(zoneId).toLocalDate() }
    .toSortedMap()
    .values
    .mapNotNull { hours -> hours.sortedBy { it.timestamp }.maxByOrNull { it.uvIndex } }
    .filter { peak -> peak.timestamp - PEAK_NOTIFICATION_LEAD_TIME_MS > nowMillis }
