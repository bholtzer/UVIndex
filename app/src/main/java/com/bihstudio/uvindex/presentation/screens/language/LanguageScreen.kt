package com.bihstudio.uvindex.presentation.screens.language

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bihstudio.uvindex.R
import com.bihstudio.uvindex.analytics.AnalyticsManager
import com.bihstudio.uvindex.domain.model.AppLanguage
import com.bihstudio.uvindex.presentation.theme.GlassWhite
import com.bihstudio.uvindex.presentation.theme.NightBlue
import com.bihstudio.uvindex.presentation.theme.NightMid
import com.bihstudio.uvindex.presentation.theme.SunGold
import com.bihstudio.uvindex.presentation.theme.TextPrimary
import com.bihstudio.uvindex.presentation.theme.TextSecondary

@Composable
fun LanguageScreen(
    onLanguageSelected: () -> Unit,
    viewModel: LanguageViewModel = hiltViewModel()
) {
    val analytics = remember { AnalyticsManager() }
    LaunchedEffect(Unit) { analytics.logScreen(AnalyticsManager.Events.SCREEN_LANGUAGE) }

    val selectedLanguage by viewModel.selectedLanguage.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(NightBlue, NightMid)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))

            Text(
                text = "🌍",
                fontSize = 56.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.choose_language),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.language_subtitle),
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(Modifier.height(40.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(AppLanguage.values()) { lang ->
                    LanguageCard(
                        language = lang,
                        isSelected = selectedLanguage == lang,
                        onClick = { viewModel.selectLanguage(lang) }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    analytics.logLanguageSelected(selectedLanguage.code)
                    viewModel.saveLanguage()
                    onLanguageSelected()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SunGold),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.continue_btn),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = NightBlue
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LanguageCard(
    language: AppLanguage,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) SunGold else GlassWhite
    val bgColor = if (isSelected) SunGold.copy(alpha = 0.15f) else GlassWhite

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.6f)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = when (language) {
                    AppLanguage.ENGLISH -> "🇬🇧"
                    AppLanguage.HEBREW -> "🇮🇱"
                    AppLanguage.FRENCH -> "🇫🇷"
                    AppLanguage.SPANISH -> "🇪🇸"
                    AppLanguage.GERMAN -> "🇩🇪"
                    AppLanguage.ARABIC -> "🇸🇦"
                },
                fontSize = 28.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = language.displayName,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) SunGold else TextPrimary
            )
        }
    }
}
