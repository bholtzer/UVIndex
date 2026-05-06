package com.bihstudio.uvindex.presentation.components

import android.app.Activity
import android.content.Context
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.viewinterop.AndroidView
import com.bihstudio.uvindex.presentation.theme.SunGold
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlin.math.cos
import kotlin.math.sin

// ─── Sun Animation ──────────────────────────────────────────────────────────

@Composable
fun SunAnimation(
    modifier: Modifier = Modifier,
    glowAlpha: Float = 0.8f,
    uvColor: Color = SunGold
) {
    val rotAnim = rememberInfiniteTransition(label = "rot")
    val rotation by rotAnim.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            tween(12000, easing = LinearEasing)
        ),
        label = "sunRot"
    )

    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = size.minDimension / 4f

        // Outer glow
        drawCircle(
            color = uvColor.copy(alpha = 0.08f * glowAlpha),
            radius = radius * 2.6f,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = uvColor.copy(alpha = 0.12f * glowAlpha),
            radius = radius * 2.0f,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = uvColor.copy(alpha = 0.2f * glowAlpha),
            radius = radius * 1.5f,
            center = Offset(cx, cy)
        )

        // Rays
        rotate(rotation, pivot = Offset(cx, cy)) {
            for (i in 0 until 12) {
                val angle = Math.toRadians((i * 30).toDouble())
                val startR = radius * 1.15f
                val endR = radius * 1.55f + if (i % 2 == 0) radius * 0.2f else 0f
                val sx = (cx + startR * cos(angle)).toFloat()
                val sy = (cy + startR * sin(angle)).toFloat()
                val ex = (cx + endR * cos(angle)).toFloat()
                val ey = (cy + endR * sin(angle)).toFloat()
                drawLine(
                    color = uvColor.copy(alpha = 0.6f),
                    start = Offset(sx, sy),
                    end = Offset(ex, ey),
                    strokeWidth = if (i % 2 == 0) 4f else 2.5f
                )
            }
        }

        // Core
        drawCircle(
            color = uvColor.copy(alpha = 0.3f),
            radius = radius * 1.1f,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = uvColor,
            radius = radius,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.15f),
            radius = radius * 0.5f,
            center = Offset(cx - radius * 0.2f, cy - radius * 0.2f)
        )
    }
}

// ─── AdMob Banner ───────────────────────────────────────────────────────────

// Replace with your real banner ID in production:
private const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"   // test

@Composable
fun AdBanner() {
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = BANNER_AD_UNIT_ID
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}

// ─── Interstitial Ad ─────────────────────────────────────────────────────────

// Replace with your real interstitial ID in production:
private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"   // test

@Composable
fun InterstitialAdManager(
    context: Context,
    onAdDismissed: () -> Unit
) {
    LaunchedEffect(Unit) {
        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() { onAdDismissed() }
                        override fun onAdFailedToShowFullScreenContent(e: AdError) { onAdDismissed() }
                    }
                    (context as? Activity)?.let { ad.show(it) } ?: onAdDismissed()
                }
                override fun onAdFailedToLoad(e: LoadAdError) { onAdDismissed() }
            }
        )
    }
}
