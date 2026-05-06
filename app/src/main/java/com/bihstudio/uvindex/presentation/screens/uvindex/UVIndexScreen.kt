package com.bihstudio.uvindex.presentation.screens.uvindex

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.toArgb
import androidx.hilt.navigation.compose.hiltViewModel
import com.bihstudio.uvindex.R
import com.bihstudio.uvindex.analytics.AnalyticsManager
import com.bihstudio.uvindex.domain.model.UVHourly
import com.bihstudio.uvindex.domain.model.UVIndexLevel
import com.bihstudio.uvindex.presentation.components.AdBanner
import com.bihstudio.uvindex.presentation.components.InterstitialAdManager
import com.bihstudio.uvindex.presentation.components.SunAnimation
import com.bihstudio.uvindex.presentation.theme.GlassWhite
import com.bihstudio.uvindex.presentation.theme.NightBlue
import com.bihstudio.uvindex.presentation.theme.NightMid
import com.bihstudio.uvindex.presentation.theme.SkyBlue
import com.bihstudio.uvindex.presentation.theme.SunGold
import com.bihstudio.uvindex.presentation.theme.TextPrimary
import com.bihstudio.uvindex.presentation.theme.TextSecondary
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@Composable
fun UVIndexScreen(
    onNavigateToLocation: () -> Unit,
    viewModel: UVIndexViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val analytics = remember { AnalyticsManager() }
    val state by viewModel.state.collectAsState()
    val isFirstLaunch by viewModel.isFirstLaunch.collectAsState()

    var showAd by remember { mutableStateOf(false) }
    var adShown by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { analytics.logScreen(AnalyticsManager.Events.SCREEN_UV_INDEX) }

    LaunchedEffect(state, isFirstLaunch) {
        if (state is UVState.Success && isFirstLaunch && !adShown) {
            showAd = true
        }
    }

    if (showAd) {
        InterstitialAdManager(
            context = context,
            onAdDismissed = {
                showAd = false
                adShown = true
                viewModel.markFirstLaunchDone()
                analytics.logEvent(AnalyticsManager.Events.AD_SHOWN)
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(NightBlue, NightMid, SkyBlue.copy(alpha = 0.4f)))
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            when (val s = state) {
                is UVState.Loading -> LoadingContent()
                is UVState.Error -> ErrorContent(s.message) { viewModel.loadUVData() }
                is UVState.Success -> SuccessContent(s, analytics, onNavigateToLocation)
            }
        }
    }
}

@Composable
private fun SuccessContent(
    state: UVState.Success,
    analytics: AnalyticsManager,
    onNavigateToLocation: () -> Unit
) {
    val uvData = state.data
    val level = state.level

    LaunchedEffect(uvData) {
        analytics.logUVDataLoaded(uvData.currentUV, uvData.latitude, uvData.longitude)
    }

    val uvColor = Color(level.color)
    val glowAnim = rememberInfiniteTransition(label = "glow")
    val glowAlpha by glowAnim.animateFloat(
        0.5f,
        1f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
        label = "glowAlpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.LocationOn, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(
            uvData.locationName,
            color = TextSecondary,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onNavigateToLocation) {
            Icon(Icons.Default.Search, null, tint = SunGold, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.look_another_location), color = SunGold, fontSize = 12.sp)
        }
    }

    Spacer(Modifier.height(24.dp))

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        SunAnimation(
            modifier = Modifier.size(220.dp),
            glowAlpha = glowAlpha,
            uvColor = uvColor
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = String.format("%.1f", uvData.currentUV),
                fontSize = 64.sp,
                fontWeight = FontWeight.Black,
                color = uvColor
            )
            Text(
                text = stringResource(level.labelRes()).uppercase(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = uvColor.copy(alpha = 0.8f),
                letterSpacing = 3.sp
            )
        }
    }

    Spacer(Modifier.height(16.dp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = uvColor.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = level.icon(), fontSize = 28.sp)
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(level.adviceRes()),
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }

    Spacer(Modifier.height(24.dp))

    UVScaleBar(currentUV = uvData.currentUV)

    Spacer(Modifier.height(24.dp))

    UVTimelineGraph(
        timeline = uvData.timelineForecast,
        currentUV = uvData.currentUV
    )

    Spacer(Modifier.height(24.dp))

    BestTimeCard(bestHour = uvData.hourlyForecast.maxByOrNull { it.uvIndex })

    Spacer(Modifier.height(24.dp))

    TwoDayForecast(hourlyForecast = uvData.hourlyForecast)

    Spacer(Modifier.height(24.dp))

    Text(
        text = stringResource(R.string.next_4_hours),
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextSecondary,
        modifier = Modifier.padding(horizontal = 24.dp)
    )
    Spacer(Modifier.height(12.dp))

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 24.dp)
    ) {
        items(uvData.hourlyForecast.take(4)) { hourly ->
            HourlyCard(hourly)
        }
    }

    Spacer(Modifier.height(24.dp))

    AdBanner()

    Spacer(Modifier.height(24.dp))
}

@Composable
private fun UVTimelineGraph(timeline: List<UVHourly>, currentUV: Double) {
    if (timeline.isEmpty()) return

    val zone = ZoneId.systemDefault()
    val title = stringResource(R.string.uv_curve_graph)
    val nowLabel = stringResource(R.string.now)
    var zoom by remember { mutableStateOf(1f) }
    val hourWidth = 42f * zoom
    val chartWidth = maxOf(360f, timeline.size * hourWidth).dp
    val scrollState = rememberScrollState()
    val now = remember { System.currentTimeMillis() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NightMid)
    ) {
        Column(Modifier.padding(vertical = 16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { zoom = (zoom / 1.25f).coerceAtLeast(0.65f) }) {
                    Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.zoom_out), tint = SunGold)
                }
                Text(
                    text = "${(zoom * 100).toInt()}%",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.width(44.dp),
                    textAlign = TextAlign.Center
                )
                IconButton(onClick = { zoom = (zoom * 1.25f).coerceAtMost(3f) }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.zoom_in), tint = SunGold)
                }
            }
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
            ) {
                Canvas(
                    modifier = Modifier
                        .width(chartWidth)
                        .height(260.dp)
                        .padding(horizontal = 12.dp)
                ) {
                    val left = 52f
                    val right = size.width - 16f
                    val top = 18f
                    val bottom = size.height - 54f
                    val graphHeight = bottom - top
                    val graphWidth = right - left
                    val maxUv = 12f
                    val step = if (timeline.size > 1) graphWidth / (timeline.size - 1) else graphWidth
                    val textPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        textSize = 22f
                        color = TextSecondary.toArgb()
                    }
                    val strongPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        textSize = 24f
                        color = TextPrimary.toArgb()
                        isFakeBoldText = true
                    }
                    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
                    val dayFormatter = DateTimeFormatter.ofPattern("MMM d")

                    fun yFor(uv: Double): Float {
                        return bottom - ((uv.coerceIn(0.0, maxUv.toDouble()).toFloat() / maxUv) * graphHeight)
                    }

                    listOf(0, 2, 4, 6, 8, 10, 12).forEach { value ->
                        val y = yFor(value.toDouble())
                        drawLine(
                            color = GlassWhite,
                            start = Offset(left, y),
                            end = Offset(right, y),
                            strokeWidth = 1.2f
                        )
                        drawIntoCanvas {
                            it.nativeCanvas.drawText(value.toString(), 14f, y + 7f, textPaint)
                        }
                    }

                    drawIntoCanvas {
                        it.nativeCanvas.drawText("UV", 10f, top - 2f, textPaint)
                    }

                    drawLine(
                        color = TextSecondary.copy(alpha = 0.7f),
                        start = Offset(left, top),
                        end = Offset(left, bottom),
                        strokeWidth = 2f
                    )
                    drawLine(
                        color = TextSecondary.copy(alpha = 0.7f),
                        start = Offset(left, bottom),
                        end = Offset(right, bottom),
                        strokeWidth = 2f
                    )

                    listOf(3.0, 6.0, 8.0, 11.0).forEach { threshold ->
                        val y = yFor(threshold)
                        drawLine(
                            color = Color(UVIndexLevel.fromIndex(threshold).color).copy(alpha = 0.45f),
                            start = Offset(left, y),
                            end = Offset(right, y),
                            strokeWidth = 2f
                        )
                    }

                    val path = Path()
                    timeline.forEachIndexed { idx, hourly ->
                        val x = left + idx * step
                        val y = yFor(hourly.uvIndex)
                        if (idx == 0) {
                            path.moveTo(x, y)
                        } else {
                            val prev = timeline[idx - 1]
                            val prevX = left + (idx - 1) * step
                            val prevY = yFor(prev.uvIndex)
                            val controlX = (prevX + x) / 2f
                            path.cubicTo(controlX, prevY, controlX, y, x, y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = SunGold,
                        style = Stroke(width = 4f, cap = StrokeCap.Round)
                    )

                    val currentIdx = timeline.indices.minByOrNull { idx ->
                        abs(timeline[idx].timestamp - now)
                    } ?: 0

                    timeline.forEachIndexed { idx, hourly ->
                        val x = left + idx * step
                        val y = yFor(hourly.uvIndex)
                        val pointColor = Color(UVIndexLevel.fromIndex(hourly.uvIndex).color)
                        drawCircle(pointColor, radius = if (idx == currentIdx) 7f else 4f, center = Offset(x, y))

                        if (idx % 6 == 0 || idx == currentIdx) {
                            val dateTime = Instant.ofEpochMilli(hourly.timestamp).atZone(zone)
                            val label = if (idx == currentIdx) nowLabel else dateTime.format(timeFormatter)
                            drawIntoCanvas {
                                it.nativeCanvas.drawText(label, x - 26f, bottom + 24f, textPaint)
                            }
                        }
                        if (idx % 24 == 0) {
                            val dateTime = Instant.ofEpochMilli(hourly.timestamp).atZone(zone)
                            drawIntoCanvas {
                                it.nativeCanvas.drawText(dateTime.format(dayFormatter), x - 30f, bottom + 48f, textPaint)
                            }
                        }
                    }

                    val currentX = left + currentIdx * step
                    drawLine(
                        color = TextPrimary.copy(alpha = 0.6f),
                        start = Offset(currentX, top),
                        end = Offset(currentX, bottom),
                        strokeWidth = 2f
                    )
                    drawIntoCanvas {
                        it.nativeCanvas.drawText(String.format("%.1f", currentUV), currentX + 8f, top + 24f, strongPaint)
                    }
                }
            }
        }
    }
}

@Composable
private fun UVScaleBar(currentUV: Double) {
    val segments = listOf(
        Pair(Color(0xFF4CAF50), stringResource(R.string.uv_low_short)),
        Pair(Color(0xFFFFC107), stringResource(R.string.uv_moderate_short)),
        Pair(Color(0xFFFF9800), stringResource(R.string.uv_high_short)),
        Pair(Color(0xFFF44336), stringResource(R.string.uv_very_high_short)),
        Pair(Color(0xFF9C27B0), stringResource(R.string.uv_extreme_short))
    )
    val activeIdx = when {
        currentUV < 3 -> 0
        currentUV < 6 -> 1
        currentUV < 8 -> 2
        currentUV < 11 -> 3
        else -> 4
    }

    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            segments.forEachIndexed { idx, (color, _) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(if (idx == activeIdx) 12.dp else 8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (idx == activeIdx) color else color.copy(alpha = 0.3f))
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            segments.forEachIndexed { idx, (color, label) ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    fontSize = 9.sp,
                    color = if (idx == activeIdx) color else TextSecondary,
                    textAlign = TextAlign.Center,
                    fontWeight = if (idx == activeIdx) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun HourlyCard(hourly: UVHourly) {
    val level = UVIndexLevel.fromIndex(hourly.uvIndex)
    val color = Color(level.color)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NightMid),
        modifier = Modifier.width(90.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(hourly.hour, fontSize = 13.sp, color = TextSecondary)
            Spacer(Modifier.height(8.dp))
            Text(
                text = String.format("%.1f", hourly.uvIndex),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color.copy(alpha = 0.5f))
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(level.labelRes()),
                fontSize = 10.sp,
                color = color,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun BestTimeCard(bestHour: UVHourly?) {
    if (bestHour == null) return

    val level = UVIndexLevel.fromIndex(bestHour.uvIndex)
    val color = Color(level.color)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NightMid)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.AccessTime, null, tint = color)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.recommended_time),
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    stringResource(R.string.best_time_value, bestHour.uvIndex, bestHour.hour),
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun TwoDayForecast(hourlyForecast: List<UVHourly>) {
    val zone = ZoneId.systemDefault()
    val formatter = remember { DateTimeFormatter.ofPattern("EEE, MMM d") }
    val today = remember { LocalDate.now(zone) }
    val dailyBest = hourlyForecast
        .groupBy { Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate() }
        .filterKeys { it.isAfter(today) }
        .toSortedMap()
        .entries
        .take(2)
        .mapNotNull { entry -> entry.value.maxByOrNull { it.uvIndex }?.let { entry.key to it } }

    if (dailyBest.isEmpty()) return

    Text(
        text = stringResource(R.string.next_two_days),
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextSecondary,
        modifier = Modifier.padding(horizontal = 24.dp)
    )
    Spacer(Modifier.height(12.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        dailyBest.forEach { (date, bestHour) ->
            val level = UVIndexLevel.fromIndex(bestHour.uvIndex)
            val color = Color(level.color)
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NightMid)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(date.format(formatter), color = TextSecondary, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        String.format("%.1f", bestHour.uvIndex),
                        color = color,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        stringResource(R.string.daily_best_at, bestHour.hour),
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(Modifier.fillMaxWidth().height(400.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = SunGold, modifier = Modifier.size(56.dp))
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.loading), color = TextSecondary)
        }
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(400.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Text("!", fontSize = 48.sp, color = SunGold)
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.could_not_load_uv_data), color = TextPrimary, fontWeight = FontWeight.Bold)
            Text(message, color = TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = SunGold)
            ) {
                Text(stringResource(R.string.retry), color = NightBlue)
            }
        }
    }
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

private fun UVIndexLevel.icon(): String = when {
    index < 3 -> ":)"
    index < 6 -> "SPF"
    index < 8 -> "UV"
    index < 11 -> "!"
    else -> "!!"
}
