package com.bihstudio.uvindex.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Palette ───────────────────────────────────────────────────────────────────
val SunGold      = Color(0xFFFFBE0B)
val SunOrange    = Color(0xFFFF6B35)
val SunDeep      = Color(0xFFFF4500)
val SkyBlue      = Color(0xFF1A3A5C)
val SkyLight     = Color(0xFF2D6A8F)
val NightBlue    = Color(0xFF0D1B2A)
val NightMid     = Color(0xFF1B2D40)
val GlassWhite   = Color(0x1AFFFFFF)
val TextPrimary  = Color(0xFFF5F5F5)
val TextSecondary = Color(0xFFB0BEC5)

// UV level colours
val UVLow        = Color(0xFF4CAF50)
val UVModerate   = Color(0xFFFFC107)
val UVHigh       = Color(0xFFFF9800)
val UVVeryHigh   = Color(0xFFF44336)
val UVExtreme    = Color(0xFF9C27B0)

private val DarkColorScheme = darkColorScheme(
    primary        = SunGold,
    onPrimary      = NightBlue,
    secondary      = SunOrange,
    onSecondary    = NightBlue,
    background     = NightBlue,
    onBackground   = TextPrimary,
    surface        = NightMid,
    onSurface      = TextPrimary,
    tertiary       = SkyLight,
    onTertiary     = TextPrimary
)

@Composable
fun UVIndexTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
