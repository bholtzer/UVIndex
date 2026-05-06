package com.bihstudio.uvindex.presentation.screens.location

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bihstudio.uvindex.R
import com.bihstudio.uvindex.analytics.AnalyticsManager
import com.bihstudio.uvindex.domain.model.NearbyLocation
import com.bihstudio.uvindex.domain.model.UVIndexLevel
import com.bihstudio.uvindex.presentation.theme.GlassWhite
import com.bihstudio.uvindex.presentation.theme.NightBlue
import com.bihstudio.uvindex.presentation.theme.NightMid
import com.bihstudio.uvindex.presentation.theme.SkyBlue
import com.bihstudio.uvindex.presentation.theme.SunGold
import com.bihstudio.uvindex.presentation.theme.TextPrimary
import com.bihstudio.uvindex.presentation.theme.TextSecondary
import com.bihstudio.uvindex.presentation.theme.UVVeryHigh

@Composable
fun LocationSearchScreen(
    onBack: () -> Unit,
    viewModel: LocationSearchViewModel = hiltViewModel()
) {
    val analytics = remember { AnalyticsManager() }
    LaunchedEffect(Unit) { analytics.logScreen(AnalyticsManager.Events.SCREEN_LOCATION) }

    val state by viewModel.state.collectAsState()
    var query by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(NightBlue, NightMid)))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = SunGold)
                }
                Text(
                    stringResource(R.string.search_location),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.city_or_coordinates), color = TextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = SunGold) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = {
                            analytics.logEvent(AnalyticsManager.Events.CUSTOM_LOCATION)
                            viewModel.searchLocation(query)
                        }) {
                            Icon(Icons.AutoMirrored.Filled.Send, null, tint = SunGold)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SunGold,
                    unfocusedBorderColor = GlassWhite,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = SunGold
                ),
                singleLine = true
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    analytics.logNearbySearch(25.0)
                    viewModel.findNearbyGoodLocations()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SkyBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.NearMe, null, tint = SunGold)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.find_nearby), color = TextPrimary)
            }

            Spacer(Modifier.height(16.dp))

            when (val s = state) {
                is LocationSearchState.Loading -> {
                    Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) {
                        CircularProgressIndicator(color = SunGold)
                    }
                }

                is LocationSearchState.NearbyResults -> {
                    Text(
                        stringResource(R.string.best_spots),
                        color = SunGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        items(s.locations) { loc ->
                            NearbyLocationCard(loc)
                        }
                    }
                }

                is LocationSearchState.SearchResult -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = NightMid)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(s.name, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(stringResource(R.string.uv_value, s.uvIndex), color = SunGold, fontSize = 20.sp)
                            Text(stringResource(UVIndexLevel.fromIndex(s.uvIndex).adviceRes()), color = TextSecondary)
                        }
                    }
                }

                is LocationSearchState.Error -> {
                    Text(
                        s.message,
                        color = UVVeryHigh,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                else -> Unit
            }
        }
    }
}

@Composable
private fun NearbyLocationCard(loc: NearbyLocation) {
    val level = UVIndexLevel.fromIndex(loc.bestUVIndex)
    val color = Color(level.color)
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NightMid),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(loc.name, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(
                    stringResource(R.string.km_away_best_at, loc.distanceKm, loc.bestUVHour),
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    String.format("%.1f", loc.bestUVIndex),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(stringResource(level.labelRes()), fontSize = 10.sp, color = color)
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
