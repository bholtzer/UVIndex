package com.bihstudio.uvindex.data.repository

import com.bihstudio.uvindex.data.local.PreferencesManager
import com.bihstudio.uvindex.data.local.UVCacheDao
import com.bihstudio.uvindex.data.local.UVCacheEntity
import com.bihstudio.uvindex.data.remote.UVApiService
import com.bihstudio.uvindex.domain.model.UVData
import com.bihstudio.uvindex.domain.model.UVHourly
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDateTime
import java.time.ZoneOffset
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

    private val CACHE_TTL_MS = 30 * 60 * 1000L   // 30 minutes

    suspend fun getUVData(
        latitude: Double,
        longitude: Double,
        locationName: String = ""
    ): Result<UVData> {
        val cacheKey = "${String.format("%.3f", latitude)}_${String.format("%.3f", longitude)}"

        // Try cache first
        val cached = cacheDao.getCache(cacheKey)
        if (cached != null && System.currentTimeMillis() - cached.timestamp < CACHE_TTL_MS) {
            return Result.success(cached.toDomain(gson))
        }

        // Fetch from network
        return try {
            val response = apiService.getUVForecast(latitude, longitude)
            val now = LocalDateTime.now()
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")

            val times = response.hourly.time
            val uvValues = response.hourly.uvIndex

            // Find current hour index
            val currentIndex = times.indexOfFirst {
                try {
                    val t = LocalDateTime.parse(it, formatter)
                    t.hour == now.hour && t.toLocalDate() == now.toLocalDate()
                } catch (e: Exception) { false }
            }.takeIf { it >= 0 } ?: 0

            val currentUV = uvValues.getOrElse(currentIndex) { 0.0 }

            val timelineForecast = times.mapIndexedNotNull { idx, rawTime ->
                try {
                    val t = LocalDateTime.parse(rawTime, formatter)
                    UVHourly(
                        hour = t.format(DateTimeFormatter.ofPattern("HH:mm")),
                        uvIndex = uvValues.getOrElse(idx) { 0.0 },
                        timestamp = t.toEpochSecond(ZoneOffset.UTC) * 1000
                    )
                } catch (e: Exception) {
                    null
                }
            }

            // Keep the next 48 hours so the UI can show near-term cards,
            // best planning time, and the next two days from one payload.
            val hourlyForecast = (1..48).mapNotNull { offset ->
                val idx = currentIndex + offset
                if (idx < times.size) {
                    val t = LocalDateTime.parse(times[idx], formatter)
                    UVHourly(
                        hour = t.format(DateTimeFormatter.ofPattern("HH:mm")),
                        uvIndex = uvValues.getOrElse(idx) { 0.0 },
                        timestamp = t.toEpochSecond(ZoneOffset.UTC) * 1000
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
        val futureHourly = hourly
            .filter { it.timestamp > now }
            .take(48)
            .ifEmpty { hourly.take(48) }
        return UVData(
            latitude = latitude,
            longitude = longitude,
            currentUV = currentUV,
            hourlyForecast = futureHourly,
            timelineForecast = hourly,
            locationName = locationName,
            timestamp = timestamp
        )
    }
}
