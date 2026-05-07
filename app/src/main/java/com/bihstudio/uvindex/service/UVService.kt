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
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.bihstudio.uvindex.R
import com.bihstudio.uvindex.data.local.PreferencesManager
import com.bihstudio.uvindex.data.repository.LocationRepository
import com.bihstudio.uvindex.data.repository.UVRepository
import com.bihstudio.uvindex.domain.model.UVHourly
import com.bihstudio.uvindex.domain.model.UVIndexLevel
import com.bihstudio.uvindex.presentation.MainActivity
import com.bihstudio.uvindex.widget.UVIndexWidgetProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

const val CHANNEL_ID = "uv_alerts"
const val NOTIFICATION_ID = 1001

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
            val locationResult = locationRepository.getCurrentLocation().getOrNull()
                ?: return Result.retry()

            val uvResult = uvRepository.getUVData(
                locationResult.latitude,
                locationResult.longitude,
                locationResult.name
            ).getOrNull() ?: return Result.retry()

            val bestHourInNextThreeHours = uvResult.hourlyForecast
                .take(3)
                .maxByOrNull { it.uvIndex }

            if (uvResult.currentUV >= 3.0 || (bestHourInNextThreeHours?.uvIndex ?: 0.0) >= 3.0) {
                val languageCode = preferencesManager.language.first()
                sendUVNotification(
                    context = applicationContext.localized(languageCode),
                    uvIndex = uvResult.currentUV,
                    location = uvResult.locationName.ifEmpty { locationResult.name },
                    bestHour = bestHourInNextThreeHours
                )
            }
            UVIndexWidgetProvider.updateAllWidgets(applicationContext)

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

fun scheduleUVChecks(context: Context) {
    val request = PeriodicWorkRequestBuilder<UVCheckWorker>(1, TimeUnit.HOURS)
        .setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        )
        .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "uv_check",
        ExistingPeriodicWorkPolicy.KEEP,
        request
    )
}

fun cancelUVChecks(context: Context) {
    WorkManager.getInstance(context).cancelUniqueWork("uv_check")
}

fun sendUVNotification(context: Context, uvIndex: Double, location: String, bestHour: UVHourly?) {
    createNotificationChannel(context)

    val level = UVIndexLevel.fromIndex(uvIndex)
    val label = context.getString(level.labelRes())
    val advice = context.getString(level.adviceRes())
    val bestUv = bestHour?.uvIndex ?: uvIndex
    val bestTime = bestHour?.hour ?: "--"
    val title = context.getString(R.string.notif_uv_title, uvIndex, label)
    val text = context.getString(R.string.notif_uv_text, location, bestUv, bestTime)
    val badgeNumber = uvIndex.roundToInt().coerceIn(0, 12)
    val bigText = context.getString(
        R.string.notif_uv_big_text,
        uvIndex,
        location,
        bestUv,
        bestTime,
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
