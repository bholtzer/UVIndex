package com.bihstudio.uvindex.data.remote.dto

import com.google.gson.annotations.SerializedName

data class OpenMeteoResponse(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("timezone") val timezone: String,
    @SerializedName("current") val current: CurrentData?,
    @SerializedName("hourly") val hourly: HourlyData
)

data class CurrentData(
    @SerializedName("time") val time: String,
    @SerializedName("uv_index") val uvIndex: Double
)

data class HourlyData(
    @SerializedName("time") val time: List<String>,
    @SerializedName("uv_index") val uvIndex: List<Double>
)
