package com.bihstudio.uvindex.data.remote.dto

import com.google.gson.annotations.SerializedName

data class OpenMeteoResponse(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("timezone") val timezone: String,
    @SerializedName("hourly") val hourly: HourlyData
)

data class HourlyData(
    @SerializedName("time") val time: List<String>,
    @SerializedName("uv_index") val uvIndex: List<Double>
)
