package com.bihstudio.uvindex.data.repository

import com.bihstudio.uvindex.data.local.PreferencesManager
import com.bihstudio.uvindex.data.local.UVCacheDao
import com.bihstudio.uvindex.data.local.UVCacheEntity
import com.bihstudio.uvindex.data.remote.UVApiService
import com.bihstudio.uvindex.domain.model.UVData
import com.bihstudio.uvindex.domain.model.UVHourly
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UVRepository @Inject constructor(
    private val apiService: UVApiService,
    private val cacheDao: UVCacheDao,
    private val preferencesManager: PreferencesManager,
    private val gson: Gson
) {

    private val CACHE_TTL_MS = 5 * 60 * 1000L   // 5 minutes

    suspend fun getUVData(
        latitude: Double,
        longitude: Double,
        locationName: String = "",
        forceRefresh: Boolean = false
    ): Result<UVData> {
        val cacheKey = "uv_v3_${String.format("%.3f", latitude)}_${String.format("%.3f", longitude)}"

        // Try cache first
        val cached = cacheDao.getCache(cacheKey)
        if (!forceRefresh && cached != null && System.currentTimeMillis() - cached.timestamp < CACHE_TTL_MS) {
            return Result.success(cached.toDomain(gson))
        }

        // Fetch from network
        return try {
            val response = apiService.getUVForecast(latitude, longitude)
            val responseZone = runCatching { ZoneId.of(response.timezone) }
                .getOrDefault(ZoneId.systemDefault())
            val systemZone = ZoneId.systemDefault()
            val now = LocalDateTime.now(responseZone)
            val nowEpoch = now.atZone(responseZone).toInstant().toEpochMilli()
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
            val hourFormatter = DateTimeFormatter.ofPattern("HH:mm")

            val times = response.hourly.time
            val uvValues = response.hourly.uvIndex

            fun epochMillis(time: LocalDateTime): Long =
                time.atZone(responseZone).toInstant().toEpochMilli()

            val parsedTimes = times.mapNotNull { rawTime ->
                runCatching { LocalDateTime.parse(rawTime, formatter) }.getOrNull()
            }

            val timelineForecast = parsedTimes.mapIndexed { idx, t ->
                val timestamp = epochMillis(t)
                UVHourly(
                    hour = Instant.ofEpochMilli(timestamp).atZone(systemZone).format(hourFormatter),
                    uvIndex = uvValues.getOrElse(idx) { 0.0 },
                    timestamp = timestamp
                )
            }
            val currentIndex = currentHourIndex(timelineForecast, nowEpoch)
            val currentUV = timelineForecast.getOrNull(currentIndex)?.uvIndex ?: 0.0

            // Include current hour (offset 0) and the next 48 hours.
            val hourlyForecast = (0..48).mapNotNull { offset ->
                val idx = currentIndex + offset
                if (idx < times.size) {
                    val t = LocalDateTime.parse(times[idx], formatter)
                    val timestamp = epochMillis(t)
                    UVHourly(
                        hour = Instant.ofEpochMilli(timestamp).atZone(systemZone).format(hourFormatter),
                        uvIndex = uvValues.getOrElse(idx) { 0.0 },
                        timestamp = timestamp
                    )
                } else null
            }

            val uvData = UVData(
                latitude = latitude,
                longitude = longitude,
                currentUV = currentUV,
                hourlyForecast = hourlyForecast,
                timelineForecast = timelineForecast,
                locationName = locationName.ifEmpty { "Lat: %.2f, Lon: %.2f".format(latitude, longitude) }
            )

            // Cache result
            val entity = UVCacheEntity(
                id = cacheKey,
                latitude = latitude,
                longitude = longitude,
                locationName = uvData.locationName,
                currentUV = currentUV,
                hourlyJson = gson.toJson(timelineForecast),
                timestamp = System.currentTimeMillis()
            )
            cacheDao.insertCache(entity)
            preferencesManager.setLastLocation(latitude, longitude)

            Result.success(uvData)
        } catch (e: Exception) {
            // Return stale cache on error
            if (cached != null) Result.success(cached.toDomain(gson))
            else Result.failure(e)
        }
    }

    // ── Helper extension ──────────────────────────────────────────────────────
    private fun UVCacheEntity.toDomain(gson: Gson): UVData {
        val type = object : TypeToken<List<UVHourly>>() {}.type
        val hourly: List<UVHourly> = gson.fromJson(hourlyJson, type) ?: emptyList()
        val now = System.currentTimeMillis()
        val systemZone = ZoneId.systemDefault()
        val hourFormatter = DateTimeFormatter.ofPattern("HH:mm")

        // Re-format hours from cached timestamps to match current system timezone
        val updatedHourly = hourly.map {
            it.copy(hour = Instant.ofEpochMilli(it.timestamp).atZone(systemZone).format(hourFormatter))
        }

        val currentUv = updatedHourly.getOrNull(currentHourIndex(updatedHourly, now))?.uvIndex ?: currentUV
        val futureHourly = updatedHourly
            .filter { it.timestamp >= now - 30 * 60 * 1000L } // Include current hour
            .take(48)
            .ifEmpty { updatedHourly.take(48) }
        return UVData(
            latitude = latitude,
            longitude = longitude,
            currentUV = currentUv,
            hourlyForecast = futureHourly,
            timelineForecast = updatedHourly,
            locationName = locationName,
            timestamp = timestamp
        )
    }

    private fun currentHourIndex(timeline: List<UVHourly>, timestamp: Long): Int {
        if (timeline.isEmpty()) return 0
        val nextIndex = timeline.indexOfFirst { it.timestamp >= timestamp }
        return when {
            nextIndex == -1 -> timeline.lastIndex
            timeline[nextIndex].timestamp == timestamp -> nextIndex
            nextIndex == 0 -> 0
            else -> nextIndex - 1
        }
    }
}
