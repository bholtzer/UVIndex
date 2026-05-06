package com.bihstudio.uvindex.widget

import android.Manifest
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.bihstudio.uvindex.R
import com.bihstudio.uvindex.data.local.PreferencesManager
import com.bihstudio.uvindex.data.repository.LocationRepository
import com.bihstudio.uvindex.data.repository.UVRepository
import com.bihstudio.uvindex.domain.model.UVIndexLevel
import com.bihstudio.uvindex.presentation.MainActivity
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

class UVIndexWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        updateWidgets(context, manager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            updateAllWidgets(context)
        }
    }

    companion object {
        private const val ACTION_REFRESH = "com.bihstudio.uvindex.widget.REFRESH"

        fun updateAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, UVIndexWidgetProvider::class.java))
            updateWidgets(context, manager, ids)
        }

        private fun updateWidgets(
            context: Context,
            manager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            if (appWidgetIds.isEmpty()) return

            appWidgetIds.forEach { id ->
                manager.updateAppWidget(id, loadingViews(context))
            }

            CoroutineScope(Dispatchers.IO).launch {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    UVWidgetEntryPoint::class.java
                )
                val preferences = entryPoint.preferencesManager()
                val localizedContext = context.localized(preferences.language.first())

                val views = if (!context.hasLocationPermission()) {
                    messageViews(localizedContext, localizedContext.getString(R.string.widget_location_needed))
                } else {
                    runCatching {
                        val location = entryPoint.locationRepository().getCurrentLocation().getOrThrow()
                        val uvData = entryPoint.uvRepository()
                            .getUVData(location.latitude, location.longitude, location.name)
                            .getOrThrow()
                        val zone = ZoneId.systemDefault()
                        val today = LocalDate.now(zone)
                        val bestHour = uvData.hourlyForecast
                            .filter { Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate() == today }
                            .maxByOrNull { it.uvIndex }
                            ?: uvData.hourlyForecast.maxByOrNull { it.uvIndex }
                        dataViews(
                            context = localizedContext,
                            location = uvData.locationName.ifEmpty { location.name },
                            currentUv = uvData.currentUV,
                            bestUv = bestHour?.uvIndex ?: uvData.currentUV,
                            bestTime = bestHour?.hour ?: "--"
                        )
                    }.getOrElse {
                        messageViews(localizedContext, localizedContext.getString(R.string.could_not_load_uv_data))
                    }
                }

                appWidgetIds.forEach { id ->
                    manager.updateAppWidget(id, views)
                }
            }
        }

        private fun loadingViews(context: Context): RemoteViews =
            baseViews(context).apply {
                setTextViewText(R.id.widget_location, "")
                setTextViewText(R.id.widget_current_uv, "--")
                setTextViewText(R.id.widget_level, context.getString(R.string.loading))
                setTextViewText(R.id.widget_best_time, context.getString(R.string.widget_tap_refresh))
                setTextViewText(R.id.widget_hint, "")
            }

        private fun messageViews(context: Context, message: String): RemoteViews =
            baseViews(context).apply {
                setTextViewText(R.id.widget_location, "")
                setTextViewText(R.id.widget_current_uv, "--")
                setTextViewText(R.id.widget_level, message)
                setTextViewText(R.id.widget_best_time, context.getString(R.string.widget_tap_refresh))
                setTextViewText(R.id.widget_hint, "")
            }

        private fun dataViews(
            context: Context,
            location: String,
            currentUv: Double,
            bestUv: Double,
            bestTime: String
        ): RemoteViews {
            val level = UVIndexLevel.fromIndex(currentUv)
            return baseViews(context).apply {
                setTextViewText(R.id.widget_location, location)
                setTextViewText(R.id.widget_current_uv, String.format(Locale.US, "%.1f", currentUv))
                setTextColor(R.id.widget_current_uv, ContextCompat.getColor(context, R.color.text_primary))
                setTextViewText(R.id.widget_level, context.getString(level.labelRes()))
                setTextColor(R.id.widget_level, level.color.toInt())
                setTextViewText(R.id.widget_best_time, context.getString(R.string.widget_best_time, bestUv, bestTime))
                setTextViewText(R.id.widget_hint, context.getString(R.string.widget_tap_refresh))
            }
        }

        private fun baseViews(context: Context): RemoteViews =
            RemoteViews(context.packageName, R.layout.widget_uv_index).apply {
                setTextViewText(R.id.widget_title, context.getString(R.string.widget_title))
                setOnClickPendingIntent(R.id.widget_root, openAppIntent(context))
                setOnClickPendingIntent(R.id.widget_best_time, refreshIntent(context))
            }

        private fun openAppIntent(context: Context): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_OPEN_UV_INDEX, true)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            return PendingIntent.getActivity(
                context,
                10,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun refreshIntent(context: Context): PendingIntent {
            val intent = Intent(context, UVIndexWidgetProvider::class.java).apply {
                action = ACTION_REFRESH
            }
            return PendingIntent.getBroadcast(
                context,
                11,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun Context.hasLocationPermission(): Boolean {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        }

        private fun Context.localized(languageCode: String): Context {
            val locale = Locale(languageCode)
            val config = Configuration(resources.configuration).apply {
                setLocale(locale)
                setLayoutDirection(locale)
            }
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                createConfigurationContext(config)
            } else {
                @Suppress("DEPRECATION")
                resources.updateConfiguration(config, resources.displayMetrics)
                this
            }
        }

        private fun UVIndexLevel.labelRes(): Int = when {
            index < 3 -> R.string.uv_low
            index < 6 -> R.string.uv_moderate
            index < 8 -> R.string.uv_high
            index < 11 -> R.string.uv_very_high
            else -> R.string.uv_extreme
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface UVWidgetEntryPoint {
    fun uvRepository(): UVRepository
    fun locationRepository(): LocationRepository
    fun preferencesManager(): PreferencesManager
}
