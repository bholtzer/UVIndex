package com.bihstudio.uvindex.presentation.screens.splash

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bihstudio.uvindex.R
import com.bihstudio.uvindex.analytics.AnalyticsManager
import com.bihstudio.uvindex.presentation.components.SunAnimation
import com.bihstudio.uvindex.presentation.theme.NightBlue
import com.bihstudio.uvindex.presentation.theme.NightMid
import com.bihstudio.uvindex.presentation.theme.SkyBlue
import com.bihstudio.uvindex.presentation.theme.SunGold
import com.bihstudio.uvindex.presentation.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onFinished: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val analytics = remember { AnalyticsManager() }
    LaunchedEffect(Unit) {
        analytics.logScreen(AnalyticsManager.Events.SCREEN_SPLASH)
        delay(2800)
        onFinished()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "splash")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "glow"
    )
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(NightBlue, NightMid, SkyBlue.copy(alpha = 0.6f))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            SunAnimation(
                modifier = Modifier.size(160.dp).scale(scale),
                glowAlpha = glowAlpha
            )

            Spacer(Modifier.height(32.dp))

            Text(
                text = stringResource(R.string.app_name),
                fontSize = 38.sp,
                fontWeight = FontWeight.Black,
                color = SunGold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Know before you go",
                fontSize = 16.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                letterSpacing = 2.sp
            )

            Spacer(Modifier.height(64.dp))

            Text(
                text = "BIH Studio",
                fontSize = 13.sp,
                color = SunGold.copy(alpha = 0.7f),
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 3.sp
            )
            Text(
                text = "© ${java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)}",
                fontSize = 11.sp,
                color = TextSecondary.copy(alpha = 0.5f)
            )
        }
    }
}
