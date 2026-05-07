package com.bihstudio.uvindex.presentation.screens.uvindex

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bihstudio.uvindex.data.local.PreferencesManager
import com.bihstudio.uvindex.data.repository.LocationRepository
import com.bihstudio.uvindex.data.repository.UVRepository
import com.bihstudio.uvindex.domain.model.CountryHighUvCity
import com.bihstudio.uvindex.domain.model.UVData
import com.bihstudio.uvindex.domain.model.UVIndexLevel
import com.bihstudio.uvindex.service.updateLauncherUvInfo
import com.bihstudio.uvindex.widget.UVIndexWidgetProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class UVState {
    object Loading : UVState()
    data class Success(
        val data: UVData,
        val level: UVIndexLevel,
        val countryHighUvCity: CountryHighUvCity? = null
    ) : UVState()
    data class Error(val message: String) : UVState()
}

@HiltViewModel
class UVIndexViewModel @Inject constructor(
    private val uvRepository: UVRepository,
    private val locationRepository: LocationRepository,
    private val preferencesManager: PreferencesManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow<UVState>(UVState.Loading)
    val state: StateFlow<UVState> = _state.asStateFlow()

    val isFirstLaunch: StateFlow<Boolean> = preferencesManager.isFirstLaunch
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    init { loadUVData() }

    fun loadUVData() {
        viewModelScope.launch {
            _state.value = UVState.Loading
            try {
                val loc = locationRepository.getCurrentLocation().getOrThrow()
                val uv = uvRepository.getUVData(loc.latitude, loc.longitude, loc.name).getOrThrow()
                val countryHighUvCity = loadHighestUvCityInCountry(loc.countryCode, loc.latitude, loc.longitude)
                updateLauncherUvInfo(context, uv.currentUV, uv.locationName.ifEmpty { loc.name })
                _state.value = UVState.Success(
                    data = uv,
                    level = UVIndexLevel.fromIndex(uv.currentUV),
                    countryHighUvCity = countryHighUvCity
                )
                UVIndexWidgetProvider.updateAllWidgets(context)
            } catch (e: Exception) {
                _state.value = UVState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun markFirstLaunchDone() {
        viewModelScope.launch { preferencesManager.setFirstLaunch(false) }
    }

    private suspend fun loadHighestUvCityInCountry(
        countryCode: String,
        currentLatitude: Double,
        currentLongitude: Double
    ): CountryHighUvCity? {
        val cities = locationRepository.getMajorCitiesForCountry(countryCode)
            .filter { city ->
                locationRepository.haversineKm(
                    currentLatitude,
                    currentLongitude,
                    city.latitude,
                    city.longitude
                ) > 25.0
            }
            .takeIf { it.isNotEmpty() }
            ?: return null

        return cities.mapNotNull { city ->
            val data = uvRepository.getUVData(city.latitude, city.longitude, city.name).getOrNull()
                ?: return@mapNotNull null
            CountryHighUvCity(
                name = city.name,
                countryCode = city.countryCode,
                uvIndex = data.currentUV,
                latitude = city.latitude,
                longitude = city.longitude
            )
        }.maxByOrNull { it.uvIndex }
    }
}
