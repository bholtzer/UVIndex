package com.bihstudio.uvindex.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.bihstudio.uvindex.R
import com.bihstudio.uvindex.data.local.PreferencesManager
import com.bihstudio.uvindex.data.repository.LocationRepository
import com.bihstudio.uvindex.data.repository.LocationResult
import com.bihstudio.uvindex.data.repository.UVRepository
import com.bihstudio.uvindex.domain.model.UVHourly
import com.bihstudio.uvindex.domain.model.UVIndexLevel
import com.bihstudio.uvindex.domain.model.PEAK_NOTIFICATION_LEAD_TIME_MS
import com.bihstudio.uvindex.domain.model.notifiablePeaks
import com.bihstudio.uvindex.presentation.MainActivity
import com.bihstudio.uvindex.widget.UVIndexWidgetProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

const val CHANNEL_ID = "uv_alerts"
const val NOTIFICATION_ID = 1001
private const val UV_CHECK_WORK_NAME = "uv_check"
private const val UV_CHECK_NOW_WORK_NAME = "uv_check_now"
private const val UV_PEAK_NOTIFICATION_WORK_PREFIX = "uv_peak_notification"
private const val UV_PEAK_NOTIFICATION_WORK_TAG = "uv_peak_notifications"
private const val KEY_PEAK_UV = "peak_uv"
private const val KEY_PEAK_HOUR = "peak_hour"
private const val KEY_PEAK_TIMESTAMP = "peak_timestamp"
private const val KEY_LOCATION = "location"

@HiltWorker
class UVCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val uvRepository: UVRepository,
    private val locationRepository: LocationRepository,
    private val preferencesManager: PreferencesManager
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val notificationsEnabled = preferencesManager.notificationsEnabled.first()
        if (!notificationsEnabled) return Result.success()

        return try {
            val savedLocation = preferencesManager.lastLocation.first()
            val locationResult = if (savedLocation != null) {
                val (latitude, longitude) = savedLocation
                LocationResult(
                    latitude = latitude,
                    longitude = longitude,
                    name = locationRepository.getLocationName(latitude, longitude)
                )
            } else {
                locationRepository.getCurrentLocation().getOrNull() ?: return Result.retry()
            }

            val uvResult = uvRepository.getUVData(
                locationResult.latitude,
                locationResult.longitude,
                locationResult.name,
                forceRefresh = true
            ).getOrNull() ?: return Result.retry()

            updateLauncherUvInfo(
                context = applicationContext,
                uvIndex = uvResult.currentUV,
                location = uvResult.locationName.ifEmpty { locationResult.name }
            )

            notifiablePeaks(uvResult.timelineForecast).forEach { peak ->
                schedulePeakNotification(
                    context = applicationContext,
                    location = uvResult.locationName.ifEmpty { locationResult.name },
                    peak = peak
                )
            }
            UVIndexWidgetProvider.updateAllWidgets(applicationContext)

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

@HiltWorker
class UVPeakNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val preferencesManager: PreferencesManager
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        if (!preferencesManager.notificationsEnabled.first()) return Result.success()

        val peakUv = inputData.getDouble(KEY_PEAK_UV, -1.0)
        val peakHour = inputData.getString(KEY_PEAK_HOUR) ?: return Result.failure()
        val peakTimestamp = inputData.getLong(KEY_PEAK_TIMESTAMP, 0L)
        val location = inputData.getString(KEY_LOCATION) ?: return Result.failure()
        if (peakUv < 0.0 || peakTimestamp <= 0L) return Result.failure()

        val languageCode = preferencesManager.language.first()
        sendUVNotification(
            context = applicationContext.localized(languageCode),
            location = location,
            peak = UVHourly(peakHour, peakUv, peakTimestamp)
        )
        return Result.success()
    }
}

private fun schedulePeakNotification(context: Context, location: String, peak: UVHourly) {
    val notificationTime = peak.timestamp - PEAK_NOTIFICATION_LEAD_TIME_MS
    val delayMillis = (notificationTime - System.currentTimeMillis()).coerceAtLeast(0L)
    val input = Data.Builder()
        .putDouble(KEY_PEAK_UV, peak.uvIndex)
        .putString(KEY_PEAK_HOUR, peak.hour)
        .putLong(KEY_PEAK_TIMESTAMP, peak.timestamp)
        .putString(KEY_LOCATION, location)
        .build()
    val request = OneTimeWorkRequestBuilder<UVPeakNotificationWorker>()
        .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
        .setInputData(input)
        .addTag(UV_PEAK_NOTIFICATION_WORK_TAG)
        .build()
    val peakDate = Instant.ofEpochMilli(peak.timestamp)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    val workName = "${UV_PEAK_NOTIFICATION_WORK_PREFIX}_$peakDate"

    WorkManager.getInstance(context).enqueueUniqueWork(
        workName,
        ExistingWorkPolicy.REPLACE,
        request
    )
}

fun scheduleUVChecks(context: Context) {
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()
    val immediateRequest = OneTimeWorkRequestBuilder<UVCheckWorker>()
        .setConstraints(constraints)
        .build()
    val periodicRequest = PeriodicWorkRequestBuilder<UVCheckWorker>(12, TimeUnit.HOURS)
        .setConstraints(constraints)
        .build()

    WorkManager.getInstance(context).enqueueUniqueWork(
        UV_CHECK_NOW_WORK_NAME,
        ExistingWorkPolicy.REPLACE,
        immediateRequest
    )

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        UV_CHECK_WORK_NAME,
        ExistingPeriodicWorkPolicy.UPDATE,
        periodicRequest
    )
}

fun cancelUVChecks(context: Context) {
    WorkManager.getInstance(context).cancelUniqueWork(UV_CHECK_WORK_NAME)
    WorkManager.getInstance(context).cancelUniqueWork(UV_CHECK_NOW_WORK_NAME)
    WorkManager.getInstance(context).cancelAllWorkByTag(UV_PEAK_NOTIFICATION_WORK_TAG)
}

fun sendUVNotification(context: Context, location: String, peak: UVHourly) {
    createNotificationChannel(context)

    val level = UVIndexLevel.fromIndex(peak.uvIndex)
    val label = context.getString(level.labelRes())
    val advice = context.getString(level.adviceRes())
    val title = context.getString(R.string.notif_uv_title, peak.uvIndex, label)
    val text = context.getString(R.string.notif_uv_text, location, peak.uvIndex, peak.hour)
    val badgeNumber = peak.uvIndex.roundToInt().coerceIn(0, 12)
    val bigText = context.getString(
        R.string.notif_uv_big_text,
        peak.uvIndex,
        location,
        peak.hour,
        advice
    )
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val pi = PendingIntent.getActivity(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val publicNotification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_sun_notification)
        .setContentTitle(title)
        .setContentText(text)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(NotificationCompat.CATEGORY_STATUS)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
        .setNumber(badgeNumber)
        .build()

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_sun_notification)
        .setContentTitle(title)
        .setContentText(text)
        .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(NotificationCompat.CATEGORY_STATUS)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
        .setNumber(badgeNumber)
        .setPublicVersion(publicNotification)
        .setContentIntent(pi)
        .setAutoCancel(true)
        .build()

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        == PackageManager.PERMISSION_GRANTED
    ) {
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}

private fun createNotificationChannel(context: Context) {
    val channel = NotificationChannel(
        CHANNEL_ID,
        context.getString(R.string.notif_channel_name),
        NotificationManager.IMPORTANCE_HIGH
    ).apply {
        description = context.getString(R.string.notif_channel_desc)
        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        setShowBadge(true)
    }
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    nm.createNotificationChannel(channel)
}

private fun Context.localized(languageCode: String): Context {
    val locale = Locale(languageCode)
    val config = Configuration(resources.configuration).apply {
        setLocale(locale)
        setLayoutDirection(locale)
    }
    return createConfigurationContext(config)
}

private fun UVIndexLevel.labelRes(): Int = when {
    index < 3 -> R.string.uv_low
    index < 6 -> R.string.uv_moderate
    index < 8 -> R.string.uv_high
    index < 11 -> R.string.uv_very_high
    else -> R.string.uv_extreme
}

private fun UVIndexLevel.adviceRes(): Int = when {
    index < 3 -> R.string.advice_low
    index < 6 -> R.string.advice_moderate
    index < 8 -> R.string.advice_high
    index < 11 -> R.string.advice_very_high
    else -> R.string.advice_extreme
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            scheduleUVChecks(context)
        }
    }
}
