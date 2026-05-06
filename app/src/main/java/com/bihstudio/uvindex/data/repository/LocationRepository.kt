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
    val name: String = ""
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
                    val name = getLocationName(location.latitude, location.longitude)
                    cont.resume(Result.success(LocationResult(location.latitude, location.longitude, name)))
                } else {
                    cont.resume(Result.failure(Exception("Location unavailable")))
                }
            }.addOnFailureListener { e ->
                cont.resumeWithException(e)
            }
        }

    fun getLocationName(lat: Double, lon: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            val address = addresses?.firstOrNull()
            val name = address?.let {
                listOfNotNull(
                    it.locality ?: it.subAdminArea ?: it.adminArea,
                    it.countryCode
                ).joinToString(", ")
            }.orEmpty()
            name.ifEmpty { "Lat: %.2f, Lon: %.2f".format(lat, lon) }
        } catch (e: Exception) {
            "Lat: %.2f, Lon: %.2f".format(lat, lon)
        }
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
            LocationResult(address.latitude, address.longitude, name)
        } catch (e: Exception) {
            null
        }
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
}
