package com.bihstudio.uvindex.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import com.google.android.gms.location.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.*

data class LocationResult(
    val latitude: Double,
    val longitude: Double,
    val name: String = "",
    val countryCode: String = ""
)

data class MajorCity(
    val name: String,
    val countryCode: String,
    val latitude: Double,
    val longitude: Double
)

@Singleton
class LocationRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Result<LocationResult> =
        suspendCancellableCoroutine { cont ->
            fusedClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                null
            ).addOnSuccessListener { location ->
                if (location != null) {
                    val address = getAddress(location.latitude, location.longitude)
                    cont.resume(
                        Result.success(
                            LocationResult(
                                latitude = location.latitude,
                                longitude = location.longitude,
                                name = address?.displayName
                                    ?: "Lat: %.2f, Lon: %.2f".format(location.latitude, location.longitude),
                                countryCode = address?.countryCode.orEmpty()
                            )
                        )
                    )
                } else {
                    cont.resume(Result.failure(Exception("Location unavailable")))
                }
            }.addOnFailureListener { e ->
                cont.resumeWithException(e)
            }
        }

    fun getLocationName(lat: Double, lon: Double): String {
        return getAddress(lat, lon)?.displayName
            ?: "Lat: %.2f, Lon: %.2f".format(lat, lon)
    }

    fun getLocalizedCityName(lat: Double, lon: Double, languageCode: String, fallback: String): String {
        return getAddress(lat, lon, Locale(languageCode))
            ?.cityName
            ?.takeIf { it.isNotBlank() }
            ?: fallback
    }

    fun getCountryCode(lat: Double, lon: Double): String {
        return getAddress(lat, lon)?.countryCode.orEmpty()
    }

    fun searchLocationByName(query: String): LocationResult? {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val address = geocoder.getFromLocationName(query, 1)?.firstOrNull() ?: return null
            val name = listOfNotNull(
                address.locality ?: address.subAdminArea ?: address.adminArea ?: address.featureName,
                address.countryCode
            ).joinToString(", ").ifEmpty { query }
            LocationResult(address.latitude, address.longitude, name, address.countryCode.orEmpty())
        } catch (e: Exception) {
            null
        }
    }

    fun getMajorCitiesForCountry(countryCode: String): List<MajorCity> {
        return majorCitiesByCountry[countryCode.uppercase(Locale.US)].orEmpty()
    }

    /**
     * Generate nearby candidate locations within radiusKm
     * (grid of points around the user's location)
     */
    fun getNearbyLocations(
        centerLat: Double,
        centerLon: Double,
        radiusKm: Double = 25.0,
        count: Int = 6
    ): List<Pair<Double, Double>> {
        val results = mutableListOf<Pair<Double, Double>>()
        val steps = listOf(0.1, 0.2, -0.1, -0.2, 0.15, -0.15)
        for (dlat in steps) {
            for (dlon in steps) {
                val lat = centerLat + dlat
                val lon = centerLon + dlon
                val dist = haversineKm(centerLat, centerLon, lat, lon)
                if (dist <= radiusKm) results.add(Pair(lat, lon))
            }
        }
        return results.sortedBy { haversineKm(centerLat, centerLon, it.first, it.second) }
            .take(count)
    }

    fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return R * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    private fun getAddress(lat: Double, lon: Double, locale: Locale = Locale.getDefault()): AddressInfo? {
        return try {
            val geocoder = Geocoder(context, locale)
            @Suppress("DEPRECATION")
            val address = geocoder.getFromLocation(lat, lon, 1)?.firstOrNull() ?: return null
            val cityName = address.locality ?: address.subAdminArea ?: address.adminArea
            val displayName = listOfNotNull(
                cityName,
                address.countryCode
            ).joinToString(", ")
            AddressInfo(
                displayName = displayName,
                cityName = cityName.orEmpty(),
                countryCode = address.countryCode.orEmpty()
            )
        } catch (e: Exception) {
            null
        }
    }

    private data class AddressInfo(
        val displayName: String,
        val cityName: String,
        val countryCode: String
    )

    private val majorCitiesByCountry = mapOf(
        "IL" to listOf(
            MajorCity("Jerusalem", "IL", 31.7683, 35.2137),
            MajorCity("Tel Aviv", "IL", 32.0853, 34.7818),
            MajorCity("Haifa", "IL", 32.7940, 34.9896),
            MajorCity("Eilat", "IL", 29.5577, 34.9519),
            MajorCity("Beersheba", "IL", 31.2520, 34.7915),
            MajorCity("Tiberias", "IL", 32.7959, 35.5310)
        ),
        "US" to listOf(
            MajorCity("New York", "US", 40.7128, -74.0060),
            MajorCity("Los Angeles", "US", 34.0522, -118.2437),
            MajorCity("Chicago", "US", 41.8781, -87.6298),
            MajorCity("Houston", "US", 29.7604, -95.3698),
            MajorCity("Phoenix", "US", 33.4484, -112.0740),
            MajorCity("Miami", "US", 25.7617, -80.1918)
        ),
        "GB" to listOf(
            MajorCity("London", "GB", 51.5072, -0.1276),
            MajorCity("Manchester", "GB", 53.4808, -2.2426),
            MajorCity("Birmingham", "GB", 52.4862, -1.8904),
            MajorCity("Glasgow", "GB", 55.8642, -4.2518),
            MajorCity("Cardiff", "GB", 51.4816, -3.1791)
        ),
        "FR" to listOf(
            MajorCity("Paris", "FR", 48.8566, 2.3522),
            MajorCity("Marseille", "FR", 43.2965, 5.3698),
            MajorCity("Lyon", "FR", 45.7640, 4.8357),
            MajorCity("Nice", "FR", 43.7102, 7.2620),
            MajorCity("Toulouse", "FR", 43.6047, 1.4442)
        ),
        "DE" to listOf(
            MajorCity("Berlin", "DE", 52.5200, 13.4050),
            MajorCity("Hamburg", "DE", 53.5511, 9.9937),
            MajorCity("Munich", "DE", 48.1351, 11.5820),
            MajorCity("Cologne", "DE", 50.9375, 6.9603),
            MajorCity("Frankfurt", "DE", 50.1109, 8.6821)
        ),
        "ES" to listOf(
            MajorCity("Madrid", "ES", 40.4168, -3.7038),
            MajorCity("Barcelona", "ES", 41.3874, 2.1686),
            MajorCity("Valencia", "ES", 39.4699, -0.3763),
            MajorCity("Seville", "ES", 37.3891, -5.9845),
            MajorCity("Malaga", "ES", 36.7213, -4.4214)
        ),
        "IT" to listOf(
            MajorCity("Rome", "IT", 41.9028, 12.4964),
            MajorCity("Milan", "IT", 45.4642, 9.1900),
            MajorCity("Naples", "IT", 40.8518, 14.2681),
            MajorCity("Palermo", "IT", 38.1157, 13.3615),
            MajorCity("Cagliari", "IT", 39.2238, 9.1217)
        ),
        "CA" to listOf(
            MajorCity("Toronto", "CA", 43.6532, -79.3832),
            MajorCity("Montreal", "CA", 45.5019, -73.5674),
            MajorCity("Vancouver", "CA", 49.2827, -123.1207),
            MajorCity("Calgary", "CA", 51.0447, -114.0719),
            MajorCity("Ottawa", "CA", 45.4215, -75.6972)
        ),
        "AU" to listOf(
            MajorCity("Sydney", "AU", -33.8688, 151.2093),
            MajorCity("Melbourne", "AU", -37.8136, 144.9631),
            MajorCity("Brisbane", "AU", -27.4698, 153.0251),
            MajorCity("Perth", "AU", -31.9523, 115.8613),
            MajorCity("Adelaide", "AU", -34.9285, 138.6007)
        ),
        "BR" to listOf(
            MajorCity("Sao Paulo", "BR", -23.5558, -46.6396),
            MajorCity("Rio de Janeiro", "BR", -22.9068, -43.1729),
            MajorCity("Brasilia", "BR", -15.7939, -47.8828),
            MajorCity("Salvador", "BR", -12.9777, -38.5016),
            MajorCity("Fortaleza", "BR", -3.7319, -38.5267)
        ),
        "IN" to listOf(
            MajorCity("Delhi", "IN", 28.6139, 77.2090),
            MajorCity("Mumbai", "IN", 19.0760, 72.8777),
            MajorCity("Bengaluru", "IN", 12.9716, 77.5946),
            MajorCity("Chennai", "IN", 13.0827, 80.2707),
            MajorCity("Hyderabad", "IN", 17.3850, 78.4867)
        ),
        "MX" to listOf(
            MajorCity("Mexico City", "MX", 19.4326, -99.1332),
            MajorCity("Guadalajara", "MX", 20.6597, -103.3496),
            MajorCity("Monterrey", "MX", 25.6866, -100.3161),
            MajorCity("Merida", "MX", 20.9674, -89.5926),
            MajorCity("Cancun", "MX", 21.1619, -86.8515)
        ),
        "ZA" to listOf(
            MajorCity("Johannesburg", "ZA", -26.2041, 28.0473),
            MajorCity("Cape Town", "ZA", -33.9249, 18.4241),
            MajorCity("Durban", "ZA", -29.8587, 31.0218),
            MajorCity("Pretoria", "ZA", -25.7479, 28.2293),
            MajorCity("Gqeberha", "ZA", -33.9608, 25.6022)
        )
    )
}
