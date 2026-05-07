package com.bihstudio.uvindex.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Icon
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.bihstudio.uvindex.R
import com.bihstudio.uvindex.presentation.MainActivity
import java.util.Locale
import kotlin.math.roundToInt

private const val UV_SHORTCUT_ID = "current_uv_shortcut"
private const val UV_BADGE_CHANNEL_ID = "uv_launcher_badge"
private const val UV_BADGE_NOTIFICATION_ID = 1002

fun updateLauncherUvInfo(context: Context, uvIndex: Double, location: String) {
    updatePrimaryLauncherIcon(context, uvIndex)
    updateUvShortcut(context, uvIndex, location)
    updateUvBadge(context, uvIndex, location)
}

private fun updatePrimaryLauncherIcon(context: Context, uvIndex: Double) {
    val packageManager = context.packageManager
    val selectedUv = uvIndex.roundToInt().coerceIn(0, 12)
    val selectedAlias = launcherAliasForUv(context, selectedUv)
    val allAliases = listOf(launcherDefaultAlias(context)) + (0..12).map { launcherAliasForUv(context, it) }

    packageManager.setComponentEnabledSetting(
        selectedAlias,
        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
        PackageManager.DONT_KILL_APP
    )

    allAliases
        .filterNot { it == selectedAlias }
        .forEach { alias ->
            packageManager.setComponentEnabledSetting(
                alias,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }
}

private fun launcherDefaultAlias(context: Context): ComponentName {
    return ComponentName(context, "${context.packageName}.presentation.MainActivityDefault")
}

private fun launcherAliasForUv(context: Context, uv: Int): ComponentName {
    return ComponentName(context, "${context.packageName}.presentation.MainActivityUv$uv")
}

private fun updateUvShortcut(context: Context, uvIndex: Double, location: String) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return

    val shortcutManager = context.getSystemService(ShortcutManager::class.java) ?: return
    val label = "UV ${String.format(Locale.US, "%.1f", uvIndex)}"
    val intent = Intent(context, MainActivity::class.java).apply {
        action = Intent.ACTION_VIEW
        putExtra(MainActivity.EXTRA_OPEN_UV_INDEX, true)
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }

    val shortcut = ShortcutInfo.Builder(context, UV_SHORTCUT_ID)
        .setShortLabel(label)
        .setLongLabel("$label - $location")
        .setIcon(Icon.createWithBitmap(createUvShortcutIcon(uvIndex)))
        .setIntent(intent)
        .build()

    shortcutManager.dynamicShortcuts = listOf(shortcut)
}

private fun updateUvBadge(context: Context, uvIndex: Double, location: String) {
    createBadgeChannel(context)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED
    ) {
        return
    }

    val badgeNumber = uvIndex.roundToInt().coerceIn(0, 12)
    val intent = Intent(context, MainActivity::class.java).apply {
        putExtra(MainActivity.EXTRA_OPEN_UV_INDEX, true)
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
        context,
        20,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, UV_BADGE_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_sun_notification)
        .setContentTitle("UV ${String.format(Locale.US, "%.1f", uvIndex)}")
        .setContentText(location)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setCategory(NotificationCompat.CATEGORY_STATUS)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
        .setNumber(badgeNumber)
        .setSilent(true)
        .setLocalOnly(true)
        .setOngoing(false)
        .setContentIntent(pendingIntent)
        .build()

    NotificationManagerCompat.from(context).notify(UV_BADGE_NOTIFICATION_ID, notification)
}

private fun createBadgeChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

    val channel = NotificationChannel(
        UV_BADGE_CHANNEL_ID,
        "Current UV on app icon",
        NotificationManager.IMPORTANCE_LOW
    ).apply {
        description = "Shows the latest UV index as a launcher badge where supported"
        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        setShowBadge(true)
    }
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    manager.createNotificationChannel(channel)
}

private fun createUvShortcutIcon(uvIndex: Double): Bitmap {
    val size = 144
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val color = uvColor(uvIndex)
    val center = size / 2f

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = Color.rgb(10, 22, 44) }
    val sunPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = 42f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = Color.rgb(255, 214, 102)
        textAlign = Paint.Align.CENTER
        textSize = 24f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    canvas.drawRoundRect(0f, 0f, size.toFloat(), size.toFloat(), 32f, 32f, bgPaint)
    canvas.drawCircle(center, center - 10f, 46f, sunPaint)
    canvas.drawText(String.format(Locale.US, "%.1f", uvIndex), center, center + 5f, textPaint)
    canvas.drawText("UV", center, center + 50f, labelPaint)

    return bitmap
}

private fun uvColor(uvIndex: Double): Int = when {
    uvIndex < 3 -> Color.rgb(76, 175, 80)
    uvIndex < 6 -> Color.rgb(255, 193, 7)
    uvIndex < 8 -> Color.rgb(255, 152, 0)
    uvIndex < 11 -> Color.rgb(244, 67, 54)
    else -> Color.rgb(156, 39, 176)
}
