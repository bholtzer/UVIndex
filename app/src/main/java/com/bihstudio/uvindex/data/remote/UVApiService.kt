package com.bihstudio.uvindex.data.remote

import com.bihstudio.uvindex.data.remote.dto.OpenMeteoResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Open-Meteo API (free, no key required)
 * https://open-meteo.com/en/docs/
 */
interface UVApiService {

    @GET("v1/forecast")
    suspend fun getUVForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("hourly") hourly: String = "uv_index",
        @Query("current") current: String = "uv_index",
        @Query("forecast_days") forecastDays: Int = 4,
        @Query("past_days") pastDays: Int = 3,
        @Query("timezone") timezone: String = "auto"
    ): OpenMeteoResponse
}
