package com.bihstudio.uvindex.presentation.screens.location

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bihstudio.uvindex.R
import com.bihstudio.uvindex.data.repository.LocationRepository
import com.bihstudio.uvindex.data.repository.UVRepository
import com.bihstudio.uvindex.domain.model.NearbyLocation
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class LocationSearchState {
    object Idle : LocationSearchState()
    object Loading : LocationSearchState()
    data class NearbyResults(val locations: List<NearbyLocation>) : LocationSearchState()
    data class SearchResult(val name: String, val uvIndex: Double) : LocationSearchState()
    data class Error(val message: String) : LocationSearchState()
}

@HiltViewModel
class LocationSearchViewModel @Inject constructor(
    private val uvRepository: UVRepository,
    private val locationRepository: LocationRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow<LocationSearchState>(LocationSearchState.Idle)
    val state: StateFlow<LocationSearchState> = _state.asStateFlow()

    fun searchLocation(query: String) {
        viewModelScope.launch {
            _state.value = LocationSearchState.Loading
            try {
                // Try parsing as lat,lon
                val parts = query.split(",").map { it.trim() }
                if (parts.size == 2) {
                    val lat = parts[0].toDoubleOrNull()
                    val lon = parts[1].toDoubleOrNull()
                    if (lat != null && lon != null) {
                        val name = locationRepository.getLocationName(lat, lon)
                        val uv = uvRepository.getUVData(lat, lon, name).getOrThrow()
                        _state.value = LocationSearchState.SearchResult(
                            name = uv.locationName,
                            uvIndex = uv.currentUV
                        )
                        return@launch
                    }
                }

                val location = locationRepository.searchLocationByName(query)
                    ?: run {
                        _state.value = LocationSearchState.Error(context.getString(R.string.location_not_found))
                        return@launch
                    }
                val uv = uvRepository.getUVData(
                    location.latitude,
                    location.longitude,
                    location.name
                ).getOrThrow()
                _state.value = LocationSearchState.SearchResult(
                    name = uv.locationName,
                    uvIndex = uv.currentUV
                )
            } catch (e: Exception) {
                _state.value = LocationSearchState.Error(e.message ?: context.getString(R.string.error_unknown))
            }
        }
    }

    fun findNearbyGoodLocations() {
        viewModelScope.launch {
            _state.value = LocationSearchState.Loading
            try {
                val loc = locationRepository.getCurrentLocation().getOrThrow()
                val candidates = locationRepository.getNearbyLocations(loc.latitude, loc.longitude)

                val results = candidates.map { (lat, lon) ->
                    async {
                        try {
                            val name = locationRepository.getLocationName(lat, lon)
                            val uv = uvRepository.getUVData(lat, lon, name).getOrNull()
                                ?: return@async null
                            val bestHour = uv.hourlyForecast.maxByOrNull { it.uvIndex }
                            NearbyLocation(
                                name = uv.locationName.ifEmpty { "%.2f, %.2f".format(lat, lon) },
                                latitude = lat,
                                longitude = lon,
                                distanceKm = locationRepository.haversineKm(loc.latitude, loc.longitude, lat, lon),
                                bestUVHour = bestHour?.hour ?: uv.hourlyForecast.firstOrNull()?.hour ?: "--",
                                bestUVIndex = bestHour?.uvIndex ?: uv.currentUV
                            )
                        } catch (e: Exception) { null }
                    }
                }.awaitAll().filterNotNull()
                    .filter { it.bestUVIndex >= 3.0 }  // Only show meaningful UV
                    .sortedByDescending { it.bestUVIndex }

                _state.value = LocationSearchState.NearbyResults(results)
            } catch (e: Exception) {
                _state.value = LocationSearchState.Error(e.message ?: context.getString(R.string.error_unknown))
            }
        }
    }
}
