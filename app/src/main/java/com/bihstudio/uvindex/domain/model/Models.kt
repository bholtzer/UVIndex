package com.bihstudio.uvindex.domain.model

data class UVData(
    val latitude: Double,
    val longitude: Double,
    val currentUV: Double,
    val hourlyForecast: List<UVHourly>,
    val timelineForecast: List<UVHourly> = hourlyForecast,
    val locationName: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class UVHourly(
    val hour: String,       // e.g. "14:00"
    val uvIndex: Double,
    val timestamp: Long
)

data class UVIndexLevel(
    val index: Double,
    val label: String,
    val color: Long,
    val advice: String
) {
    companion object {
        fun fromIndex(index: Double): UVIndexLevel = when {
            index < 3 -> UVIndexLevel(index, "Low", 0xFF4CAF50, "Safe – enjoy the outdoors")
            index < 6 -> UVIndexLevel(index, "Moderate", 0xFFFFC107, "Wear sunscreen SPF 30+")
            index < 8 -> UVIndexLevel(index, "High", 0xFFFF9800, "Seek shade midday")
            index < 11 -> UVIndexLevel(index, "Very High", 0xFFF44336, "Avoid midday sun")
            else -> UVIndexLevel(index, "Extreme", 0xFF9C27B0, "Stay indoors if possible")
        }
    }
}

data class NearbyLocation(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val distanceKm: Double,
    val bestUVHour: String,
    val bestUVIndex: Double
)

data class CountryHighUvCity(
    val name: String,
    val countryCode: String,
    val uvIndex: Double,
    val latitude: Double,
    val longitude: Double
)

enum class AppLanguage(val code: String, val displayName: String, val isRtl: Boolean) {
    ENGLISH("en", "English", false),
    HEBREW("he", "עברית", true),
    FRENCH("fr", "Français", false),
    SPANISH("es", "Español", false),
    GERMAN("de", "Deutsch", false),
    ARABIC("ar", "العربية", true),
    RUSSIAN("ru", "Русский", false),
    JAPANESE("ja", "日本語", false),
    ITALIAN("it", "Italiano", false),
    SWEDISH("sv", "Svenska", false),
    BULGARIAN("bg", "Български", false),
    GREEK("el", "Ελληνικά", false),
    PORTUGUESE("pt", "Português", false),
    CROATIAN("hr", "Hrvatski", false),
    TURKISH("tr", "Türkçe", false)
}
