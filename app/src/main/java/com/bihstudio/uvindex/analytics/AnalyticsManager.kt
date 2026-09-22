package com.bihstudio.uvindex.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsManager @Inject constructor() {

    private val analytics: FirebaseAnalytics = Firebase.analytics

    object Events {
        const val SCREEN_SPLASH       = "screen_splash"
        const val SCREEN_LANGUAGE     = "screen_language"
        const val SCREEN_PERMISSION   = "screen_permission"
        const val SCREEN_UV_INDEX     = "screen_uv_index"
        const val SCREEN_LOCATION     = "screen_location_search"

        const val LANGUAGE_SELECTED   = "language_selected"
        const val LOCATION_GRANTED    = "location_granted"
        const val LOCATION_DENIED     = "location_denied"
        const val NOTIFICATION_OPT_IN = "notification_opt_in"
        const val NEARBY_SEARCH       = "nearby_location_search"
        const val UV_DATA_LOADED      = "uv_data_loaded"
        const val AD_SHOWN            = "interstitial_ad_shown"
        const val CUSTOM_LOCATION     = "custom_location_searched"
    }

    fun logScreen(screenName: String) {
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
        })
    }

    fun logEvent(event: String, params: Bundle = Bundle()) {
        analytics.logEvent(event, params)
    }

    fun logLanguageSelected(code: String) {
        analytics.logEvent(Events.LANGUAGE_SELECTED, Bundle().apply {
            putString("language_code", code)
        })
    }

    fun logUVDataLoaded(uvIndex: Double) {
        analytics.logEvent(Events.UV_DATA_LOADED, Bundle().apply {
            putDouble("uv_index", uvIndex)
        })
    }

    fun logNearbySearch(radiusKm: Double) {
        analytics.logEvent(Events.NEARBY_SEARCH, Bundle().apply {
            putDouble("radius_km", radiusKm)
        })
    }
}
