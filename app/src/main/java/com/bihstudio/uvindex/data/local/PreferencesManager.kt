package com.bihstudio.uvindex.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "uv_prefs")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        val KEY_LANGUAGE       = stringPreferencesKey("language")
        val KEY_FIRST_LAUNCH   = booleanPreferencesKey("first_launch")
        val KEY_NOTIF_ENABLED  = booleanPreferencesKey("notifications_enabled")
        val KEY_LOCATION_GRANTED = booleanPreferencesKey("location_granted")
        val KEY_LAST_LAT       = floatPreferencesKey("last_lat")
        val KEY_LAST_LON       = floatPreferencesKey("last_lon")
    }

    val language: Flow<String> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_LANGUAGE] ?: "en" }

    val isFirstLaunch: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_FIRST_LAUNCH] ?: true }

    val notificationsEnabled: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_NOTIF_ENABLED] ?: false }

    val lastLocation: Flow<Pair<Double, Double>?> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map {
            val lat = it[KEY_LAST_LAT]
            val lon = it[KEY_LAST_LON]
            if (lat != null && lon != null) lat.toDouble() to lon.toDouble() else null
        }

    suspend fun setLanguage(code: String) {
        dataStore.edit { it[KEY_LANGUAGE] = code }
    }

    suspend fun setFirstLaunch(isFirst: Boolean) {
        dataStore.edit { it[KEY_FIRST_LAUNCH] = isFirst }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_NOTIF_ENABLED] = enabled }
    }

    suspend fun setLocationGranted(granted: Boolean) {
        dataStore.edit { it[KEY_LOCATION_GRANTED] = granted }
    }

    suspend fun setLastLocation(lat: Double, lon: Double) {
        dataStore.edit {
            it[KEY_LAST_LAT] = lat.toFloat()
            it[KEY_LAST_LON] = lon.toFloat()
        }
    }
}
