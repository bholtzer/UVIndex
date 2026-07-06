package com.bihstudio.uvindex.service

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
import android.util.Log
import com.bihstudio.uvindex.presentation.MainActivity
import java.util.Locale

private const val TAG = "LauncherUvUpdater"
private const val UV_SHORTCUT_ID = "current_uv_shortcut"

// Track the alias we just enabled so we can clean up others when the app goes to background.
private var pendingUvAliasCleanup: ComponentName? = null

fun updateLauncherUvInfo(context: Context, uvIndex: Double, location: String) {
    useBrandedPrimaryLauncherIcon(context.applicationContext)
    updateUvShortcut(context, uvIndex, location)
}

private fun useBrandedPrimaryLauncherIcon(context: Context) {
    val packageManager = context.packageManager
    val selectedAlias = launcherDefaultAlias(context)

    try {
        Log.d(TAG, "Using branded launcher icon: $selectedAlias")

        // Only proceed if the desired alias is not already enabled to avoid redundant IPC calls
        // and potential crashes in Google Play Services (Phenotype).
        if (packageManager.getComponentEnabledSetting(selectedAlias) != PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
            packageManager.setComponentEnabledSetting(
                selectedAlias,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
        }
        pendingUvAliasCleanup = selectedAlias
    } catch (e: Exception) {
        Log.e(TAG, "Failed to enable alias: $selectedAlias. Ensure it exists in AndroidManifest.xml", e)
    }
}

/**
 * Disabling the alias that launched the current foreground task can make Android stop that task.
 * We call this when the app moves to the background to clean up stale icons safely.
 */
fun cleanupPendingLauncherAliases(context: Context) {
    val selectedAlias = pendingUvAliasCleanup ?: return
    val packageManager = context.packageManager
    
    val allAliases = mutableListOf<ComponentName>().apply {
        add(launcherDefaultAlias(context))
        (0..12).forEach { add(launcherAliasForUv(context, it)) }
    }

    allAliases.filterNot { it == selectedAlias }.forEach { alias ->
        try {
            // Check state before disabling to minimize changes
            if (packageManager.getComponentEnabledSetting(alias) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                packageManager.setComponentEnabledSetting(
                    alias,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            }
        } catch (e: Exception) {
            // Component might not exist in manifest; ignore to prevent crash
            Log.w(TAG, "Could not disable alias: $alias", e)
        }
    }
    pendingUvAliasCleanup = null
}

private fun launcherDefaultAlias(context: Context): ComponentName {
    // Construct name relative to the current package to handle applicationId changes correctly
    return ComponentName(context.packageName, "${context.packageName}.presentation.MainActivityDefault")
}

private fun launcherAliasForUv(context: Context, uv: Int): ComponentName {
    return ComponentName(context.packageName, "${context.packageName}.presentation.MainActivityUv$uv")
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

    try {
        shortcutManager.dynamicShortcuts = listOf(shortcut)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to update shortcut", e)
    }
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
